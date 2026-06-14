# Screen Log Module

`screen-log` 是独立的 App 内屏幕日志查看模块，用于开发、测试或 Release 白名单入口下查看业务主动写入的日志。

## 接入

在 app 模块依赖中引入：

```kotlin
implementation(project(":screen-log"))
```

在应用启动后初始化：

```kotlin
ScreenLogManager.init(
    ScreenLogConfig(
        application = application,
        enable = true,
        enablePersist = true,
        keepDays = 3,
        maxFileSizeMb = 10,
        minLevel = ScreenLogLevel.DEBUG
    )
)
```

当前页面恢复时建议显式绑定 Activity：

```kotlin
ScreenLogManager.attachTo(activity)
```

由业务已有入口控制悬浮按钮显示：

```kotlin
ScreenLogManager.showFloatingEntry()
ScreenLogManager.hideFloatingEntry()
```

## 写入日志

业务方主动写入，不读取系统 `logcat`，也不理解具体业务场景。

```kotlin
ScreenLog.d("WebView", "load url = $url", mapOf("url" to url))
ScreenLog.i("JSBridge", "call method = $methodName", mapOf("method" to methodName))
ScreenLog.e("AccountDelete", "delete account failed", throwable, mapOf("code" to code))
```

建议 Tag 保持稳定，例如 `WebView`、`JSBridge`、`Login`、`Network`、`Page`。

## 触摸模式

模块支持切换屏幕日志显示时的触摸目标：

- `LOG_PANEL`：操作日志面板，支持滚动、搜索、筛选、复制、清空。
- `UNDERLYING_PAGE`：操作下层页面，日志面板只读显示，触摸事件透传给业务页面。

可以通过高级设置页切换，也可以通过悬浮菜单里的 `Touch: Log` / `Touch: Page` 快捷切换。

```kotlin
ScreenLogManager.setTouchMode(ScreenLogTouchMode.LOG_PANEL)
ScreenLogManager.setTouchMode(ScreenLogTouchMode.UNDERLYING_PAGE)
ScreenLogManager.toggleTouchMode()
```

## 能力说明

- 悬浮入口挂载到当前 `Activity` 的内容容器，不申请系统悬浮窗权限。
- 悬浮按钮支持拖动，松手后吸附到屏幕左右边缘。
- 日志面板显示在当前页面底部，默认高度为屏幕 `1/3`，双击切换 `2/3`。
- 面板使用 `RecyclerView` 展示日志，支持关键字、Tag、等级筛选。
- 长按单条日志复制；点击 `Copy filtered` 复制当前筛选结果。
- 暂停滚动只影响 UI 自动跟随，日志仍会写入内存和文件。
- 持久化默认保留最近 3 天，每天一个 `screen_log_yyyy-MM-dd.log` 文件。
- 当天文件达到 10MB 后停止继续写入，避免无限增长。

## 开发约束

- UI 使用模块内独立 View，不和业务页面布局耦合。
- 面板布局采用 `ConstraintLayout` 分区，列表使用 `RecyclerView`。
- 模块布局访问使用 ViewBinding。
- 文件写入使用单线程后台队列，不阻塞主线程。
- UI 刷新做轻量节流，避免高频日志导致列表频繁刷新。
- 第一版不做脱敏、不上传、不抓包、不读取系统日志。
