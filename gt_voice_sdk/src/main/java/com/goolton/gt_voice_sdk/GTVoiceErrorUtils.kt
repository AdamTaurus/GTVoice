package com.goolton.gt_voice_sdk

/**
 * SDK 错误详情。
 */
data class GTVoiceErrorDetail(
    val code: Int,
    val title: String,
    val message: String,
    val suggestion: String
)

/**
 * 根据错误码获取可展示的错误详情，供接入方统一处理。
 */
object GTVoiceErrorUtils {

    private const val inactivated = 18007

    @JvmStatic
    fun getErrorDetail(code: Int): GTVoiceErrorDetail {
        return when (code) {
            inactivated -> GTVoiceErrorDetail(
                code = code,
                title = "语音服务未激活",
                message = "语音服务未激活：$code",
                suggestion = "请先确认 VoiceDriver 已激活并具备运行条件。"
            )

            GTVoiceManager.error_code_sdk_uninitialized -> GTVoiceErrorDetail(
                code = code,
                title = "SDK未初始化",
                message = "SDK未初始化：$code",
                suggestion = "请先调用 GTVoiceManager.init(context) 再进行后续操作。"
            )

            GTVoiceManager.error_code_service_unregistered -> GTVoiceErrorDetail(
                code = code,
                title = "语音服务未连接",
                message = "语音服务未连接：$code",
                suggestion = "请确认 VoiceDriver 已安装、已启动，并检查服务绑定是否成功。"
            )

            GTVoiceManager.error_code_invalid_keywords -> GTVoiceErrorDetail(
                code = code,
                title = "命令词无效",
                message = "命令词设置失败：命令词为空或全部无效：$code",
                suggestion = "请传入非空命令词，并避免只传空白字符。"
            )

            GTVoiceManager.error_code_global_keywords_forbidden -> GTVoiceErrorDetail(
                code = code,
                title = "命令词冲突",
                message = "命令词设置失败：不允许注册全局命令词：$code",
                suggestion = "请改用应用内专属命令词，避免使用回到主页、上一页等全局命令。"
            )

            else -> GTVoiceErrorDetail(
                code = code,
                title = "语音服务异常",
                message = "语音服务异常：$code",
                suggestion = "请记录错误码并联系语音服务提供方排查。"
            )
        }
    }

    @JvmStatic
    fun getErrorMessage(code: Int): String = getErrorDetail(code).message
}
