package com.example.pitchmasterbeta.model

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.PlaybackParams
import android.net.Uri
import android.os.Environment
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchDetectionResult
import be.tarsos.dsp.pitch.PitchProcessor
import com.example.pitchmasterbeta.MainActivity.Companion.appContext
import com.example.pitchmasterbeta.model.PitchSoundPlayer.Companion.processPitchHeavy
import com.example.pitchmasterbeta.model.PitchSoundPlayer.Companion.sortedNotes
import com.example.pitchmasterbeta.utils.convertAudioFileToMp3
import com.example.pitchmasterbeta.utils.math.shiftArrayRight
import com.example.pitchmasterbeta.utils.updateWavHeader
import com.example.pitchmasterbeta.utils.writeWavHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt


class AudioProcessor(private val mediaInfo: MediaInfo) {

    enum class NotesSimilarity {
        Wrong, Neutral, Idle, Close, Equal
    }

    private var microphoneDispatcher: VocalAudioDispatcher? = null
    private var musicDispatcher: SongAudioDispatcher? = null
    private var mainAudioTrack: AudioTrack? = null
    var volumeFactor: Float = 0f
    var pitchFactor: Float = 1.0f
    private val channels = 1 // 1 = Mono, 2 = Streo

    val playbackParams = PlaybackParams()
    var computeAndPlaySingerSoundMode: Boolean = false
    var computeAndPlayRecordedSoundMode: Boolean = false
    private var generatedSingerByteSound: ByteArray = byteArrayOf()
    private var generatedRecordByteSound: ByteArray = byteArrayOf()

    private val playSound: PitchSoundPlayer = PitchSoundPlayer(mediaInfo.voiceSampleRate, 1)

    private var singNoteI = 0
    private var micNoteI = 0

