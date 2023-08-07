package com.example.pitchmasterbeta.model

import androidx.compose.ui.graphics.Color
import kotlin.math.floor


class PitchSoundPlayer(private val sampleRate: Int, duration: Int) {
    private val numSamples: Int
    private val sample: DoubleArray
    val generatedSnd: Array<ByteArray>

    fun buildSounds() {
        for (j in sortedPlayerFrequencies.indices) {
            for (i in 0 until numSamples) {
                // sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone));
                sample[i] =
                    Math.sin((2 * Math.PI - .001) * i / (sampleRate / sortedPlayerFrequencies[j]))
            }

            // convert to 16 bit pcm sound array
            // assumes the sample buffer is normalised.
            var idx = 0
            val ramp = numSamples / 20
            for (i in 0 until ramp) {
                // scale to maximum amplitude
                val `val` = (sample[i] * 32767 * i / ramp).toInt().toShort()
                // in 16 bit wav PCM, first byte is the low order byte
                generatedSnd[j][idx++] = (`val`.toInt() and 0x00ff).toByte()
                generatedSnd[j][idx++] = (`val`.toInt() and 0xff00 ushr 8).toByte()
            }
            for (i in ramp until numSamples - ramp) {
                // scale to maximum amplitude
                val `val` = (sample[i] * 32767).toInt().toShort()
                // in 16 bit wav PCM, first byte is the low order byte
                generatedSnd[j][idx++] = (`val`.toInt() and 0x00ff).toByte()
                generatedSnd[j][idx++] = (`val`.toInt() and 0xff00 ushr 8).toByte()
            }
            for (i in numSamples - ramp until numSamples) {
                // scale to maximum amplitude
                val `val` = (sample[i] * 32767 * (numSamples - i) / ramp).toInt().toShort()
                // in 16 bit wav PCM, first byte is the low order byte
                generatedSnd[j][idx++] = (`val`.toInt() and 0x00ff).toByte()
                generatedSnd[j][idx++] = (`val`.toInt() and 0xff00 ushr 8).toByte()
            }
        }
    }

    init {
        numSamples = duration * sampleRate
        sample = DoubleArray(numSamples)
        generatedSnd = Array(sortedPlayerFrequencies.size) {
            ByteArray(
                2 * numSamples
            )
        }
        buildSounds()
    }

