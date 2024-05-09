#pragma once

namespace pitchmaster {

    class NativeCPPBridge {
    public:
        void play();
        void pause();
        void stop();
        bool isPlaying();
        void skipTo(double timestamp);
        void setPitchHeight(float factor);
        void setVolume(float factor);
        void setMicEcho(bool active);
    private:
        bool _isPlaying;
    };

}
