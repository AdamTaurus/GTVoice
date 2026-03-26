# GT Voice SDK Demo

该仓库演示如何通过 AIDL 与 `com.goolton.voicedriver` 通讯，为第三方应用接入应用内语音命令词能力。

## 能力说明

- 支持初始化语音服务连接
- 支持动态下发和清空应用内命令词
- 支持识别结果、连接状态和异常回调
- 支持控制界面角标显示状态
- 支持根据错误码获取错误详情
- 命令词仅在应用位于前台时生效

## 接入前提

- 设备中已安装 `com.goolton.voicedriver`
- VoiceDriver 语音服务可正常启动
- 接入方 SDK 版本需与当前仓库保持一致

## 集成方式

### 1. JitPack

在项目的 `settings.gradle.kts` 或根构建配置中加入 JitPack 仓库：

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}
```

然后添加依赖：

```kotlin
dependencies {
    implementation("com.github.AdamTaurus:GTVoice:<tag>")
}
```

说明：

- `<tag>` 请替换为实际发布版本标签
- 当前仓库已包含 `jitpack.yml` 和 `maven-publish` 配置，可用于 JitPack 构建

### 2. AAR 包

下载对应版本的 `GTVoice-<version>.aar`，再以本地 AAR 的方式引入工程。

### 3. 模块依赖

将 `gt_voice_sdk` 作为本地模块引入，再在业务模块中依赖：

```kotlin
dependencies {
    implementation(project(mapOf("path" to ":gt_voice_sdk")))
}
```

当前 `app` 模块 demo 使用的就是这种方式。

## 快速开始

### 1. 初始化

```kotlin
GTVoiceManager.init(context)
```

建议传入 `Application` 或 `applicationContext`。

### 2. 注册回调

```kotlin
GTVoiceManager.addVoiceCallbackListener { state, data ->
    when (state) {
        GTVoiceManager.VoiceState.START -> {
            // 服务已连接或识别已开始
        }
        GTVoiceManager.VoiceState.RESULT -> {
            // data 为识别结果
        }
        GTVoiceManager.VoiceState.ERROR -> {
            // data 为错误文案
        }
        GTVoiceManager.VoiceState.STOP -> {
            // 服务断开或识别结束
        }
    }
}
```

### 3. 添加命令词

```kotlin
GTVoiceManager.addKeywords(listOf("打开订单", "关闭订单"))
```

说明：

- 每次调用会覆盖上一次为当前应用设置的命令词
- 若服务尚未连接，命令词会先缓存，连接成功后自动下发
- 命中命令词后，结果优先回调给当前 SDK

### 4. 清空命令词

```kotlin
GTVoiceManager.clearKeywords()
```

### 5. 控制角标显示

```kotlin
GTVoiceManager.setViewIndexState(show = true)
```

### 6. 释放

```kotlin
GTVoiceManager.unInit(context)
```

### 推荐调用顺序

```kotlin
GTVoiceManager.addVoiceCallbackListener(...)
GTVoiceManager.init(context)
GTVoiceManager.addKeywords(...)
```

结束时：

```kotlin
GTVoiceManager.clearKeywords()
GTVoiceManager.unInit(context)
```

## 错误处理

SDK 通过 `VoiceState.ERROR` 返回错误文案，也支持通过错误码获取详情：

```kotlin
val detail = GTVoiceManager.getErrorDetail(code)
val message = GTVoiceManager.getErrorMessage(code)
```

当前公开错误码：

- `GTVoiceManager.error_code_sdk_uninitialized = -1`
- `GTVoiceManager.error_code_service_unregistered = -2`
- `GTVoiceManager.error_code_invalid_keywords = -3`
- `GTVoiceManager.error_code_global_keywords_forbidden = -4`

## app Demo 说明

`app` 模块中的主 demo 已按横屏设备场景调整为自适应布局：

- 竖屏时使用上下分区布局
- 横屏时自动切换为左右分栏
- 操作面板提供初始化、下发命令词、清空命令词、显示角标、隐藏角标和释放 SDK
- 输入框支持使用英文逗号、中文逗号或换行分隔多个命令词

## 不接 SDK 的场景

如果你的页面本身就是标准触摸交互，很多情况下并不需要接入 SDK。语音能力会根据控件是否可点击、是否可见，以及控件及其子控件的文本自动生成命令词。

同时，也可以通过 `contentDescription` 显式配置命令词行为。

## `contentDescription` 配置说明

普通命令词：

```xml
android:contentDescription="命令词"
```

不希望识别该控件命令词：

```xml
android:contentDescription="gtKeyword_unsupported"
```

适用于图片名、无规律字符串或不希望被识别的中英文混合内容。

不希望显示该控件序号：

```xml
android:contentDescription="gtKeyword_hindIndex"
```

多个属性可使用 `|` 组合，命令词必须放在第一位。例如：

```xml
android:contentDescription="回到首页|gtKeyword_hindIndex"
android:contentDescription="gtKeyword_unsupported|gtKeyword_hindIndex"
```

## JitPack 发布与刷新构建

仓库内已具备以下基础配置：

- 根目录 `jitpack.yml` 使用 JDK 17
- `gt_voice_sdk/build.gradle` 已配置 `maven-publish`
- 根工程已声明 `GROUP=com.github.AdamTaurus`

发布新版本的常用流程：

1. 提交并推送代码到 GitHub
2. 创建并推送版本标签，例如 `git tag 1.0.8 && git push origin 1.0.8`
3. 打开 [JitPack](https://jitpack.io)，输入仓库地址并触发对应 tag 构建
4. 构建成功后，将依赖版本改为新的 tag
