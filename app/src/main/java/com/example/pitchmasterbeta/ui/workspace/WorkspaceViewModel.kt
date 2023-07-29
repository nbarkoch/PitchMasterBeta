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

    private val _scrollToPosition = MutableStateFlow(0)
    val scrollToPosition: StateFlow<Int> = _scrollToPosition

    // Function to trigger the smooth scroll from your ViewModel
    fun scrollToPosition(position: Int) {
        _scrollToPosition.value = position
    }

    // MutableState for storing your list of items obtained from the server
    private val _yourItemsList = MutableStateFlow<List<LyricsSegment>>(emptyList())
    val yourItemsList: StateFlow<List<LyricsSegment>> = _yourItemsList

    fun fetchLyrics(lyricsApi: LyricsApi) {
        viewModelScope.launch {
            try {
                // Perform your Retrofit request here and update the list
                val itemsFromServer = lyricsApi.getItems()
                _yourItemsList.value = itemsFromServer
            } catch (e: Exception) {
                // Handle any errors here
            }
        }
    }

    fun mockupLyrics() {
        val payloadString = "{\"statusCode\": 200, \"body\": \"[{\\\"start\\\": 0.0, \\\"end\\\": 26.5, \\\"text\\\": \\\" There ain't no gold in this river That I've been washing my hands in forever\\\"}, {\\\"start\\\": 26.5, \\\"end\\\": 38.66, \\\"text\\\": \\\" I know there is hope in these waters But I can't bring myself to swim when I am\\\"}, {\\\"start\\\": 38.66, \\\"end\\\": 50.74, \\\"text\\\": \\\" drowning in this silence, baby Let me in, go easy\\\"}, {\\\"start\\\": 50.74, \\\"end\\\": 66.74000000000001, \\\"text\\\": \\\" Help me, baby, I was still a child Didn't get the chance to feel the world around me\\\"}, {\\\"start\\\": 66.74, \\\"end\\\": 86.74, \\\"text\\\": \\\" I had no time to choose what I chose to do So go easy on me\\\"}, {\\\"start\\\": 86.74, \\\"end\\\": 100.74, \\\"text\\\": \\\" There ain't no room for things to change When we are both so deeply stuck in our ways\\\"}, {\\\"start\\\": 100.74, \\\"end\\\": 117.74, \\\"text\\\": \\\" You can't deny how hard I've tried I changed who I was to put you both first But now I give up\\\"}, {\\\"start\\\": 117.74, \\\"end\\\": 137.74, \\\"text\\\": \\\" Go easy on me, baby, I was still a child Didn't get the chance to feel the world around me\\\"}, {\\\"start\\\": 137.74, \\\"end\\\": 157.74, \\\"text\\\": \\\" I had no time to choose what I chose to do So go easy on me\\\"}, {\\\"start\\\": 158.74, \\\"end\\\": 174.74, \\\"text\\\": \\\" I had good intentions and the highest hopes But I know right now that probably doesn't even show\\\"}, {\\\"start\\\": 174.74, \\\"end\\\": 194.74, \\\"text\\\": \\\" Go easy on me, baby, I was still a child Didn't get the chance to feel the world around me\\\"}, {\\\"start\\\": 194.74, \\\"end\\\": 206.74, \\\"text\\\": \\\" I had no time to choose what I chose to do So go easy on me\\\"}]\"}"
        val lyricsSegmentListType: Type = object : TypeToken<List<LyricsSegment?>?>() {}.type
        _yourItemsList.value = Gson().fromJson(JSONObject(payloadString).getString("body"), lyricsSegmentListType)
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
    private fun setWorkspaceState(state: WorkspaceState) {
        _workspaceState.value = state
    }

    suspend fun handleResultUriForAudioIntent(context: Context, contentResolver: ContentResolver?, uri: Uri?) {
        uri?.let {
            if (devTestMode) {
                contentResolver?.let {
                    val isGonnaBeReady = ((mediaInfo.bgMusicInputStream == null && mediaInfo.singerInputStream == null) ||
                            (mediaInfo.bgMusicInputStream != null && mediaInfo.singerInputStream != null))
                    if (!isGonnaBeReady) {
                        mediaInfo.bgMusicInputStream = BufferedInputStream(contentResolver.openInputStream(uri))
                        mediaInfo.singerInputStream = null
                    } else {
                        mediaInfo.singerInputStream = BufferedInputStream(contentResolver.openInputStream(uri))
                    }
                    mediaInfo.max(context, uri)
                    if (isGonnaBeReady) {
                        audioProcessor = AudioProcessor(mediaInfo)
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

    fun startAudioDispatchers() {
        // Start the microphone audio dispatcher
        coroutineScope.launch {
            val exampleLambda: (Double, Int, AudioProcessor.NotesSimilarity) -> Unit =
                { param1, param2, similarity ->
                // Do something with the parameters
                val result = param1 * param2
            }

            microphoneAudioDispatcher = audioProcessor.buildMicrophoneAudioDispatcher(exampleLambda)
            // Start the audio dispatcher
            microphoneAudioDispatcher?.run()


        }

        // Start the music audio dispatcher
        coroutineScope.launch {
            val exampleLambda: (Double, Int) -> Unit =
                { param1, param2 ->
                    // Do something with the parameters
                    val result = param1 * param2
                }

            musicAudioDispatcher = audioProcessor.buildMusicAudioDispatcher(exampleLambda, {})
            // Start the audio dispatcher
            musicAudioDispatcher?.run()
        }
    }







}