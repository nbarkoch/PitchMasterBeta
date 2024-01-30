package com.example.pitchmasterbeta.model

import android.content.Context
import android.database.Cursor
import android.media.AudioRecord
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.Immutable
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
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
                    duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toIntOrNull()
                        ?: 2000000
                    overlap = (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                        ?.toIntOrNull()
                        ?: DEFAULT_WAV_BITRATE) / 512
                } catch (_: NumberFormatException) {
                } finally {
                    timeStampDuration = duration / 1000.0
                    mmr.release()
                }

                voiceSampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE) * 2
                audioFloatBuffer = AudioRecord.getMinBufferSize(voiceSampleRate, 16, 2)
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
                val returnCursor: Cursor =
                    context.contentResolver.query(uri, null, null, null, null)!!
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

    suspend fun getAudioBufferedInputStream(
        context: Context,
        audioFile: File,
        musicFile: File
    ): BufferedInputStream {
        return withContext(Dispatchers.IO) {
            val audioFileUri = Uri.fromFile(audioFile)
            val wavFileUri = Uri.fromFile(musicFile)
            return@withContext transformAndPrepareAudio(
                context,
                audioFileUri,
                wavFileUri
            )
        }
    }

    fun downloadAudioFile(url: URL): File? {
        try {
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                // Create a temporary file to store the downloaded content
                val audioFile =
                    File.createTempFile(url.path.replace("/", "").replace(".", "_"), null)
                val outputStream = FileOutputStream(audioFile)

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

                return audioFile
            } else {
                // Handle the case when the connection fails
            }

        } catch (e: IOException) {
            e.printStackTrace()
            // Handle any errors that occur during download or extraction
        }
        return null
    }

    private fun transformToWAV(context: Context, inputUri: Uri, outputUri: Uri) {
        val inputUriPath = FFmpegKitConfig.getSafParameterForRead(context, inputUri)
        val outputUriPath = FFmpegKitConfig.getSafParameterForWrite(context, outputUri)
        FFmpegKit.execute(" -i $inputUriPath -acodec pcm_s16le -ar 44100 -ac 2 -f wav $outputUriPath")
        Log.d(
            "FFMPEG",
            "executing: ffmpeg -i ${inputUri.path} -acodec pcm_s16le -ar 44100 -ac 2 -f wav ${outputUri.path}"
        )
    }

    /**
     * Function that gets a context, audio file uri, and output WAV file uri
     * and do the following:
     * 1. transform the audio file to a WAV file
     * 2. prepare the WAV file to be streamed properly in the studio
     * 3. create a bufferInputStream to the referenced WAV file, and returns it
     * **/
    private suspend fun transformAndPrepareAudio(
        context: Context,
        anyAudioFileUri: Uri,
        wavFileUri: Uri
    ): BufferedInputStream {

        // first, turn the audio file to a WAV file
        transformToWAV(context, anyAudioFileUri, wavFileUri)

        // then, init the studio, and turn to buffer input stream
        max(context, wavFileUri)
        return BufferedInputStream(context.contentResolver.openInputStream(wavFileUri))
    }

    fun closeStreams() {
        singerInputStream?.run { this.close() }
        bgMusicInputStream?.run { this.close() }
        singerInputStream = null
        bgMusicInputStream = null
    }

}