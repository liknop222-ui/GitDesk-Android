package com.gitdesk.app

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gitdesk.app.data.Artifact
import com.gitdesk.app.data.BuildTargets
import com.gitdesk.app.data.GhUser
import com.gitdesk.app.data.GitHubApi
import com.gitdesk.app.data.LocalFile
import com.gitdesk.app.data.LocalFiles
import com.gitdesk.app.data.Mirror
import com.gitdesk.app.data.Mirrors
import com.gitdesk.app.data.Prefs
import com.gitdesk.app.data.RunInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = Prefs(app)
    val api = GitHubApi(prefs, app.contentResolver)

    // ---- 全局 ----
    var theme by mutableStateOf(prefs.theme)
    var user by mutableStateOf<GhUser?>(null)
    var busy by mutableStateOf(false)
    var toast by mutableStateOf("")
    var rateText by mutableStateOf("")

    // ---- 推送 ----
    var files by mutableStateOf<List<LocalFile>>(emptyList())
    var projectKind by mutableStateOf("")
    var filtering by mutableStateOf(true)
    var repoOwner by mutableStateOf(prefs.repoOwner)
    var repoName by mutableStateOf(prefs.repoName)
    var repoBranch by mutableStateOf(prefs.repoBranch)
    var repoPrivate by mutableStateOf(prefs.repoPrivate)
    var commitMessage by mutableStateOf("通过 GitDesk 推送")
    var pushProgress by mutableStateOf("")
    var lastCommit by mutableStateOf("")

    // ---- 编译 ----
    var targetId by mutableStateOf(prefs.target)
    var params by mutableStateOf<Map<String, String>>(emptyMap())
    var currentRun by mutableStateOf<RunInfo?>(null)
    var artifacts by mutableStateOf<List<Artifact>>(emptyList())
    var logs by mutableStateOf<List<String>>(emptyList())
    var downloadPercent by mutableStateOf(-1)

    // ---- 加速源 ----
    var mirrors by mutableStateOf(Mirrors.fromJson(prefs.mirrorsJson))
    var speed by mutableStateOf<Map<String, String>>(emptyMap())

    init {
        val saved = try {
            JSONObject(prefs.paramsJson)
        } catch (e: Exception) {
            JSONObject()
        }
        val t = BuildTargets.byId(prefs.target)
        val m = LinkedHashMap<String, String>()
        BuildTargets.defaultsFor(t).forEach { (k, v) ->
            m[k] = if (saved.has(k)) saved.optString(k) else v
        }
        params = m
    }

    // ------------------------------------------------------------ 工具

    fun say(msg: String) {
        toast = msg
    }

    fun addLog(line: String) {
        val stamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        logs = (logs + "[$stamp] $line").takeLast(120)
    }

    private fun workflowYaml(name: String): String = try {
        getApplication<Application>().assets.open("workflows/$name")
            .bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        ""
    }

    fun statusText(status: String, conclusion: String?): String = when {
        status == "queued" -> "排队中"
        status == "in_progress" -> "编译中"
        status == "completed" && conclusion == "success" -> "编译成功"
        status == "completed" && conclusion == "failure" -> "编译失败"
        status == "completed" && conclusion == "cancelled" -> "已取消"
        status == "completed" -> "已结束（${conclusion ?: "未知"}）"
        else -> status
    }

    fun humanSize(bytes: Long): String = when {
        bytes >= 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
        bytes >= 1024L -> "${bytes / 1024} KB"
        else -> "$bytes B"
    }

    // ------------------------------------------------------------ 账号

    fun boot() {
        if (prefs.token.isBlank() && prefs.cookie.isBlank()) return
        viewModelScope.launch {
            busy = true
            try {
                val u = api.me()
                user = u
                prefs.login = u.login
                if (repoOwner.isBlank()) {
                    repoOwner = u.login
                    prefs.repoOwner = u.login
                }
                rateText = try {
                    api.rateLimit()
                } catch (e: Exception) {
                    ""
                }
                addLog("已登录 ${u.login}")
            } catch (e: Exception) {
                toast = e.message ?: "登录状态已失效"
            } finally {
                busy = false
            }
        }
    }

    fun login(mode: String, value: String, apiBaseIn: String) {
        if (value.isBlank()) {
            toast = "请先填写凭据"
            return
        }
        viewModelScope.launch {
            busy = true
            try {
                if (apiBaseIn.isNotBlank()) prefs.apiBase = apiBaseIn.trim()
                if (mode == "cookie") {
                    prefs.cookie = value.trim()
                    prefs.authMode = "cookie"
                } else {
                    prefs.token = value.trim()
                    prefs.authMode = "token"
                }
                val u = api.me()
                user = u
                prefs.login = u.login
                repoOwner = u.login
                prefs.repoOwner = u.login
                rateText = try {
                    api.rateLimit()
                } catch (e: Exception) {
                    ""
                }
                toast = "登录成功：${u.login}"
                addLog("登录成功：${u.login}")
            } catch (e: Exception) {
                prefs.clearAuth()
                toast = e.message ?: "登录失败"
            } finally {
                busy = false
            }
        }
    }

    fun logout() {
        prefs.clearAuth()
        user = null
        files = emptyList()
        artifacts = emptyList()
        currentRun = null
        logs = emptyList()
        rateText = ""
        toast = "已退出登录，本机凭据已清除"
    }

    fun changeTheme(t: String) {
        theme = t
        prefs.theme = t
    }

    // ------------------------------------------------------------ 选文件

    fun loadTree(uri: Uri) {
        viewModelScope.launch {
            busy = true
            pushProgress = "正在读取目录…"
            try {
                val list = withContext(Dispatchers.IO) {
                    LocalFiles.stripCommonRoot(
                        LocalFiles.fromTree(getApplication(), uri, filtering)
                    )
                }
                applyPicked(list, "目录")
            } catch (e: Exception) {
                toast = e.message ?: "读取目录失败"
            } finally {
                pushProgress = ""
                busy = false
            }
        }
    }

    fun loadZip(uri: Uri) {
        viewModelScope.launch {
            busy = true
            pushProgress = "正在解压…"
            try {
                val list = withContext(Dispatchers.IO) {
                    LocalFiles.stripCommonRoot(LocalFiles.fromZip(getApplication(), uri))
                }
                applyPicked(list, "压缩包")
            } catch (e: Exception) {
                toast = e.message ?: "读取压缩包失败"
            } finally {
                pushProgress = ""
                busy = false
            }
        }
    }

    private fun applyPicked(list: List<LocalFile>, source: String) {
        files = list
        if (list.isEmpty()) {
            toast = "没有读到文件，换个位置试试"
            return
        }
        val resolver = getApplication<Application>().contentResolver
        val (id, label) = LocalFiles.detect(list.map { it.path }) { p ->
            val f = list.firstOrNull { it.path == p } ?: return@detect null
            try {
                f.bytes?.toString(Charsets.UTF_8)
                    ?: f.uri?.let { u ->
                        resolver.openInputStream(u)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    }
            } catch (e: Exception) {
                null
            }
        }
        projectKind = label
        selectTarget(id)
        addLog("从${source}读取 ${list.size} 个文件 · $label")
        toast = "读到 ${list.size} 个文件 · $label"
    }

    fun clearFiles() {
        files = emptyList()
        projectKind = ""
        toast = "已清空文件列表"
    }

    // ------------------------------------------------------------ 推送

    fun push() {
        if (files.isEmpty()) {
            toast = "先选一个文件夹或 ZIP 压缩包"
            return
        }
        viewModelScope.launch {
            busy = true
            try {
                val owner = repoOwner.ifBlank { prefs.login }
                if (owner.isBlank()) {
                    toast = "请先登录"
                    return@launch
                }
                if (repoName.isBlank()) {
                    toast = "请填写仓库名"
                    return@launch
                }
                pushProgress = "正在准备仓库…"
                val r = api.ensureRepo(owner, repoName, repoPrivate)
                repoOwner = r.owner
                repoName = r.name
                repoPrivate = r.isPrivate
                prefs.repoOwner = r.owner
                prefs.repoName = r.name
                prefs.repoPrivate = r.isPrivate
                val branch = r.defaultBranch.ifBlank { "main" }
                repoBranch = branch
                prefs.repoBranch = branch

                pushProgress = "正在提交 0/${files.size}…"
                val sha = api.commitFiles(
                    r.owner, r.name, branch, files,
                    commitMessage.ifBlank { "通过 GitDesk 推送" }
                ) { done, total, path ->
                    pushProgress = "正在提交 $done/$total · ${path.takeLast(36)}"
                }

                lastCommit = sha.take(7)
                addLog("推送成功 ${r.fullName}@$branch · ${sha.take(7)} · ${files.size} 个文件")
                toast = "推送成功：${r.fullName}@$branch"
            } catch (e: Exception) {
                addLog("推送失败：${e.message}")
                toast = e.message ?: "推送失败"
            } finally {
                pushProgress = ""
                busy = false
            }
        }
    }

    // ------------------------------------------------------------ 编译

    fun selectTarget(id: String) {
        if (id == targetId && params.isNotEmpty()) return
        targetId = id
        prefs.target = id
        params = BuildTargets.defaultsFor(BuildTargets.byId(id))
    }

    fun setParam(key: String, value: String) {
        params = params + (key to value)
        val o = JSONObject()
        params.forEach { (k, v) -> o.put(k, v) }
        prefs.paramsJson = o.toString()
    }

    fun startBuild() {
        viewModelScope.launch {
            busy = true
            try {
                val t = BuildTargets.byId(targetId)
                val owner = repoOwner.ifBlank { prefs.login }
                if (owner.isBlank() || repoName.isBlank()) {
                    toast = "请先填写仓库，或先推送一次"
                    return@launch
                }
                addLog("准备编译 ${t.name}")

                val r = api.ensureRepo(owner, repoName, repoPrivate)
                repoOwner = r.owner
                repoName = r.name
                val branch = r.defaultBranch.ifBlank { "main" }
                repoBranch = branch

                val yaml = workflowYaml(t.wf)
                if (yaml.isBlank()) {
                    toast = "缺少工作流模板：${t.wf}"
                    return@launch
                }
                val wfPath = ".github/workflows/${t.wf}"
                val bytes = yaml.toByteArray(Charsets.UTF_8)
                addLog("写入 $wfPath")
                api.commitFiles(
                    r.owner, r.name, branch,
                    listOf(LocalFile(wfPath, bytes.size.toLong(), null, bytes)),
                    "GitDesk：配置云端编译"
                ) { _, _, _ -> }

                delay(2000)
                val inputs = BuildTargets.inputsFor(t, params)
                addLog("派发 ${t.wf}（${inputs.size} 个参数）")
                api.dispatch(r.owner, r.name, t.wf, branch, inputs)

                prefs.lastRunOwner = r.owner
                prefs.lastRunRepo = r.name
                prefs.lastRunId = 0
                currentRun = null
                artifacts = emptyList()

                delay(5000)
                refreshRun()
            } catch (e: Exception) {
                addLog("编译触发失败：${e.message}")
                toast = e.message ?: "编译触发失败"
            } finally {
                busy = false
            }
        }
    }

    fun refreshRun() {
        viewModelScope.launch {
            try {
                val owner = prefs.lastRunOwner.ifBlank { repoOwner }
                val repo = prefs.lastRunRepo.ifBlank { repoName }
                if (owner.isBlank() || repo.isBlank()) {
                    toast = "还没有编译记录"
                    return@launch
                }
                val t = BuildTargets.byId(targetId)
                var r: RunInfo? = prefs.lastRunId
                    .takeIf { it > 0 }
                    ?.let { api.getRun(owner, repo, it) }
                if (r == null) {
                    r = api.latestRun(owner, repo, t.wf, repoBranch)
                }
                if (r == null) {
                    toast = "还没找到编译记录，稍等一下再刷新"
                    return@launch
                }
                currentRun = r
                prefs.lastRunId = r.id
                prefs.lastRunConclusion = r.conclusion ?: ""
                addLog("运行 #${r.number} · ${statusText(r.status, r.conclusion)}")
                if (r.status == "completed") {
                    artifacts = api.artifacts(owner, repo, r.id)
                    if (artifacts.isEmpty()) {
                        addLog("这次没有产物")
                    } else {
                        addLog("找到 ${artifacts.size} 个产物")
                    }
                }
            } catch (e: Exception) {
                toast = e.message ?: "查询失败"
            }
        }
    }

    fun download(artifact: Artifact) {
        viewModelScope.launch {
            val owner = prefs.lastRunOwner.ifBlank { repoOwner }
            val repo = prefs.lastRunRepo.ifBlank { repoName }
            if (owner.isBlank() || repo.isBlank()) {
                toast = "还没有编译记录"
                return@launch
            }
            busy = true
            downloadPercent = 0
            try {
                val app = getApplication<Application>()
                val dir = File(app.getExternalFilesDir(null), "GitDesk")
                val zip = File(dir, artifact.name + ".zip")
                api.downloadArtifact(owner, repo, artifact.id, zip) { p ->
                    downloadPercent = p
                }
                downloadPercent = -1
                addLog("产物已下载 ${humanSize(zip.length())}")

                val apks = withContext(Dispatchers.IO) { unzipApks(zip, dir) }
                if (apks.isEmpty()) {
                    toast = "产物已保存到 ${zip.absolutePath}"
                } else {
                    addLog("已解出安装包 ${apks[0].name}")
                    installApk(apks[0])
                }
            } catch (e: Exception) {
                downloadPercent = -1
                addLog("下载失败：${e.message}")
                toast = e.message ?: "下载失败"
            } finally {
                busy = false
            }
        }
    }

    private fun unzipApks(zip: File, dir: File): List<File> {
        val out = ArrayList<File>()
        try {
            ZipInputStream(zip.inputStream().buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory && name.endsWith(".apk", ignoreCase = true)) {
                        val f = File(dir, File(name).name)
                        f.outputStream().use { o -> zis.copyTo(o) }
                        out.add(f)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            // 解压失败时保留原始 zip 即可
        }
        return out
    }

    private fun installApk(f: File) {
        val app = getApplication<Application>()
        try {
            val uri = FileProvider.getUriForFile(app, app.packageName + ".fileprovider", f)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(intent)
            toast = "已调起安装程序"
        } catch (e: Exception) {
            toast = "安装包已保存：${f.absolutePath}"
        }
    }

    // ------------------------------------------------------------ 加速源

    private fun persistMirrors() {
        prefs.mirrorsJson = Mirrors.toJson(mirrors)
    }

    fun toggleMirror(m: Mirror) {
        mirrors = mirrors.map {
            if (it.name == m.name) Mirror(it.name, it.prefix, it.test, it.official, !it.enabled)
            else it
        }
        persistMirrors()
    }

    fun addMirror(name: String, prefix: String, test: String) {
        if (prefix.isBlank()) {
            toast = "前缀不能为空"
            return
        }
        val m = Mirror(
            name = name.ifBlank { prefix },
            prefix = prefix,
            test = test.ifBlank { prefix }
        )
        mirrors = mirrors + m
        persistMirrors()
        toast = "已添加镜像"
    }

    fun removeMirror(m: Mirror) {
        mirrors = mirrors.filter { it.name != m.name }
        persistMirrors()
        toast = "已删除"
    }

    fun testMirror(m: Mirror) {
        viewModelScope.launch {
            speed = speed + (m.name to "测速中…")
            val client = OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            val result = withContext(Dispatchers.IO) {
                try {
                    val t0 = System.currentTimeMillis()
                    client.newCall(
                        Request.Builder()
                            .url(m.test)
                            .header("User-Agent", "GitDesk-Android")
                            .build()
                    ).execute().use { res ->
                        res.body?.bytes()
                        val ms = System.currentTimeMillis() - t0
                        if (res.isSuccessful) "$ms ms" else "HTTP ${res.code}"
                    }
                } catch (e: Exception) {
                    "不可用"
                }
            }
            speed = speed + (m.name to result)
        }
    }

    fun gitConfigCommands(): String = Mirrors.gitConfigCommands(mirrors)
}
