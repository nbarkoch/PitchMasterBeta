package com.example.pitchmasterbeta.ui.workspace

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Constraints.Companion.Infinity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pitchmasterbeta.MainActivity.Companion.appContext
import com.example.pitchmasterbeta.model.AudioProcessor
import com.example.pitchmasterbeta.model.LyricsSegment
import com.example.pitchmasterbeta.model.LyricsTimestampedSegment
import com.example.pitchmasterbeta.model.LyricsWord
import com.example.pitchmasterbeta.model.MediaInfo
import com.example.pitchmasterbeta.model.SongAudioDispatcher
import com.example.pitchmasterbeta.model.VocalAudioDispatcher
import com.example.pitchmasterbeta.model.getColor
import com.example.pitchmasterbeta.notifications.SpleeterProgressNotification
import com.example.pitchmasterbeta.services.LyricsProvider
import com.example.pitchmasterbeta.services.SpleeterService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.lang.reflect.Type
import java.net.URL


class WorkspaceViewModel : ViewModel(), SpleeterService.ServiceNotifier {

    private var mediaInfo = MediaInfo()
    private var audioProcessor: AudioProcessor? = null
    private var notification: SpleeterProgressNotification? = null
    private var lyricsProvider: LyricsProvider? = null

    private val devTestMode: Boolean = true

    fun isDevMode(): Boolean {
        return devTestMode
    }

    private val _lyricsScrollToPosition = MutableStateFlow(0)
    val lyricsScrollToPosition: StateFlow<Int> = _lyricsScrollToPosition

    private val _lyricsActiveWordIndex = MutableStateFlow(-1)
    val lyricsActiveWordIndex: StateFlow<Int> = _lyricsActiveWordIndex


    // MutableState for storing your list of items obtained from the server
    private val _lyricsSegments = MutableStateFlow<List<LyricsTimestampedSegment>>(emptyList())
    val lyricsSegments: StateFlow<List<LyricsTimestampedSegment>> = _lyricsSegments

