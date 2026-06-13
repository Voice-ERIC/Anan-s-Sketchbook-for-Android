# 安安的素描本 · Android / Anan's Sketchbook · Android

本项目是 [MarkCup-Official/Anan-s-Sketchbook-Chat-Box](https://github.com/MarkCup-Official/Anan-s-Sketchbook-Chat-Box) 的 **文字渲染部分** 的 Android 移植版。

This is an Android port of the **text-rendering component** from [MarkCup-Official/Anan-s-Sketchbook-Chat-Box](https://github.com/MarkCup-Official/Anan-s-Sketchbook-Chat-Box).

> ⚠️ 仅移植了文字渲染到底图并复制到剪贴板的功能。**没有**全局热键、剪贴板图片捕获、自动粘贴/发送等功能（那些是原 Python Windows 版本特有的）。
>
> ⚠️ Only the text-onto-base-image rendering + clipboard copy feature is ported. **No** global hotkeys, clipboard image capture, auto-paste/send, etc. (those are unique to the original Python Windows version).

---

## 功能 / Features

- **悬浮窗输入** — 在任意应用上层显示可拖动的悬浮窗，输入文字后一键生成图片并复制到剪贴板
  Floating overlay window — draggable, always-on-top. Type text, tap "生成并复制", the image is copied to clipboard.
- **多表情底图** — 12 种表情（普通、开心、生气、无语、脸红、病娇、闭眼、难受、害怕、激动、惊讶、哭泣）
  12 emotion base images (neutral, happy, angry, speechless, blush, yandere, eyes-closed, sad, afraid, excited, surprised, crying)
- **文字排版** — 自动换行、字号适配、颜色标记（`[紫色]` `【紫色】`）、横竖对齐选项
  Auto-wrapping, font sizing, color markup (`[purple]` `【purple】`), horizontal & vertical alignment
- **置顶图层** — 可选用 `base_overlay.png` 覆盖在最终图片上
  Optional overlay image (`base_overlay.png`) on the final render
- **App 内底图预览** — 在启动悬浮窗前即可查看各表情底图效果
  Preview each emotion's base image inside the app before launching the overlay

---

## 与原版的差异 / Differences from the original

| 功能 Feature | 原版 Python / Original | Android 版 / This port |
|---|---|---|
| 全局热键 Global hotkey | ✅ `Ctrl+Alt+H` 截取选中文字 | ❌ |
| 剪贴板图片 Clipboard image | ✅ 同时处理文本与图片 | ❌ 仅文字 |
| 自动粘贴/发送 Auto paste/send | ✅ 生成后自动粘贴到聊天窗口 | ❌ 仅复制到剪贴板 |
| 热键切换表情 Hotkey switch emotion | ✅ | ❌ |
| 前台进程白名单 Process whitelist | ✅ | ❌ |
| 悬浮窗 Floating overlay | ❌ 后台监听热键 | ✅ 悬浮窗直接输入 |
| 选择底图预览 Preview base image | ❌ | ✅ |

---

## 构建 / Build

前置条件 / Prerequisites:
- JDK 17
- Android SDK (platform 35, build-tools 35.0.0+)

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
./gradlew assembleDebug
```

APK 输出 / Output: `app/build/outputs/apk/debug/app-debug.apk`

---

## 使用 / Usage

1. 安装 APK 后打开应用
   Install the APK and open the app.
2. 选择表情 / 文字对齐方式 / 是否启用置顶图层
   Select emotion, alignment, and whether to use the overlay image.
3. 点击「启动悬浮窗」→ 授予悬浮窗权限（SYSTEM_ALERT_WINDOW）
   Tap "启动悬浮窗" → grant overlay permission.
4. 浮动窗会显示在屏幕顶部，输入文字后点击「生成并复制」
   A floating window appears near the top of the screen. Type text and tap "生成并复制".
5. 图片已复制到系统剪贴板，可粘贴到任意聊天或笔记应用
   The image is copied to the system clipboard — paste it into any chat or notes app.

**小技巧 / Tips:**
- 点击输入框弹出键盘，点击外部 / 拖动标题栏自动收起键盘
  Tap the text field to show the keyboard; tap outside or drag the title bar to dismiss it.
- 长按标题栏可拖动悬浮窗到屏幕任意位置
  Drag the title bar to move the floating window anywhere.
- 输入 `[紫色文字]` 或 `【紫色文字】` 可渲染为紫色
  Wrap text in `[brackets]` or `【brackets】` to render it in purple.

---

## 项目结构 / Project Structure

```
Anan-s-Sketchbook-for-Android/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/sketchbook/
│   │   │   ├── MainActivity.kt          # 主界面 / Launcher activity
│   │   │   ├── FloatingWindowService.kt # 悬浮窗服务 / Overlay service
│   │   │   ├── SketchbookRenderer.kt    # 文字渲染引擎 / Text renderer
│   │   │   ├── Emotion.kt              # 表情定义 / Emotion definitions
│   │   │   └── AppConfig.kt            # 配置数据类 / Config data class
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml    # 主界面布局
│   │   │   │   └── floating_window.xml  # 悬浮窗布局
│   │   │   ├── values/strings.xml
│   │   │   └── xml/file_paths.xml
│   │   ├── assets/
│   │   │   ├── BaseImages/             # 底图 + 置顶图层 / Base images + overlay
│   │   │   └── fonts/font.ttf          # 自定义字体 / Custom font
│   │   ├── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew / gradlew.bat
└── copy_assets.sh                      # 从项目根目录同步资源的脚本
```

---

## 许可证 / License

原项目使用 MIT 许可证。本移植版同样以 MIT 许可证发布。

The original project is MIT-licensed. This port is also released under the MIT license.
