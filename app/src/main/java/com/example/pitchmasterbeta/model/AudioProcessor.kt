package com.example.pitchmasterbeta.model

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchDetectionResult
import be.tarsos.dsp.pitch.PitchProcessor
import com.example.pitchmasterbeta.model.PitchSoundPlayer.Companion.processPitchHeavy
import com.example.pitchmasterbeta.model.PitchSoundPlayer.Companion.sortedNotes
import com.example.pitchmasterbeta.utils.math.shiftArrayRight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.math.sqrt


class AudioProcessor {

    enum class NotesSimilarity {
        Wrong, Neutral, Idle, Close, Equal
    }

    constructor(mediaInfo: MediaInfo) {
        this.mediaInfo = mediaInfo
        playSound = PitchSoundPlayer(mediaInfo.voiceSampleRate, 1)
    }

    private val mediaInfo: MediaInfo

    private var microphoneDispatcher: VocalAudioDispatcher? = null
    private var musicDispatcher: SongAudioDispatcher? = null
    private var mainAudioTrack: AudioTrack? = null
    var volumeFactor: Float = 0f
    var computeAndPlaySingerSoundMode: Boolean = false
    var computeAndPlayRecordedSoundMode: Boolean = false
    var generatedSingerByteSound: ByteArray = byteArrayOf()
    var generatedRecordByteSound: ByteArray = byteArrayOf()

    private val playSound: PitchSoundPlayer

    var micTimeStamp = 0.0
    var songTimeStamp = 0.0
    var timeStampDuration = 0.0

    var singNoteI = 0
    var micNoteI = 0
    var singVolume = 0
    var micVolume = 0