    fun mockupLyrics() {
        val payloadString =
            "{  \"statusCode\": 200,  \"body\": \"[{\\\"start\\\": 12.9524, \\\"end\\\": 25.6667, \\\"text\\\": \\\" There ain't no gold in this river That I've been washing my hands in forever\\\"}, {\\\"start\\\": 26.5, \\\"end\\\": 39.94, \\\"text\\\": \\\" I know there is hope in these waters But I can't bring myself to swim when I am drowning\\\"}, {\\\"start\\\": 39.94, \\\"end\\\": 53.7, \\\"text\\\": \\\" In this silence, baby, let me in Go easy on me, baby\\\"}, {\\\"start\\\": 53.7, \\\"end\\\": 66.74000000000001, \\\"text\\\": \\\" I was still a child, didn't get the chance to Feel the world around me\\\"}, {\\\"start\\\": 66.74000000000001, \\\"end\\\": 78.18, \\\"text\\\": \\\" I had no time to choose what I chose to do So go easy on me\\\"}, {\\\"start\\\": 83.7, \\\"end\\\": 100.34, \\\"text\\\": \\\" There ain't no room for things to change When we are both so deeply stuck in our ways\\\"}, {\\\"start\\\": 100.82000000000001, \\\"end\\\": 113.14, \\\"text\\\": \\\" You can't deny how hard I've tried I changed who I was to put you both first\\\"}, {\\\"start\\\": 113.14, \\\"end\\\": 124.66, \\\"text\\\": \\\" But now I give up Go easy on me, baby\\\"}, {\\\"start\\\": 124.66, \\\"end\\\": 137.94, \\\"text\\\": \\\" I was still a child, didn't get the chance to Feel the world around me\\\"}, {\\\"start\\\": 137.94, \\\"end\\\": 159.06, \\\"text\\\": \\\" I had no time to choose what I chose to do So go easy on me\\\"}, {\\\"start\\\": 159.06, \\\"end\\\": 175.14000000000001, \\\"text\\\": \\\" I had good intentions and the highest hopes But I know right now that probably doesn't even show\\\"}, {\\\"start\\\": 175.22, \\\"end\\\": 195.14, \\\"text\\\": \\\" Go easy on me, baby I was still a child, didn't get the chance to Feel the world around me\\\"}, {\\\"start\\\": 195.14, \\\"end\\\": 215.14, \\\"text\\\": \\\" I had no time to choose what I chose to do So go easy on me\\\"}]\"}"
        //val payloadString ="{ \"statusCode\": 200, \"body\": \"[{\\\"start\\\": 1.0, \\\"end\\\": 2.0, \\\"text\\\": \\\" \\\\u05d1\\\\u05d5\\\\u05e7\\\\u05e8 \\\\u05d8\\\\u05d5\\\\u05d1!\\\"}, {\\\"start\\\": 2.0, \\\"end\\\": 4.0, \\\"text\\\": \\\" \\\\u05d1\\\\u05d5\\\\u05e7\\\\u05e8 \\\\u05d8\\\\u05d5\\\\u05d1, \\\\u05de\\\\u05d4 \\\\u05e7\\\\u05d5\\\\u05e8\\\\u05d4?\\\"}, {\\\"start\\\": 4.0, \\\"end\\\": 6.0, \\\"text\\\": \\\" \\\\u05d0\\\\u05ea\\\\u05d4 \\\\u05d1\\\\u05d9\\\\u05ea\\\\u05d9 \\\\u05dc\\\\u05d5\\\\u05d9\\\\u05e7\\\\u05d9 \\\\u05e2\\\\u05d6\\\\u05e8\\\\u05d4 \\\\u05dc\\\\u05d0\\\\u05db\\\\u05d5\\\\u05dc \\\\u05de\\\\u05e9\\\\u05d4\\\\u05d5?\\\"}, {\\\"start\\\": 6.0, \\\"end\\\": 8.0, \\\"text\\\": \\\" \\\\u05de\\\\u05d4 \\\\u05d0\\\\u05d5\\\\u05db\\\\u05dc\\\\u05d9\\\\u05dd \\\\u05e9\\\\u05dd?\\\"}, {\\\"start\\\": 8.0, \\\"end\\\": 11.0, \\\"text\\\": \\\" \\\\u05d4\\\\u05db\\\\u05dc \\\\u05d1\\\\u05ea\\\\u05d5\\\\u05da \\\\u05d1\\\\u05d2\\\\u05d8\\\\u05d4 \\\\u05de\\\\u05d5\\\\u05df \\\\u05e4\\\\u05dc\\\\u05e4\\\\u05dc \\\\u05ea\\\\u05e9\\\\u05d5\\\\u05de\\\\u05d4\\\"}, {\\\"start\\\": 11.0, \\\"end\\\": 14.0, \\\"text\\\": \\\" \\\\u05e9\\\\u05e7\\\\u05e9\\\\u05d5\\\\u05e7 \\\\u05d7\\\\u05d1\\\\u05d9\\\\u05ea\\\\u05d4 \\\\u05e7\\\\u05d1\\\\u05d1 \\\\u05de\\\\u05e8\\\\u05d2\\\\u05d6\\\"}, {\\\"start\\\": 14.0, \\\"end\\\": 18.0, \\\"text\\\": \\\" \\\\u05ea\\\\u05d8\\\\u05e2\\\\u05d1 \\\\u05d5\\\\u05ea\\\\u05ea\\\\u05de\\\\u05e7\\\\u05e8 \\\\u05ea\\\\u05d7\\\\u05d6\\\\u05d5\\\\u05e8 \\\\u05d1\\\\u05db\\\\u05dc \\\\u05d1\\\\u05d5\\\\u05e7\\\\u05e8\\\"}, {\\\"start\\\": 18.0, \\\"end\\\": 21.0, \\\"text\\\": \\\" \\\\u05e2\\\\u05db\\\\u05e9\\\\u05d9\\\\u05d5 \\\\u05d0\\\\u05e1\\\\u05d1\\\\u05d9\\\\u05e8 \\\\u05dc\\\\u05db\\\\u05dd \\\\u05d0\\\\u05d9\\\\u05da \\\\u05d6\\\\u05d4 \\\\u05e2\\\\u05d5\\\\u05d1\\\\u05d3\\\"}, {\\\"start\\\": 21.0, \\\"end\\\": 24.0, \\\"text\\\": \\\" \\\\u05d6\\\\u05d4 \\\\u05d5\\\\u05d9\\\\u05e7\\\\u05d9 \\\\u05e2\\\\u05d6\\\\u05e8\\\\u05d4 \\\\u05de\\\\u05e1\\\\u05ea\\\\u05d5\\\\u05d1\\\\u05d1 \\\\u05d1\\\\u05e0\\\\u05ea\\\\u05e0\\\\u05d9\\\\u05d4\\\"}, {\\\"start\\\": 24.0, \\\"end\\\": 27.0, \\\"text\\\": \\\" \\\\u05d5\\\\u05db\\\\u05dc \\\\u05d4\\\\u05d0\\\\u05d9\\\\u05e0\\\\u05e1\\\\u05d8\\\\u05d2\\\\u05e8\\\\u05dd \\\\u05e9\\\\u05dc\\\\u05d5\\\"}, {\\\"start\\\": 27.0, \\\"end\\\": 30.0, \\\"text\\\": \\\" \\\\u05d6\\\\u05d4 \\\\u05d5\\\\u05d9\\\\u05e7\\\\u05d9 \\\\u05e2\\\\u05d6\\\\u05e8\\\\u05d4 \\\\u05de\\\\u05e1\\\\u05ea\\\\u05d5\\\\u05d1\\\\u05d1 \\\\u05d1\\\\u05e0\\\\u05ea\\\\u05e0\\\\u05d9\\\\u05d4\\\"}, {\\\"start\\\": 30.0, \\\"end\\\": 33.0, \\\"text\\\": \\\" \\\\u05d5\\\\u05db\\\\u05dc \\\\u05d4\\\\u05e2\\\\u05d9\\\\u05e8 \\\\u05de\\\\u05ea\\\\u05dc\\\\u05db\\\\u05e9\\\\u05e9\\\\u05d9\\\\u05dd \\\\u05d6\\\\u05d4 \\\\u05d4\\\\u05d0\\\\u05d9\\\\u05e9\\\"}, {\\\"start\\\": 33.0, \\\"end\\\": 36.0, \\\"text\\\": \\\" \\\\u05e9\\\\u05d0\\\\u05ea \\\\u05d0\\\\u05d4\\\\u05d1\\\\u05ea\\\\u05d5 \\\\u05de\\\\u05db\\\\u05e0\\\\u05d9\\\\u05e1 \\\\u05dc\\\\u05d1\\\\u05d2\\\\u05d8\\\\u05d9\\\\u05dd\\\"}, {\\\"start\\\": 36.0, \\\"end\\\": 41.0, \\\"text\\\": \\\" \\\\u05d2\\\\u05dd \\\\u05dc\\\\u05e6\\\\u05e2\\\\u05d9\\\\u05e8 \\\\u05d5\\\\u05d2\\\\u05dd \\\\u05dc\\\\u05e7\\\\u05e9\\\\u05d9\\\"}, {\\\"start\\\": 41.0, \\\"end\\\": 45.0, \\\"text\\\": \\\" \\\\u05db\\\\u05df \\\\u05d6\\\\u05d4\\\\u05d5 \\\\u05d4\\\\u05d0\\\\u05d9\\\\u05e9\\\"}, {\\\"start\\\": 45.0, \\\"end\\\": 48.0, \\\"text\\\": \\\" \\\\u05dc\\\\u05db\\\\u05dd \\\\u05d7\\\\u05d1\\\\u05d9\\\\u05ea\\\\u05d4 \\\\u05d1\\\\u05e0\\\\u05ea\\\\u05e0\\\\u05d9\\\\u05d4\\\"}, {\\\"start\\\": 48.0, \\\"end\\\": 51.0, \\\"text\\\": \\\" \\\\u05d1\\\\u05d9\\\\u05d1\\\\u05d5\\\\u05d0\\\\u05d9 \\\\u05d9\\\\u05e9\\\\u05e8\\\\u05d0\\\\u05dc 24\\\"}, {\\\"start\\\": 51.0, \\\"end\\\": 54.0, \\\"text\\\": \\\" \\\\u05d1\\\\u05d2\\\\u05d8\\\\u05d9\\\\u05dd \\\\u05dc\\\\u05e9\\\\u05de\\\\u05d4 \\\\u05e2\\\\u05db\\\\u05e9\\\\u05d9\\\\u05d5 \\\\u05d0\\\\u05db\\\\u05dc\\\\u05ea\\\"}, {\\\"start\\\": 54.0, \\\"end\\\": 57.0, \\\"text\\\": \\\" \\\\u05d1\\\\u05d8\\\\u05d5\\\\u05d7 \\\\u05e9\\\\u05ea\\\\u05d7\\\\u05d6\\\\u05d5\\\\u05e8 \\\\u05d1\\\\u05d7\\\\u05d6\\\\u05e8\\\\u05d4\\\"}, {\\\"start\\\": 57.0, \\\"end\\\": 60.0, \\\"text\\\": \\\" \\\\u05dc\\\\u05db\\\\u05dd \\\\u05d7\\\\u05d1\\\\u05d9\\\\u05ea\\\\u05d4 \\\\u05d1\\\\u05e0\\\\u05ea\\\\u05e0\\\\u05d9\\\\u05d4\\\"}, {\\\"start\\\": 60.0, \\\"end\\\": 63.0, \\\"text\\\": \\\" \\\\u05d1\\\\u05d9\\\\u05d1\\\\u05d5\\\\u05d0\\\\u05d9 \\\\u05d9\\\\u05e9\\\\u05e8\\\\u05d0\\\\u05dc 24\\\"}, {\\\"start\\\": 63.0, \\\"end\\\": 66.0, \\\"text\\\": \\\" \\\\u05d1\\\\u05d2\\\\u05d8\\\\u05d9\\\\u05dd \\\\u05dc\\\\u05e9\\\\u05de\\\\u05d4 \\\\u05e2\\\\u05db\\\\u05e9\\\\u05d9\\\\u05d5 \\\\u05d0\\\\u05db\\\\u05dc\\\\u05ea\\\"}, {\\\"start\\\": 66.0, \\\"end\\\": 73.0, \\\"text\\\": \\\" \\\\u05d1\\\\u05d8\\\\u05d5\\\\u05d7 \\\\u05e9\\\\u05ea\\\\u05d7\\\\u05d6\\\\u05d5\\\\u05e8 \\\\u05d1\\\\u05d7\\\\u05d6\\\\u05e8\\\\u05d4\\\"}, {\\\"start\\\": 85.0, \\\"end\\\": 88.0, \\\"text\\\": \\\" \\\\u05e2\\\\u05e1\\\\u05e7\\\\u05ea \\\\u05d1\\\\u05e0\\\\u05d3\\\\u05dc\\\\u05df \\\\u05ea\\\\u05d5\\\\u05e4\\\\u05e4\\\\u05ea \\\\u05ea\\\\u05e8\\\\u05d1\\\\u05d5\\\\u05e7\\\\u05d4\\\"}, {\\\"start\\\": 88.0, \\\"end\\\": 91.0, \\\"text\\\": \\\" \\\\u05d3\\\\u05de\\\\u05d9\\\\u05d9\\\\u05e0\\\\u05ea \\\\u05e9\\\\u05e8\\\\u05d0\\\\u05d9\\\\u05ea \\\\u05db\\\\u05d1\\\\u05e8 \\\\u05d0\\\\u05ea \\\\u05d4\\\\u05db\\\\u05dc\\\"}, {\\\"start\\\": 91.0, \\\"end\\\": 94.0, \\\"text\\\": \\\" \\\\u05d4\\\\u05d2\\\\u05e2\\\\u05ea \\\\u05dc\\\\u05de\\\\u05e7\\\\u05d5\\\\u05dd \\\\u05e9\\\\u05db\\\\u05dc\\\\u05dc \\\\u05dc\\\\u05d0 \\\\u05e6\\\\u05d9\\\\u05e4\\\\u05d9\\\\u05ea\\\"}, {\\\"start\\\": 94.0, \\\"end\\\": 98.0, \\\"text\\\": \\\" \\\\u05d7\\\\u05d6\\\\u05e8\\\\u05ea \\\\u05d5\\\\u05d9\\\\u05e7\\\\u05d9 \\\\u05e2\\\\u05dd \\\\u05e2\\\\u05d5\\\\u05e8\\\\u05e7\\\\u05d5 \\\\u05d2\\\\u05d3\\\\u05d5\\\\u05dc\\\"}, {\\\"start\\\": 98.0, \\\"end\\\": 101.0, \\\"text\\\": \\\" \\\\u05e8\\\\u05e7 \\\\u05d0\\\\u05ea \\\\u05e4\\\\u05e0\\\\u05d9\\\\u05de\\\\u05da \\\\u05d6\\\\u05d4 \\\\u05d8\\\\u05e2\\\\u05dd \\\\u05e9\\\\u05dc \\\\u05e2\\\\u05d5\\\\u05e9\\\\u05e8\\\"}, {\\\"start\\\": 101.0, \\\"end\\\": 104.0, \\\"text\\\": \\\" \\\\u05ea\\\\u05d4\\\\u05d9\\\\u05d4 \\\\u05e8\\\\u05e7 \\\\u05e2\\\\u05d5\\\\u05d3 \\\\u05d1\\\\u05e6\\\\u05e2 \\\\u05ea\\\\u05de\\\\u05d9\\\\u05d3 \\\\u05de\\\\u05e2\\\\u05e0\\\\u05d9\\\\u05d9\\\\u05df\\\"}, {\\\"start\\\": 104.0, \\\"end\\\": 107.0, \\\"text\\\": \\\" \\\\u05dc\\\\u05db\\\\u05dd \\\\u05d7\\\\u05d1\\\\u05d9\\\\u05ea\\\\u05d4 \\\\u05ea\\\\u05d2\\\\u05d9\\\\u05e2 \\\\u05db\\\\u05dc \\\\u05d1\\\\u05d5\\\\u05e7\\\\u05e8\\\"}, {\\\"start\\\": 107.0, \\\"end\\\": 111.0, \\\"text\\\": \\\" \\\\u05d5\\\\u05d4\\\\u05e9\\\\u05d9\\\\u05e8 \\\\u05d4\\\\u05d6\\\\u05d4 \\\\u05ea\\\\u05de\\\\u05d9\\\\u05d3 \\\\u05d9\\\\u05ea\\\\u05e0\\\\u05d2\\\\u05d3\\\"}, {\\\"start\\\": 112.0, \\\"end\\\": 115.0, \\\"text\\\": \\\" \\\\u05dc\\\\u05db\\\\u05dd \\\\u05d7\\\\u05d1\\\\u05d9\\\\u05ea\\\\u05d4 \\\\u05d1\\\\u05e0\\\\u05ea\\\\u05e0\\\\u05d9\\\\u05d4\\\"}, {\\\"start\\\": 115.0, \\\"end\\\": 118.0, \\\"text\\\": \\\" \\\\u05d1\\\\u05d9\\\\u05d1\\\\u05d5\\\\u05d0\\\\u05d9 \\\\u05d9\\\\u05e9\\\\u05e8\\\\u05d0\\\\u05dc 24\\\"}, {\\\"start\\\": 118.0, \\\"end\\\": 122.0, \\\"text\\\": \\\" \\\\u05d1\\\\u05d2\\\\u05d8\\\\u05d9\\\\u05dd \\\\u05dc\\\\u05e9\\\\u05de\\\\u05d4 \\\\u05e2\\\\u05db\\\\u05e9\\\\u05d9\\\\u05d5 \\\\u05d0\\\\u05db\\\\u05dc\\\\u05ea\\\"}, {\\\"start\\\": 122.0, \\\"end\\\": 139.0, \\\"text\\\": \\\" \\\\u05d1\\\\u05d8\\\\u05d5\\\\u05d7 \\\\u05e9\\\\u05ea\\\\u05d7\\\\u05d6\\\\u05d5\\\\u05e8 \\\\u05d1\\\\u05d7\\\\u05d6\\\\u05e8\\\\u05d4\\\"}, {\\\"start\\\": 141.0, \\\"end\\\": 154.0, \\\"text\\\": \\\" \\\\u05dc\\\\u05db\\\\u05dd \\\\u05d7\\\\u05d1\\\\u05d9\\\\u05ea\\\\u05d4 \\\\u05d1\\\\u05e0\\\\u05ea\\\\u05e0\\\\u05d9\\\\u05d4\\\"}, {\\\"start\\\": 154.0, \\\"end\\\": 157.0, \\\"text\\\": \\\" \\\\u05d1\\\\u05d9\\\\u05d1\\\\u05d5\\\\u05d0\\\\u05d9 \\\\u05d9\\\\u05e9\\\\u05e8\\\\u05d0\\\\u05dc 24\\\"}, {\\\"start\\\": 157.0, \\\"end\\\": 160.0, \\\"text\\\": \\\" \\\\u05d1\\\\u05d2\\\\u05d8\\\\u05d9\\\\u05dd \\\\u05dc\\\\u05e9\\\\u05de\\\\u05d4 \\\\u05e2\\\\u05db\\\\u05e9\\\\u05d9\\\\u05d5 \\\\u05d0\\\\u05db\\\\u05dc\\\\u05ea\\\"}, {\\\"start\\\": 160.0, \\\"end\\\": 164.0, \\\"text\\\": \\\" \\\\u05d1\\\\u05d8\\\\u05d5\\\\u05d7 \\\\u05e9\\\\u05ea\\\\u05d7\\\\u05d6\\\\u05d5\\\\u05e8 \\\\u05d1\\\\u05d7\\\\u05d6\\\\u05e8\\\\u05d4\\\"}, {\\\"start\\\": 164.0, \\\"end\\\": 167.0, \\\"text\\\": \\\" \\\\u05dc\\\\u05db\\\\u05dd \\\\u05d7\\\\u05d1\\\\u05d9\\\\u05ea\\\\u05d4 \\\\u05d1\\\\u05e0\\\\u05ea\\\\u05e0\\\\u05d9\\\\u05d4\\\"}, {\\\"start\\\": 167.0, \\\"end\\\": 170.0, \\\"text\\\": \\\" \\\\u05d1\\\\u05d9\\\\u05d1\\\\u05d5\\\\u05d0\\\\u05d9 \\\\u05d9\\\\u05e9\\\\u05e8\\\\u05d0\\\\u05dc 24\\\"}, {\\\"start\\\": 170.0, \\\"end\\\": 173.0, \\\"text\\\": \\\" \\\\u05d1\\\\u05d2\\\\u05d8\\\\u05d9\\\\u05dd \\\\u05dc\\\\u05e9\\\\u05de\\\\u05d4 \\\\u05e2\\\\u05db\\\\u05e9\\\\u05d9\\\\u05d5 \\\\u05d0\\\\u05db\\\\u05dc\\\\u05ea\\\"}, {\\\"start\\\": 173.0, \\\"end\\\": 176.0, \\\"text\\\": \\\" \\\\u05d1\\\\u05d8\\\\u05d5\\\\u05d7 \\\\u05e9\\\\u05ea\\\\u05d7\\\\u05d6\\\\u05d5\\\\u05e8 \\\\u05d1\\\\u05d7\\\\u05d6\\\\u05e8\\\\u05d4\\\"}]\" }"
        val lyricsSegmentListType: Type = object : TypeToken<List<LyricsSegment?>?>() {}.type
        val lyricsSegments: List<LyricsSegment> =
            Gson().fromJson(JSONObject(payloadString).getString("body"), lyricsSegmentListType)
        _lyricsSegments.value = lyricsSegments.map { lyricsSegment ->
            val words = lyricsSegment.text.trim().split(" ")
            val segmentDuration = lyricsSegment.end - lyricsSegment.start
            val totalLength = words.joinToString("").length
            var offsetDuration = lyricsSegment.start
            val lyricsWords = words.map { word ->
                val wordDuration = segmentDuration * (word.length.toDouble() / totalLength)
                val lw = LyricsWord(word, start = offsetDuration, duration = wordDuration)
                offsetDuration += wordDuration
                lw
            }
            LyricsTimestampedSegment(text = lyricsWords)
        }
    }

