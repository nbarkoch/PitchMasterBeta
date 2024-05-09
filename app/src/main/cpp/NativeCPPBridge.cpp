#include "include/NativeCPPBridge.h"
#include "include/Log.h"

namespace pitchmaster {

    void NativeCPPBridge::play() {
        LOGD("invoked -> play()");
        this->_isPlaying = true;
    }

    void NativeCPPBridge::pause() {
        LOGD("invoked -> pause()");
    }

    void NativeCPPBridge::stop() {
        LOGD("invoked -> stop()");
        this->_isPlaying = false;
    }

    bool NativeCPPBridge::isPlaying() {
        LOGD("invoked -> isPlaying()");
        return _isPlaying;
    }

    void NativeCPPBridge::skipTo(double timestamp) {
        LOGD("invoked -> skipTo(timestamp)");

    }

    void NativeCPPBridge::setPitchHeight(float factor) {
        LOGD("invoked -> setPitchHeight(factor)");
    }

    void NativeCPPBridge::setVolume(float factor) {
        LOGD("invoked -> setVolume(factor)");
    }

    void NativeCPPBridge::setMicEcho(bool active) {
        LOGD("invoked -> setMicEcho(active)");
    }
}