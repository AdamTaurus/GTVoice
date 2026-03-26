# GT Voice SDK

`gt_voice_sdk` 用于给第三方应用接入 VoiceDriver 的应用内语音命令词能力。

## 能力说明

- 通过 AIDL 与 `com.goolton.voicedriver` 通讯
- 支持第三方应用动态设置命令词
- 支持命令识别结果回调
- 支持根据错误码获取错误详情
- 命令词仅在对应应用位于前台时才会回调给该应用
- 不支持注册全局命令词

## 接入前提

- 设备中已安装 `com.goolton.voicedriver`
- VoiceDriver 语音服务可正常启动
- 第三方应用与当前 SDK 版本保持一致

## 基本使用

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
            // 命中命令词，data 为识别结果
        }
        GTVoiceManager.VoiceState.ERROR -> {
            // 异常或命令词设置失败，data 为错误文案
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
- 如果服务尚未连接，命令词会先缓存，待连接成功后自动下发
- 命令词命中后，会优先回调给 SDK，不再继续执行下层页面命令

### 4. 清空命令词

```kotlin
GTVoiceManager.clearKeywords()
```

### 5. 控制角标显示

```kotlin
GTVoiceManager.setViewIndexState(show = true)
```

该能力只在 VoiceDriver 无障碍能力已初始化时生效。

### 6. 释放

```kotlin
GTVoiceManager.unInit(context)
```

## 错误处理

SDK 通过现有回调 `VoiceState.ERROR` 返回错误信息，也支持直接按错误码查询详情。

### 公开错误码

- `GTVoiceManager.error_code_sdk_uninitialized = -1`
- `GTVoiceManager.error_code_service_unregistered = -2`
- `GTVoiceManager.error_code_invalid_keywords = -3`
- `GTVoiceManager.error_code_global_keywords_forbidden = -4`

### 获取错误详情

```kotlin
val detail = GTVoiceManager.getErrorDetail(code)
// 或 val detail = GTVoiceErrorUtils.getErrorDetail(code)

val title = detail.title
val message = detail.message
val suggestion = detail.suggestion
```

### 仅获取错误文案

```kotlin
val message = GTVoiceManager.getErrorMessage(code)
// 或 GTVoiceErrorUtils.getErrorMessage(code)
```

## 命令词限制

- 不允许传空字符串或全空白字符串
- 不允许注册全局命令词
- SDK 命令词按“精确匹配”命中
- 只有当前位于前台的应用，其 SDK 命令词才会参与识别与回调

## 行为说明

### 应用未调用 `unInit()` 直接退出

SDK 服务端会在回调进程死亡后自动清理该应用的命令词，避免命令词长期残留。

### 应用退到后台

即使未主动清理命令词，后台应用的 SDK 命令词也不会继续参与识别或收到回调。

### 服务重连

SDK 会缓存当前命令词和角标状态，服务重连后会自动回放。

## 推荐调用顺序

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