    companion object {
        private const val PITCH_THRESHOLD = 0.2f
        fun processPitchHeavy(pitchInHz: Float): Int {
            if (pitchInHz < PITCH_THRESHOLD) {
                return 0
            }
            // binary search:
            var i = 0
            var j = sortedFrequencies.size - 1
            while (i <= j) {
                val m = floor(((i + j) / 2f).toDouble()).toInt()
                if (pitchInHz > sortedFrequencies[m]) {
                    i = m + 1
                } else if (pitchInHz < sortedFrequencies[m]) {
                    j = m - 1
                } else {
                    return m
                }
            }
            return j
        }

        const val OCTAVE_SIZE = 12

        val sortedPlayerFrequencies = floatArrayOf( /* nothing */
            0.0f,  /*C4*/
            261.63f,  //131.87
            /*C#4/Db4*/
            277.18f,  //124.47
            /*D4*/
            293.66f,  //117.48
            /*D#4/Eb4*/
            311.13f,  //110.89
            /*E4*/
            329.63f,  //104.66
            /*F4*/
            349.23f,  //98.79
            /*F#4/Gb4*/
            369.99f,  //93.24
            /*G4*/
            392.00f,  //88.01
            /*G#4/Ab4*/
            415.30f,  //166.14
            /*A4*/
            440.00f,  //78.41
            /*A#3/Bb3*/
            233.08f,  //148.02
            /*B3*/
            246.94f
        )
        val sortedFrequencies = floatArrayOf( /* nothing */
            0.0f,  /*C0 */
            16.35f,  //2109.89
            /*C#0/Db0 */
            17.32f,  //1991.47
            /*D0 */
            18.35f,  //1879.69
            /*D#0/Eb0*/
            19.45f,  //1774.20
            /*E0*/
            20.60f,  //1674.62
            /*F0*/
            21.83f,  //1580.63
            /*F#0/Gb0*/
            23.12f,  //1491.91
            /*G0*/
            24.50f,  //1408.18
            /*G#0/Ab0*/
            25.96f,  //1329.14
            /*A0*/
            27.50f,  //1254.55
            /*A#0/Bb0*/
            29.14f,  //1184.13
            /*B0*/
            30.87f,  //1117.67
            /*C1*/
            32.70f,  //1054.94
            /*C#1/Db1*/
            34.65f,  //995.73
            /*D1*/
            36.71f,  //939.85
            /*D#1/Eb1*/
            38.89f,  //887.10
            /*E1*/
            41.20f,  //837.31
            /*F1*/
            43.65f,  //790.31
            /*F#1/Gb1*/
            46.25f,  //745.96
            /*G1*/
            49.00f,  //704.09
            /*G#1/Ab1*/
            51.91f,  //664.57
            /*A1*/
            55.00f,  //627.27
            /*A#1/Bb1*/
            58.27f,  //592.07
            /*B1*/
            61.74f,  //558.84
            /*C2*/
            65.41f,  //527.47
            /*C#2/Db2*/
            69.30f,  //497.87
            /*D2*/
            73.42f,  //469.92
            /*D#2/Eb2*/
            77.78f,  //443.55
            /*E2*/
            82.41f,  //418.65
            /*F2*/
            87.31f,  //395.16
            /*F#2/Gb2*/
            92.50f,  //372.98
            /*G2*/
            98.00f,  //352.04
            /*G#2/Ab2*/
            103.83f,  //332.29
            /*A2*/
            110.00f,  //313.64
            /*A#2/Bb2*/
            116.54f,  //296.03
            /*B2*/
            123.47f,  //279.42
            /*C3*/
            130.81f,  //263.74
            /*C#3/Db3*/
            138.59f,  //248.93
            /*D3*/
            146.83f,  //234.96
            /*D#3/Eb3*/
            155.56f,  //221.77
            /*E3*/
            164.81f,  //209.33
            /*F3*/
            174.61f,  //197.58
            /*F#3/Gb3*/
            185.00f,  //186.49
            /*G3*/
            196.00f,  //176.02
            /*G#3/Ab3*/
            207.65f,  //166.14
            /*A3*/
            220.00f,  //156.82
            /*A#3/Bb3*/
            233.08f,  //148.02
            /*B3*/
            246.94f,  //139.71
            /*C4*/
            261.63f,  //131.87
            /*C#4/Db4*/
            277.18f,  //124.47
            /*D4*/
            293.66f,  //117.48
            /*D#4/Eb4*/
            311.13f,  //110.89
            /*E4*/
            329.63f,  //104.66
            /*F4*/
            349.23f,  //98.79
            /*F#4/Gb4*/
            369.99f,  //93.24
            /*G4*/
            392.00f,  //88.01
            /*G#4/Ab4*/
            415.30f,  //83.07
            /*A4*/
            440.00f,  //78.41
            /*A#4/Bb4*/
            466.16f,  //74.01
            /*B4*/
            493.88f,  //69.85
            /*C5*/
            523.25f,  //65.93
            /*C#5/Db5*/
            554.37f,  //62.23
            /*D5*/
            587.33f,  //58.74
            /*D#5/Eb5*/
            622.25f,  //55.44
            /*E5*/
            659.25f,  //52.33
            /*F5*/
            698.46f,  //49.39
            /*F#5/Gb5*/
            739.99f,  //46.62
            /*G5*/
            783.99f,  //44.01
            /*G#5/Ab5*/
            830.61f,  //41.54
            /*A5*/
            880.00f,  //39.20
            /*A#5/Bb5*/
            932.33f,  //37.00
            /*B5*/
            987.77f,  //34.93
            /*C6*/
            1046.50f,  //32.97
            /*C#6/Db6*/
            1108.73f,  //31.12
            /*D6*/
            1174.66f,  //29.37
            /*D#6/Eb6*/
            1244.51f,  //27.72
            /*E6*/
            1318.51f,  //26.17
            /*F6*/
            1396.91f,  //24.70
            /*F#6/Gb6*/
            1479.98f,  //23.31
            /*G6*/
            1567.98f,  //22.00
            /*G#6/Ab6*/
            1661.22f,  //20.77
            /*A6*/
            1760.00f,  //19.60
            /*A#6/Bb6*/
            1864.66f,  //18.50
            /*B6*/
            1975.53f,  //17.46
            /*C7*/
            2093.00f,  //16.48
            /*C#7/Db7*/
            2217.46f,  //15.56
            /*D7*/
            2349.32f,  //14.69
            /*D#7/Eb7*/
            2489.02f,  //13.86
            /*E7*/
            2637.02f,  //13.08
            /*F7*/
            2793.83f,  //12.35
            /*F#7/Gb7*/
            2959.96f,  //11.66
            /*G7*/
            3135.96f,  //11.00
            /*G#7/Ab7*/
            3322.44f,  //10.38
            /*A7*/
            3520.00f,  //9.80
            /*A#7/Bb7*/
            3729.31f,  //9.25
            /*B7*/
            3951.07f,  //8.73
            /*C8*/
            4186.01f,  //8.24
            /*C#8/Db8*/
            4434.92f,  //7.78
            /*D8*/
            4698.63f,  //7.34
            /*D#8/Eb8*/
            4978.03f,  //6.93
            /*E8*/
            5274.04f,  //6.54
            /*F8*/
            5587.65f,  //6.17
            /*F#8/Gb8*/
            5919.91f,  //5.83
            /*G8*/
            6271.93f,  //5.50
            /*G#8/Ab8*/
            6644.88f,  //5.19
            /*A8*/
            7040.00f,  //4.90
            /*A#8/Bb8*/
            7458.62f,  //4.63
            /*B8*/
            7902.13f
        )
        val sortedNotes = arrayOf(
            " ",
            "C0",
            "C#0/Db0 ",
            "D0 ",
            "D#0/Eb0",
            "E0",
            "F0",
            "F#0/Gb0",
            "G0",
            "G#0/Ab0",
            "A0",
            "A#0/Bb0",
            "B0",
            "C1",
            "C#1/Db1",
            "D1",
            "D#1/Eb1",
            "E1",
            "F1",
            "F#1/Gb1",
            "G1",
            "G#1/Ab1",
            "A1",
            "A#1/Bb1",
            "B1",
            "C2",
            "C#2/Db2",
            "D2",
            "D#2/Eb2",
            "E2",
            "F2",
            "F#2/Gb2",
            "G2",
            "G#2/Ab2",
            "A2",
            "A#2/Bb2",
            "B2",
            "C3",
            "C#3/Db3",
            "D3",
            "D#3/Eb3",
            "E3",
            "F3",
            "F#3/Gb3",
            "G3",
            "G#3/Ab3",
            "A3",
            "A#3/Bb3",
            "B3",
            "C4",
            "C#4/Db4",
            "D4",
            "D#4/Eb4",
            "E4",
            "F4",
            "F#4/Gb4",
            "G4",
            "G#4/Ab4",
            "A4",
            "A#4/Bb4",
            "B4",
            "C5",
            "C#5/Db5",
            "D5",
            "D#5/Eb5",
            "E5",
            "F5",
            "F#5/Gb5",
            "G5",
            "G#5/Ab5",
            "A5",
            "A#5/Bb5",
            "B5",
            "C6",
            "C#6/Db6",
            "D6",
            "D#6/Eb6",
            "E6",
            "F6",
            "F#6/Gb6",
            "G6",
            "G#6/Ab6",
            "A6",
            "A#6/Bb6",
            "B6",
            "C7",
            "C#7/Db7",
            "D7",
            "D#7/Eb7",
            "E7",
            "F7",
            "F#7/Gb7",
            "G7",
            "G#7/Ab7",
            "A7",
            "A#7/Bb7",
            "B7",
            "C8",
            "C#8/Db8",
            "D8",
            "D#8/Eb8",
            "E8",
            "F8",
            "F#8/Gb8",
            "G8",
            "G#8/Ab8",
            "A8",
            "A#8/Bb8",
            ""
        )
    }

}


val colorMap = hashMapOf(
    AudioProcessor.NotesSimilarity.Idle to Color(0xFFFFFFFF),
    AudioProcessor.NotesSimilarity.Neutral to Color(0xFFFFFFFF),
    AudioProcessor.NotesSimilarity.Close to Color(0xff7dab52),
    AudioProcessor.NotesSimilarity.Equal to Color(0xff27d57e),
    AudioProcessor.NotesSimilarity.Wrong to Color(0xffd52737)
)
fun getColor(notesSimilarity: AudioProcessor.NotesSimilarity?): Color {
    return colorMap[notesSimilarity]!!
}