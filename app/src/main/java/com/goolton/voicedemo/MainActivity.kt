package com.goolton.voicedemo

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

    override fun onBackPressed() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        GTVoiceManager.unInit(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTestScreen(context: Context) {
    val appContext = context.applicationContext
    var statusText by remember { mutableStateOf("等待初始化，建议先注册回调再调用 init(context)。") }
    var keywordInput by remember { mutableStateOf("打开订单,关闭订单") }
    var indexVisible by remember { mutableStateOf(true) }
    var listenerRegistered by remember { mutableStateOf(false) }
    val currentContext by rememberUpdatedState(appContext)

    val voiceCallback: (GTVoiceManager.VoiceState, String) -> Unit = remember {
        { state: GTVoiceManager.VoiceState, data: String ->
            statusText = when (state) {
                GTVoiceManager.VoiceState.START -> "服务已连接，语音识别开始。"
                GTVoiceManager.VoiceState.STOP -> "服务已断开，语音识别结束。"
                GTVoiceManager.VoiceState.RESULT -> "命中命令词：$data"
                GTVoiceManager.VoiceState.ERROR -> "发生错误：$data"
            }
            Log.i("语音回调", statusText)
        }
    }

    DisposableEffect(voiceCallback) {
        onDispose {
            GTVoiceManager.removeVoiceCallbackListener(voiceCallback)
        }
    }

    fun ensureListenerRegistered() {
        if (!listenerRegistered) {
            GTVoiceManager.addVoiceCallbackListener(voiceCallback)
            listenerRegistered = true
            statusText = "回调已注册，可继续初始化 SDK。"
        }
    }

    fun updateViewIndex(show: Boolean) {
        ensureListenerRegistered()
        indexVisible = show
        GTVoiceManager.setViewIndexState(show)
        statusText = if (show) {
            "角标显示状态已缓存，服务连接成功后会自动下发。"
        } else {
            "角标隐藏状态已缓存，服务连接成功后会自动下发。"
        }
    }

    val keywords = remember(keywordInput) { parseKeywords(keywordInput) }

    fun initSdk() {
        ensureListenerRegistered()
        GTVoiceManager.init(currentContext)
        statusText = "正在初始化 SDK，等待语音服务连接..."
    }

    fun submitKeywords() {
        ensureListenerRegistered()
        if (keywords.isEmpty()) {
            statusText = GTVoiceManager.getErrorDetail(
                GTVoiceManager.error_code_invalid_keywords
            ).message
            Toast.makeText(currentContext, "请先输入至少一个命令词", Toast.LENGTH_SHORT).show()
            return
        }
        GTVoiceManager.addKeywords(keywords)
        statusText = "已提交命令词：${keywords.joinToString("、")}"
        Toast.makeText(
            currentContext,
            "命令词已下发：${keywords.joinToString("、")}",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun clearKeywords() {
        ensureListenerRegistered()
        GTVoiceManager.clearKeywords()
        statusText = "命令词已清空。"
        Toast.makeText(currentContext, "命令词已清空", Toast.LENGTH_SHORT).show()
    }

    fun releaseSdk() {
        GTVoiceManager.unInit(currentContext)
        listenerRegistered = false
        statusText = "SDK 已释放，命令词与回调均已清空。"
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val isLandscapeLayout = maxWidth > maxHeight

        if (isLandscapeLayout) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                VoiceInfoPanel(
                    keywordInput = keywordInput,
                    onKeywordChange = { keywordInput = it },
                    statusText = statusText,
                    keywords = keywords,
                    indexVisible = indexVisible,
                    modifier = Modifier
                        .fillMaxWidth(0.48f)
                        .verticalScroll(rememberScrollState())
                )
                VoiceActionPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    onInitClick = ::initSdk,
                    onAddKeywordsClick = ::submitKeywords,
                    onClearKeywordsClick = ::clearKeywords,
                    onShowIndexClick = { updateViewIndex(true) },
                    onHideIndexClick = { updateViewIndex(false) },
                    onUnInitClick = ::releaseSdk
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                VoiceInfoPanel(
                    keywordInput = keywordInput,
                    onKeywordChange = { keywordInput = it },
                    statusText = statusText,
                    keywords = keywords,
                    indexVisible = indexVisible,
                    modifier = Modifier.fillMaxWidth()
                )
                VoiceActionPanel(
                    modifier = Modifier.fillMaxWidth(),
                    onInitClick = ::initSdk,
                    onAddKeywordsClick = ::submitKeywords,
                    onClearKeywordsClick = ::clearKeywords,
                    onShowIndexClick = { updateViewIndex(true) },
                    onHideIndexClick = { updateViewIndex(false) },
                    onUnInitClick = ::releaseSdk
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceInfoPanel(
    keywordInput: String,
    onKeywordChange: (String) -> Unit,
    statusText: String,
    keywords: List<String>,
    indexVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "GT Voice SDK Demo", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "推荐顺序：addVoiceCallbackListener -> init(context) -> addKeywords(...)",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = keywordInput,
                onValueChange = onKeywordChange,
                label = { Text("输入命令词，支持逗号或换行分隔") },
                singleLine = false,
                maxLines = 4
            )
            Text(
                text = if (keywords.isEmpty()) {
                    "当前命令词：未设置"
                } else {
                    "当前命令词：${keywords.joinToString("、")}"
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "角标状态：${if (indexVisible) "显示" else "隐藏"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(text = "最近状态：$statusText", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun VoiceActionPanel(
    onInitClick: () -> Unit,
    onAddKeywordsClick: () -> Unit,
    onClearKeywordsClick: () -> Unit,
    onShowIndexClick: () -> Unit,
    onHideIndexClick: () -> Unit,
    onUnInitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "操作面板", style = MaterialTheme.typography.headlineSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VoiceActionButton(
                    text = "初始化 SDK",
                    onClick = onInitClick,
                    modifier = Modifier.fillMaxWidth(0.48f)
                )
                VoiceActionButton(
                    text = "下发命令词",
                    onClick = onAddKeywordsClick,
                    modifier = Modifier.fillMaxWidth(0.48f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VoiceActionButton(
                    text = "清空命令词",
                    onClick = onClearKeywordsClick,
                    modifier = Modifier.fillMaxWidth(0.48f)
                )
                VoiceActionButton(
                    text = "释放 SDK",
                    onClick = onUnInitClick,
                    modifier = Modifier.fillMaxWidth(0.48f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "角标控制", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VoiceActionButton(
                    text = "显示角标",
                    onClick = onShowIndexClick,
                    modifier = Modifier.fillMaxWidth(0.48f)
                )
                VoiceActionButton(
                    text = "隐藏角标",
                    onClick = onHideIndexClick,
                    modifier = Modifier.fillMaxWidth(0.48f)
                )
            }
        }
    }
}

@Composable
private fun VoiceActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp)
    ) {
        Text(text = text)
    }
}

private fun parseKeywords(input: String): List<String> {
    return input
        .split(',', '，', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VoiceDemoTheme {
        VoiceTestScreen(LocalContext.current)
    }
}