package com.example.pitchmasterbeta.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Throws(IOException::class)
fun saveRawAsWavFile(
    waveOutputFile: File,
    byteArrayOutputStream: ByteArrayOutputStream,
    sampleRate: Int
) {
    val rawData = byteArrayOutputStream.toByteArray()
    var output: DataOutputStream? = null
    try {
        output = DataOutputStream(FileOutputStream(waveOutputFile))
        // WAVE header
        // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
        writeString(output, "RIFF") // chunk id
        writeInt(output, 36 + rawData.size) // chunk size
        writeString(output, "WAVE") // format
        writeString(output, "fmt ") // subchunk 1 id
        writeInt(output, 16) // subchunk 1 size
        writeShort(output, 1.toShort()) // audio format (1 = PCM)
        writeShort(output, 1.toShort()) // number of channels
        writeInt(output, sampleRate) // sample rate
        writeInt(output, sampleRate * 2) // byte rate
        writeShort(output, 2.toShort()) // block align
        writeShort(output, 16.toShort()) // bits per sample
        writeString(output, "data") // subchunk 2 id
        writeInt(output, rawData.size) // subchunk 2 size
        output.write(rawData)
    } finally {
        output?.close()
    }
}

@Throws(IOException::class)
private fun writeInt(output: DataOutputStream, value: Int) {
    output.write(value)
    output.write(value shr 8)
    output.write(value shr 16)
    output.write(value shr 24)
}

@Throws(IOException::class)
private fun writeShort(output: DataOutputStream, value: Short) {
    output.write(value.toInt())
    output.write(value.toInt() shr 8)
}

@Throws(IOException::class)
private fun writeString(output: DataOutputStream, value: String) {
    for (i in value.indices) {
        output.write(value[i].code)
    }
}

fun convertAudioFileToWAV(context: Context, inputUri: Uri, outputUri: Uri) {
    val inputUriPath = FFmpegKitConfig.getSafParameterForRead(context, inputUri)
    val outputUriPath = FFmpegKitConfig.getSafParameterForWrite(context, outputUri)
    FFmpegKit.execute(" -i $inputUriPath -acodec pcm_s16le -ar 44100 -ac 2 -f wav $outputUriPath")
    Log.d(
        "FFMPEG",
        "executing: ffmpeg -i ${inputUri.path} -acodec pcm_s16le -ar 44100 -ac 2 -f wav ${outputUri.path}"
    )
}

fun convertAudioFileToMp3(context: Context, inputUri: Uri, outputUri: Uri) {
    val inputUriPath = FFmpegKitConfig.getSafParameterForRead(context, inputUri)
    val outputUriPath = FFmpegKitConfig.getSafParameterForWrite(context, outputUri)
    FFmpegKit.execute(" -i $inputUriPath -codec:a libmp3lame -qscale:a 2 -f mp3 $outputUriPath")
    Log.d(
        "FFMPEG",
        "executing: ffmpeg -i ${inputUri.path} -codec:a libmp3lame -qscale:a 2 -f mp3 ${outputUri.path}"
    )
}