    var singerCurrentPitches = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0)
    var micCurrentPitches = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0)

    companion object {
        const val SAMPLE_THRESHOLD = 255.toByte()

        fun soundVolume(audioBuffer: FloatArray): Int {
            var sumOfSquares = 0.0
            for (sample in audioBuffer) {
                sumOfSquares += (sample * sample).toDouble()
            }
            val meanSquare = sumOfSquares / audioBuffer.size
            return 100.0.coerceAtMost(0.0.coerceAtLeast(100.0 * sqrt(meanSquare))).toInt()
        }

        private fun compareHeavy(singerHZs: IntArray, speakerHZs: IntArray): NotesSimilarity {
            var maxVal = -2
            for (singerHZ in singerHZs) {
                for (speakerHZ in speakerHZs) {
                    if (maxVal < 0 && (singerHZ == 0 || speakerHZ == 0 || singerHZ == PitchSoundPlayer.sortedNotes.size - 1 || speakerHZ == PitchSoundPlayer.sortedNotes.size - 1)) {
                        return NotesSimilarity.Idle
                    } else {
                        if (Math.abs(speakerHZ - singerHZ) % 12 == 0) {
                            if (speakerHZ == singerHZ) {
                                return NotesSimilarity.Equal
                            }
                            maxVal = 1
                        } else if (Math.abs(speakerHZ - singerHZ) % 12 < 2) {
                            maxVal = 1
                        }
                    }
                }
            }
            return NotesSimilarity.values()[maxVal + 2]
        }

        private fun mixedBuffers(
            buffer1: ByteArray,
            buffer2: ByteArray,
            buffer3: ByteArray
        ): ByteArray {
            val length = buffer1.size.coerceAtMost(buffer2.size.coerceAtMost(buffer3.size))
            val mixedBuffer = ByteArray(length)

            val maxSample = SAMPLE_THRESHOLD - Byte.MIN_VALUE
            for (i in 0 until length) {
                var mixedSample = buffer1[i] + buffer2[i] + buffer3[i]
                if (mixedSample > maxSample) {
                    mixedSample = maxSample
                } else if (mixedSample < -maxSample) {
                    mixedSample = -maxSample
                }
                mixedBuffer[i] = mixedSample.toByte()
            }
            return mixedBuffer
        }

        fun adjustVolume(audioBuffer: ByteArray, volume: Float): ByteArray {
            var i = 0
            while (i < audioBuffer.size) {
                // Convert bytes to short samples
                var sample =
                    (audioBuffer[i].toInt() and 0xff or (audioBuffer[i + 1].toInt() shl 8)).toShort()
                // Apply volume adjustment
                sample = (sample * volume).toInt().toShort()
                // Convert short sample back to bytes
                audioBuffer[i] = (sample.toInt() and 0xff).toByte()
                audioBuffer[i + 1] = (sample.toInt() shr 8 and 0xff).toByte()
                i += 2
            }
            return audioBuffer
        }

    }


    suspend fun buildMicrophoneAudioDispatcher(pitchHandler: (timestamp: Double, noteI: Int, sim: NotesSimilarity) -> Unit)
       = withContext(Dispatchers.Default) {

        val pitchDetectionHandler =
            PitchDetectionHandler { pitchDetectionResult: PitchDetectionResult, audioEvent: AudioEvent ->
                val newTimeStamp = audioEvent.timeStamp
                micNoteI = processPitchHeavy(pitchDetectionResult.pitch)
                micVolume = soundVolume(audioEvent.floatBuffer)
                if (micNoteI != sortedNotes.size - 1) {
                    shiftArrayRight(micCurrentPitches, micNoteI)
                }
                generatedRecordByteSound = playSound.generatedSnd[if (micNoteI == 0) 0 else (micNoteI - 1) % 12 + 1]
                val similarity = compareHeavy(singerCurrentPitches.copyOfRange(6, 9), micCurrentPitches.copyOfRange(0, 5))

                micTimeStamp = newTimeStamp

                pitchHandler(micTimeStamp, micNoteI, similarity)
            }

        val overlap = mediaInfo.overlap
        val sampleRate = mediaInfo.voiceSampleRate
        val floatBuffer = mediaInfo.audioFloatBuffer
        microphoneDispatcher =
            VocalAudioDispatcher.fromDefaultMicrophone(sampleRate, floatBuffer, overlap)

        val p: AudioProcessor = PitchProcessor(
            PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
            sampleRate.toFloat(),
            floatBuffer,
            pitchDetectionHandler
        )
        microphoneDispatcher?.addAudioProcessor(p)
        microphoneDispatcher
    }


    suspend fun buildMusicAudioDispatcher(
        pitchHandler: (timestamp: Double, noteI: Int) -> Unit,
        onCompletion: () -> Unit
    ) = withContext(Dispatchers.Default) {
        val overlap = mediaInfo.overlap
        val sampleRate = mediaInfo.voiceSampleRate.toFloat()
        val floatBuffer = mediaInfo.audioFloatBuffer

        val tarsosDSPAudioFormat = TarsosDSPAudioFormat(
            TarsosDSPAudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            1,
            2,
            sampleRate,
            ByteOrder.BIG_ENDIAN == ByteOrder.nativeOrder()
        )
        musicDispatcher = SongAudioDispatcher(
            UniversalAudioInputStream(mediaInfo.singerInputStream, tarsosDSPAudioFormat),
            UniversalAudioInputStream(mediaInfo.bgMusicInputStream, tarsosDSPAudioFormat),
            floatBuffer,
            overlap
        )

        val pitchDetectionHandler =
            PitchDetectionHandler { pitchDetectionResult: PitchDetectionResult, audioEvent: AudioEvent ->
                val sTimeStamp = audioEvent.timeStamp
                singNoteI = processPitchHeavy(pitchDetectionResult.pitch)
                singVolume = soundVolume(audioEvent.floatBuffer)
                if (singNoteI != sortedNotes.size - 1) {
                    shiftArrayRight(singerCurrentPitches, singNoteI)
                }
                generatedSingerByteSound =
                    playSound.generatedSnd[if (singNoteI == 0) 0 else (singNoteI - 1) % 12 + 1]
                pitchHandler(sTimeStamp, singNoteI)
            }

        val p = PitchProcessor(
            PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
            sampleRate,
            floatBuffer,
            pitchDetectionHandler
        )

        val bufferSizeInBytes = floatBuffer * tarsosDSPAudioFormat.getSampleSizeInBits() / 8
        mainAudioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            tarsosDSPAudioFormat.getSampleRate().toInt(),
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSizeInBytes * 2,
            AudioTrack.MODE_STREAM
        )
        mainAudioTrack?.play()
        val bytesToRead = (floatBuffer - overlap) * 2

        musicDispatcher?.addAudioProcessor(object : SongAudioDispatcher.MicAudioProcessor {
            override fun process(audioEvent: AudioEvent, musicBuffer: ByteArray): Boolean {
                p.process(audioEvent)
                var singerBuffer = ByteArray(musicBuffer.size)
                if (volumeFactor > 0.05f) {
                    singerBuffer = if (computeAndPlaySingerSoundMode) generatedSingerByteSound else audioEvent.byteBuffer
                    singerBuffer = adjustVolume(singerBuffer, volumeFactor)
                }
                val soundBuffer = mixedBuffers(musicBuffer, singerBuffer, if (computeAndPlayRecordedSoundMode) generatedRecordByteSound else ByteArray(musicBuffer.size))
                mainAudioTrack?.write(soundBuffer, overlap * 2, bytesToRead)
                return false
            }

            override fun process(audioEvent: AudioEvent): Boolean {
                return false
            }

            override fun processingFinished() {
                mainAudioTrack?.flush()
                mainAudioTrack?.stop()
                mainAudioTrack?.release()
                onCompletion()
            }
        })
        musicDispatcher
    }







}


