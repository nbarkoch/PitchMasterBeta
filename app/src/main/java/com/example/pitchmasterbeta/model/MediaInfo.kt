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
    var duration: Int = 0,
    var singerInputStream: BufferedInputStream? = null,
    var bgMusicInputStream: BufferedInputStream? = null,
) {
    companion object {
        val DEFAULT_WAV_BITRATE = 1411
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
                MediaMetadataRetriever().use { mmr ->
                    try {
                        mmr.setDataSource(context, uri)
                        duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull()
                            ?: 2000000
                        overlap = (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: DEFAULT_WAV_BITRATE) / 512
                    } catch (e: NumberFormatException) {
                        duration = 2000000
                        overlap = DEFAULT_WAV_BITRATE / 512
                    }
                }

                voiceSampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE) * 2
                audioFloatBuffer = AudioRecord.getMinBufferSize(voiceSampleRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_FLOAT)
                // Init your pitch sounds here or do other actions with the extracted data
            }
        }
    }




}