    enum class WorkspaceState {
        INTRO, PICK, WAITING, IDLE,
    }

    enum class PlayerState {
        IDLE, PLAYING, PAUSE, END
    }

    private val _workspaceState = MutableStateFlow(WorkspaceState.PICK)
    val workspaceState: StateFlow<WorkspaceState> = _workspaceState
    fun setWorkspaceState(state: WorkspaceState) {
        _playingState.value = PlayerState.IDLE
        _workspaceState.value = state
    }

    private val _playingState = MutableStateFlow(PlayerState.IDLE)
    val playingState: StateFlow<PlayerState> = _playingState
    fun setPlayingState(state: PlayerState) {
        _playingState.value = state
    }

    private val _songFullName = MutableStateFlow("")
    val songFullName: StateFlow<String> = _songFullName

    suspend fun handleResultUriForAudioIntent(
        context: Context, contentResolver: ContentResolver?, uri: Uri?
    ) {
        uri?.let {
            contentResolver?.let {
                if (devTestMode) {

                    // resetting the streams because we are starting a new work
                    if (mediaInfo.bgMusicInputStream != null && mediaInfo.singerInputStream != null) {
                        mediaInfo.bgMusicInputStream?.close()
                        mediaInfo.singerInputStream?.close()
                        mediaInfo.bgMusicInputStream = null
                        mediaInfo.singerInputStream = null
                    }

                    if (mediaInfo.bgMusicInputStream == null && mediaInfo.singerInputStream == null) {
                        mediaInfo.bgMusicInputStream =
                            BufferedInputStream(contentResolver.openInputStream(uri))
                        setWorkspaceState(WorkspaceState.PICK)
                    } else {
                        mediaInfo.singerInputStream =
                            BufferedInputStream(contentResolver.openInputStream(uri))
                        mediaInfo.max(context, uri)
                        mediaInfo.getSongInfo(context, uri)
                        _songFullName.value = "${mediaInfo.sponsorTitle}${
                            if (mediaInfo.sponsorArtist.isNotEmpty()) " - ${mediaInfo.sponsorArtist}" else ""
                        }"
                        audioProcessor = AudioProcessor(mediaInfo)
                        val sec: Int = (mediaInfo.timeStampDuration % 1000 % 60).toInt()
                        val min: Int = (mediaInfo.timeStampDuration % 1000 / 60).toInt()
                        _durationTime.value =
                            "${if (min / 10 == 0) "0$min" else min}:${if (sec / 10 == 0) "0$sec" else sec}"
                        setWorkspaceState(WorkspaceState.IDLE)
                        resetAudio()
                        mockupLyrics()
                    }
                } else {
                    mediaInfo.getSongInfo(context, uri)
                    _songFullName.value = "${mediaInfo.sponsorTitle}${
                        if (mediaInfo.sponsorArtist.isNotEmpty()) " - ${mediaInfo.sponsorArtist}" else ""
                    }"

                    beginGenerateKaraoke(context, uri)
                    setWorkspaceState(WorkspaceState.WAITING)
                }
            }
        }

    }

    private fun beginGenerateKaraoke(context: Context, fileUri: Uri) {
        try {
            lyricsProvider = LyricsProvider(context)
            notification = SpleeterProgressNotification(context)
            val serviceSpleeterIntent = Intent(context, SpleeterService::class.java)
            serviceSpleeterIntent.putExtra(SpleeterService.KEYS.EXTRA_FILE_URI, fileUri)
            // TODO: UUID.randomUUID().toString()
            serviceSpleeterIntent.putExtra(SpleeterService.KEYS.EXTRA_OBJECT_KEY, "mashu")
            context.startService(serviceSpleeterIntent)
            notification?.showNotification()
        } catch (e: Exception) {
            e.printStackTrace()
            stopGenerateKaraoke(context)
        }
    }

    fun stopGenerateKaraoke(context: Context) {
        val serviceSpleeterIntent = Intent(context, SpleeterService::class.java)
        context.stopService(serviceSpleeterIntent)
        notification?.hideNotification()
    }

    private var microphoneAudioDispatcher: VocalAudioDispatcher? = null
    private var musicAudioDispatcher: SongAudioDispatcher? = null
    private var lastWindowPosition: Int = 0
    private var goodForThisWindowWatch: Boolean = false
    private var goodForOpinionWatch: Boolean = false
    private var expectedScore = 0
    fun getExpectedScore(): Int {
        return expectedScore
    }

    private var _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score


    data class NoteState(var noteF: Float, val volume: Float)

    private val _micNote = MutableStateFlow(NoteState(0f, 0f))
    private val _micNoteActive = MutableStateFlow(false)
    val micNoteActive: StateFlow<Boolean> = _micNoteActive
    val micNote: StateFlow<NoteState> = _micNote

    private val _sinNote = MutableStateFlow(NoteState(0f, 0f))
    private val _sinNoteActive = MutableStateFlow(false)
    val sinNoteActive: StateFlow<Boolean> = _sinNoteActive
    val sinNote: StateFlow<NoteState> = _sinNote

    private val _currentTime = MutableStateFlow("00:00")
    val currentTime: StateFlow<String> = _currentTime
    private val _durationTime = MutableStateFlow("00:00")
    val durationTime: StateFlow<String> = _durationTime
    private val _progress = MutableStateFlow(0.0f)
    val progress: StateFlow<Float> = _progress

    private val _similarityColor = MutableStateFlow(Color(0xFFFFFFFF))
    val similarityColor: StateFlow<Color> = _similarityColor

    private val _displaySingerVolume = MutableStateFlow(false)
    val displaySingerVolume: StateFlow<Boolean> = _displaySingerVolume
    fun displaySingerVolume(b: Boolean) {
        _displaySingerVolume.value = b
    }

    fun setSingerVolume(volume: Float) {
        if (volume > 0.06f) {
            audioProcessor?.volumeFactor = volume
        } else {
            audioProcessor?.volumeFactor = 0f
        }
    }

    fun getSingerVolume(): Float {
        return audioProcessor?.volumeFactor ?: 0f
    }


    private val _displayPitchFactor = MutableStateFlow(false)
    val displayPitchFactor: StateFlow<Boolean> = _displayPitchFactor
    fun displayPitchFactor(b: Boolean) {
        _displayPitchFactor.value = b
    }

    fun getPitchFactor(): Float {
        return (audioProcessor?.pitchFactor ?: 1f) - 0.5f
    }

    fun setPitchFactor(pitchFactor: Float) {
        if (pitchFactor > 0.06f) {
            audioProcessor?.pitchFactor = pitchFactor + 0.5f
        } else {
            audioProcessor?.pitchFactor = 0.5f
        }
    }


