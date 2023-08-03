package com.example.pitchmasterbeta.ui.workspace

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pitchmasterbeta.model.AudioProcessor
import com.example.pitchmasterbeta.model.LyricsSegment
import com.example.pitchmasterbeta.model.MediaInfo
import com.example.pitchmasterbeta.model.SongAudioDispatcher
import com.example.pitchmasterbeta.model.VocalAudioDispatcher
import com.example.pitchmasterbeta.utils.network.LyricsApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedInputStream
import java.lang.reflect.Type


class WorkspaceViewModel : ViewModel() {

    private var mediaInfo = MediaInfo()
    private lateinit var audioProcessor: AudioProcessor

    private val devTestMode: Boolean = true

    private val _lyricsScrollToPosition = MutableStateFlow(0)
    val lyricsScrollToPosition: StateFlow<Int> = _lyricsScrollToPosition

    // Function to trigger the smooth scroll from your ViewModel
    fun lyricsScrollToPosition(position: Int) {
        _lyricsScrollToPosition.value = position
    }

    // MutableState for storing your list of items obtained from the server
    private val _lyricsSegments = MutableStateFlow<List<LyricsSegment>>(emptyList())
    val lyricsSegments: StateFlow<List<LyricsSegment>> = _lyricsSegments

    fun fetchLyrics(lyricsApi: LyricsApi) {
        viewModelScope.launch {
            try {
                // Perform your Retrofit request here and update the list
                val itemsFromServer = lyricsApi.getItems()
                _lyricsSegments.value = itemsFromServer
            } catch (e: Exception) {
                // Handle any errors here
            }
        }
    }

    fun mockupLyrics() {
        val payloadString = "{\"statusCode\": 200, \"body\": \"[{\\\"start\\\": 0.0, \\\"end\\\": 26.5, \\\"text\\\": \\\" There ain't no gold in this river That I've been washing my hands in forever\\\"}, {\\\"start\\\": 26.5, \\\"end\\\": 38.66, \\\"text\\\": \\\" I know there is hope in these waters But I can't bring myself to swim when I am\\\"}, {\\\"start\\\": 38.66, \\\"end\\\": 50.74, \\\"text\\\": \\\" drowning in this silence, baby Let me in, go easy\\\"}, {\\\"start\\\": 50.74, \\\"end\\\": 66.74000000000001, \\\"text\\\": \\\" Help me, baby, I was still a child Didn't get the chance to feel the world around me\\\"}, {\\\"start\\\": 66.74, \\\"end\\\": 86.74, \\\"text\\\": \\\" I had no time to choose what I chose to do So go easy on me\\\"}, {\\\"start\\\": 86.74, \\\"end\\\": 100.74, \\\"text\\\": \\\" There ain't no room for things to change When we are both so deeply stuck in our ways\\\"}, {\\\"start\\\": 100.74, \\\"end\\\": 117.74, \\\"text\\\": \\\" You can't deny how hard I've tried I changed who I was to put you both first But now I give up\\\"}, {\\\"start\\\": 117.74, \\\"end\\\": 137.74, \\\"text\\\": \\\" Go easy on me, baby, I was still a child Didn't get the chance to feel the world around me\\\"}, {\\\"start\\\": 137.74, \\\"end\\\": 157.74, \\\"text\\\": \\\" I had no time to choose what I chose to do So go easy on me\\\"}, {\\\"start\\\": 158.74, \\\"end\\\": 174.74, \\\"text\\\": \\\" I had good intentions and the highest hopes But I know right now that probably doesn't even show\\\"}, {\\\"start\\\": 174.74, \\\"end\\\": 194.74, \\\"text\\\": \\\" Go easy on me, baby, I was still a child Didn't get the chance to feel the world around me\\\"}, {\\\"start\\\": 194.74, \\\"end\\\": 206.74, \\\"text\\\": \\\" I had no time to choose what I chose to do So go easy on me\\\"}]\"}"
        val lyricsSegmentListType: Type = object : TypeToken<List<LyricsSegment?>?>() {}.type
        _lyricsSegments.value = Gson().fromJson(JSONObject(payloadString).getString("body"), lyricsSegmentListType)
    }

    enum class WorkspaceState {
        INTRO,
        PICK,
        WAITING,
        IDLE,
    }

    enum class PlayerState {
        IDLE,
        PLAYING,
        PAUSE
    }

    private val _workspaceState = MutableStateFlow(WorkspaceState.PICK)
    val workspaceState: StateFlow<WorkspaceState> = _workspaceState
    fun setWorkspaceState(state: WorkspaceState) {
        _workspaceState.value = state
    }

    private val _playingState = MutableStateFlow(PlayerState.IDLE)
    val playingState: StateFlow<PlayerState> = _playingState
    private fun setPlayingState(state: PlayerState) {
        _playingState.value = state
    }

