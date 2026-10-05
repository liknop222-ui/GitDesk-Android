package com.gitdesk.app.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedInputStream
import java.util.zip.ZipInputStream

/** 从设备存储采集待推送的文件：支持整目录（SAF）与 ZIP 压缩包。 */
object LocalFiles {

    private const val MAX_FILE = 100L * 1024 * 1024
    private const val MAX_TOTAL = 250L * 1024 * 1024

    private val ALWAYS_IGNORE_FILES = setOf(".DS_Store", "Thumbs.db", "desktop.ini")
    private val IGNORE_DIRS = setOf(
        ".git", "node_modules", "build", ".gradle", ".idea", ".dart_tool",
        "__pycache__", ".venv", "venv", "env", "obj", "bin", ".vs",
        ".next", "target", "dist", ".pytest_cache", ".mypy_cache", "coverage"
    )

    fun shouldIgnoreFile(name: String): Boolean = ALWAYS_IGNORE_FILES.contains(name)

    fun shouldIgnoreDir(name: String, filtering: Boolean): Boolean {
        if (name == ".git") return true
        return filtering && IGNORE_DIRS.contains(name)
    }

    fun fromTree(context: Context, uri: Uri, filtering: Boolean): List<LocalFile> {
        val root = DocumentFile.fromTreeUri(context, uri) ?: return emptyList()
        val out = ArrayList<LocalFile>()
        walk(root, "", filtering, out, LongArray(1))
        return out
    }

    private fun walk(
        dir: DocumentFile,
        prefix: String,
        filtering: Boolean,
        out: MutableList<LocalFile>,
        total: LongArray
    ) {
        val children: Array<DocumentFile> = try {
            dir.listFiles()
        } catch (e: Exception) {
            emptyArray<DocumentFile>()
        }
        for (child in children) {
            val name = child.name ?: continue
            val path = prefix + name
            if (child.isDirectory) {
                if (shouldIgnoreDir(name, filtering)) continue
                walk(child, "$path/", filtering, out, total)
            } else {
                if (shouldIgnoreFile(name)) continue
                val size = child.length()
                if (size > MAX_FILE) continue
                if (total[0] + size > MAX_TOTAL) continue
                total[0] += size
                out.add(LocalFile(path, size, child.uri, null))
            }
        }
    }

    fun fromZip(context: Context, uri: Uri): List<LocalFile> {
        val out = ArrayList<LocalFile>()
        var total = 0L
        context.contentResolver.openInputStream(uri)?.use { raw ->
            ZipInputStream(BufferedInputStream(raw)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory && name.isNotBlank()) {
                        val base = name.substringAfterLast('/')
                        if (!shouldIgnoreFile(base)) {
                            val bytes = zis.readBytes()
                            if (bytes.size <= MAX_FILE && total + bytes.size <= MAX_TOTAL) {
                                total += bytes.size
                                out.add(LocalFile(name, bytes.size.toLong(), null, bytes))
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
        return out
    }

    /** 压缩包里常见一层顶层目录，推送时把它去掉，避免项目多套一层。 */
    fun stripCommonRoot(files: List<LocalFile>): List<LocalFile> {
        if (files.isEmpty()) return files
        if (files.any { !it.path.contains('/') }) return files
        val root = files.first().path.substringBefore('/')
        if (root.isBlank()) return files
        if (files.all { it.path.startsWith("$root/") }) {
            return files.map {
                LocalFile(it.path.removePrefix("$root/"), it.size, it.uri, it.bytes)
            }
        }
        return files
    }

    /** 按文件清单粗略判断项目类型，用于预选编译目标。 */
    fun detect(paths: List<String>, readText: (String) -> String?): Pair<String, String> {
        fun hasExact(p: String) = paths.any { it == p || it.endsWith("/$p") }
        fun hasExt(e: String) = paths.any { it.lowercase().endsWith(e) }

        if (hasExact("pubspec.yaml")) return "flutter-apk" to "Flutter 工程"
        if (hasExact("gradlew") || hasExact("build.gradle") || hasExact("build.gradle.kts") ||
            hasExact("settings.gradle") || hasExact("settings.gradle.kts")
        ) {
            return "android-apk" to "Android（Gradle）工程"
        }
        if (hasExt(".sln") || hasExt(".csproj")) return "win-dotnet" to ".NET 工程"
        if (hasExact("CMakeLists.txt")) return "win-cpp" to "C/C++（CMake）工程"
        if (hasExact("package.json")) {
            val pkg = readText("package.json") ?: ""
            return "win-electron" to
                if (pkg.contains("\"electron\"")) "Electron 桌面应用" else "Node / Electron 工程"
        }
        if (hasExact("requirements.txt") || hasExt(".py")) return "win-python" to "Python 工程"
        return "android-apk" to "未识别，默认按 Android 处理"
    }
}
