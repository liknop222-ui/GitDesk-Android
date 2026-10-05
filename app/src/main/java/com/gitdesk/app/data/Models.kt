package com.gitdesk.app.data

import android.net.Uri

data class GhUser(
    val login: String,
    val name: String?,
    val avatarUrl: String?,
    val publicRepos: Int
)

data class GhRepo(
    val owner: String,
    val name: String,
    val defaultBranch: String,
    val isPrivate: Boolean
) {
    val fullName: String get() = "$owner/$name"
}

/** 待推送的本地文件。来自 SAF 目录时只带 uri，来自 ZIP 时直接带字节。 */
class LocalFile(
    val path: String,
    val size: Long,
    val uri: Uri? = null,
    val bytes: ByteArray? = null
)

data class Artifact(
    val id: Long,
    val name: String,
    val sizeBytes: Long,
    val expired: Boolean
)

data class RunInfo(
    val id: Long,
    val number: Int,
    val status: String,
    val conclusion: String?,
    val htmlUrl: String,
    val createdAt: String
)

data class Mirror(
    val name: String,
    val prefix: String,
    val test: String,
    val official: Boolean = false,
    val enabled: Boolean = false
)
