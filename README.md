# GTVoice  

该项目为通过aidl与VoiceDrive通信实现命令词设置和回调的演示项目，直接引用aar包或model：gt_voice_sdk，暂时无法通过implementation 'com.github.AdamTaurus:GTVoice:Tag'引用
  

# GTVoiceManager

## 简介

GTVoiceManager 是一个用于管理 VoiceDrive 应用中语音服务的实用工具类。它提供了与语音服务交互的功能，包括添加自定义关键词、初始化和反初始化语音服务，并注册回调以接收语音识别结果。  
```需要 VoiceDriver 版本为 1.0.66 或更高。```
## 使用方法

1. **初始化**：通过调用 `init()` 方法并传递应用程序的上下文来初始化 GTVoiceManager。
```kotlin
GTVoiceManager.init(context)
```
添加回调监听器：注册回调监听器以接收语音识别结果。您可以在初始化之前或之后添加监听器。

```kotlin
GTVoiceManager.addVoiceCallbackListener { state, data ->
    // 在此处处理语音识别结果和状态变化
}
```

添加关键词：添加自定义关键词以进行语音识别。每次添加关键词时，之前添加的关键词将被清除。

```kotlin
GTVoiceManager.addKeywords(listOf("关键词1", "关键词2"))
```
清除关键词：清除所有先前添加的关键词。

```kotlin
GTVoiceManager.clearKeywords()
```


反初始化：当不再需要 GTVoiceManager 时，调用 unInit() 进行反初始化。

```kotlin
GTVoiceManager.unInit(context)
```
注意事项
确保在不再需要 GTVoiceManager 时调用 unInit() 以释放资源并注销回调。
确保 VoiceDriver 版本为 1.0.66 或更高。
即使未反初始化，应用程序处于后台时也无法接收语音命令。
有关详细使用示例，请参考演示代码。


How to To get a Git project into your build:

Step 1. Add the JitPack repository to your build file

gradle
maven
sbt
leiningen
Add it in your root build.gradle at the end of repositories:

	dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url 'https://jitpack.io' }
		}
	}

Step 2. Add the dependency

	dependencies {
	        implementation 'com.github.AdamTaurus:GTVoice:Tag'
	}