    suspend fun handleResultUriForAudioIntent(context: Context, contentResolver: ContentResolver?, uri: Uri?) {
        uri?.let {
            if (devTestMode) {
                contentResolver?.let {
                    val startProgress = ((mediaInfo.bgMusicInputStream == null && mediaInfo.singerInputStream == null) ||
                            (mediaInfo.bgMusicInputStream != null && mediaInfo.singerInputStream != null))
                    if (startProgress) {
                        mediaInfo.bgMusicInputStream = BufferedInputStream(contentResolver.openInputStream(uri))
                        mediaInfo.singerInputStream = null
                    } else {
                        mediaInfo.singerInputStream = BufferedInputStream(contentResolver.openInputStream(uri))
                    }
                    mediaInfo.max(context, uri) //223000 //2756 (sr: 88200, fb: 7104)
                    if (!startProgress) { //223000 //2756 (sr: 88200, fb: 7104)
                        audioProcessor = AudioProcessor(mediaInfo)
                        val sec: Int = (mediaInfo.timeStampDuration % 1000 % 60).toInt()
                        val min: Int = (mediaInfo.timeStampDuration % 1000 / 60).toInt()
                        _durationTime.value = "${if (min / 10 == 0) "0$min" else min}:${if (sec / 10 == 0) "0$sec" else sec}"
                        _durationTimestamp.value = mediaInfo.timeStampDuration
                        setWorkspaceState(WorkspaceState.IDLE)
                    }
                }
            } else {
                beginGenerateKaraoke(uri)
                setWorkspaceState(WorkspaceState.WAITING)
            }
        }

    }

    private fun beginGenerateKaraoke(uri: Uri) {

    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var microphoneAudioDispatcher: VocalAudioDispatcher? = null
    private var musicAudioDispatcher: SongAudioDispatcher? = null
    private var lastWindowPosition: Int = 0
    private var goodForThisWindowWatch: Boolean = false
    private var _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score

    private val _micNoteI = MutableStateFlow(0f)
    val micNoteI: StateFlow<Float> = _micNoteI
    private val _micActive = MutableStateFlow(false)
    val micActive: StateFlow<Boolean> = _micActive
    private val _micVolume = MutableStateFlow(0)
    val micVolume: StateFlow<Int> = _micVolume
    private val _sinNoteI = MutableStateFlow(0f)
    val sinNoteI: StateFlow<Float> = _sinNoteI
    private val _sinActive = MutableStateFlow(false)
    val sinActive: StateFlow<Boolean> = _sinActive
    private val _sinVolume = MutableStateFlow(0)
    val sinVolume : StateFlow<Int> = _sinVolume

    private val _currentTime = MutableStateFlow("00:00")
    val currentTime: StateFlow<String> = _currentTime
    private val _durationTime = MutableStateFlow("00:00")
    val durationTime: StateFlow<String> = _durationTime
    private val _currentTimestamp = MutableStateFlow(0.0)
    val currentTimestamp: StateFlow<Double> = _currentTimestamp
    private val _durationTimestamp = MutableStateFlow(0.0)
    val durationTimestamp: StateFlow<Double> = _durationTimestamp

    fun startAudioDispatchers() {
        // Start the microphone audio dispatcher
        coroutineScope.launch {
            val handlePitch: (Double, Int, Int, AudioProcessor.NotesSimilarity) -> Unit =
                { _, noteI, volume, similarity ->
                    if (noteI > 0) {
                        _micActive.value = true
                        _micNoteI.value = AudioProcessor.rangeIndex(noteI)
                        _micVolume.value = AudioProcessor.shrinkVolume(volume)
                        if (similarity == AudioProcessor.NotesSimilarity.Equal || similarity == AudioProcessor.NotesSimilarity.Close) {
                            goodForThisWindowWatch = true
                            _sinNoteI.value = _micNoteI.value
                        }
                    } else {
                        _micActive.value = false
                    }
            }
            microphoneAudioDispatcher = audioProcessor.buildMicrophoneAudioDispatcher(handlePitch)
            // Start the audio dispatcher
            microphoneAudioDispatcher?.run()
        }

        // Start the music audio dispatcher
        coroutineScope.launch {
            val handlePitch: (Double, Int, Int) -> Unit =
                { musicTimeStamp, noteI, volume ->
                    _currentTimestamp.value = musicTimeStamp
                    val sec: Int = (musicTimeStamp % 1000 % 60).toInt()
                    val min: Int = (musicTimeStamp % 1000 / 60).toInt()
                    _currentTime.value = "${if (min / 10 == 0) "0$min" else min}:${if (sec / 10 == 0) "0$sec" else sec}"
                    val currentWindowPosition: Int = (musicTimeStamp % 1000 % 60).toInt()
                    if (goodForThisWindowWatch && (lastWindowPosition != currentWindowPosition)) {
                        _score.value++
                        goodForThisWindowWatch = false
                    }
                    lastWindowPosition = currentWindowPosition

                    if (noteI > 0) {
                        _sinActive.value = true
                        _sinNoteI.value = AudioProcessor.rangeIndex(noteI)
                        _sinVolume.value = AudioProcessor.shrinkVolume(volume)
                    } else {
                        _sinActive.value = false
                    }
                    if (_lyricsSegments.value[lyricsScrollToPosition.value].start < musicTimeStamp) {
                        _lyricsScrollToPosition.value += ((_lyricsScrollToPosition.value + 1) % _lyricsSegments.value.size)
                    }
                }
            val onCompletion: () -> Unit = {
                setPlayingState(PlayerState.IDLE)
            }
            setPlayingState(PlayerState.PLAYING)
            musicAudioDispatcher = audioProcessor.buildMusicAudioDispatcher(handlePitch, onCompletion)
            // Start the audio dispatcher
            musicAudioDispatcher?.run()
        }
    }

}