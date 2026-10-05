package com.gitdesk.app.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gitdesk.app.AppViewModel
import com.gitdesk.app.data.BuildTargets
import com.gitdesk.app.data.LocalFiles
import com.gitdesk.app.data.ParamKind

@Composable
fun GdHeader(title: String, subtitle: String) {
    val c = Gd.c
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 14.dp)
    ) {
        Text(
            title,
            color = c.text1,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp
        )
        Spacer(Modifier.height(5.dp))
        Text(subtitle, color = c.text2, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun ScreenColumn(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        content()
        Spacer(Modifier.height(28.dp))
    }
}

// ------------------------------------------------------------------ 登录

@Composable
fun LoginScreen(vm: AppViewModel) {
    val c = Gd.c
    var mode by remember { mutableStateOf("token") }
    var value by remember { mutableStateOf("") }
    var apiBase by remember { mutableStateOf(vm.prefs.apiBase) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
    ) {
        Spacer(Modifier.height(56.dp))
        Text(
            "GitDesk",
            color = c.text1,
            fontSize = 40.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "把整个项目推上云端编译成安装包",
            color = c.text2,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(28.dp))

        GdCard(title = "登录方式", subtitle = "凭据只保存在这台设备上，退出登录即清除") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GdChip("访问令牌", mode == "token", { mode = "token" })
                GdChip("Cookie", mode == "cookie", { mode = "cookie" })
            }
            Spacer(Modifier.height(14.dp))
            if (mode == "token") {
                GdField(
                    label = "访问令牌 Personal Access Token",
                    value = value,
                    onValueChange = { value = it },
                    hint = "ghp_ 开头，或 github_pat_ 开头",
                    secret = true
                )
            } else {
                GdField(
                    label = "Cookie",
                    value = value,
                    onValueChange = { value = it },
                    hint = "从浏览器复制 github.com 的 Cookie",
                    secret = true,
                    singleLine = false,
                    lines = 3
                )
            }
            Spacer(Modifier.height(12.dp))
            GdField(
                label = "API 地址",
                value = apiBase,
                onValueChange = { apiBase = it },
                hint = "默认 https://api.github.com"
            )
            Spacer(Modifier.height(16.dp))
            GdButton(
                text = "登录",
                onClick = { vm.login(mode, value, apiBase) },
                loading = vm.busy
            )
        }

        Spacer(Modifier.height(14.dp))

        GdCard(title = "怎么拿到访问令牌", subtitle = "跟着做一遍就行，一分钟") {
            GdStep(1, "打开令牌页面", "浏览器访问 github.com/settings/tokens，点右上角 Generate new token（classic）")
            GdStep(2, "勾选权限", "至少勾上 repo 和 workflow 两项，其余不用管")
            GdStep(3, "生成并复制", "点最下面的 Generate token，复制那串以 ghp_ 开头的字符")
            GdStep(4, "粘贴回来", "切回这里选「访问令牌」，粘贴后点登录")
            Spacer(Modifier.height(6.dp))
            GdNote("建议用只勾选 repo、workflow 的细粒度令牌；用完随时可以在 GitHub 上吊销，不影响账号安全。")
        }

        Spacer(Modifier.height(14.dp))

        GdCard(title = "关于账号密钥", subtitle = "和桌面版的一点差别") {
            Text(
                "桌面版里的「设备码授权」需要先注册一个 OAuth 应用拿到 client_id，" +
                    "手机端这里没有预置应用，所以直接用访问令牌最省事——" +
                    "效果一样，而且随时可以吊销。",
                color = c.text2,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

// ------------------------------------------------------------------ 概览

@Composable
fun OverviewScreen(vm: AppViewModel, onGo: (Int) -> Unit) {
    val c = Gd.c
    val user = vm.user

    ScreenColumn {
        GdHeader("概览", "你好，${user?.login ?: "朋友"}")

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GdStat("待推送文件", "${vm.files.size}", Modifier.weight(1f))
            GdStat("当前目标", BuildTargets.byId(vm.targetId).name.substringBefore(" "), Modifier.weight(1f))
            GdStat("产物", "${vm.artifacts.size}", Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))

        GdCard(title = "四步走完整个流程", subtitle = "第一次用照着顺序点就行") {
            GdStep(1, "选项目文件夹", "在「推送」页点选文件夹或 ZIP，App 会自己认出是什么项目")
            GdStep(2, "一键推送到 GitHub", "整棵目录一次提交，不受网页端一次只能传一个文件的限制")
            GdStep(3, "选编译目标并触发", "在「编译」页挑要出的包，参数都填好了默认值")
            GdStep(4, "取回安装包", "编译完成后点产物即可下载，App 会直接调起安装")
        }

        Spacer(Modifier.height(14.dp))

        GdCard(title = "账号") {
            GdRow("登录用户", user?.login ?: "未登录")
            GdRow("昵称", user?.name ?: "—")
            GdRow("公开仓库", "${user?.publicRepos ?: 0}")
            GdRow("接口额度", vm.rateText.ifBlank { "—" })
            GdRow("默认仓库", if (vm.repoName.isBlank()) "尚未设置" else "${vm.repoOwner}/${vm.repoName}")
        }

        Spacer(Modifier.height(14.dp))

        GdCard(title = "快速开始") {
            GdButton("去推送项目", { onGo(1) })
            Spacer(Modifier.height(10.dp))
            GdButton("去云端编译", { onGo(2) }, filled = false)
        }
    }
}

// ------------------------------------------------------------------ 推送

@Composable
fun PushScreen(vm: AppViewModel) {
    val c = Gd.c
    val context = LocalContext.current

    val treePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // 部分来源不支持持久授权，不影响本次读取
            }
            vm.loadTree(uri)
        }
    }

    val zipPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vm.loadZip(uri)
    }

    ScreenColumn {
        GdHeader("推送", "整个项目一次提交，不丢目录结构")

        GdCard(title = "1 · 选择来源", subtitle = "推荐直接选文件夹；打包成 ZIP 也可以") {
            GdButton("选择项目文件夹", { treePicker.launch(null) }, loading = vm.busy)
            Spacer(Modifier.height(10.dp))
            GdButton("选择 ZIP 压缩包", { zipPicker.launch(arrayOf("*/*")) }, filled = false)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GdChip(
                    if (vm.filtering) "已开启自动过滤" else "不过滤任何文件",
                    vm.filtering,
                    { vm.filtering = !vm.filtering }
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (vm.filtering) {
                    "会自动跳过 .git、node_modules、build、.gradle 等目录"
                } else {
                    "所有文件都会推送，目录很大时会更慢"
                },
                color = c.text3,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(Modifier.height(14.dp))

        GdCard(title = "2 · 待推送内容") {
            if (vm.files.isEmpty()) {
                Text(
                    "还没有选择文件。选好文件夹后，这里会显示读到多少个文件。",
                    color = c.text2,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            } else {
                GdRow("文件数量", "${vm.files.size} 个")
                GdRow("总体积", vm.humanSize(vm.files.sumOf { it.size }))
                GdRow("识别结果", vm.projectKind)
                if (vm.lastCommit.isNotBlank()) {
                    GdRow("最近提交", vm.lastCommit, c.text1)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    vm.files.take(12).joinToString("\n") { "· " + it.path },
                    color = c.text3,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                if (vm.files.size > 12) {
                    Spacer(Modifier.height(4.dp))
                    Text("…… 另外 ${vm.files.size - 12} 个文件", color = c.text3, fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                GdButton("清空", { vm.clearFiles() }, filled = false)
            }
        }

        Spacer(Modifier.height(14.dp))

        GdCard(title = "3 · 推到哪个仓库", subtitle = "仓库不存在会自动创建") {
            GdField("仓库所有者", vm.repoOwner, { vm.repoOwner = it }, hint = "你的用户名或组织名")
            Spacer(Modifier.height(10.dp))
            GdField("仓库名", vm.repoName, { vm.repoName = it }, hint = "例如 my-android-app")
            Spacer(Modifier.height(10.dp))
            GdField("分支", vm.repoBranch, { vm.repoBranch = it }, hint = "main")
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GdChip(if (vm.repoPrivate) "私有仓库" else "公开仓库", vm.repoPrivate, {
                    vm.repoPrivate = !vm.repoPrivate
                })
            }
            Spacer(Modifier.height(10.dp))
            GdField("提交说明", vm.commitMessage, { vm.commitMessage = it })
        }

        Spacer(Modifier.height(14.dp))

        GdCard(title = "4 · 开始推送") {
            if (vm.pushProgress.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = c.text1,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(vm.pushProgress, color = c.text2, fontSize = 13.sp)
                }
                Spacer(Modifier.height(12.dp))
            }
            GdButton(
                "推送到 GitHub",
                { vm.push() },
                enabled = vm.files.isNotEmpty(),
                loading = vm.busy
            )
            Spacer(Modifier.height(10.dp))
            GdNote("整目录提交走的是 Git 数据接口，一次写入整棵文件树，几千个文件也是一次完成。")
        }
    }
}

// ------------------------------------------------------------------ 编译

@Composable
fun BuildScreen(vm: AppViewModel) {
    val c = Gd.c
    val target = BuildTargets.byId(vm.targetId)

    ScreenColumn {
        GdHeader("云端编译", "在手机上点一下，GitHub 的机器帮你打包")

        GdCard(title = "1 · 选择要出的包") {
            BuildTargets.all.forEach { t ->
                GdChip(
                    t.name,
                    t.id == vm.targetId,
                    { vm.selectTarget(t.id) },
                    modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(target.desc, color = c.text2, fontSize = 13.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(4.dp))
            Text(target.tag, color = c.text3, fontSize = 12.sp)
        }

        Spacer(Modifier.height(14.dp))

        GdCard(title = "2 · 编译参数", subtitle = "不确定的直接用默认值") {
            target.params.forEach { key ->
                val def = BuildTargets.paramDefs[key] ?: return@forEach
                if (def.kind == ParamKind.SELECT) {
                    GdSelect(
                        label = def.label,
                        value = vm.params[key] ?: def.def,
                        options = def.options,
                        onSelect = { vm.setParam(key, it) }
                    )
                } else {
                    GdField(
                        label = def.label,
                        value = vm.params[key] ?: "",
                        onValueChange = { vm.setParam(key, it) },
                        hint = def.hint
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            GdNote("会把这些参数写进仓库的 Actions 工作流，然后在云端触发一次编译。")
        }

        Spacer(Modifier.height(14.dp))

        GdCard(title = "3 · 触发编译") {
            GdRow("仓库", if (vm.repoName.isBlank()) "尚未设置" else "${vm.repoOwner}/${vm.repoName}")
            GdRow("目标", target.name)
            Spacer(Modifier.height(12.dp))
            GdButton("开始云端编译", { vm.startBuild() }, loading = vm.busy)
            Spacer(Modifier.height(10.dp))
            GdButton("刷新编译状态", { vm.refreshRun() }, filled = false)
        }

        Spacer(Modifier.height(14.dp))

        GdCard(title = "4 · 编译状态") {
            val run = vm.currentRun
            if (run == null) {
                Text(
                    "还没有编译记录。触发一次之后，这里会显示进度和产物。",
                    color = c.text2,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            } else {
                GdRow("运行编号", "#${run.number}")
                GdRow("状态", vm.statusText(run.status, run.conclusion))
                GdRow("触发时间", run.createdAt.replace("T", " ").removeSuffix("Z"))
            }
        }

        Spacer(Modifier.height(14.dp))

        GdCard(title = "5 · 产物下载") {
            if (vm.artifacts.isEmpty()) {
                Text(
                    "编译成功后，安装包会出现在这里。",
                    color = c.text2,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            } else {
                vm.artifacts.forEach { a ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(a.name, color = c.text1, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (a.expired) "已过期" else vm.humanSize(a.sizeBytes),
                                    color = c.text3,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Box(
                                Modifier
                                    .clip(FieldShape)
                                    .background(c.ink)
                                    .clickable(enabled = !a.expired && vm.downloadPercent < 0) { vm.download(a) }
                                    .padding(horizontal = 16.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    if (vm.downloadPercent >= 0) "${vm.downloadPercent}%"
                                    else if (a.expired) "已过期" else "下载",
                                    color = c.paper,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                GdNote("下载完成后会自动调起安装程序；若被系统拦下，安装包保存在 App 的 GitDesk 目录里。")
            }
        }

        Spacer(Modifier.height(14.dp))

        GdCard(title = "运行日志") {
            if (vm.logs.isEmpty()) {
                Text("暂无日志", color = c.text3, fontSize = 12.sp)
            } else {
                Text(
                    vm.logs.reversed().take(30).joinToString("\n"),
                    color = c.text2,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ------------------------------------------------------------------ 加速源

@Composable
fun MirrorScreen(vm: AppViewModel) {
    val c = Gd.c
    var newName by remember { mutableStateOf("") }
    var newPrefix by remember { mutableStateOf("") }
    var newTest by remember { mutableStateOf("") }

    ScreenContainer(vm) {
        GdHeader("加速源", "网络慢的时候，换个前缀再拉代码会快很多")

        GdCard(title = "镜像前缀", subtitle = "点一下开启或关闭；第三方镜像的可用性会变，请以实测为准") {
            vm.mirrors.forEach { m ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (m.enabled) c.ink else c.paper)
                            .clickable { vm.toggleMirror(m) }
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            m.name,
                            color = c.text1,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            m.prefix,
                            color = c.text3,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                        val s = vm.speed[m.name]
                        if (s != null) {
                            Spacer(Modifier.height(2.dp))
                            Text("测速：$s", color = c.text2, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(PillShape)
                            .background(c.card2)
                            .clickable { vm.testMirror(m) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text("测速", color = c.text1, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        GdCard(title = "让 git 走镜像", subtitle = "复制到电脑的命令行执行，之后 git clone 会自动走镜像") {
            Text(
                vm.gitConfigCommands(),
                color = c.text1,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FieldShape)
                    .background(c.card2)
                    .padding(12.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        GdCard(title = "添加自定义镜像") {
            GdField("名称", newName, { newName = it }, hint = "随便起个好记的名字")
            Spacer(Modifier.height(10.dp))
            GdField(
                "前缀",
                newPrefix,
                { newPrefix = it },
                hint = "https://你的镜像/https://github.com/"
            )
            Spacer(Modifier.height(10.dp))
            GdField("测速地址", newTest, { newTest = it }, hint = "留空则用前缀本身")
            Spacer(Modifier.height(14.dp))
            GdButton("添加", {
                vm.addMirror(newName, newPrefix, newTest)
                newName = ""
                newPrefix = ""
                newTest = ""
            })
        }

        Spacer(Modifier.height(14.dp))

        GdCard(title = "云端编译时的依赖加速") {
            Text(
                "编译参数里的「依赖下载加速」打开后，云端机器会用国内镜像拉 Maven、" +
                    "npm、pip 依赖，编译会明显快一些。",
                color = c.text2,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

// ------------------------------------------------------------------ 设置

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val c = Gd.c
    var apiBase by remember { mutableStateOf(vm.prefs.apiBase) }

    ScreenContainer(vm) {
        GdHeader("设置", "外观、接口地址与账号")

        GdCard(title = "外观", subtitle = "默认跟随系统，深色浅色自动切换") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GdChip("跟随系统", vm.theme == "system", { vm.changeTheme("system") })
                GdChip("浅色", vm.theme == "light", { vm.changeTheme("light") })
                GdChip("深色", vm.theme == "dark", { vm.changeTheme("dark") })
            }
        }

        Spacer(Modifier.height(14.dp))

        GdCard(title = "接口地址", subtitle = "企业版或自建网关时改这里") {
            GdField("API 地址", apiBase, { apiBase = it })
            Spacer(Modifier.height(12.dp))
            GdButton("保存", {
                vm.prefs.apiBase = apiBase.trim()
                vm.say("已保存")
            })
        }

        Spacer(Modifier.height(14.dp))

        GdCard(title = "账号") {
            GdRow("登录用户", vm.user?.login ?: "未登录")
            GdRow("凭据类型", when (vm.prefs.authMode) {
                "cookie" -> "Cookie"
                "token" -> "访问令牌"
                else -> "无"
            })
            Spacer(Modifier.height(12.dp))
            GdButton("退出登录", { vm.logout() }, danger = true)
            Spacer(Modifier.height(10.dp))
            GdNote("退出会清除本机保存的令牌与 Cookie，不会影响 GitHub 上的任何数据。")
        }

        Spacer(Modifier.height(14.dp))

        GdCard(title = "关于") {
            GdRow("版本", "1.0.0")
            GdRow("编译目标", "${BuildTargets.all.size} 种")
            Spacer(Modifier.height(8.dp))
            Text(
                "GitDesk 把整个项目推送到 GitHub，再借 GitHub Actions 的机器" +
                    "打包成 APK 或 EXE，省去本地配置环境这一步。",
                color = c.text2,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun ScreenContainer(vm: AppViewModel, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        content()
        Spacer(Modifier.height(28.dp))
    }
}
