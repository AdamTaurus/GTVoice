package com.goolton.gt_voice_sdk

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.goolton.gt_voice_sdk.vm.GTVoiceModel
import com.goolton.m_accessibility.IGTVoiceSDKAidlInterface
import com.goolton.m_accessibility.VoiceCallback
import java.lang.ref.SoftReference
import java.util.LinkedHashSet

/**
 * Description: 语音服务管理类
 * Author: Adam
 * Date: 2024/3/26
 *
 * 通过与语音服务通信添加 app 专属命令词。
 * 确保 VoiceDriver 版本大于等于：1.0.66
 * 当应用位于后台时无法接受语音命令，即使未逆初始化也是如此
 *
 * 使用方式：
 * 1. [addVoiceCallbackListener]
 * 2. [init]
 * 3. [addKeywords]
 * 完成后即可在命令词被识别到时收到回调
 * 用完后记得逆初始化，逆初始化后无论之前设置了多少个回调，都会被清空：
 * 4. [unInit]
 *
 * 更加详细使用代码请参考 demo
 */
object GTVoiceManager {

    const val error_code_sdk_uninitialized = -1
    const val error_code_service_unregistered = -2
    const val error_code_invalid_keywords = -3
    const val error_code_global_keywords_forbidden = -4

    enum class VoiceState {
        STOP, RESULT, ERROR, START
    }

    private val tag = this::class.simpleName
    private const val gtPkg = "com.goolton.voicedriver"
    private const val voiceSDKService = "com.goolton.m_accessibility.service.VoiceSDKService"

    private var isServiceBound = false
    private var contextHolder: SoftReference<Context> = SoftReference(null)
    private var binder: IGTVoiceSDKAidlInterface? = null
    private val model = GTVoiceModel()
    private var voiceServiceState = VoiceState.STOP
    private var lastStateData = ""

    // 保存当前期望的命令词状态，服务重连后会自动回放。
    private var pendingKeywords: List<String> = emptyList()
    private var hasPendingKeywords = false
    private var pendingViewIndexState: Boolean? = null

    /**
     * 语音状态回调
     */
    private val voiceCallbackList = LinkedHashSet<(state: VoiceState, data: String) -> Unit>()

    /**
     * 统一维护最近一次状态，便于新监听器立即同步当前结果。
     */
    private fun notifyCallbacks(state: VoiceState, data: String) {
        voiceServiceState = state
        lastStateData = data
        voiceCallbackList.toList().forEach { listener ->
            listener(state, data)
        }
    }

    private fun sanitizeKeywords(keywords: List<String>): List<String> {
        return LinkedHashSet<String>().apply {
            keywords.forEach { keyword ->
                val value = keyword.trim()
                if (value.isNotEmpty()) add(value)
            }
        }.toList()
    }

    /**
     * 服务重连后回放本地缓存状态，避免远端进程重启导致命令词丢失。
     */
    private fun flushPendingState() {
        val context = contextHolder.get() ?: return
        val remote = binder ?: return
        if (!isServiceBound) return

        pendingViewIndexState?.let { show ->
            try {
                remote.setViewIndexState(context.packageName, show)
            } catch (e: Exception) {
                Log.e(tag, "setViewIndexState flush error:${e.message}")
            }
        }

        if (hasPendingKeywords) {
            try {
                if (pendingKeywords.isEmpty()) {
                    remote.endSpeechRecognition(context.packageName)
                } else {
                    remote.startSpeechRecognition(context.packageName, pendingKeywords)
                }
            } catch (e: Exception) {
                Log.e(tag, "flushPendingState error:${e.message}")
            }
        }
    }

    private val connection by lazy {
        object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                binder = IGTVoiceSDKAidlInterface.Stub.asInterface(service)
                Log.i(tag, "服务绑定")
                val context = contextHolder.get()
                if (context != null) {
                    try {
                        isServiceBound = true
                        binder?.registerListener(context.packageName, callback)
                        flushPendingState()
                        if (voiceServiceState != VoiceState.START) {
                            notifyCallbacks(VoiceState.START, "")
                        }
                    } catch (e: Exception) {
                        isServiceBound = false
                        binder = null
                        Log.e(tag, "服务连接初始化失败:${e.message}")
                        callback.onError(error_code_service_unregistered)
                    }
                } else {
                    isServiceBound = false
                    binder = null
                    callback.onError(-1)
                }
            }

