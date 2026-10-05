# GitDesk · 安卓客户端

把整个项目推上 GitHub，借 GitHub Actions 的机器打包成 APK / EXE。
手机上点几下就能出安装包，不用在本机装 Android SDK、JDK、Python 工具链。

原生 Android 应用，Kotlin + Jetpack Compose 编写。

## 界面

- 苹果式极简黑白配色，只有黑、白、灰三阶，不引入无关彩色
- 默认跟随系统深浅色，也可以在设置里锁定浅色或深色
- 底部五页签：概览 / 推送 / 编译 / 加速 / 设置

## 功能

### 登录

| 方式 | 说明 |
| --- | --- |
| 访问令牌 | 推荐。支持 classic（`ghp_`）与细粒度（`github_pat_`）令牌 |
| Cookie | 从浏览器复制 github.com 的 Cookie |
| 自定义 API 地址 | 指向 GitHub Enterprise 或自建网关 |

凭据只存在设备本地的 SharedPreferences，退出登录即清除。

> 桌面版里的「设备码授权」需要预先注册一个 OAuth 应用拿到 `client_id`，
> 手机端没有预置应用，因此改用访问令牌，效果一致且可随时吊销。

### 整目录推送

核心能力。网页端一次只能上传一个文件，这里走 Git 数据接口：

```
git/blobs  →  git/trees  →  git/commits  →  git/refs
```

一次提交写入整棵文件树，几千个文件也是一次完成，目录结构完整保留。

- 来源：系统文件选择器选文件夹（SAF），或直接选 ZIP 压缩包
- ZIP 解析用 JDK 自带的 `java.util.zip`，无第三方依赖
- 文本文件（≤512 KB 且可判定为文本）走 inline content，其余走 base64 blob，二进制不会损坏
- 自动跳过 `.git`、`node_modules`、`build`、`.gradle`、`__pycache__` 等目录
- 单文件上限 100 MB，总量上限 250 MB

### 云端编译

| 目标 | 运行环境 | 产物 |
| --- | --- | --- |
| Android APK | ubuntu-latest | `.apk` |
| Android AAB | ubuntu-latest | `.aab` |
| Flutter APK | ubuntu-latest | `.apk` |
| Windows EXE · .NET | windows-latest | `.exe` |
| Windows EXE · Python | windows-latest | `.exe` |
| Windows EXE · Electron | windows-latest | 安装包 / 免安装包 |
| Windows EXE · C/C++ | windows-latest | `.exe` |

选好项目类型后，App 会把对应的 Actions 工作流写进仓库并派发一次运行，
然后轮询状态、列出产物。点下载即可取回，解出 `.apk` 后会直接调起系统安装程序。

工作流模板存放在 `app/src/main/assets/workflows/`，可以直接改。

### 多源加速

预置 8 条镜像前缀，可增删改、可单条测速，并能一键生成 `git config` 改写命令：

```bash
git config --global url."https://ghfast.top/https://github.com/".insteadOf "https://github.com/"
```

云端编译时也能开启依赖镜像（Maven / npm / pip）。

> 第三方镜像的可用性会随时间变化，请以 App 内的实测结果为准。

## 构建

本仓库自带 Actions 工作流，推到 `main` 分支即自动出包，
也可以在 Actions 页面手动触发（可选 debug / release）。

本地构建：

```bash
# 需要 JDK 17 与 Android SDK（compileSdk 34）
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 工程结构

```
app/src/main/java/com/gitdesk/app/
├── MainActivity.kt          入口、底部导航
├── AppViewModel.kt          全部状态与业务动作
├── data/
│   ├── GitHubApi.kt         REST + Git Data API 封装
│   ├── LocalFiles.kt        目录/ZIP 采集、项目类型识别
│   ├── BuildTargets.kt      编译目标与参数定义
│   ├── Mirrors.kt           镜像列表与 git 命令生成
│   ├── Prefs.kt             本机配置与凭据
│   └── Models.kt            数据模型
└── ui/
    ├── Theme.kt             单色阶设计令牌
    ├── Components.kt        通用组件
    └── Screens.kt           五个页面
```

## 技术要点

- `minSdk 24` / `targetSdk 34` / `compileSdk 34`，Kotlin 1.9.24，AGP 8.5.2
- Compose BOM 2024.06.00，Material 3
- 网络层 OkHttp 4.12，JSON 用 Android 自带的 `org.json`
- 无 Hilt / Room / Retrofit / 导航框架等重型依赖，便于阅读和二次开发
- 动态颜色（Material You）已关闭，保证严格单色