    private var singerCurrentPitches = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0)
    private var micCurrentPitches = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0)

    var recording = true
    private var fileOutputStream: FileOutputStream? = null
    private var outputFile: File? = null

    companion object {
        private const val SAMPLE_THRESHOLD = 255.toByte()

        fun shrinkVolume(volume: Int): Float {
            return (volume / 17f).coerceAtMost(1f)
        }

        fun rangeIndex(noteI: Int): Float {
            return ((noteI - 1) % PitchSoundPlayer.OCTAVE_SIZE + 1).toFloat() / PitchSoundPlayer.sortedPlayerFrequencies.size
        }

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
                    if (maxVal < 0 && (singerHZ == 0 || singerHZ == sortedNotes.size - 1)) {
                        return NotesSimilarity.Idle
                    } else if (maxVal < 0 && (speakerHZ == 0 || speakerHZ == sortedNotes.size - 1)) {
                        return NotesSimilarity.Neutral
                    } else {
                        if (abs(speakerHZ - singerHZ) % 12 == 0) {
                            if (speakerHZ == singerHZ) {
                                return NotesSimilarity.Equal
                            }
                            maxVal = 1
                        } else if (abs(speakerHZ - singerHZ) % 12 < 2) {
                            maxVal = 1
                        }
                    }
                }
            }
            return NotesSimilarity.entries[maxVal + 2]
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


    suspend fun buildMicrophoneAudioDispatcher(pitchHandler: (timestamp: Double, noteI: Int, volume: Int, sim: NotesSimilarity) -> Unit) =
        withContext(Dispatchers.IO) {

            val overlap = mediaInfo.overlap
            val sampleRate = mediaInfo.voiceSampleRate
            val floatBuffer = mediaInfo.audioFloatBuffer
            val bytesToRead = (floatBuffer - overlap) * 2

            val pitchDetectionHandler =
                PitchDetectionHandler { pitchDetectionResult: PitchDetectionResult, audioEvent: AudioEvent ->
                    val newTimeStamp = audioEvent.timeStamp
                    micNoteI = processPitchHeavy(pitchDetectionResult.pitch)
                    val micVolume = soundVolume(audioEvent.floatBuffer)
                    if (micNoteI != sortedNotes.size - 1) {
                        shiftArrayRight(micCurrentPitches, micNoteI)
                    }
                    generatedRecordByteSound =
                        playSound.generatedSnd[if (micNoteI == 0) 0 else (micNoteI - 1) % 12 + 1]
                    val similarity = compareHeavy(
                        singerCurrentPitches.copyOfRange(6, 9),
                        micCurrentPitches.copyOfRange(0, 5)
                    )

                    pitchHandler(newTimeStamp, micNoteI, micVolume, similarity)
                }

            microphoneDispatcher =
                VocalAudioDispatcher.fromDefaultMicrophone(sampleRate, floatBuffer, overlap)

            val p: AudioProcessor = PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
                sampleRate.toFloat(),
                floatBuffer,
                pitchDetectionHandler
            )

            fileOutputStream = if (recording) {
                outputFile = File.createTempFile("record", ".m4a")
                outputFile?.deleteOnExit()
                FileOutputStream(outputFile).apply {
                    writeWavHeader(this, mediaInfo.voiceSampleRate)
                }
            } else {
                outputFile?.delete()
                fileOutputStream?.close()
                null
            }

            microphoneDispatcher?.addAudioProcessor(object : AudioProcessor {
                override fun process(audioEvent: AudioEvent): Boolean {
                    p.process(audioEvent)
                    fileOutputStream?.let { stream ->
                        val dataToWrite =
                            if (recording) audioEvent.byteBuffer else ByteArray(audioEvent.byteBuffer.size)
                        stream.write(dataToWrite, overlap * 2, bytesToRead)
                    }
                    return false
                }

                override fun processingFinished() {
                }
            })
            microphoneDispatcher
        }


    suspend fun buildMusicAudioDispatcher(
        pitchHandler: (timestamp: Double, noteI: Int, volume: Int, audioData: Pair<List<Float>, List<Float>>) -> Unit,
        onCompletion: () -> Unit
    ) = withContext(Dispatchers.Default) {
        val overlap = mediaInfo.overlap
        val sampleRate = mediaInfo.voiceSampleRate.toFloat()
        val floatBuffer = mediaInfo.audioFloatBuffer

        mediaInfo.run {
            bgMusicInputStream?.takeIf { it.markSupported() }?.apply {
                mark(Int.MAX_VALUE)
                bgMusicInputStream = this
            }
            singerInputStream?.takeIf { it.markSupported() }?.apply {
                mark(Int.MAX_VALUE)
                singerInputStream = this
            }
        }

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
                singNoteI = processPitchHeavy(pitchDetectionResult.pitch * pitchFactor)
                val singVolume = soundVolume(audioEvent.floatBuffer)
                if (singNoteI != sortedNotes.size - 1) {
                    shiftArrayRight(singerCurrentPitches, singNoteI)
                }
                generatedSingerByteSound =
                    playSound.generatedSnd[if (singNoteI == 0) 0 else (singNoteI - 1) % 12 + 1]
                val audioData = processAudioData(audioEvent.byteBuffer, samplesPerChannel = 50)
                pitchHandler(sTimeStamp, singNoteI, singVolume, audioData)
            }

        val p = PitchProcessor(
            PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
            sampleRate,
            floatBuffer,
            pitchDetectionHandler
        )

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(sampleRate.toInt())
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO) // Adjust channel mask as needed
            .build()

        val minBufferSize = AudioTrack.getMinBufferSize(
            audioFormat.sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            audioFormat.encoding
        )

        mainAudioTrack = AudioTrack(
            audioAttributes,
            audioFormat,
            minBufferSize * 2,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE // Or specify your own audio session ID
        )
        mainAudioTrack?.play()
        val bytesToRead = (floatBuffer - overlap) * 2

        musicDispatcher?.addAudioProcessor(object : SongAudioDispatcher.MicAudioProcessor {
            override fun process(audioEvent: AudioEvent, musicBuffer: ByteArray): Boolean {
                pitchFactor.takeIf { it in 0.5..2.0 }?.let {
                    playbackParams.pitch = it
                    mainAudioTrack?.playbackParams = playbackParams
                }
                p.process(audioEvent)
                var singerBuffer = ByteArray(musicBuffer.size)
                volumeFactor.takeIf { it > 0.0f }?.let {
                    singerBuffer =
                        if (computeAndPlaySingerSoundMode) generatedSingerByteSound else audioEvent.byteBuffer
                    singerBuffer = adjustVolume(singerBuffer, it)
                }
                val soundBuffer = mixedBuffers(
                    musicBuffer,
                    singerBuffer,
                    if (computeAndPlayRecordedSoundMode) generatedRecordByteSound else ByteArray(
                        musicBuffer.size
                    )
                )
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

    suspend fun saveRecording(fileName: String) {
        withContext(Dispatchers.IO) {
            val context = appContext ?: return@withContext
            val recordFile = createFileInStorageDir("$fileName.mp3")
            fileOutputStream?.let {
                outputFile?.let { outputFile ->
                    updateWavHeader(outputFile)
                    it.close()
                }
            }
            val wavFileUri = Uri.fromFile(outputFile)
            val mp3FileUri = Uri.fromFile(recordFile)
            convertAudioFileToMp3(context, wavFileUri, mp3FileUri)
            outputFile?.delete()
            outputFile = null
        }
    }

    private fun createFileInStorageDir(fileName: String): File {
        val recordingsDirectory = File(Environment.getExternalStorageDirectory(), "Recordings")
        recordingsDirectory.mkdirs()
        val recordings = File(recordingsDirectory, "Karaoke")
        recordings.mkdirs()
        val file = File(recordings, fileName)
        file.parentFile?.mkdirs()
        file.createNewFile() // Create the file
        return file
    }

    private fun processAudioData(
        byteArray: ByteArray,
        samplesPerChannel: Int
    ): Pair<List<Float>, List<Float>> {
        val shortArray = byteArray.toShortArray()
        val leftChannel = mutableListOf<Float>()
        val rightChannel = mutableListOf<Float>()

        val samplesPerFrame = channels
        val framesToProcess = minOf(shortArray.size / samplesPerFrame, samplesPerChannel)

        for (i in 0 until framesToProcess) {
            val leftSample = shortArray[i * samplesPerFrame].toFloat() / Short.MAX_VALUE
            leftChannel.add(abs(leftSample))

            if (channels > 1) {
                val rightSample = shortArray[i * samplesPerFrame + 1].toFloat() / Short.MAX_VALUE
                rightChannel.add(abs(rightSample))
            } else {
                rightChannel.add(abs(leftSample))
            }
        }

        // Ensure we have exactly samplesPerChannel samples
        while (leftChannel.size < samplesPerChannel) {
            leftChannel.add(0f)
            rightChannel.add(0f)
        }

        return Pair(leftChannel, rightChannel)
    }

    private fun ByteArray.toShortArray(): ShortArray {
        val shortArray = ShortArray(size / 2)
        for (i in shortArray.indices) {
            shortArray[i] =
                ((this[i * 2 + 1].toInt() shl 8) or (this[i * 2].toInt() and 0xFF)).toShort()
        }
        return shortArray
    }

}