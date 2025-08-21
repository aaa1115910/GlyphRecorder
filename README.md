# GlyphRecorder

一个 Ingress Glyph 识别工具

## 使用指南

### 1. 校准定位

选择一张没有 Glyph 图案的入侵截图，软件可自动识别坐标。如果有误，可从上到下从左到右依次点击坐标圆圈进行标记。

### 2. 悬浮窗权限

需要在应用详细页开启“显示在其他应用的上层”权限。启动服务时如未授权，会自动跳转到设置页面。

### 3. 启动服务

在首页点击“启动服务”开关。

### 4. 识别 Glyph

点击悬浮窗的“▶︎”按钮开始捕获屏幕并识别，识别完成或连续 10 秒未检测到 Glyph 后会自动停止。

### 5. 停止服务

可在应用内点击“停止服务”开关，或将悬浮窗拖动到屏幕下边缘关闭。

## 工作模式对比

### 媒体投射

通过 `Media Projection` 接口获取屏幕内容。

**优点：**
- Android 官方接口，兼容性好，无需额外权限
- 延迟低，稳定性高

**缺点：**
- 每次启动需手动选择共享内容
- 可能会导致所有通知被系统隐藏
- 状态栏有共享屏幕提示，无法去除
- 不能与屏幕录制同时使用

### Shizuku

通过 Shizuku 的 adb 权限运行 `scrcpy-server` 获取屏幕内容。

**优点：**
- 可自动获取屏幕内容，无需用户交互
- 可与屏幕录制同时使用

**缺点：**
- 依赖 Shizuku
- 延迟略高于媒体投射模式

## License

[MIT](LICENSE) © aaa1115910

## 致谢

- [Yitong](https://github.com/yitong-ovo)
- [chibatching/glyph-predictor-data](https://github.com/chibatching/glyph-predictor-data)
