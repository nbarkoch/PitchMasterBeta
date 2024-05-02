package com.example.pitchmasterbeta.model

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.pitchmasterbeta.MainActivity.Companion.appContext
import java.io.File
import java.io.IOException

class AudioRecorder(val context: Context, private val outputFile: File) {

    private var mediaRecorder: MediaRecorder? = null

    fun startRecording() {
        mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else MediaRecorder()).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            try {
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
    }
}
