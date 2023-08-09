package com.example.pitchmasterbeta.model

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.media.AudioRecord
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


@Immutable
data class MediaInfo(
    var voiceSampleRate: Int = 41000,
    var audioFloatBuffer: Int = 1024 * 2,
    var overlap: Int = 0,
    var timeStampDuration: Double = 0.0,
    var singerInputStream: BufferedInputStream? = null,
    var bgMusicInputStream: BufferedInputStream? = null,
    var sponsorArtist: String = "",
    var sponsorTitle: String = ""
) {
    companion object {
        const val DEFAULT_WAV_BITRATE = 1411
    }


    suspend fun max(context: Context, uri: Uri) {

        withContext(Dispatchers.IO) {
            MediaExtractor().let { mex ->
                try {
                    mex.setDataSource(context, uri, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val mf = mex.getTrackFormat(0)
                val mmr = MediaMetadataRetriever()
                var duration = 2000000
                overlap = DEFAULT_WAV_BITRATE / 512
                try {
                    mmr.setDataSource(context, uri)
                    duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull()
                        ?: 2000000
                    overlap = (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
                        ?: DEFAULT_WAV_BITRATE) / 512
                } catch (_: NumberFormatException) {
                } finally {
                    timeStampDuration = duration / 1000.0
                    mmr.release()
                }


                voiceSampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE) * 2
                audioFloatBuffer = AudioRecord.getMinBufferSize(voiceSampleRate, 16, 2)
                // Init your pitch sounds here or do other actions with the extracted data
            }
        }
    }

    fun getSongInfo(context: Context, uri: Uri) {
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(context, uri)
            sponsorArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
            sponsorTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
            if (sponsorTitle.isEmpty()) {
                val returnCursor: Cursor = context.contentResolver.query(uri, null, null, null, null)!!
                val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                returnCursor.moveToFirst()
                sponsorTitle = returnCursor.getString(nameIndex)
                returnCursor.close()
            }
        } catch (e: java.lang.NumberFormatException) {
            uri.path?.let {
                sponsorTitle = it.substring(it.lastIndexOf('/') + 1)
            }
        }
    }


    fun downloadAndExtractMedia(contextResolver: ContentResolver, url: URL, tempFile: File): BufferedInputStream? {
        try {
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                // Create a temporary file to store the downloaded content
                val outputStream = FileOutputStream(tempFile)

                // Download the content
                val inputStream = connection.inputStream
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                // Close the streams
                inputStream.close()
                outputStream.close()

                // Get the Uri reference to the downloaded file
                val uri = Uri.fromFile(tempFile)
                return BufferedInputStream(contextResolver.openInputStream(uri))
                // Rest of your code...
            } else {
                // Handle the case when the connection fails
            }
        } catch (e: IOException) {
            e.printStackTrace()
            // Handle any errors that occur during download or extraction
        }
        return null
    }

    fun closeStreams() {
        singerInputStream?.run { this.close() }
        bgMusicInputStream?.run { this.close() }
    }


    fun prepareForExecution(bitRate: Int, sampleRate: Int, duration: Int) {
        audioFloatBuffer = bitRate / 16
        overlap = audioFloatBuffer / 4
        voiceSampleRate = sampleRate * 2
        audioFloatBuffer = AudioRecord.getMinBufferSize(voiceSampleRate, 16, 2)
        timeStampDuration = duration.toDouble()
    }

}