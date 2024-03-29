package com.goolton.voicedemo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.goolton.gt_voice_sdk.GTVoiceManager;

import java.util.Arrays;

public class VoiceTestActivityJava extends Activity {

    private static final String TAG = VoiceTestActivityJava.class.getSimpleName();

    private TextView textView;
    private Button button1;
    private Button button2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        setContentView(layout);

        // 添加 TextView
        textView = new TextView(this);
        layout.addView(textView);

        // 添加按钮1
        button1 = new Button(this);
        button1.setText("开启角标");
        button1.setOnClickListener(view -> {
            // 开启角标
            GTVoiceManager.INSTANCE.setViewIndexState(true);
        });
        layout.addView(button1);

        // 添加按钮2
        button2 = new Button(this);
        button2.setText("隐藏角标");
        button2.setOnClickListener(view -> {
            // 隐藏角标
            GTVoiceManager.INSTANCE.setViewIndexState(false);
        });
        layout.addView(button2);

        // 添加语音回调监听
        GTVoiceManager.INSTANCE.addVoiceCallbackListener((state, data) -> {
            Log.i(TAG, "语音状态：" + state + " 识别结果回调：" + data);

            runOnUiThread(() -> {
                switch (state) {
                    case START:
                        // 语音识别开始
                        textView.setText("语音已初始化");
                        break;
                    case RESULT:
                        // 语音识别结果
                        textView.setText(data);
                        break;
                    case ERROR:
                        // 语音识别错误
                        textView.setText("语音异常：" + data);
                        break;
                    case STOP:
                        // 语音识别结束
                        textView.setText("语音关闭");
                        break;
                }
            });

            return null;
        });

        // 初始化 GTVoiceManager
        GTVoiceManager.INSTANCE.init(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放 GTVoiceManager
        GTVoiceManager.INSTANCE.unInit(this);
    }
}
