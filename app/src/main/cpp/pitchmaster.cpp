#include <jni.h>
#include <memory.h>
#include <functional>
#include "NativeCPPBridge.h"
#include "Log.h"


extern "C" {
    void checkNativeBridgeValidity(jobject thiz, std::function<void(pitchmaster::NativeCPPBridge*)> func);

    JNIEXPORT jlong JNICALL
    Java_com_example_pitchmasterbeta_bridge_NativeWaveBridge_create(JNIEnv *env, jobject thiz) {
        auto nativeBridge = std::make_unique<pitchmaster::NativeCPPBridge>();
        if (not nativeBridge) {
            LOGD("Failed to init nativeBridge");
            nativeBridge.reset(nullptr);
        }
        LOGD("invoked -> @create()");
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
        LOGD("invoked -> @delete()");
        delete nativeBridge;
    }

    JNIEXPORT void JNICALL
    Java_com_example_pitchmasterbeta_bridge_NativeWaveBridge_play(JNIEnv *env, jobject thiz, jlong bridge_native_handle) {
        checkNativeBridgeValidity(thiz, [&](pitchmaster::NativeCPPBridge* nativeBridge) {
            nativeBridge->play();
        });
    }

    JNIEXPORT jboolean JNICALL
    Java_com_example_pitchmasterbeta_bridge_NativeWaveBridge_isPlaying(JNIEnv *env, jobject thiz, jlong bridge_native_handle) {
        bool isPlaying = false;
        checkNativeBridgeValidity(thiz, [&](pitchmaster::NativeCPPBridge* nativeBridge) {
            isPlaying = nativeBridge->isPlaying();
        });
        return isPlaying;
    }

    JNIEXPORT void JNICALL
    Java_com_example_pitchmasterbeta_bridge_NativeWaveBridge_pause(JNIEnv *env, jobject thiz, jlong bridge_native_handle) {
        checkNativeBridgeValidity(thiz, [&](pitchmaster::NativeCPPBridge* nativeBridge) {
            return nativeBridge->pause();
        });
    }

    JNIEXPORT void JNICALL
    Java_com_example_pitchmasterbeta_bridge_NativeWaveBridge_stop(JNIEnv *env, jobject thiz, jlong bridge_native_handle) {
        checkNativeBridgeValidity(thiz, [&](pitchmaster::NativeCPPBridge* nativeBridge) {
            return nativeBridge->stop();
        });
    }

    JNIEXPORT void JNICALL
    Java_com_example_pitchmasterbeta_bridge_NativeWaveBridge_skipTo(JNIEnv *env, jobject thiz, jlong bridge_native_handle, jdouble jtimestamp) {
        const auto timestamp = static_cast<double >(jtimestamp);
        checkNativeBridgeValidity(thiz, [&](pitchmaster::NativeCPPBridge* nativeBridge) {
            return nativeBridge->skipTo(timestamp);
        });
    }

    JNIEXPORT void JNICALL
    Java_com_example_pitchmasterbeta_bridge_NativeWaveBridge_setPitchHeight(JNIEnv *env, jobject thiz, jlong bridge_native_handle, jfloat jfactor) {
        const auto factor = static_cast<float >(jfactor);
        checkNativeBridgeValidity(thiz, [&](pitchmaster::NativeCPPBridge* nativeBridge) {
            return nativeBridge->setPitchHeight(factor);
        });
    }

    JNIEXPORT void JNICALL
    Java_com_example_pitchmasterbeta_bridge_NativeWaveBridge_setVolume(JNIEnv *env, jobject thiz, jlong bridge_native_handle, jfloat jfactor) {
        const auto factor = static_cast<float >(jfactor);
        checkNativeBridgeValidity(thiz, [&](pitchmaster::NativeCPPBridge* nativeBridge) {
            return nativeBridge->setVolume(factor);
        });
    }

    JNIEXPORT void JNICALL
    Java_com_example_pitchmasterbeta_bridge_NativeWaveBridge_setMicEcho(JNIEnv *env, jobject thiz, jlong bridge_native_handle, jboolean jactive) {
        const auto active = static_cast<bool>(jactive);
        checkNativeBridgeValidity(thiz, [&](pitchmaster::NativeCPPBridge* nativeBridge) {
            return nativeBridge->setMicEcho(active);
        });
    }

    void checkNativeBridgeValidity(jobject thiz, std::function<void(pitchmaster::NativeCPPBridge*)> func) {
        auto* nativeBridge = reinterpret_cast<pitchmaster::NativeCPPBridge*>(thiz);
        if (!nativeBridge) {
            LOGD("NativeBridge not created. Please, create the synthesizer first by calling create().");
            return;
        }
        func(nativeBridge);
    }
}


