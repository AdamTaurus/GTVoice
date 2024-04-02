package com.goolton.voicedemo

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.goolton.gt_voice_sdk.GTVoiceManager

class VoiceTestActivity : Activity() {

    private val tag = this::class.simpleName

    private lateinit var textView: TextView
    private lateinit var button1: Button
    private lateinit var button2: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        setContentView(layout)

        // 添加 TextView
        textView = TextView(this)
        layout.addView(textView)
        button1 = Button(this)
        button1.text = "添加指令"
        button1.setOnClickListener {
            // 添加命令词
            GTVoiceManager.addKeywords(listOf("指令一", "指令二"))
            Toast.makeText(this,"添加了关键词：指令一、指令二",Toast.LENGTH_SHORT).show()
        }
        layout.addView(button1)
        button2 = Button(this)
        button2.text = "移除指令"
        button2.setOnClickListener {
            //清空命令词
            GTVoiceManager.clearKeywords()
        }
        layout.addView(button2)

        // 添加语音回调监听
        GTVoiceManager.addVoiceCallbackListener { state, data ->
            Log.i(tag, "语音状态：$state 识别结果回调：$data")
            runOnUiThread {
                textView.text = when (state) {
                    GTVoiceManager.VoiceState.START -> {
                        // 语音识别开始
                        // 可以在这里处理开始识别的逻辑
                        "语音已初始化"
                    }

                    GTVoiceManager.VoiceState.RESULT -> {
                        // 语音识别结果
                        // 可以在这里处理识别结果的逻辑
                        data
                    }

                    GTVoiceManager.VoiceState.ERROR -> {
                        // 语音识别错误
                        // 可以在这里处理识别错误的逻辑
                        "语音异常：$data"
                    }

                    GTVoiceManager.VoiceState.STOP -> {
                        // 语音识别结束
                        // 可以在这里处理识别结束的逻辑
                        "语音关闭"
                    }
                }
            }
        }

        // 初始化 GTVoiceManager
        GTVoiceManager.init(this)
    }

    override fun onPause() {
        super.onPause()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 释放 GTVoiceManager
        GTVoiceManager.unInit(this)
    }
}
