package com.goolton.gt_voice_sdk.vm

/**
 * Description: 简介
 * Author: Adam
 * Date: 2024/3/26
 */
class GTVoiceModel {
    private val inactivated = 18007


    fun getCodeMessageByCode(code:Int):String{
        return when(code){
            inactivated-> "语音服务未激活：$code"
            -1-> "SDK未初始化：$code"
            else -> "语音服务异常：$code"
        }
    }
}