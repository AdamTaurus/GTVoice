package com.goolton.voicedemo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.goolton.gt_voice_sdk.GTVoiceManager
import com.goolton.voicedemo.ui.theme.VoiceDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VoiceDemoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VoiceTestScreen(this)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTestScreen(context: Context) {
    var result by remember { mutableStateOf("语音未初始化") }
    var keywordInput by remember { mutableStateOf("") } // 添加输入框内容的状态

    Column {
        TextField(
            value = keywordInput,
            onValueChange = { keywordInput = it },
            label = { Text("输入关键词") }
        )
        Button(onClick = {
            // 添加语音回调监听器
            GTVoiceManager.addVoiceCallbackListener { state, data ->
                result = when (state) {
                    GTVoiceManager.VoiceState.START -> "语音识别开始"
                    GTVoiceManager.VoiceState.STOP -> "语音识别结束"
                    GTVoiceManager.VoiceState.RESULT -> "语音识别结果：$data"
                    GTVoiceManager.VoiceState.ERROR -> "发生错误：$data"
                }
            }
            GTVoiceManager.init(context)
        }) {
            Text(text = "初始化SDK")
        }
        Button(onClick = { GTVoiceManager.addKeywords(listOf(keywordInput,"关键字二")) }) {
            Text(text = "添加关键词")
        }
        Button(onClick = { GTVoiceManager.clearKeywords() }) {
            Text(text = "清空关键词")
        }
        Button(onClick = { GTVoiceManager.unInit(context) }) {
            Text(text = "反初始化SDK")
        }
        Text(text = result)
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VoiceDemoTheme {
        VoiceTestScreen(LocalContext.current)
    }
}