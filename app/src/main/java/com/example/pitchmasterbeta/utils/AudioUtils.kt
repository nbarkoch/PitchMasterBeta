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
import java.io.OutputStream
import java.io.RandomAccessFile

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


fun writeWavHeader(outputStream: OutputStream, sampleRate: Int) {
    val totalAudioLen: Long = 0 // Set to 0 initially, will be updated later
    val totalDataLen: Long = totalAudioLen + 36 // 36 is the size of the header

    // Write the "RIFF" chunk descriptor
    outputStream.write("RIFF".toByteArray())
    outputStream.write(intToByteArray(totalDataLen.toInt()))

    // Write the "WAVE" format identifier
    outputStream.write("WAVE".toByteArray())

    // Write the "fmt " sub-chunk
    outputStream.write("fmt ".toByteArray())
    outputStream.write(intToByteArray(16)) // Sub-chunk size (16 for PCM)
    outputStream.write(shortToByteArray(1)) // Audio format (1 for PCM)
    outputStream.write(shortToByteArray(1)) // Number of channels
    outputStream.write(intToByteArray(sampleRate)) // Sample rate
    outputStream.write(intToByteArray(sampleRate * 2)) // Byte rate
    outputStream.write(shortToByteArray(2)) // Block align
    outputStream.write(shortToByteArray(16)) // Bits per sample

    // Write the "data" sub-chunk (placeholder, will be updated later)
    outputStream.write("data".toByteArray())
    outputStream.write(intToByteArray(totalAudioLen.toInt()))
}

fun updateWavHeader(file: File) {
    val dataChunkSize = file.length() - 36
    val riffChunkSize =
        dataChunkSize + 36 - 8  // 36 is the size of the header and 8 is for "RIFF" and chunk size fields

    // Open the WAV file for reading and writing
    RandomAccessFile(file, "rw").use { randomAccessFile ->
        // Calculate the actual sizes of the RIFF chunk and the data chunk

        // Write the updated sizes to the header
        randomAccessFile.seek(4)
        randomAccessFile.write(intToByteArray(riffChunkSize.toInt()))

        randomAccessFile.seek(40)
        randomAccessFile.write(intToByteArray(dataChunkSize.toInt()))
    }
}

/**
 * Function to convert an integer to a little-endian byte array.
 */
fun intToByteArray(value: Int): ByteArray {
    return byteArrayOf(
        value.toByte(),
        (value shr 8).toByte(),
        (value shr 16).toByte(),
        (value shr 24).toByte()
    )
}

/**
 * Function to convert a short to a little-endian byte array.
 */
fun shortToByteArray(value: Int): ByteArray {
    return byteArrayOf(
        value.toByte(),
        (value shr 8).toByte()
    )
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