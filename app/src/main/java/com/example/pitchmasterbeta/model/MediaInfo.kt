package com.example.pitchmasterbeta.model

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream

@Immutable
data class MediaInfo(
    var voiceSampleRate: Int = 41000,
    var audioFloatBuffer: Int = 1024 * 2,
    var overlap: Int = 0,
    var timeStampDuration: Double = 0.0,
    var singerInputStream: BufferedInputStream? = null,
    var bgMusicInputStream: BufferedInputStream? = null,
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
                    val minutes = duration % (1000 * 60 * 60) / (1000 * 60)
                    val seconds = duration % (1000 * 60 * 60) % (1000 * 60) / 1000
                    timeStampDuration = minutes * 60.0 + seconds
                    mmr.release()
                }


                voiceSampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE) * 2
                audioFloatBuffer = AudioRecord.getMinBufferSize(voiceSampleRate, 16, 2)
                // Init your pitch sounds here or do other actions with the extracted data
            }
        }
    }




}