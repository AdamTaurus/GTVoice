package com.goolton.gt_voice_sdk.vm

import com.goolton.gt_voice_sdk.GTVoiceErrorUtils

/**
 * Description: 简介
 * Author: Adam
 * Date: 2024/3/26
 */
class GTVoiceModel {
    fun getCodeMessageByCode(code:Int):String{
        return GTVoiceErrorUtils.getErrorMessage(code)
    }
}
