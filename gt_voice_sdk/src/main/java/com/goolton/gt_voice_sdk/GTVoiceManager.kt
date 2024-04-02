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
 *
 * 通过与语音服务通信添加app专属命令词
 * 确保VoiceDriver版本大于等于：1.0.66
 * 当应用位于后台时无法接受语音命令，即使未逆初始化也是如此
 *
 * 使用方式：
 * 1. [addVoiceCallbackListener]
 * 2. [init]
 * 3. [addKeywords]
 * 完成后即可在命令词被识别到时收到回调
 * 用完后记得逆初始化，逆初始化后无论之前设置了多少个回调，都会被清空：
 * 4.[until]
 *
 * 更加详细使用代码请参考demo
 */
object GTVoiceManager {

    const val error_code_service_unregistered = -2

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
    private var voiceServiceState = VoiceState.STOP

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
                    if (voiceServiceState != VoiceState.START) {
                        voiceCallbackList.forEach {
                            voiceServiceState = VoiceState.START
                            it(VoiceState.START, "")
                        }
                    }
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
            if (voiceServiceState != VoiceState.START) {
                voiceServiceState = VoiceState.START
                voiceCallbackList.forEach {
                    it(VoiceState.START, "")
                }
            }
        }

        override fun onStop() {
            if (voiceServiceState != VoiceState.STOP) {
                voiceServiceState = VoiceState.STOP
                voiceCallbackList.forEach {
                    it(VoiceState.STOP, "")
                }
            }
        }

        override fun onError(code: Int) {
            voiceServiceState = VoiceState.ERROR
            voiceCallbackList.forEach {
                it(VoiceState.ERROR, model.getCodeMessageByCode(code))
            }
        }

    }

    /**
     * SDK 初始化 将绑定语音服务
     */
    @Synchronized
    fun init(context: Context) {
        if (isServiceBound){
            try {
                binder.unregisterListener(context.packageName, callback)
                context.unbindService(connection)
            }catch (e:Exception){
                Log.e(tag,"服务取消绑定异常：${e.message}")
            }
        }
        contextHolder = SoftReference(context.applicationContext)
        val intent = Intent()
        intent.component = ComponentName(gtPkg, voiceSDKService)
        try {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.e(tag, "init error:${e.message}")
        }
    }

    /**
     * 添加监听
     * 可以在初始化前添加监听
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
     * 设置按钮序号状态
     * @param show
     * true：显示
     * false:隐藏
     * 只在应用内生效
     */
    fun setViewIndexState(show: Boolean) {
        val context = contextHolder.get()
        if (isServiceBound && context != null) {
            binder.setViewIndexState(context.packageName, show)
        } else {
            Log.e(
                tag,
                "setViewIndexState 错误 请检查SDK是否初始化：${context != null} ServiceBound:$isServiceBound"
            )
        }
    }

    /**
     * SDK 逆初始化
     */
    @Synchronized
    fun unInit(context: Context) {
        if (isServiceBound) {
            try {
                voiceServiceState = VoiceState.STOP
                voiceCallbackList.clear()
                binder.unregisterListener(context.packageName, callback)
                context.unbindService(connection)
                isServiceBound = false
                callback.onStop()
            } catch (e: Exception) {
                Log.e(tag, "服务解绑异常：${e.message}")
                e.printStackTrace()
                isServiceBound = false
                callback.onError(error_code_service_unregistered)
            }
        } else {
            Log.w(tag, "SDK已释放，无需重复释放")
        }
    }

    /**
     * 添加命令词
     * 每次新增会清空上次添加的命令词
     * 英文下只支持纯英文，中文时只支持纯中文
     */
    fun addKeywords(keywords: List<String>) {
        val context = contextHolder.get()
        if (isServiceBound && context != null) {
            if (keywords.contains("")){
                Log.w(tag,"不能设置空字符串为命令词")
            }else {
                binder.startSpeechRecognition(context.packageName, keywords)
            }
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