            override fun onServiceDisconnected(className: ComponentName) {
                isServiceBound = false
                binder = null
                Log.i(tag, "服务断开")
                if (voiceServiceState != VoiceState.STOP) {
                    notifyCallbacks(VoiceState.STOP, "")
                }
            }
        }
    }

    private val callback = object : VoiceCallback.Stub() {
        override fun onResult(result: String?) {
            Log.i(tag, "语音结果：$result")
            if (!result.isNullOrBlank()) notifyCallbacks(VoiceState.RESULT, result)
        }

        override fun onStart() {
            if (voiceServiceState != VoiceState.START) {
                notifyCallbacks(VoiceState.START, "")
            }
        }

        override fun onStop() {
            if (voiceServiceState != VoiceState.STOP) {
                notifyCallbacks(VoiceState.STOP, "")
            }
        }

        override fun onError(code: Int) {
            notifyCallbacks(VoiceState.ERROR, model.getCodeMessageByCode(code))
        }
    }

    /**
     * SDK 初始化，将绑定语音服务。
     */
    @Synchronized
    fun init(context: Context) {
        val appContext = context.applicationContext
        if (isServiceBound) {
            try {
                val boundContext = contextHolder.get() ?: appContext
                binder?.unregisterListener(boundContext.packageName, callback)
                boundContext.unbindService(connection)
            } catch (e: Exception) {
                Log.e(tag, "服务取消绑定异常：${e.message}")
            } finally {
                isServiceBound = false
                binder = null
            }
        }
        contextHolder = SoftReference(appContext)
        val intent = Intent().apply {
            component = ComponentName(gtPkg, voiceSDKService)
        }
        try {
            appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.e(tag, "init error:${e.message}")
            notifyCallbacks(VoiceState.ERROR, model.getCodeMessageByCode(error_code_service_unregistered))
        }
    }

    /**
     * 添加监听，可以在初始化前添加监听。
     */
    fun addVoiceCallbackListener(listener: (state: VoiceState, data: String) -> Unit) {
        voiceCallbackList.add(listener)
        if (voiceServiceState != VoiceState.STOP) {
            listener(voiceServiceState, lastStateData)
        }
    }

    /**
     * 移除监听
     */
    fun removeVoiceCallbackListener(listener: (state: VoiceState, data: String) -> Unit) {
        voiceCallbackList.remove(listener)
    }

    /**
     * 设置按钮序号状态，只在应用内生效。
     */
    fun setViewIndexState(show: Boolean) {
        pendingViewIndexState = show
        val context = contextHolder.get()
        val remote = binder
        if (isServiceBound && context != null && remote != null) {
            remote.setViewIndexState(context.packageName, show)
        } else {
            Log.i(tag, "setViewIndexState 已缓存，等待服务连接后下发")
        }
    }

    /**
     * SDK 逆初始化，主动清理当前包名下的命令词和监听。
     */
    @Synchronized
    fun unInit(context: Context) {
        val appContext = contextHolder.get() ?: context.applicationContext
        if (isServiceBound) {
            try {
                binder?.endSpeechRecognition(appContext.packageName)
                binder?.unregisterListener(appContext.packageName, callback)
                appContext.unbindService(connection)
            } catch (e: Exception) {
                Log.e(tag, "服务解绑异常：${e.message}")
                callback.onError(error_code_service_unregistered)
            } finally {
                isServiceBound = false
                binder = null
            }
        } else {
            Log.w(tag, "SDK已释放，无需重复释放")
        }
        pendingKeywords = emptyList()
        hasPendingKeywords = false
        pendingViewIndexState = null
        lastStateData = ""
        voiceCallbackList.clear()
        voiceServiceState = VoiceState.STOP
    }

    /**
     * 添加命令词，每次新增会覆盖上次添加的命令词。
     */
    fun addKeywords(keywords: List<String>) {
        val sanitized = sanitizeKeywords(keywords)
        pendingKeywords = sanitized
        hasPendingKeywords = true
        val context = contextHolder.get()
        val remote = binder
        if (isServiceBound && context != null && remote != null) {
            if (sanitized.isEmpty()) {
                Log.w(tag, "不能设置空字符串为命令词")
            } else {
                remote.startSpeechRecognition(context.packageName, sanitized)
            }
        } else {
            Log.i(tag, "命令词已缓存，等待服务连接后下发")
        }
    }

    /**
     * 清空命令词
     */
    fun clearKeywords() {
        pendingKeywords = emptyList()
        hasPendingKeywords = true
        val context = contextHolder.get()
        val remote = binder
        if (isServiceBound && context != null && remote != null) {
            remote.endSpeechRecognition(context.packageName)
        } else {
            Log.i(tag, "清空命令词请求已缓存，等待服务连接后下发")
        }
    }

    /**
     * 便捷方法：根据错误码获取错误详情。
     */
    fun getErrorDetail(code: Int): GTVoiceErrorDetail {
        return GTVoiceErrorUtils.getErrorDetail(code)
    }

    /**
     * 便捷方法：根据错误码获取错误文案。
     */
    fun getErrorMessage(code: Int): String {
        return GTVoiceErrorUtils.getErrorMessage(code)
    }
}
