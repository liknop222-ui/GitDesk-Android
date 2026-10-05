package com.gitdesk.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 镜像前缀列表。第三方镜像的可用性会随时间变化，
 * 界面提供测速，请以实测结果为准。
 */
object Mirrors {

    val defaults: List<Mirror> = listOf(
        Mirror(
            name = "GitHub 官方直连",
            prefix = "https://github.com/",
            test = "https://github.com/octocat/Hello-World/raw/master/README",
            official = true,
            enabled = true
        ),
        Mirror(
            name = "ghfast.top",
            prefix = "https://ghfast.top/https://github.com/",
            test = "https://ghfast.top/https://github.com/octocat/Hello-World/raw/master/README"
        ),
        Mirror(
            name = "gh-proxy.com",
            prefix = "https://gh-proxy.com/https://github.com/",
            test = "https://gh-proxy.com/https://github.com/octocat/Hello-World/raw/master/README"
        ),
        Mirror(
            name = "ghproxy.net",
            prefix = "https://ghproxy.net/https://github.com/",
            test = "https://ghproxy.net/https://github.com/octocat/Hello-World/raw/master/README"
        ),
        Mirror(
            name = "hub.gitmirror.com",
            prefix = "https://hub.gitmirror.com/https://github.com/",
            test = "https://hub.gitmirror.com/https://github.com/octocat/Hello-World/raw/master/README"
        ),
        Mirror(
            name = "gh.llkk.cc",
            prefix = "https://gh.llkk.cc/https://github.com/",
            test = "https://gh.llkk.cc/https://github.com/octocat/Hello-World/raw/master/README"
        ),
        Mirror(
            name = "jsDelivr（raw 文件）",
            prefix = "https://cdn.jsdelivr.net/gh/",
            test = "https://cdn.jsdelivr.net/gh/octocat/Hello-World@master/README"
        ),
        Mirror(
            name = "raw.gitmirror.com（raw 文件）",
            prefix = "https://raw.gitmirror.com/",
            test = "https://raw.gitmirror.com/octocat/Hello-World/master/README"
        )
    )

    fun toJson(list: List<Mirror>): String {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(
                JSONObject()
                    .put("name", m.name)
                    .put("prefix", m.prefix)
                    .put("test", m.test)
                    .put("official", m.official)
                    .put("enabled", m.enabled)
            )
        }
        return arr.toString()
    }

    fun fromJson(text: String): List<Mirror> {
        if (text.isBlank()) return defaults
        return try {
            val arr = JSONArray(text)
            val out = ArrayList<Mirror>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    Mirror(
                        name = o.optString("name"),
                        prefix = o.optString("prefix"),
                        test = o.optString("test"),
                        official = o.optBoolean("official"),
                        enabled = o.optBoolean("enabled")
                    )
                )
            }
            if (out.isEmpty()) defaults else out
        } catch (e: Exception) {
            defaults
        }
    }

    /** 生成让 git 把 github.com 请求改写到镜像的命令。 */
    fun gitConfigCommands(list: List<Mirror>): String {
        val on = list.filter { it.enabled && !it.official }
        if (on.isEmpty()) {
            return "当前只启用了官方直连，没有需要改写的镜像。\n\n" +
                "若网络较慢，可启用列表中的镜像后回到这里复制命令。"
        }
        val sb = StringBuilder()
        sb.append("# 让 git 把 github.com 的请求改写到镜像\n\n")
        on.forEach {
            sb.append("git config --global url.\"").append(it.prefix)
                .append("\".insteadOf \"https://github.com/\"\n")
        }
        sb.append("\n# 恢复官方直连（撤销上面的改写）\n")
        on.forEach {
            sb.append("git config --global --unset-all url.\"").append(it.prefix)
                .append("\".insteadOf\n")
        }
        return sb.toString()
    }
}
