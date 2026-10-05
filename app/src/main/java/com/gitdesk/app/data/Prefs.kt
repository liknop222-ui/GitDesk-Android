package com.gitdesk.app.data

import android.content.Context
import android.content.SharedPreferences

/** 本机配置与凭据。凭据只保存在设备本地，退出登录即清除。 */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("gitdesk", Context.MODE_PRIVATE)

    var theme: String
        get() = sp.getString("theme", "system") ?: "system"
        set(v) = sp.edit().putString("theme", v).apply()

    var apiBase: String
        get() = sp.getString("apiBase", DEFAULT_API) ?: DEFAULT_API
        set(v) = sp.edit().putString("apiBase", v).apply()

    var token: String
        get() = sp.getString("token", "") ?: ""
        set(v) = sp.edit().putString("token", v).apply()

    var cookie: String
        get() = sp.getString("cookie", "") ?: ""
        set(v) = sp.edit().putString("cookie", v).apply()

    var authMode: String
        get() = sp.getString("authMode", "none") ?: "none"
        set(v) = sp.edit().putString("authMode", v).apply()

    var login: String
        get() = sp.getString("login", "") ?: ""
        set(v) = sp.edit().putString("login", v).apply()

    var repoOwner: String
        get() = sp.getString("repoOwner", "") ?: ""
        set(v) = sp.edit().putString("repoOwner", v).apply()

    var repoName: String
        get() = sp.getString("repoName", "") ?: ""
        set(v) = sp.edit().putString("repoName", v).apply()

    var repoBranch: String
        get() = sp.getString("repoBranch", "main") ?: "main"
        set(v) = sp.edit().putString("repoBranch", v).apply()

    var repoPrivate: Boolean
        get() = sp.getBoolean("repoPrivate", false)
        set(v) = sp.edit().putBoolean("repoPrivate", v).apply()

    var target: String
        get() = sp.getString("target", "android-apk") ?: "android-apk"
        set(v) = sp.edit().putString("target", v).apply()

    var paramsJson: String
        get() = sp.getString("paramsJson", "{}") ?: "{}"
        set(v) = sp.edit().putString("paramsJson", v).apply()

    var mirrorsJson: String
        get() = sp.getString("mirrorsJson", "") ?: ""
        set(v) = sp.edit().putString("mirrorsJson", v).apply()

    var lastRunId: Long
        get() = sp.getLong("lastRunId", 0L)
        set(v) = sp.edit().putLong("lastRunId", v).apply()

    var lastRunOwner: String
        get() = sp.getString("lastRunOwner", "") ?: ""
        set(v) = sp.edit().putString("lastRunOwner", v).apply()

    var lastRunRepo: String
        get() = sp.getString("lastRunRepo", "") ?: ""
        set(v) = sp.edit().putString("lastRunRepo", v).apply()

    var lastRunConclusion: String
        get() = sp.getString("lastRunConclusion", "") ?: ""
        set(v) = sp.edit().putString("lastRunConclusion", v).apply()

    fun clearAuth() {
        sp.edit()
            .remove("token")
            .remove("cookie")
            .remove("authMode")
            .remove("login")
            .apply()
    }

    companion object {
        const val DEFAULT_API = "https://api.github.com"
    }
}
