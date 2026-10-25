<div align="center">

<img src="artwork/tuck-icon-512.png" alt="Tuck app icon" width="112" height="112">

# Tuck

**Keep the messages that matter. Let the rest drift by.**

A calm, local-first Android companion that quietly picks out the notifications you care about,
alerts you your way, and keeps them next to a pocket notepad for ideas and to-dos.

![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![No internet permission](https://img.shields.io/badge/Internet%20permission-none-E8486A)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

[English](#english) · [简体中文](#简体中文)

</div>

---

## English

Most notifications can wait. A few can't: a message from your manager, a keyword like "urgent",
a reply in one group chat out of thirty. Tuck watches your notification stream for exactly
those. It gives them an alert of their own, tucks them into a quiet Priority Inbox, and lets
you turn any of them into a note or a to-do in one tap.

### Philosophy and privacy

- **Non-intrusive by default.** Tuck never blocks, hides or replaces other apps' notifications.
  It reads a copy through Android's `NotificationListenerService`, and your messaging apps keep
  notifying you exactly as before. The one exception is **Tidy up the original**, a per-rule
  switch that is off by default (see below).
- **Local-first, nothing leaves the phone.** The app has **no `INTERNET` permission**, so it
  can't send anything anywhere. There are no accounts, no analytics and no cloud sync.
  Captured messages, rules and memos live in an on-device Room database and are excluded from
  cloud backups and device transfers.
- **You stay in control.** Inbox messages are deleted after 7, 30, 90 or 365 days, or kept
  forever; you choose. Memos are never deleted automatically. The "Recently seen" diagnostics
  list is kept in memory only.

### Key features

**Smart notification rules**
- Match by **source app**, **sender / group / account name** and **keywords**. Conditions
  combine with AND and values within one condition with OR. Matching ignores case and
  full-width/half-width differences.
- A step-by-step wizard (apps → senders → keywords → alert → review) that shows which recent
  notifications the new rule would catch before you save it.
- Chat-aware parsing: WeChat, QQ, TIM and WeCom group messages are split into person and group.
  When an app delivers several messages at once, each one is checked on its own.
- Each rule has its own page with statistics, plus pause, duplicate and rename.

**Alerts, Quiet Hours and "Avoid double alerts"**
- A default alert plus optional per-rule alerts: sound (with a custom tone), vibration,
  heads-up banner, or silent collection.
- **Quiet Hours** silence Tuck's alerts overnight, except for rules marked
  "Alert during quiet hours". Messages are still collected.
- **Avoid double alerts:** each rule shows whether its source apps still ring on their own.
  **Make quiet** opens the right system page so you can set that app to *Silent*, so only Tuck
  rings. Use Silent, not Off: an app set to Off posts nothing for Tuck to read.
- **Tidy up the original** (optional, per rule, off by default) dismisses the source
  notification once Tuck's own alert has actually been posted. If Tuck stayed quiet, the
  original is always kept, so a starred message can never disappear unseen.

**Priority Inbox, Memos and To-dos**
- Starred messages grouped by day, with "To handle" / "All" views and filters by app and tag.
  Mark messages read or handled (also straight from the alert), open the original chat, copy,
  delete, or save a message as a memo.
- **Ideas** for free-form notes and **To-dos** as a checklist, with one-line quick capture,
  pins and tags. A memo made from a message keeps a quote of it and shows where it came from.

**Built-in diagnostics and phone-maker guidance**
- **Permission checklist:** Notification access, Tuck alerts and Run in background. Includes
  help for Android 13+'s "restricted settings" for sideloaded apps.
- **Alert check:** spots muted alert categories, Do Not Disturb, silent or vibrate mode and
  zero volume, with **Send test alert** and **Reset alerts**.
- **Reader health:** live status of the notification reader and a **Reconnect** that reports
  back. The inbox warns when the reader isn't running, and Settings shows the last notification
  Tuck couldn't read.
- **Recently seen:** shows exactly what Tuck read from recent notifications and which rules
  match them, so you can test rules live.
- **Brand-specific guidance** for Xiaomi / Redmi / POCO (MIUI, HyperOS), Huawei, Honor, OPPO /
  realme, OnePlus, vivo / iQOO, Samsung and Meizu. Covers autostart and battery steps, and
  settings shortcuts that land on each brand's real sound and vibration switches.

**Appearance**
- Eight accent colors (Rose by default, plus Coral, Amber, Green, Teal, Blue, Violet and Orchid),
  or **Wallpaper** colors on Android 12+.
- Light, Dark or follow the system, on clean white and `#121212` backgrounds.
- English and Simplified Chinese UI, with Android 13+ per-app language support.

### Tech stack and architecture

| Area | Choice |
| --- | --- |
| Language | Kotlin 2.2 |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| State and async | Coroutines, Flow / StateFlow, AndroidX ViewModel |
| Storage | Room 2.8 (KSP, auto-migrations, exported schemas), SharedPreferences for settings |
| System APIs | `NotificationListenerService`, notification channels, `NotificationManager` / `AudioManager` checks |
| Dependency injection | A small hand-wired `AppContainer`, with no DI framework |
| Platform | Min SDK 26 (Android 8.0), target SDK 36, AGP 8.13, Gradle 8.14 |

The only dependencies are AndroidX and kotlinx libraries; there are no third-party SDKs.

```
NotificationListenerService      reads a copy of each notification
 └─► NotificationParser          chat-aware: one entry per new message
      └─► RuleEngine             apps · senders · keywords (pure Kotlin)
           └─► CaptureProcessor  dedup, tags
                ├─► Room database ─► ViewModels (Flow) ─► Compose UI
                └─► AlertNotifier  Tuck's own alert, Quiet Hours, optional tidy-up
```

```
app/src/main/java/com/example/starbox/
  capture/   notification listener, parser, WeChat/QQ normalizer, capture pipeline
  rules/     rule engine, alert policy, quiet hours, duplicate policy (pure Kotlin, unit tested)
  alerts/    Tuck's own notifications, alert health checks and actions
  data/      Room database (rules, messages, memos) and retention cleanup
  system/    permissions, installed apps, phone-maker settings, opening source apps
  ui/        Compose screens: onboarding, inbox, memos, rules, settings
```

> The code still uses the original working name (`com.example.starbox`, `StarBox*` classes), so
> existing installs update in place and keep their data.

### Getting started

**Requirements:** JDK 17 or newer, and the Android SDK with platform 36, either through a
recent Android Studio or the command-line tools. Point Gradle at the SDK with `ANDROID_HOME` or
a `local.properties` file containing `sdk.dir=…`.

```bash
# Clone
git clone https://github.com/ytsu-ki/tuck.git
cd tuck

# Build the debug APK
./gradlew assembleDebug

# Run the unit tests and lint
./gradlew testDebugUnitTest lintDebug

# Install on a phone with USB debugging on, then launch
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.starbox/.MainActivity
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

**Signed release build**

1. Create a keystore once and keep it safe; every update must be signed with the same key:

   ```bash
   keytool -genkeypair -v -keystore tuck-release.jks -alias tuck -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Add a `keystore.properties` file in the project root. It is git-ignored, as are `*.jks` files:

   ```properties
   storeFile=tuck-release.jks
   storePassword=…
   keyAlias=tuck
   keyPassword=…
   ```

3. Run `./gradlew assembleRelease`. The APK is written to `app/build/outputs/apk/release/`.
   Without `keystore.properties` the release build still works, but the APK is unsigned.

**First run on a phone**

1. Allow **Notification access**. On Android 13+, a sideloaded app's switch may be greyed out:
   open App info → ⋮ → **Allow restricted settings**, then try again.
2. Allow **Tuck alerts**, and let Tuck **run in the background**.
3. On Xiaomi, Huawei, Honor, OPPO, OnePlus, vivo, Samsung and Meizu phones, follow the
   brand-specific steps shown in onboarding and Settings.

### Known limits

- Tuck only sees what a notification shows. If an app hides message previews (for example
  WeChat with message details turned off), only the sender can be matched.
- Nothing from before notification access was granted can be recovered.
- An app whose notifications are turned **Off**, or a chat muted inside the app, posts nothing,
  so Tuck can't see it. Set it to **Silent** instead.
- "Open in app" jumps to the exact chat only while the original notification still exists;
  otherwise it opens the app.

### License

Tuck is released under the [MIT License](LICENSE).

---

## 简体中文

大多数通知都可以晚点再看，但总有几条不能错过：领导的消息、带“紧急”字样的提醒，三十个群里你真正关心的那一个。Tuck 只替你留意这些通知：用属于它们的方式提醒你，把它们收进安静的“重要收件箱”，还能一键把任意一条变成灵感或待办。

### 设计理念与隐私

- **默认不打扰。** Tuck 从不拦截、隐藏或替换其他应用的通知。它通过 Android 的 `NotificationListenerService` 读取通知副本，你的聊天应用照常提醒。唯一的例外是按规则单独开启、默认关闭的 **收起原通知**（见下文）。
- **本地优先，数据不出手机。** 应用**没有 `INTERNET`（联网）权限**，从根本上无法向外发送任何数据；没有账号、没有统计分析、没有云同步。捕获的消息、规则和备忘都保存在本机的 Room 数据库中，并且不参与云备份和换机迁移。
- **一切由你决定。** 收件箱消息可保留 7、30、90、365 天或永久保留；备忘永远不会被自动删除。用于诊断的“最近读取”列表只保存在内存中。

### 主要功能

**智能通知规则**
- 按 **来源应用**、**发送者 / 群聊 / 账号名称** 和 **关键词** 匹配；不同条件之间为“且”，同一条件内多个值为“或”；匹配时忽略大小写和全角/半角差异。
- 分步向导（应用 → 发送者 → 关键词 → 提醒方式 → 确认），保存前就能看到这条规则会命中哪些最近的通知。
- 理解聊天通知：微信、QQ、TIM、企业微信的群消息会拆分出发送人和群名；应用一次送达多条消息时，每一条都会单独检查。
- 每条规则都有独立页面，显示命中统计，并可暂停、复制和重命名。

**提醒方式、勿扰时段与“避免重复提醒”**
- 默认提醒之外，每条规则还可单独设置：声音（可选自定义铃声）、振动、横幅弹出，或静默收集。
- **勿扰时段** 让 Tuck 的提醒在夜间保持安静，开启了“勿扰时段也提醒”的规则除外；消息照常收集。
- **避免重复提醒：** 每条规则会显示其来源应用是否还会自己响铃。点击 **设为静默** 即可跳转到正确的系统设置页，把该应用设为 *静默*，从此只有 Tuck 会响。请选择“静默”而不是“关闭”：关闭通知后应用不会发出任何通知，Tuck 也就无从读取。
- **收起原通知**（可选，按规则设置，默认关闭）会在 Tuck 自己的提醒确实发出后，收起来源应用的原通知；只要 Tuck 没有发出提醒，原通知就一定会保留，重要消息绝不会悄无声息地消失。

**重要收件箱、灵感与待办**
- 已加星的消息按天分组，提供“待处理 / 全部”视图，并可按应用和标签筛选；可标为已读或已处理（也可以直接在提醒通知上操作）、跳回原聊天、复制、删除，或存为备忘。
- **灵感** 用于随手记录，**待办** 是清单，支持一行快速记录、置顶和标签；从消息创建的备忘会保留原文引用并注明出处。

**内置诊断与机型适配指引**
- **权限清单：** 通知使用权、Tuck 提醒、后台运行；并针对 Android 13+ 侧载应用的“受限制的设置”给出操作指引。
- **提醒自检：** 能发现被静音的提醒类别、勿扰模式、静音或振动模式以及音量为零等情况，提供 **发送测试提醒** 和 **重置提醒**。
- **通知读取状态：** 实时显示通知读取是否在运行，**重新连接** 会反馈结果；读取停止时收件箱会给出提示，设置页会显示最近一条无法读取的通知。
- **最近读取：** 显示 Tuck 从最近的通知中读到了什么、命中了哪些规则，方便实时测试规则。
- **机型专属指引：** 覆盖小米 / Redmi / POCO（MIUI、HyperOS）、华为、荣耀、OPPO / realme、一加、vivo / iQOO、三星和魅族，包括自启动与电池优化设置；设置跳转会直达各品牌真正放置声音和振动开关的页面。

**外观**
- 八种强调色（默认玫瑰红，另有珊瑚橙、琥珀金、青绿、湖蓝、海蓝、紫罗兰、兰花紫），Android 12+ 还可选择 **壁纸取色**。
- 浅色、深色或跟随系统，背景为干净的纯白与 `#121212`。
- 支持英文和简体中文界面，并支持 Android 13+ 的“应用语言”设置。

### 技术栈与架构

| 方面 | 选型 |
| --- | --- |
| 语言 | Kotlin 2.2 |
| 界面 | Jetpack Compose、Material 3、Navigation Compose |
| 状态与异步 | Coroutines、Flow / StateFlow、AndroidX ViewModel |
| 存储 | Room 2.8（KSP、自动迁移、导出 schema），设置项使用 SharedPreferences |
| 系统接口 | `NotificationListenerService`、通知渠道，以及 `NotificationManager` / `AudioManager` 状态检查 |
| 依赖注入 | 手写的轻量 `AppContainer`，不使用 DI 框架 |
| 平台 | 最低 SDK 26（Android 8.0），目标 SDK 36，AGP 8.13，Gradle 8.14 |

依赖仅限 AndroidX 和 kotlinx 官方库，不含任何第三方 SDK。

```
NotificationListenerService      读取每条通知的副本
 └─► NotificationParser          理解聊天通知：每条新消息单独成条
      └─► RuleEngine             应用 · 发送者 · 关键词（纯 Kotlin）
           └─► CaptureProcessor  去重、打标签
                ├─► Room 数据库 ─► ViewModel（Flow）─► Compose 界面
                └─► AlertNotifier  Tuck 自己的提醒、勿扰时段、可选的收起原通知
```

```
app/src/main/java/com/example/starbox/
  capture/   通知监听、解析、微信/QQ 文本整理、捕获流程
  rules/     规则引擎、提醒策略、勿扰时段、重复提醒策略（纯 Kotlin，有单元测试）
  alerts/    Tuck 自己的通知、提醒自检及通知按钮操作
  data/      Room 数据库（规则、消息、备忘）与过期清理
  system/    权限、已安装应用、各品牌设置页、打开来源应用
  ui/        Compose 界面：引导、收件箱、备忘、规则、设置
```

> 代码中仍沿用最初的工作名（`com.example.starbox`、`StarBox*` 类名），以便已安装的版本可以直接覆盖升级并保留数据。

### 快速开始

**环境要求：** JDK 17 或更高版本；Android SDK（Platform 36），可通过较新版本的 Android Studio 或命令行工具安装。通过 `ANDROID_HOME` 环境变量或在 `local.properties` 中写入 `sdk.dir=…` 指定 SDK 路径。

```bash
# 克隆仓库
git clone https://github.com/ytsu-ki/tuck.git
cd tuck

# 构建调试版 APK
./gradlew assembleDebug

# 运行单元测试和 Lint 检查
./gradlew testDebugUnitTest lintDebug

# 在已开启 USB 调试的手机上安装并启动
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.starbox/.MainActivity
```

调试版 APK 输出在 `app/build/outputs/apk/debug/app-debug.apk`。

**签名发布版**

1. 创建一次签名密钥并妥善保管，之后每次更新都必须使用同一个密钥签名：

   ```bash
   keytool -genkeypair -v -keystore tuck-release.jks -alias tuck -keyalg RSA -keysize 2048 -validity 10000
   ```

2. 在项目根目录新建 `keystore.properties`（已被 git 忽略，`*.jks` 文件同样会被忽略）：

   ```properties
   storeFile=tuck-release.jks
   storePassword=…
   keyAlias=tuck
   keyPassword=…
   ```

3. 运行 `./gradlew assembleRelease`，APK 输出在 `app/build/outputs/apk/release/`。没有 `keystore.properties` 时也能构建发布版，但生成的 APK 未签名。

**在手机上首次运行**

1. 开启 **通知使用权**。在 Android 13+ 上，侧载应用的开关可能是灰色的：打开“应用信息”→ 右上角 ⋮ →“允许受限制的设置”，然后重试。
2. 允许 **Tuck 提醒**，并允许 Tuck **后台运行**。
3. 小米、华为、荣耀、OPPO、一加、vivo、三星和魅族手机，请按照引导页和设置页中的机型专属步骤操作。

### 已知限制

- Tuck 只能看到通知里显示的内容。如果应用隐藏了消息详情（例如微信关闭了“通知显示消息详情”），就只能按发送者匹配。
- 开启通知使用权之前的消息无法找回。
- 通知被 **关闭** 的应用，或在应用内设为免打扰的聊天，不会发出任何通知，Tuck 也就无法看到；请改为 **静默**。
- “在应用中打开”只有在原通知仍存在时才能直达对应聊天，否则只会打开该应用。

### 许可证

Tuck 以 [MIT 许可证](LICENSE) 开源。
