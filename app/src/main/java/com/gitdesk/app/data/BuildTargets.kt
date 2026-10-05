package com.gitdesk.app.data

enum class ParamKind { TEXT, SELECT }

data class ParamDef(
    val key: String,
    val label: String,
    val kind: ParamKind = ParamKind.TEXT,
    val def: String = "",
    val hint: String = "",
    val options: List<String> = emptyList()
)

data class BuildTarget(
    val id: String,
    val name: String,
    val tag: String,
    val desc: String,
    val wf: String,
    val params: List<String>,
    val fixed: Map<String, String> = emptyMap()
)

object BuildTargets {

    val paramDefs: Map<String, ParamDef> = listOf(
        ParamDef("project_dir", "项目根目录", def = ".", hint = "含 gradlew 的目录；就在仓库根目录时填 ."),
        ParamDef("gradle_task", "Gradle 任务", ParamKind.SELECT, "assembleDebug",
            options = listOf("assembleDebug", "assembleRelease", "bundleRelease")),
        ParamDef("app_name", "应用名称", hint = "留空表示不修改项目原有配置"),
        ParamDef("package_name", "包名 applicationId"),
        ParamDef("version_name", "版本号 versionName"),
        ParamDef("version_code", "版本码 versionCode"),
        ParamDef("use_mirror", "依赖下载加速", ParamKind.SELECT, "yes",
            hint = "在云端编译时启用国内镜像", options = listOf("yes", "no")),
        ParamDef("build_mode", "构建模式", ParamKind.SELECT, "release",
            options = listOf("release", "debug")),
        ParamDef("project_path", "解决方案 / 项目文件", hint = "留空则自动查找 .sln 或 .csproj"),
        ParamDef("configuration", "编译配置", ParamKind.SELECT, "Release",
            options = listOf("Release", "Debug")),
        ParamDef("runtime", "目标运行时", ParamKind.SELECT, "win-x64",
            options = listOf("win-x64", "win-x86", "win-arm64")),
        ParamDef("self_contained", "自包含运行时", ParamKind.SELECT, "yes",
            hint = "选 yes 时目标电脑无需预装 .NET", options = listOf("yes", "no")),
        ParamDef("single_file", "打包为单文件", ParamKind.SELECT, "yes",
            options = listOf("yes", "no")),
        ParamDef("entry", "入口脚本", hint = "留空则自动选择第一个 .py 文件"),
        ParamDef("windowed", "窗口程序（不显示控制台）", ParamKind.SELECT, "yes",
            options = listOf("yes", "no")),
        ParamDef("cmake_args", "CMake 附加参数", hint = "例如 -DBUILD_TESTING=OFF")
    ).associateBy { it.key }

    val all: List<BuildTarget> = listOf(
        BuildTarget(
            id = "android-apk",
            name = "Android APK",
            tag = "Gradle · ubuntu-latest",
            desc = "标准 Android 工程打包安装包，支持改应用名、包名、版本号",
            wf = "gitdesk-android.yml",
            params = listOf(
                "project_dir", "gradle_task", "app_name",
                "package_name", "version_name", "version_code", "use_mirror"
            )
        ),
        BuildTarget(
            id = "android-aab",
            name = "Android AAB",
            tag = "Gradle · bundleRelease",
            desc = "上架应用商店用的 App Bundle",
            wf = "gitdesk-android.yml",
            params = listOf(
                "project_dir", "app_name", "package_name",
                "version_name", "version_code", "use_mirror"
            ),
            fixed = mapOf("gradle_task" to "bundleRelease")
        ),
        BuildTarget(
            id = "flutter-apk",
            name = "Flutter APK",
            tag = "flutter build apk",
            desc = "Flutter 工程打包 Android 安装包",
            wf = "gitdesk-flutter.yml",
            params = listOf("project_dir", "build_mode", "use_mirror")
        ),
        BuildTarget(
            id = "win-dotnet",
            name = "Windows EXE · .NET",
            tag = "dotnet publish",
            desc = "C# / .NET 桌面程序发布为 exe",
            wf = "gitdesk-windows-dotnet.yml",
            params = listOf(
                "project_path", "configuration", "runtime",
                "self_contained", "single_file"
            )
        ),
        BuildTarget(
            id = "win-python",
            name = "Windows EXE · Python",
            tag = "PyInstaller",
            desc = "Python 脚本用 PyInstaller 打包为 exe",
            wf = "gitdesk-windows-python.yml",
            params = listOf("project_dir", "entry", "app_name", "windowed", "use_mirror")
        ),
        BuildTarget(
            id = "win-electron",
            name = "Windows EXE · Electron",
            tag = "electron-builder",
            desc = "Electron 桌面应用打包为安装包与免安装包",
            wf = "gitdesk-windows-electron.yml",
            params = listOf("project_dir", "use_mirror")
        ),
        BuildTarget(
            id = "win-cpp",
            name = "Windows EXE · C/C++",
            tag = "CMake + MSVC",
            desc = "C/C++ 工程用 CMake 编译为 exe",
            wf = "gitdesk-windows-cpp.yml",
            params = listOf("project_dir", "cmake_args")
        )
    )

    fun byId(id: String): BuildTarget = all.firstOrNull { it.id == id } ?: all[0]

    fun defaultsFor(target: BuildTarget): Map<String, String> {
        val m = LinkedHashMap<String, String>()
        target.params.forEach { key ->
            paramDefs[key]?.let { m[key] = it.def }
        }
        return m
    }

    /** 组装派发时提交的 inputs，必须与工作流里声明的输入名完全一致。 */
    fun inputsFor(target: BuildTarget, params: Map<String, String>): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        target.params.forEach { key ->
            out[key] = params[key] ?: paramDefs[key]?.def ?: ""
        }
        target.fixed.forEach { (k, v) -> out[k] = v }
        if (target.id == "android-apk" || target.id == "android-aab") {
            val task = out["gradle_task"] ?: ""
            out["build_type"] =
                if (target.id == "android-aab" || task.contains("Release", true)) "release"
                else "debug"
        }
        return out
    }
}
