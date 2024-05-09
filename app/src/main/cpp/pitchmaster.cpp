#include <jni.h>
#include <memory.h>
#include <functional>
#include "NativeCPPBridge.h"
#include "Log.h"


extern "C" {
    void getActiveNativeBridge(jobject thiz, std::function<void(pitchmaster::NativeCPPBridge*)> func);

    JNIEXPORT jlong JNICALL
    Java_com_example_pitchmasterbeta_bridge_NativeWaveBridge_create(JNIEnv *env, jobject thiz) {
        auto nativeBridge = std::make_unique<pitchmaster::NativeCPPBridge>();
        if (not nativeBridge) {
            LOGD("Failed to init nativeBridge");
            nativeBridge.reset(nullptr);
        }
        return reinterpret_cast<jlong>(nativeBridge.release());
    }
    
    JNIEXPORT void JNICALL
    Java_com_example_pitchmasterbeta_bridge_NativeWaveBridge_delete(JNIEnv *env, jobject thiz,jlong bridge_native_handle) {
        // get nativeBridge instance
        auto* nativeBridge = reinterpret_cast<pitchmaster::NativeCPPBridge*>(thiz);
        if (not nativeBridge) {
            LOGD("Attempt to destroy uninitialized nativeBridge");
            return;
        }
        delete nativeBridge;
    }

    JNIEXPORT void JNICALL
    Java_com_example_pitchmasterbeta_bridge_NativeWaveBridge_play(JNIEnv *env, jobject thiz,
                                                                  jlong bridge_native_handle) {
        auto* nativeBridge = reinterpret_cast<pitchmaster::NativeCPPBridge*>(thiz);

        if (nativeBridge) {
            nativeBridge->play();
        } else {
            LOGD("NativeBridge not created. Please, create the synthesizer first by "
                  "calling create().");
        }
    }

    JNIEXPORT jboolean JNICALL
    Java_com_example_pitchmasterbeta_bridge_NativeWaveBridge_isPlaying(JNIEnv *env, jobject thiz,
                                                                       jlong bridge_native_handle) {
        auto* nativeBridge = reinterpret_cast<pitchmaster::NativeCPPBridge*>(thiz);

        if (not nativeBridge) {
            LOGD("NativeBridge not created. Please, create the synthesizer first by "
                 "calling create().");
            return false;
        }

        return nativeBridge->isPlaying();
    }

    JNIEXPORT void JNICALL
    Java_com_example_pitchmasterbeta_bridge_NativeWaveBridge_pause(JNIEnv *env, jobject thiz,
                                                                   jlong bridge_native_handle) {
        auto* nativeBridge = reinterpret_cast<pitchmaster::NativeCPPBridge*>(thiz);

        if (nativeBridge) {
            nativeBridge->pause();
        } else {
            LOGD("NativeBridge not created. Please, create the synthesizer first by "
                 "calling create().");
        }
    }

    JNIEXPORT void JNICALL
    Java_com_example_pitchmasterbeta_bridge_NativeWaveBridge_stop(JNIEnv *env, jobject thiz,
                                                                  jlong bridge_native_handle) {
        auto *nativeBridge = reinterpret_cast<pitchmaster::NativeCPPBridge *>(thiz);

        if (nativeBridge) {
            nativeBridge->stop();
        } else {
            LOGD("NativeBridge not created. Please, create the synthesizer first by "
                 "calling create().");
        }
    }

    JNIEXPORT void JNICALL
    Java_com_example_pitchmasterbeta_bridge_NativeWaveBridge_skipTo(JNIEnv *env, jobject thiz,
                                                                    jlong bridge_native_handle,
                                                                    jdouble jtimestamp) {
        auto *nativeBridge = reinterpret_cast<pitchmaster::NativeCPPBridge *>(thiz);
        const auto timestamp = static_cast<double >(jtimestamp);
        if (nativeBridge) {
            nativeBridge->skipTo(timestamp);
        } else {
            LOGD("NativeBridge not created. Please, create the synthesizer first by "
                 "calling create().");
        }
    }

    JNIEXPORT void JNICALL
    Java_com_example_pitchmasterbeta_bridge_NativeWaveBridge_setPitchHeight(JNIEnv *env, jobject thiz,
                                                                            jlong bridge_native_handle,
                                                                            jfloat jfactor) {
        const auto factor = static_cast<float >(jfactor);
        getActiveNativeBridge(thiz, [factor](pitchmaster::NativeCPPBridge* nativeBridge) {
            nativeBridge->setPitchHeight(factor);
        });
    }

    JNIEXPORT void JNICALL
    Java_com_example_pitchmasterbeta_bridge_NativeWaveBridge_setVolume(JNIEnv *env, jobject thiz,
                                                                       jlong bridge_native_handle,
                                                                       jfloat jfactor) {
        const auto factor = static_cast<float >(jfactor);
        getActiveNativeBridge(thiz, [factor](pitchmaster::NativeCPPBridge* nativeBridge) {
            nativeBridge->setVolume(factor);
        });
    }

    JNIEXPORT void JNICALL
    Java_com_example_pitchmasterbeta_bridge_NativeWaveBridge_setMicEcho(JNIEnv *env, jobject thiz,
                                                                        jlong bridge_native_handle,
                                                                        jboolean jactive) {
        const auto active = static_cast<bool>(jactive);
        getActiveNativeBridge(thiz, [active](pitchmaster::NativeCPPBridge* nativeBridge) {
            nativeBridge->setMicEcho(active);
        });
    }

    void getActiveNativeBridge(jobject thiz, std::function<void(pitchmaster::NativeCPPBridge*)> func) {
        auto *nativeBridge = reinterpret_cast<pitchmaster::NativeCPPBridge *>(thiz);
        if (nativeBridge) {
            func(nativeBridge);
        } else {
            LOGD("NativeBridge not created. Please, create the synthesizer first by calling create().");
        }
    }
}


