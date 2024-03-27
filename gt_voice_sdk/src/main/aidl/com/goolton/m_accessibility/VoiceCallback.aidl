package com.goolton.m_accessibility;

interface VoiceCallback {
    void onResult(String result);

    void onStart();

    void onStop();

    void onError(int code);
}
