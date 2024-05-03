package com.example.pitchmasterbeta.model

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.example.pitchmasterbeta.MainActivity
import com.example.pitchmasterbeta.utils.convertAudioFileToMp3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class AudioRecorder(val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    fun startRecording(): Boolean {
        mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else MediaRecorder()).apply {
            outputFile = File.createTempFile("record", ".m4a")
            outputFile?.deleteOnExit()
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(outputFile?.absolutePath)
            try {
                prepare()
                start()
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
    }

    fun pauseRecording() {
        mediaRecorder?.apply {
            pause()
        }
    }

    fun resumeRecording() {
        mediaRecorder?.apply {
            resume()
        }
    }

    suspend fun saveRecordingAsMp3(fileName: String) {
        withContext(Dispatchers.IO) {
            val context = MainActivity.appContext ?: return@withContext
            val saveFile = createFileInStorageDir("$fileName.mp3")
            val wavFileUri = Uri.fromFile(outputFile)
            val saveFileUri = Uri.fromFile(saveFile)
            convertAudioFileToMp3(context, wavFileUri, saveFileUri)
            outputFile?.delete()
        }
    }

    fun save(fileName: String) {
        try {
            outputFile?.let { inFile ->
                val outFile = createFileInStorageDir("$fileName.m4a")
                inFile.inputStream().use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                inFile.delete()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            // failure :(
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
}
