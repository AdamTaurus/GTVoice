# GTVoice  

该项目为通过aidl与VoiceDrive通信实现命令词设置和回调的演示项目，直接引用aar包或model：gt_voice_sdk，暂时无法通过implementation 'com.github.AdamTaurus:GTVoice:Tag'引用
  
# 注意⚠️  
    
如果只是想通过语音来进行交互，只需要按照触摸交互设计就可以，无需接入SDK，语音会根据控件能否点击，
是否显示，以及控件及其子控件的文字来自动生成命令词。同时语音能通过contentDescription属性来识别当前界面上支持的命令词

##  contentDescription属性配置说明
 
例子：
>android:contentDescription="命令词"


如果不需要识别该控件命令词：
> android:contentDescription="gtKeyword_unsupported"

该属性的作用主要是用于某些不希望被识别的控件，例如图片的名字这种无规律字符串，
即使设为关键字也难以识别，或者其它中英文混合的文字。

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

显示和隐藏按钮角标：

```kotlin
GTVoiceManager.setViewIndexState(true)
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