//    private val _displayPitchControls = MutableStateFlow(false)
//    val displayPitchControls: StateFlow<Boolean> = _displayPitchControls
//    fun displayPitchControls(b: Boolean) {
//        _displayPitchControls.value = b
//    }


    private val _isComputingPitchSinger = MutableStateFlow(false)
    val isComputingPitchSinger: StateFlow<Boolean> = _isComputingPitchSinger
    fun isComputingPitchSinger(b: Boolean) {
        audioProcessor?.computeAndPlaySingerSoundMode = b
        _isComputingPitchSinger.value = b
    }

    private val _isComputingPitchMic = MutableStateFlow(false)
    val isComputingPitchMic: StateFlow<Boolean> = _isComputingPitchMic
    fun isComputingPitchMic(b: Boolean) {
        audioProcessor?.computeAndPlayRecordedSoundMode = b
        _isComputingPitchMic.value = b
    }

    private var musicJob: Job? = null
    private var micJob: Job? = null
    private var jobsCompleted: Boolean = true


    fun startAudioDispatchers() {
        // Start the microphone audio dispatcher
        micJob = CoroutineScope(Dispatchers.IO).launch {
            val handlePitch: (Double, Int, Int, AudioProcessor.NotesSimilarity) -> Unit =
                { _, noteI, volume, similarity ->
                    _similarityColor.value = getColor(similarity)
                    if (noteI > 0) {
                        if (similarity == AudioProcessor.NotesSimilarity.Equal || similarity == AudioProcessor.NotesSimilarity.Close) {
                            goodForThisWindowWatch = true
                            _micNote.value = _sinNote.value
                        } else {
                            _micNote.value = NoteState(
                                AudioProcessor.rangeIndex(noteI),
                                AudioProcessor.shrinkVolume(volume)
                            )
                        }
                        if (similarity != AudioProcessor.NotesSimilarity.Idle) {
                            goodForOpinionWatch = true
                        }
                        _micNoteActive.value = true
                    } else {
                        _micNoteActive.value = false
                    }
                }
            microphoneAudioDispatcher = audioProcessor?.buildMicrophoneAudioDispatcher(handlePitch)
            // Start the audio dispatcher
            microphoneAudioDispatcher?.run()
        }

        // Start the music audio dispatcher
        musicJob = CoroutineScope(Dispatchers.IO).launch {
            val handlePitch: (Double, Int, Int) -> Unit = { musicTimeStamp, noteI, volume ->
                _progress.value = (musicTimeStamp / mediaInfo.timeStampDuration).toFloat()
                val sec: Int = (musicTimeStamp % 1000 % 60).toInt()
                val min: Int = (musicTimeStamp % 1000 / 60).toInt()
                _currentTime.value =
                    "${if (min / 10 == 0) "0$min" else min}:${if (sec / 10 == 0) "0$sec" else sec}"
                val currentWindowPosition: Int = (musicTimeStamp % 1000 % 60).toInt()
                if (lastWindowPosition != currentWindowPosition) {
                    if (goodForThisWindowWatch) {
                        _score.value++
                        goodForThisWindowWatch = false
                    }
                    if (goodForOpinionWatch) {
                        expectedScore++
                        goodForOpinionWatch = false
                    }
                }
                lastWindowPosition = currentWindowPosition

                if (noteI > 0) {
                    _sinNote.value = NoteState(
                        AudioProcessor.rangeIndex(noteI), AudioProcessor.shrinkVolume(volume)
                    )
                    _sinNoteActive.value = true
                } else {
                    _sinNoteActive.value = false
                }

                _lyricsSegments.value.let { thisLyricsSegments ->
                    if (thisLyricsSegments.isNotEmpty()) {
                        _lyricsScrollToPosition.value.let { currentPosition ->
                            _lyricsActiveWordIndex.value.let { index ->
                                if (index > -1) {
                                    thisLyricsSegments[currentPosition].text[index].let {
                                        if (!(it.start < musicTimeStamp && musicTimeStamp <= it.duration + it.start)) {
                                            _lyricsActiveWordIndex.value =
                                                thisLyricsSegments[currentPosition].text.indexOfFirst { w ->
                                                    w.start < musicTimeStamp && musicTimeStamp <= w.start + w.duration
                                                }
                                        }
                                    }
                                } else {
                                    val firstWord = thisLyricsSegments[currentPosition].text.first()
                                    if (firstWord.start < musicTimeStamp && musicTimeStamp <= firstWord.start + firstWord.duration) {
                                        _lyricsActiveWordIndex.value = 0
                                    }
                                }
                            }

                            val nextPosition = (currentPosition + 1) % thisLyricsSegments.size
                            val segmentBoundaries = Pair(
                                thisLyricsSegments[nextPosition].text.first().start,
                                thisLyricsSegments[nextPosition].text.last().start + thisLyricsSegments[nextPosition].text.last().duration
                            )
                            if (segmentBoundaries.first <= musicTimeStamp && musicTimeStamp < segmentBoundaries.second) {
                                _lyricsScrollToPosition.value = nextPosition
                                _lyricsActiveWordIndex.value = -1
                            }
                        }
                    }

                }

            }
            val onCompletion: () -> Unit = {
                if (_playingState.value == PlayerState.PLAYING) {
                    setPlayingState(PlayerState.END)
                }
                resetAudio()
            }
            setPlayingState(PlayerState.PLAYING)
            musicAudioDispatcher =
                audioProcessor?.buildMusicAudioDispatcher(handlePitch, onCompletion)
            // Start the audio dispatcher
            musicAudioDispatcher?.run()
        }
        jobsCompleted = false
    }

    fun pauseAudioDispatchers() {
        microphoneAudioDispatcher?.pause()
        musicAudioDispatcher?.pause()
        setPlayingState(PlayerState.PAUSE)
    }

    fun continueAudioDispatchers() {
        if (!jumpInProgress) {
            musicAudioDispatcher?.resume()
        }
        microphoneAudioDispatcher?.resume()
        setPlayingState(PlayerState.PLAYING)
    }

    private fun resetAudio() {
        _lyricsScrollToPosition.value = 0
        _lyricsActiveWordIndex.value = -1
        _progress.value = 0f
        _currentTime.value = "00:00"
        goodForThisWindowWatch = false
        jumpInProgress = false
        _micNote.value = NoteState(0f, 0f)
        _sinNote.value = NoteState(0f, 0f)
        _micNoteActive.value = false
        _sinNoteActive.value = false
        try {
            musicAudioDispatcher?.run {
                if (!this.isStopped) {
                    this.stop()
                }
            }
            microphoneAudioDispatcher?.run {
                if (!this.isStopped) {
                    this.stop()
                }
            }
            mediaInfo.singerInputStream?.run {
                if (markSupported()) {
                    reset()
                }
            }
            mediaInfo.bgMusicInputStream?.run {
                if (markSupported()) {
                    reset()
                }
            }

            if (!jobsCompleted) {
                jobsCompleted = true
                runBlocking {
                    joinAll(*listOfNotNull(musicJob, micJob).toTypedArray())
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val jumpLock = Mutex()
    private var jumpInProgress = false
    suspend fun jumpToTimestamp(time: Double) {
        if (jumpInProgress || _playingState.value == PlayerState.IDLE || _playingState.value == PlayerState.END) {
            // we can't just jump to lyrics without starting playing its just doesn't make any sense
            return
        }
        jumpLock.withLock {
            withContext(Dispatchers.IO) {
                jumpInProgress = true
                // display a loader
                if (time >= 0 && time < mediaInfo.timeStampDuration) {
                    val bytesToSkip = (time.toInt() * mediaInfo.voiceSampleRate * 2).toLong()
                    musicAudioDispatcher?.let {
                        if (_playingState.value == PlayerState.PLAYING) {
                            it.pause()
                        }
                        mediaInfo.singerInputStream?.run {
                            if (markSupported()) {
                                reset()
                            }
                        }
                        mediaInfo.bgMusicInputStream?.run {
                            if (markSupported()) {
                                reset()
                            }
                        }
                        it.skipBytes(bytesToSkip)
                        if (_playingState.value == PlayerState.PLAYING) {
                            it.resume()
                        }
                        val segmentIndex = _lyricsSegments.value.indexOfFirst { lyricsSegments ->
                            lyricsSegments.text.first().start <= time && lyricsSegments.text.last().start > time
                        }
                        _lyricsScrollToPosition.value = if (segmentIndex > -1) segmentIndex else 0
                        val wordIndex = if (segmentIndex > -1) _lyricsSegments.value[segmentIndex].text.indexOfFirst { word ->
                            word.start <= time && word.start + word.duration > time
                        } else -1
                        _lyricsActiveWordIndex.value = wordIndex
                    }
                }
                jumpInProgress = false
            }
        }

    }

    fun resetScoreAndPlayingState() {
        setPlayingState(PlayerState.IDLE)
        _score.value = 0
        expectedScore = 0
    }

    private var tempSingerFile: File? = null
    private var tempMusicFile: File? = null

    override fun notifyCompletion(
        vocalsUrl: URL,
        accompanimentUrl: URL,
    ) {
        appContext?.let { context ->
            viewModelScope.launch {
                try {
                    val lyricsObservable: suspend () -> List<LyricsTimestampedSegment>?
                    deleteTempFiles()
                    val singerObservable: suspend () -> BufferedInputStream? = {
                        mediaInfo.downloadAndExtractMedia(
                            context,
                            vocalsUrl,
                            tempSingerFile!!,
                        )
                    }
                    val musicObservable: suspend () -> BufferedInputStream? = {
                        mediaInfo.downloadAndExtractMedia(
                            context, accompanimentUrl, tempMusicFile!!
                        )
                    }

                    val songName = _songFullName.value
                    lyricsObservable = {
                        lyricsProvider?.invokeLyricsLambdaFunction(
                            songName, vocalsUrl.toString()
                        )
                    }

                    val streamsAndLyrics = try {
                        val singerStream = withContext(Dispatchers.IO) { singerObservable() }
                        val musicStream = withContext(Dispatchers.IO) { musicObservable() }
                        val lyricsList = withContext(Dispatchers.IO) { lyricsObservable() }
                        Pair(Pair(singerStream, musicStream), lyricsList)
                    } catch (e: Exception) {
                        null
                    }

                    streamsAndLyrics?.let { (streams, lyrics) ->

                        if (streams.first != null && streams.second != null && lyrics != null) {
                            mediaInfo.closeStreams()
                            mediaInfo.singerInputStream = streams.first
                            mediaInfo.bgMusicInputStream = streams.second

                            _lyricsSegments.value = lyrics

                            audioProcessor = AudioProcessor(mediaInfo)
                            val sec: Int = (mediaInfo.timeStampDuration % 1000 % 60).toInt()
                            val min: Int = (mediaInfo.timeStampDuration % 1000 / 60).toInt()
                            _durationTime.value =
                                "${if (min / 10 == 0) "0$min" else min}:${if (sec / 10 == 0) "0$sec" else sec}"
                            setWorkspaceState(WorkspaceState.IDLE)
                            resetAudio()
                            notification?.hideNotification()
                        }

                    } ?: run {
                        // Handle the case where the streams and lyrics are null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }


    override fun notifyFailed() {
        resetWorkspace()
    }

    fun resetWorkspace() {
        resetAudio()
        resetScoreAndPlayingState()
        mediaInfo.closeStreams()
        deleteTempFiles()
        _songFullName.value = ""
        _workspaceState.value = WorkspaceState.PICK
        notification?.hideNotification()
    }

    private val _notificationMessage = MutableStateFlow(LyricsSegment("", 0.0, 0.0))
    val notificationMessage: StateFlow<LyricsSegment> = _notificationMessage

    override fun notifyProgressChanged(progress: Int, message: String) {
        notification?.updateProgress(progress, message)
        _notificationMessage.value = LyricsSegment(message, 0.0, 0.0)
    }

    override fun notifyProgressChanged(progress: Int, message: String, duration: Double) {
        notification?.updateProgress(progress, message, duration * 1000.0)
        _notificationMessage.value = LyricsSegment(message, 0.0, duration)
    }

    fun initTempFiles(context: Context) {
        tempSingerFile = File(context.cacheDir, "vocalFile.wav")
        tempMusicFile = File(context.cacheDir, "musicFile.wav")
        deleteTempFiles()
    }

    private fun deleteTempFiles() {
        tempSingerFile?.apply {
            if (this.exists()) {
                this.delete()
            }
        }
        tempMusicFile?.apply {
            if (this.exists()) {
                this.delete()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaInfo.closeStreams()
        deleteTempFiles()
    }

    fun giveOpinionForScore(givenScore: Int): String {
        return when (((givenScore.toFloat() / expectedScore) * 100).toInt()) {
            in 0..55 -> "Try better next time!"
            in 56..74 -> "Nice work"
            in 75..84 -> "Well Done!"
            in 85..Infinity -> "Pro Singer!"
            else -> "Umm.. Try again?"
        }
    }

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog

    fun showDialog() {
        _showDialog.value = true
    }

    fun hideDialog() {
        _showDialog.value = false
    }

    fun getTimestampFromProgress(progress: Float): Double {
        return progress * mediaInfo.timeStampDuration
    }
}