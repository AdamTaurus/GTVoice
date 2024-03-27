package com.goolton.m_accessibility;

import com.goolton.m_accessibility.VoiceCallback;
import java.util.List;

/**
*  语音SDK通讯处理
*/
interface IGTVoiceSDKAidlInterface {

      /**
      * 添加命令词
      * @prame key 可以是包名或页面名
      */
      void startSpeechRecognition(String packageName,in List<String> list);

      /**
      * 设置按钮角标状态
      */
      void setViewIndexState(String packageName,boolean state);

      /**
      * 结束语音
      */
      void endSpeechRecognition(String packageName);

      void registerListener(String packageName,VoiceCallback callback);
      void unregisterListener(String packageName,VoiceCallback callback);
}