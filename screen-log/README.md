# Screen Log 接入指南

`screen-log` 是一个独立的 App 内屏幕日志组件。业务代码主动写入日志，开发或测试人员可直接在应用界面中查看、搜索、筛选和复制，不依赖 `logcat`，也不需要系统悬浮窗权限。

详细设计、数据流和评审项见 [ARCHITECTURE.md](./ARCHITECTURE.md)。真机自测发现的待优化项见 [SELF_TEST_TODO.md](./SELF_TEST_TODO.md)。

## 适用范围

- 开发、测试环境中的业务链路排查。
- Release 包中受白名单或隐藏入口控制的现场诊断。
- 需要在设备端查看业务日志，但无法方便连接 `adb` 的场景。

当前版本不会抓取系统日志、网络包或第三方 SDK 日志，只展示业务通过 `ScreenLog` 主动写入的内容。

## 环境要求

| 项目 | 当前要求 |
| --- | --- |
| Android 最低版本 | API 24 |
| 编译 SDK | API 34，可接入 `compileSdk 34 / targetSdk 33` 的 App |
| Java 兼容级别 | Java 11 |
| Kotlin 编译器 | 2.0 或更高；AAR 元数据按 2.0 生成 |
| UI 技术 | Kotlin + View/XML，不依赖 Compose |
| 直接依赖 | AndroidX Core KTX、ConstraintLayout、RecyclerView |
| 权限 | 无新增权限 |

## 构建并导出 AAR

模块提供 `exportReleaseAar` 任务，会先构建 Release AAR，再按版本号复制到项目根目录的 `artifacts/`：

```powershell
.\gradlew.bat :screen-log:exportReleaseAar
```

默认版本来自根目录 `gradle.properties` 中的 `screenLogVersion=1.0.4_SNAPSHOT`。每次交付包含新改动的 AAR 都必须递增版本号，不得沿用旧版本号覆盖交付；同步更新构建脚本的默认版本和本文接入示例。在确认版本上线前，版本号统一保留 `_SNAPSHOT` 后缀，产物路径为：

```text
artifacts/screen-log-1.0.4_SNAPSHOT.aar
```

确认上线后，可以直接通过命令行传入正式版本号，不需要修改构建脚本：

```powershell
.\gradlew.bat :screen-log:exportReleaseAar -PscreenLogVersion=1.0.4
```

`artifacts/` 已加入 `.gitignore`，生成的二进制文件不会被误提交。

## 在其他项目中接入 AAR

将生成的 AAR 复制到目标项目的 `app/libs/`，然后在目标 App 模块的 `build.gradle.kts` 中声明：

```kotlin
dependencies {
    implementation(files("libs/screen-log-1.0.4_SNAPSHOT.aar"))

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
```

普通 AAR 文件不包含 Maven POM，无法自动向目标项目传递依赖。如果目标项目已经声明了兼容版本的上述 AndroidX 依赖，不需要重复添加。本 AAR 使用 `compileSdk 34` 构建，可接入 `compileSdk 34 / targetSdk 33`、`minSdk 24` 或更高版本的项目，并需要开启 AndroidX：

```properties
android.useAndroidX=true
```

目标项目如果没有应用 Kotlin Android 插件，还需要显式加入与本模块兼容的 Kotlin 标准库：

```kotlin
implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
```

设置页 Activity 会通过 AAR Manifest 自动合并，无需在目标项目中重复注册。AAR 内置 `consumer-rules.pro`，目标项目开启 R8/ProGuard 时会自动保留公开接入接口和设置页。完成依赖声明后，按照下文初始化和调用即可。

## 5 分钟接入

### 1. 引入模块

在工程的 `settings.gradle.kts` 中加入：

```kotlin
include(":screen-log")
```

在业务 App 模块中加入依赖：

```kotlin
dependencies {
    implementation(project(":screen-log"))
}
```

如果接入方使用发布后的 AAR/Maven 产物，将上面的本地工程依赖替换为对应坐标即可，初始化和使用方式不变。

### 2. 初始化

