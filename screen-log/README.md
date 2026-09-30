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
| Java 兼容级别 | Java 11 |
| UI 技术 | Kotlin + View/XML，不依赖 Compose |
| 直接依赖 | AndroidX Core KTX、ConstraintLayout、RecyclerView |
| 权限 | 无新增权限 |

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

建议在 `Application.onCreate()` 中初始化一次：

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        ScreenLogManager.init(
            ScreenLogConfig(
                application = this,
                enable = BuildConfig.DEBUG,
                enablePersist = true,
                keepDays = 3,
                maxFileSizeMb = 10,
                minLevel = ScreenLogLevel.DEBUG,
                maxMemoryEntries = 2000,
                copyOptions = ScreenLogCopyOptions(
                    includeTime = true,
                    includeLevel = true,
                    includeTag = true
                )
            )
        )
    }
}
```

`enable=false` 是模块总开关：停止日志写入和 Activity 生命周期挂载，清除已显示的入口/面板，并忽略后续 `attachTo()`、`showFloatingEntry()` 和 `showPanel()` 调用。

当 `enable=true` 时，运行时“记录日志”开关默认开启。手动关闭后保留已有日志和面板，只停止接收新日志；重新开启后继续记录。

### 3. 绑定页面并显示入口

组件会监听 Activity 生命周期。为了确保首次进入页面时立即绑定，建议在需要展示屏幕日志的 Activity 中显式调用：

```kotlin
override fun onResume() {
    super.onResume()
    if (BuildConfig.DEBUG) {
        ScreenLogManager.attachTo(this)
        ScreenLogManager.showFloatingEntry()
    }
}
```

隐藏入口：

```kotlin
ScreenLogManager.hideFloatingEntry()
```

也可以跳过悬浮入口，直接显示日志面板：

```kotlin
ScreenLogManager.attachTo(this)
ScreenLogManager.showPanel()
```

### 4. 写入业务日志

```kotlin
ScreenLog.d("WebView", "load url = $url", extras = mapOf("url" to url))
ScreenLog.i("JSBridge", "call method = $methodName")
ScreenLog.w("Network", "request retry", extras = mapOf("retry" to retryCount.toString()))
ScreenLog.i("Network", responseJson, prettyJson = true)
ScreenLog.e(
    tag = "AccountDelete",
    message = "delete account failed",
    throwable = throwable,
    extras = mapOf("code" to code)
)
```

建议 Tag 使用稳定的业务域名称，例如 `Login`、`Network`、`WebView`、`JSBridge`、`Page`，不要把动态 ID 拼进 Tag。

`prettyJson=true` 时，JSON Object 或 JSON Array 类型的 message 会以两空格缩进展示和复制；非法 JSON 保持原文。`extras` 位于最后一个参数，建议使用具名参数传入。

公开日志方法的参数顺序为：

```kotlin
ScreenLog.v/d/i/w(tag, message, prettyJson = false, extras = null)
ScreenLog.e(tag, message, throwable = null, prettyJson = false, extras = null)
```

## 配置说明

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `application` | 必填 | 仅持有 Application，用于生命周期监听和日志目录定位 |
| `enable` | 必填 | 模块总开关；关闭后停止写入、移除 UI 并忽略后续显示调用 |
| `enablePersist` | `true` | 是否写入 App 私有目录文件 |
| `keepDays` | `3` | 初始化时保留最近天数，最少按 1 天处理 |
| `maxFileSizeMb` | `10` | 单日文件上限，达到后当天新日志停止落盘 |
| `minLevel` | `DEBUG` | 低于该等级的日志直接忽略 |
| `maxMemoryEntries` | `2000` | 内存最多保留条数，内部最低按 200 条处理 |
| `copyOptions` | 仅复制 message | 首次运行时的复制字段默认值；用户在设置页修改后以已保存偏好为准 |

## 界面操作

- 点击“日志”悬浮入口展开菜单。
- `显示日志面板` Switch：显示或隐藏日志面板。
- `记录日志` Switch：控制是否接收新日志，关闭后已有日志保持不变。
- `自动滚动` Switch：控制列表是否跟随最新日志。
- 面板默认占屏幕高度 `1/3`，双击面板切换到 `2/3`，再次双击恢复。
- 支持关键字、日志等级和 Tag 筛选。
- 长按日志进入多选，点击其他日志增减选择，可全选当前筛选结果后统一复制。
- 顶部复制图标可一键复制当前筛选结果；复制字段由 `ScreenLogCopyOptions` 控制，默认仅复制 message。
- 关闭“自动滚动”只停止列表跟随，不影响日志记录。
- 悬浮入口支持拖动并自动吸附左右边缘。
- `设置` 按“运行控制、交互设置、复制内容、存储与信息”分类，可修改记录、自动滚动、触摸目标和复制字段，并清空日志。

切换 Activity 时，组件会重新挂载 View。记录开关、自动滚动、触摸目标、复制字段、面板高度和悬浮入口位置会保存到模块私有 `SharedPreferences`；筛选条件、列表位置、多选状态和面板是否显示仅在当前进程或会话中保留。应用重新启动后面板默认不显示，但只要“记录日志”保持开启，启动日志仍会进入内存和文件，之后打开面板即可查看。

## 触摸模式

```kotlin
ScreenLogManager.setTouchMode(ScreenLogTouchMode.LOG_PANEL)
ScreenLogManager.setTouchMode(ScreenLogTouchMode.UNDERLYING_PAGE)
```

| 模式 | 行为 |
| --- | --- |
| `LOG_PANEL` | 面板接收触摸，可滚动、筛选、复制和双击改变高度 |
| `UNDERLYING_PAGE` | 面板不主动处理触摸，用于操作下层业务页面 |

悬浮菜单和设置页中的“日志面板 / 下层页面”分段选项可快速切换。

## 对外 API

| API | 用途 |
| --- | --- |
| `ScreenLogManager.init(config)` | 初始化模块 |
| `ScreenLogManager.attachTo(activity)` | 显式绑定当前 Activity |
| `showFloatingEntry()` / `hideFloatingEntry()` | 显示或隐藏悬浮入口 |
| `showPanel()` / `hidePanel()` | 显示或隐藏日志面板 |
| `setTouchMode(mode)` / `toggleTouchMode()` | 设置触摸目标 |
| `setRecordingEnabled(enabled)` / `toggleRecording()` | 运行时开始或停止接收新日志 |
| `isRecordingEnabled()` | 查询当前是否正在记录日志 |
| `pauseAutoScroll()` / `resumeAutoScroll()` | 控制列表自动跟随 |
| `clearLogs()` | 清空内存日志和持久化文件 |
| `ScreenLog.v/d/i/w/e(...)` | 写入对应等级的业务日志 |

所有 UI 操作会切换到主线程执行；日志文件写入使用单线程后台队列。

## 日志存储

- 内存日志用于当前进程内展示，超过 `maxMemoryEntries` 后淘汰最早数据。
- 文件位于 App 私有目录：`files/screen-log/`。
- 文件名为 `screen_log_yyyy-MM-dd.log`。
- 每行是一条 JSON 记录，包含时间、等级、Tag、原始消息、线程名、JSON 格式标记、异常堆栈和 extras。
- 初始化时读取最近文件并恢复到内存，最终仍受 `maxMemoryEntries` 限制。
- `clearLogs()` 会立即清空内存，文件删除在后台队列执行。

## 接入验收

建议至少验证以下场景：

1. 写入五种等级日志，确认 `minLevel` 生效。
2. 搜索、Tag 和等级筛选结果正确。
3. 双击面板可在 `1/3` 与 `2/3` 高度间切换。
4. 切换 Activity 后筛选条件、列表位置和面板高度保持不变。
5. 暂停自动滚动后新日志仍持续写入。
6. 关闭“记录日志”后列表数量不再增长，重新开启后继续追加。
7. 进程重启后能读取保留期内的持久化日志。
8. 达到单日文件上限后应用不崩溃，内存日志仍正常工作。
9. `enable=false` 时显示 API 和记录 API 均不生效。

## 常见问题

### 调用 `showPanel()` 后没有显示

先确认已经初始化，并在当前 Activity `onResume()` 中调用过：

```kotlin
ScreenLogManager.attachTo(this)
```

面板挂载到当前 Activity 的 `android.R.id.content`，不需要申请 `SYSTEM_ALERT_WINDOW`。

### 日志没有写入

检查 `enable`、`minLevel` 和调用时机。`init()` 之前写入的日志不会补录。

### 是否可以记录账号、Token 或完整请求体

组件定位为内部调试工具，日志保存在 App 私有目录，不内置脱敏和加密。具体可记录内容遵循接入方内部日志规范。

### 为什么切换 Activity 后看起来没有消失

旧 Activity 暂停时会移除 View；新 Activity 恢复时重新挂载。模块不强持有旧 Activity。用户偏好由模块私有 `SharedPreferences` 恢复，筛选条件、列表位置和多选状态等临时上下文仍只在进程内保存。

## 当前边界

- 不读取系统 `logcat`。
- 不抓包、不上传日志。
- 内部使用，不提供日志脱敏或加密。
- 不保证多窗口/多前台 Activity 场景下的独立面板状态。
- 当前没有稳定的公开文件导出 API；复制是主要外部取数方式。
