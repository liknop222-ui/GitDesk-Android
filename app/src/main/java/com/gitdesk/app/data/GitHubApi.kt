package com.gitdesk.app.data

import android.content.ContentResolver
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class ApiException(val code: Int, message: String) : Exception(message)

/**
 * GitHub REST + Git Data API 封装。
 *
 * 整目录推送走 git/blobs → git/trees → git/commits → git/refs 四步，
 * 一次提交写入整棵文件树，不受「一次只能上传一个文件」的限制。
 */
class GitHubApi(
    private val prefs: Prefs,
    private val resolver: ContentResolver
) {

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    /** 产物下载需要手动处理 302，不能自动跟随（跨域会丢掉鉴权头）。 */
    private val noRedirectClient: OkHttpClient =
        client.newBuilder().followRedirects(false).build()

    // ---------------------------------------------------------------- 基础

    private fun urlOf(path: String): String =
        if (path.startsWith("http")) path else prefs.apiBase.trimEnd('/') + path

    private fun builder(path: String): Request.Builder {
        val b = Request.Builder()
            .url(urlOf(path))
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "GitDesk-Android")
        val t = prefs.token
        if (t.isNotBlank()) b.header("Authorization", "Bearer $t")
        if (prefs.authMode == "cookie" && prefs.cookie.isNotBlank()) {
            b.header("Cookie", prefs.cookie)
        }
        return b
    }

    private fun friendly(code: Int, text: String): String {
        val msg = try {
            JSONObject(text).optString("message")
        } catch (e: Exception) {
            ""
        }
        return when {
            code == 401 -> "凭据无效或已过期（401）"
            code == 403 && msg.contains("rate limit", true) -> "接口调用次数已达上限，请稍后再试"
            code == 404 -> "资源不存在或无权限访问（404）"
            msg.isNotBlank() -> msg
            else -> "HTTP $code"
        }
    }

    private suspend fun call(
        path: String,
        method: String = "GET",
        json: JSONObject? = null
    ): String = withContext(Dispatchers.IO) {
        val body: RequestBody? = json?.toString()?.toRequestBody(jsonType)
        val req = builder(path).method(method, body).build()
        client.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw ApiException(res.code, friendly(res.code, text))
            text
        }
    }

    // ---------------------------------------------------------------- 账号

    suspend fun me(): GhUser {
        val o = JSONObject(call("/user"))
        return GhUser(
            login = o.optString("login"),
            name = o.optString("name").ifBlank { null },
            avatarUrl = o.optString("avatar_url").ifBlank { null },
            publicRepos = o.optInt("public_repos")
        )
    }

    suspend fun rateLimit(): String {
        val o = JSONObject(call("/rate_limit"))
        val core = o.optJSONObject("resources")?.optJSONObject("core")
        return if (core != null) {
            "剩余 ${core.optInt("remaining")}/${core.optInt("limit")} 次调用"
        } else {
            "连通正常"
        }
    }

    // ---------------------------------------------------------------- 仓库

    suspend fun getRepo(owner: String, name: String): GhRepo {
        val o = JSONObject(call("/repos/$owner/$name"))
        return GhRepo(
            owner = o.getJSONObject("owner").optString("login"),
            name = o.optString("name"),
            defaultBranch = o.optString("default_branch").ifBlank { "main" },
            isPrivate = o.optBoolean("private")
        )
    }

    suspend fun createRepo(owner: String, name: String, isPrivate: Boolean): GhRepo {
        val meLogin = me().login
        val path = if (owner == meLogin) "/user/repos" else "/orgs/$owner/repos"
        val body = JSONObject()
            .put("name", name)
            .put("private", isPrivate)
            .put("auto_init", true)
            .put("description", "由 GitDesk 创建")
        val o = JSONObject(call(path, "POST", body))
        return GhRepo(
            owner = o.getJSONObject("owner").optString("login"),
            name = o.optString("name"),
            defaultBranch = o.optString("default_branch").ifBlank { "main" },
            isPrivate = o.optBoolean("private")
        )
    }

    suspend fun ensureRepo(owner: String, name: String, isPrivate: Boolean): GhRepo =
        try {
            getRepo(owner, name)
        } catch (e: ApiException) {
            if (e.code != 404) throw e
            createRepo(owner, name, isPrivate)
        }

    // ---------------------------------------------------------------- 推送

    private fun readBytes(f: LocalFile): ByteArray {
        f.bytes?.let { return it }
        val u = f.uri ?: return ByteArray(0)
        return try {
            resolver.openInputStream(u)?.use { it.readBytes() } ?: ByteArray(0)
        } catch (e: Exception) {
            ByteArray(0)
        }
    }

    private fun looksTextual(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return true
        val n = minOf(bytes.size, 8192)
        var suspicious = 0
        for (i in 0 until n) {
            val b = bytes[i].toInt() and 0xFF
            if (b == 0) return false
            if (b < 7 || (b > 13 && b < 32)) suspicious++
        }
        return suspicious.toDouble() / n <= 0.02
    }

    suspend fun commitFiles(
        owner: String,
        repo: String,
        branch: String,
        files: List<LocalFile>,
        message: String,
        onProgress: (done: Int, total: Int, path: String) -> Unit
    ): String {
        val base = "/repos/$owner/$repo"

        var refSha: String? = try {
            JSONObject(call("$base/git/ref/heads/$branch"))
                .getJSONObject("object").getString("sha")
        } catch (e: ApiException) {
            null
        }

        var baseTree: String? = null
        val parents = JSONArray()
        if (refSha != null) {
            val c = JSONObject(call("$base/git/commits/$refSha"))
            baseTree = c.getJSONObject("tree").getString("sha")
            parents.put(refSha)
        }

        val entries = JSONArray()
        var inlineBudget = 4L * 1024 * 1024
        var done = 0

        for (f in files) {
            val bytes = readBytes(f)
            val asText = looksTextual(bytes) && bytes.size <= 512 * 1024
            val entry = JSONObject()
                .put("path", f.path)
                .put("mode", "100644")
                .put("type", "blob")

            if (asText && bytes.size <= inlineBudget) {
                inlineBudget -= bytes.size
                entry.put("content", String(bytes, Charsets.UTF_8))
            } else {
                val blobBody = JSONObject()
                    .put("content", Base64.encodeToString(bytes, Base64.NO_WRAP))
                    .put("encoding", "base64")
                val blob = JSONObject(call("$base/git/blobs", "POST", blobBody))
                entry.put("sha", blob.getString("sha"))
            }

            entries.put(entry)
            done++
            onProgress(done, files.size, f.path)
        }

        val treeBody = JSONObject().put("tree", entries)
        if (baseTree != null) treeBody.put("base_tree", baseTree)
        val tree = JSONObject(call("$base/git/trees", "POST", treeBody))

        val commitBody = JSONObject()
            .put("message", message)
            .put("tree", tree.getString("sha"))
            .put("parents", parents)
        val commit = JSONObject(call("$base/git/commits", "POST", commitBody))
        val sha = commit.getString("sha")

        if (refSha != null) {
            call(
                "$base/git/refs/heads/$branch",
                "PATCH",
                JSONObject().put("sha", sha).put("force", false)
            )
        } else {
            call(
                "$base/git/refs",
                "POST",
                JSONObject().put("ref", "refs/heads/$branch").put("sha", sha)
            )
        }
        return sha
    }

    // ---------------------------------------------------------------- 编译

    suspend fun dispatch(
        owner: String,
        repo: String,
        workflowFile: String,
        ref: String,
        inputs: Map<String, String>
    ) {
        val inputsJson = JSONObject()
        inputs.forEach { (k, v) -> inputsJson.put(k, v) }
        val body = JSONObject().put("ref", ref).put("inputs", inputsJson)
        call("/repos/$owner/$repo/actions/workflows/$workflowFile/dispatches", "POST", body)
    }

    private fun toRun(o: JSONObject) = RunInfo(
        id = o.optLong("id"),
        number = o.optInt("run_number"),
        status = o.optString("status"),
        conclusion = o.optString("conclusion").ifBlank { null },
        htmlUrl = o.optString("html_url"),
        createdAt = o.optString("created_at")
    )

    suspend fun latestRun(
        owner: String,
        repo: String,
        workflowFile: String,
        branch: String
    ): RunInfo? {
        val text = call(
            "/repos/$owner/$repo/actions/workflows/$workflowFile/runs" +
                "?per_page=10&branch=$branch"
        )
        val arr = JSONObject(text).optJSONArray("workflow_runs") ?: return null
        if (arr.length() == 0) return null
        return toRun(arr.getJSONObject(0))
    }

    suspend fun getRun(owner: String, repo: String, runId: Long): RunInfo =
        toRun(JSONObject(call("/repos/$owner/$repo/actions/runs/$runId")))

    suspend fun artifacts(owner: String, repo: String, runId: Long): List<Artifact> {
        val arr = JSONObject(call("/repos/$owner/$repo/actions/runs/$runId/artifacts"))
            .optJSONArray("artifacts") ?: return emptyList()
        val out = ArrayList<Artifact>(arr.length())
        for (i in 0 until arr.length()) {
            val a = arr.getJSONObject(i)
            out.add(
                Artifact(
                    id = a.getLong("id"),
                    name = a.optString("name"),
                    sizeBytes = a.optLong("size_in_bytes"),
                    expired = a.optBoolean("expired")
                )
            )
        }
        return out
    }

    suspend fun downloadArtifact(
        owner: String,
        repo: String,
        artifactId: Long,
        dest: File,
        onProgress: (Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val url = urlOf("/repos/$owner/$repo/actions/artifacts/$artifactId/zip")
        var response: Response = noRedirectClient.newCall(builder(url).build()).execute()

        if (response.code in 300..399) {
            val loc = response.header("Location")
            val code = response.code
            response.close()
            if (loc.isNullOrBlank()) throw ApiException(code, "下载重定向缺少地址")
            response = noRedirectClient.newCall(
                Request.Builder().url(loc).header("User-Agent", "GitDesk-Android").build()
            ).execute()
        }

        response.use { res ->
            if (!res.isSuccessful) throw ApiException(res.code, "下载失败：HTTP ${res.code}")
            val body = res.body ?: throw ApiException(res.code, "响应内容为空")
            val total = body.contentLength()
            dest.parentFile?.mkdirs()
            dest.outputStream().use { out ->
                body.byteStream().use { input ->
                    val buf = ByteArray(64 * 1024)
                    var sum = 0L
                    while (true) {
                        val read = input.read(buf)
                        if (read == -1) break
                        out.write(buf, 0, read)
                        sum += read
                        onProgress(if (total > 0) ((sum * 100) / total).toInt() else 0)
                    }
                }
            }
        }
        dest
    }
}