在 `Application.onCreate()` 中初始化一次。接入 App 自行控制测试入口的可见性和使用资格：

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        ScreenLogKit.init(
            application = this,
            hiddenRequestInterfaces = setOf("/api/large-data", "/api/frequent-poll")
        )
    }
}
```

全新安装首次初始化时，“记录日志”默认关闭，也不显示日志入口。测试人员首次调用 `open(activity)` 后会持久化录制状态；下次冷启动时，`ScreenLogKit.init` 会恢复录制，因此可记录初始化之后的启动日志。文件持久化独立控制且默认关闭。

### 3. 测试入口的开启和关闭

在测试入口的按钮或暗码验证成功回调中调用：

```kotlin
ScreenLogKit.open(this)  // 开启录制并显示悬浮入口
ScreenLogKit.close()     // 停止录制并隐藏悬浮入口和面板
```

`open` 不重置已保存的接口开关、文件保存或其他设置。`close` 不清空已有日志。重启后如果录制状态为开启，组件会同时恢复悬浮入口；日志面板由测试人员从悬浮入口打开。

### 4. 写入业务日志

```kotlin
ScreenLog.d("WebView", "load url = $url", extras = mapOf("url" to url))
ScreenLog.i("JSBridge", "call method = $methodName")
ScreenLog.w("Network", "request retry", extras = mapOf("retry" to retryCount.toString()))
ScreenLog.i("Network", responseJson, prettyJson = true)
ScreenLog.request(api = "/api/orders", prettyJson = true) {
    gson.fromJson(responseBody, LogPayload::class.java).toString()
}
ScreenLog.request(
    api = "/api/orders",
    phase = ScreenLogRequestPhase.RESPONSE,
    prettyJson = true
) {
    responseBody
}
ScreenLog.e(
    tag = "AccountDelete",
    message = "delete account failed",
    throwable = throwable,
    extras = mapOf("code" to code)
)
```

建议 Tag 使用稳定的业务域名称，例如 `Login`、`Network`、`WebView`、`JSBridge`、`Page`，不要把动态 ID 拼进 Tag。

`prettyJson=true` 时，JSON Object 或 JSON Array 类型的 message 会以两空格缩进展示和复制；非法 JSON 保持原文。普通日志方法的 `extras` 位于最后一个参数，建议使用具名参数传入。

请求日志使用 `ScreenLog.request(api, ..., messageProvider)`，必须传入稳定的 API 标识。组件仅在总开关、录制开关、日志级别和该接口开关均允许时调用 `provide()`，并在日志附加信息中保存 API；其他日志方法不需要传 API。如果 `provide()` 抛出异常，组件会写入包含异常信息的 `Request` 错误日志。

公开日志方法的参数顺序为：

```kotlin
ScreenLog.v/d/i/w(tag, message, prettyJson = false, extras = null)
ScreenLog.e(tag, message, throwable = null, prettyJson = false, extras = null)
```

## 初始化参数与内部默认值

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `application` | 必填 | 用于生命周期监听和日志目录定位 |
| `hiddenRequestInterfaces` | 空集合 | 默认不记录的接口标识；使用不含动态参数的稳定路径 |
| 文件保存 | 关闭 | 测试人员可在设置页开启；开启后保留 1 天，单日文件上限 10 MB |
| 最低级别 | `DEBUG` | 低于该等级的日志直接忽略 |
| 内存容量 | 1000 条 | 超出后淘汰最早的日志 |
| 复制字段 | 仅 message | 测试人员可在设置页调整 |

## 界面操作

- 点击“日志”悬浮入口展开菜单。
- 悬浮菜单中的“关闭屏幕日志”会停止录制并隐藏入口和面板；下次启动保持关闭。需要再次开启时使用接入方的测试入口。
- `显示日志面板` Switch：显示或隐藏日志面板。
- `记录日志` Switch：控制是否接收新日志，关闭后已有日志保持不变。
- `接口日志`：默认配置和手动新增的接口统一管理，均可开启或关闭；只有手动新增的接口可删除。开关状态在重启后保留，删除后该接口按普通接口记录。
- `保存日志到文件` Switch：默认关闭；开启后仅后续日志进入文件队列。
- `自动滚动` Switch：控制列表是否跟随最新日志。
- 面板默认占屏幕高度 `1/3`，双击面板切换到 `2/3`，再次双击恢复。
- 顶部“全屏”可展开至整个页面，按钮变为“收起”；收起恢复进入全屏前的小屏或中屏尺寸。全屏期间双击不改变此前尺寸。
- 面板不再展示“记录日志”和“自动滚动”开关，这两项可在悬浮菜单或设置页调整；顶部“清空屏幕”仅清内存。
- 支持关键字、日志等级和 Tag 筛选。
- 长按日志进入多选，点击其他日志增减选择，可全选当前筛选结果后统一复制。
- 顶部复制图标可一键复制当前筛选结果；复制字段由 `ScreenLogCopyOptions` 控制，默认仅复制 message。
- 关闭“自动滚动”只停止列表跟随，不影响日志记录。
- 悬浮入口支持拖动并自动吸附左右边缘，快捷菜单会限制在当前页面范围内，矮屏下可滚动操作。
- 面板和快捷菜单中的“清空日志”仅清空内存并刷新列表，保留已保存文件；设置页的清空操作仍删除内存和文件。
- 请求日志在正文首尾显示 URL 与 `REQUEST` / `RESPONSE` 阶段，相邻日志使用深浅交替背景。

调用方需显式传入 `phase = ScreenLogRequestPhase.REQUEST`（请求参数）或 `phase = ScreenLogRequestPhase.RESPONSE`（响应结果）；组件不会根据正文自动判断。原接口省略 `phase` 时默认为请求参数。Java 调用示例：

```java
ScreenLog.request("/api/orders", ScreenLogRequestPhase.REQUEST, () -> requestParams);
ScreenLog.request("/api/orders", ScreenLogRequestPhase.RESPONSE, () -> responseBody);
```

悬浮按钮靠右时菜单向左展开，靠底部时向上展开，展开和收起均保持按钮坐标。
- `设置` 按“运行控制、交互设置、复制内容、存储与信息”分类，可修改记录、文件持久化、自动滚动、触摸目标和复制字段，也可导出或清空日志。

切换 Activity 时，组件会重新挂载 View。记录开关、接口开关、自动滚动、触摸目标、复制字段、面板高度和悬浮入口位置会保存到模块私有 `SharedPreferences`；筛选条件、列表位置、多选状态和面板是否显示仅在当前进程或会话中保留。应用重新启动后若录制状态为开启，悬浮入口会自动显示，面板仍默认关闭；启动日志会进入内存，如已开启文件保存，也会写入文件。

## 触摸模式

| 模式 | 行为 |
| --- | --- |
| `LOG_PANEL` | 面板接收触摸，可滚动、筛选、复制和双击改变高度 |
| `UNDERLYING_PAGE` | 面板不主动处理触摸，用于操作下层业务页面 |

悬浮菜单和设置页中的“日志面板 / 下层页面”分段选项可快速切换。

## 对外 API

| API | 用途 |
| --- | --- |
| `ScreenLogKit.init(application, hiddenRequestInterfaces)` | 在 Application 中初始化一次，恢复已保存的录制和接口状态 |
| `ScreenLogKit.open(activity)` | 开启录制并显示悬浮入口 |
| `ScreenLogKit.close()` | 停止录制并隐藏悬浮入口和面板 |
| `ScreenLog.v/d/i/w/e(...)` | 写入对应等级的业务日志 |
| `ScreenLog.request(api, ..., messageProvider)` | 按 API 开关决定是否调用 provider 并写入请求日志 |

所有 UI 操作会切换到主线程执行；日志文件写入使用单线程后台队列。

将仅用于日志的响应解析放进 provider，并用同一个稳定接口标识配置默认列表。例如：

```kotlin
ScreenLog.request(api = requestPath, prettyJson = true) {
    gson.fromJson(responseBody, LogPayload::class.java).toString()
}
```

provider 仅负责生成日志内容，不应改变业务响应的解析和处理；它在调用线程执行。若接入项目原有独立的接口日志黑名单，需要将请求日志生成位置改为调用 `ScreenLog.request`；组件无法恢复已经被外部丢弃的日志。

## 日志存储

- 内存日志用于当前进程内展示，超过 `maxMemoryEntries` 后淘汰最早数据。
- 文件优先位于 App 专属外部存储 `getExternalFilesDir("screen-log")`；外部存储不可用时回退到内部 `files/screen-log/`。
- 文件名为 `screen_log_yyyy-MM-dd.log`。
- 每行是一条 JSON 记录，包含时间、等级、Tag、原始消息、线程名、JSON 格式标记、异常堆栈和 extras。
- 只有文件持久化开启时才在后台删除过期文件并恢复近期日志；恢复结果会与冷启动期间的新日志合并，最终仍受 `maxMemoryEntries` 限制。
- `clearLogs()` 会立即清空内存，文件删除在后台队列执行。
- 设置页“导出日志”会打开系统保存文件界面，导出 ZIP 到测试人员选择的位置；成功后可以直接打开文件。ZIP 同时包含已有持久化文件和当前内存日志。

## 接入验收

示例 App 提供两个本地模拟接口 `/mock/large-response` 和 `/mock/frequent-poll`，都在初始化时默认屏蔽。手动验证步骤：

跨页面验证：点击 `Open screen log`，再点击 `Open second page` 进入独立 Activity；确认“日志”悬浮入口仍显示，点击它并开启“显示日志面板”，再点击 `Add log from second page` 查看新日志。点击 `Back to main page`，确认悬浮入口和日志面板仍可操作。

1. 启动示例 App，点击 `Open screen log`；可从悬浮入口进入设置页。
2. 点击示例页的 `Test large response API` 和 `Test frequent poll API`；页面显示“未调用 provide”，日志面板没有对应的 `Request` 记录。默认接口只有开关，没有删除按钮。
3. 在设置页的“接口日志”列表中开启其中一个接口，再点击对应按钮；页面显示“已记录请求日志”，日志面板出现带该 API 的 `Request` 记录。另一个接口仍被跳过。
4. 关闭该接口开关并再次点击，确认停止新增记录。点击 `Close screen log` 后所有 provider 均不执行；重新开启后接口开关状态保持不变。保持开启状态杀死并重启示例 App，确认录制、接口开关和悬浮入口都自动恢复。

5. 开启 `/mock/large-response` 后点击 `Test provider exception`，确认日志面板出现包含模拟异常及堆栈的 `Request` 错误日志；关闭该接口后再点击，确认 provider 不会执行。

这两个接口仅用于本地验证，没有实际网络请求；示例页在 provider 中执行 `gson.fromJson`。

建议至少验证以下场景：

1. 写入五种等级日志，确认 `minLevel` 生效。
2. 搜索、Tag 和等级筛选结果正确。
3. 双击面板可在 `1/3` 与 `2/3` 高度间切换。
4. 切换 Activity 后筛选条件、列表位置和面板高度保持不变。
5. 暂停自动滚动后新日志仍持续写入。
6. 关闭“记录日志”后列表数量不再增长，重新开启后继续追加。
7. 进程重启后能读取保留期内的持久化日志。
8. 达到单日文件上限后应用不崩溃，内存日志仍正常工作。
9. 未调用 `ScreenLogKit.init` 时，开启和写日志均不生效。

## 常见问题

### 点击开启后没有显示悬浮入口

先确认已在 `Application` 初始化，并在当前 Activity 调用：

```kotlin
ScreenLogKit.open(this)
```

面板挂载到当前 Activity 的 `android.R.id.content`，不需要申请 `SYSTEM_ALERT_WINDOW`。

若宿主会遍历 `android.R.id.content` 的子 ViewGroup 并统一调用 `setFitsSystemWindows(true)`，组件的悬浮入口和日志面板会忽略此设置，避免全屏时顶部操作区域因状态栏适配产生触摸偏移。

示例 App 的 `Test status bar compatibility` 按钮会执行同样的遍历与设置，并检查日志浮层是否保持 `fitsSystemWindows=false`。验证时先开启日志面板，再点击这个测试按钮，确认提示验证到 2 个浮层；随后点“全屏”，分别点击“收起”按钮的上半部和下半部，均应恢复原尺寸。示例只模拟截图中的这段状态栏处理，业务 App 仍需替换 AAR 做最终触摸验收。

### 日志没有写入

检查是否已初始化、录制开关和调用时机。`init()` 之前写入的日志不会补录。

### 是否可以记录账号、Token 或完整请求体

组件定位为内部调试工具，当前不内置脱敏和加密，也不限制单条日志长度。具体可记录内容遵循接入方内部日志规范。

### 为什么切换 Activity 后看起来没有消失

旧 Activity 暂停时会移除 View；新 Activity 恢复时重新挂载。模块不强持有旧 Activity。用户偏好由模块私有 `SharedPreferences` 恢复，筛选条件、列表位置和多选状态等临时上下文仍只在进程内保存。

## 当前边界

- 不读取系统 `logcat`。
- 不抓包、不上传日志。
- 内部使用，不提供日志脱敏或加密。
- 不保证多窗口/多前台 Activity 场景下的独立面板状态。
- 暗码验证和测试资格控制由接入 App 负责；组件只在业务方调用 `open(activity)` 时显示入口。
