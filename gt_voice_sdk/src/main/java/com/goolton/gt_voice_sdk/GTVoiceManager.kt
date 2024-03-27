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

/**
 * Description: 语音服务管理类
 * Author: Adam
 * Date: 2024/3/26
 */
object GTVoiceManager {
    enum class VoiceState {
        STOP, RESULT, ERROR, START
    }

    private val tag = this::class.simpleName
    private const val gtPkg = "com.goolton.voicedriver"
    private const val voiceSDKService = "com.goolton.m_accessibility.service.VoiceSDKService"
    private var isServiceBound = false  //是否绑定
    private var contextHolder: SoftReference<Context> = SoftReference(null)
    private lateinit var binder: IGTVoiceSDKAidlInterface
    private val model = GTVoiceModel()

    /**
     * 语音状态回调
     */
    private val voiceCallbackList = HashSet<((state: VoiceState, data: String) -> Unit)>()

    private val connection by lazy {
        object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                // 当服务绑定成功时调用此方法
                binder = IGTVoiceSDKAidlInterface.Stub.asInterface(service)
                Log.i(tag, "服务绑定")
                val context = contextHolder.get()
                if (context != null) {
                    isServiceBound = true
                    binder.registerListener(context.packageName, callback)
                } else {
                    isServiceBound = false
                    callback.onError(-1)
                }
            }

            override fun onServiceDisconnected(className: ComponentName) {
                // 当服务与客户端断开连接时调用此方法
                isServiceBound = false
                Log.i(tag, "服务断开")
            }
        }
    }

    private val callback = object : VoiceCallback.Stub() {
        override fun onResult(result: String?) {
            Log.i(tag, "语音结果：$result")
            if (!result.isNullOrBlank()) voiceCallbackList.forEach {
                it(VoiceState.RESULT, result)
            }
        }

        override fun onStart() {
            voiceCallbackList.forEach {
                it(VoiceState.START, "")
            }
        }

        override fun onStop() {
            voiceCallbackList.forEach {
                it(VoiceState.STOP, "")
            }
        }

        override fun onError(code: Int) {
            voiceCallbackList.forEach {
                it(VoiceState.ERROR, model.getCodeMessageByCode(code))
            }
        }

    }

    /**
     * SDK 初始化 所有调用都需要在初始化之后
     */
    @Synchronized
    fun init(context: Context) {
        if (isServiceBound) return
        contextHolder = SoftReference(context.applicationContext)
        val intent = Intent()
        intent.component = ComponentName(gtPkg, voiceSDKService)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    /**
     * 添加监听
     */
    fun addVoiceCallbackListener(listener: (state: VoiceState, data: String) -> Unit) {
        voiceCallbackList.add(listener)
    }

    /**
     * 移除监听
     */
    fun removeVoiceCallbackListener(listener: (state: VoiceState, data: String) -> Unit) {
        voiceCallbackList.remove(listener)
    }

    /**
     * SDK 逆初始化
     */
    @Synchronized
    fun unInit(context: Context) {
        if (isServiceBound) {
            try {
                binder.unregisterListener(context.packageName, callback)
                context.unbindService(connection)
                isServiceBound = false
                callback.onStop()
            } catch (e: Exception) {
                Log.e(tag, "服务解绑异常：${e.message}")
                e.printStackTrace()
            }
        }else{
            Log.w(tag,"SDK已释放，无需重复释放")
        }
    }

    /**
     * 添加命令词
     */
    fun addKeywords(keywords: List<String>) {
        val context = contextHolder.get()
        if (isServiceBound && context != null) {
            binder.startSpeechRecognition(context.packageName, keywords)
        } else {
            Log.e(tag, "命令设置失败，服务是否连接：$isServiceBound SDK是否初始化：${context != null}")
        }
    }

    /**
     * 清空命令词
     */
    fun clearKeywords() {
        val context = contextHolder.get()
        if (isServiceBound && context != null) {
            binder.endSpeechRecognition(context.packageName)
        } else {
            Log.e(tag, "命令设置失败，服务是否连接：$isServiceBound SDK是否初始化：${context != null}")
        }
    }
}