package com.example.pitchmasterbeta.ui.workspace

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pitchmasterbeta.MainActivity.Companion.appContentResolver
import com.example.pitchmasterbeta.model.AudioProcessor
import com.example.pitchmasterbeta.model.LyricsSegment
import com.example.pitchmasterbeta.model.MediaInfo
import com.example.pitchmasterbeta.model.SongAudioDispatcher
import com.example.pitchmasterbeta.model.VocalAudioDispatcher
import com.example.pitchmasterbeta.model.getColor
import com.example.pitchmasterbeta.notifications.SpleeterProgressNotification
import com.example.pitchmasterbeta.services.LyricsProvider
import com.example.pitchmasterbeta.services.SpleeterService
import com.example.pitchmasterbeta.utils.network.LyricsApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

    private val devTestMode: Boolean = false

    private val _lyricsScrollToPosition = MutableStateFlow(0)
    val lyricsScrollToPosition: StateFlow<Int> = _lyricsScrollToPosition

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
        PAUSE,
        END
    }

    private val _workspaceState = MutableStateFlow(WorkspaceState.PICK)
    val workspaceState: StateFlow<WorkspaceState> = _workspaceState
    fun setWorkspaceState(state: WorkspaceState) {
        _workspaceState.value = state
    }

    private val _playingState = MutableStateFlow(PlayerState.IDLE)
    val playingState: StateFlow<PlayerState> = _playingState
    fun setPlayingState(state: PlayerState) {
        _playingState.value = state
    }

    private val _songFullName = MutableStateFlow("")
    val songFullName: StateFlow<String> = _songFullName

    suspend fun handleResultUriForAudioIntent(context: Context, contentResolver: ContentResolver?, uri: Uri?) {
        uri?.let {
            contentResolver?.let {
                if (devTestMode) {

                    // resetting the streams because we are starting a new work
                    if  (mediaInfo.bgMusicInputStream != null && mediaInfo.singerInputStream != null) {
                        mediaInfo.bgMusicInputStream?.close()
                        mediaInfo.singerInputStream?.close()
                        mediaInfo.bgMusicInputStream = null
                        mediaInfo.singerInputStream = null
                    }

                    if (mediaInfo.bgMusicInputStream == null && mediaInfo.singerInputStream == null) {
                        mediaInfo.bgMusicInputStream = BufferedInputStream(contentResolver.openInputStream(uri))
                        setWorkspaceState(WorkspaceState.PICK)
                    } else {
                        mediaInfo.singerInputStream = BufferedInputStream(contentResolver.openInputStream(uri))
                        mediaInfo.max(context, uri)
                        audioProcessor = AudioProcessor(mediaInfo)
                        val sec: Int = (mediaInfo.timeStampDuration % 1000 % 60).toInt()
                        val min: Int = (mediaInfo.timeStampDuration % 1000 / 60).toInt()
                        _durationTime.value = "${if (min / 10 == 0) "0$min" else min}:${if (sec / 10 == 0) "0$sec" else sec}"
                        setWorkspaceState(WorkspaceState.IDLE)
                        resetAudio()
                    }
                } else {
                    mediaInfo.getSongInfo(context, uri)
                    _songFullName.value = "${mediaInfo.sponsorTitle}${
                        if (mediaInfo.sponsorArtist.isNotEmpty()) 
                            " - ${mediaInfo.sponsorArtist}" else ""}"

                    beginGenerateKaraoke(context, uri)
                    setWorkspaceState(WorkspaceState.WAITING)
                }
            }
        }

    }

    private fun beginGenerateKaraoke(context: Context, fileUri: Uri) {
        try
        {
            lyricsProvider = LyricsProvider(context)
            notification = SpleeterProgressNotification(context)
            val serviceSpleeterIntent = Intent(context, SpleeterService::class.java)
            serviceSpleeterIntent.putExtra(SpleeterService.KEYS.EXTRA_FILE_URI, fileUri)
            serviceSpleeterIntent.putExtra(SpleeterService.KEYS.EXTRA_OBJECT_KEY, _songFullName.value.ifEmpty { "mashu" })
            context.startService(serviceSpleeterIntent)
            notification?.showNotification()
        } catch (e: Exception) {
            e.printStackTrace()
            notification?.hideNotification()
        }
    }

    private var microphoneAudioDispatcher: VocalAudioDispatcher? = null
    private var musicAudioDispatcher: SongAudioDispatcher? = null
    private var lastWindowPosition: Int = 0
    private var goodForThisWindowWatch: Boolean = false
    private var _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score


    data class NoteState(var noteF: Float, val volume: Float)

    private val _micNote = MutableStateFlow(NoteState(0f,0f))
    private val _micNoteActive = MutableStateFlow(false)
    val micNoteActive: StateFlow<Boolean> = _micNoteActive
    val micNote: StateFlow<NoteState> = _micNote

    private val _sinNote = MutableStateFlow(NoteState(0f,0f))
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

    private val _displayPitchControls = MutableStateFlow(false)
    val displayPitchControls: StateFlow<Boolean> = _displayPitchControls
    fun displayPitchControls(b: Boolean) {
        _displayPitchControls.value = b
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
                            _micNote.value = NoteState(AudioProcessor.rangeIndex(noteI), AudioProcessor.shrinkVolume(volume))
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
            val handlePitch: (Double, Int, Int) -> Unit =
                { musicTimeStamp, noteI, volume ->
                    _progress.value = (musicTimeStamp / mediaInfo.timeStampDuration).toFloat()
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
                        _sinNote.value = NoteState(AudioProcessor.rangeIndex(noteI), AudioProcessor.shrinkVolume(volume))
                        _sinNoteActive.value = true
                    } else {
                        _sinNoteActive.value = false
                    }

                    if (_lyricsSegments.value[lyricsScrollToPosition.value].end < musicTimeStamp
                        && lyricsScrollToPosition.value < _lyricsSegments.value.size - 1 ) {
                        _lyricsScrollToPosition.value = (_lyricsScrollToPosition.value + 1) % _lyricsSegments.value.size
                    }
                }
            val onCompletion: () -> Unit = {
                setPlayingState(PlayerState.END)
                resetAudio()
            }
            setPlayingState(PlayerState.PLAYING)
            musicAudioDispatcher = audioProcessor?.buildMusicAudioDispatcher(handlePitch, onCompletion)
            // Start the audio dispatcher
            musicAudioDispatcher?.run()
        }
    }

    fun pauseAudioDispatchers() {
        microphoneAudioDispatcher?.pause()
        musicAudioDispatcher?.pause()
        setPlayingState(PlayerState.PAUSE)
    }

    fun continueAudioDispatchers() {
        microphoneAudioDispatcher?.resume()
        musicAudioDispatcher?.resume()
        setPlayingState(PlayerState.PLAYING)
    }

    private fun resetAudio() {
        _lyricsScrollToPosition.value = 0
        _progress.value = 0f
        _currentTime.value = "00:00"
        goodForThisWindowWatch = false
        _micNote.value = NoteState(0f, 0f)
        _sinNote.value = NoteState(0f, 0f)
        _micNoteActive.value = false
        _sinNoteActive.value = false
        try {
            musicAudioDispatcher?.run { if(!this.isStopped) { this.stop() } }
            microphoneAudioDispatcher?.run { if(!this.isStopped) { this.stop() } }
            mediaInfo.singerInputStream?.reset()
            mediaInfo.bgMusicInputStream?.reset()
            runBlocking {
                musicJob?.join()
                micJob?.join()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resetScoreAndWorkspaceState() {
        setPlayingState(PlayerState.IDLE)
        _score.value = 0
    }

    private var tempSingerFile: File? = null
    private var tempMusicFile: File? = null

    override fun notifyCompletion(
        vocalsUrl: URL,
        accompanimentUrl: URL,
        bitRate: Int,
        sampleRate: Int
    ) {
        appContentResolver?.let { contentResolver ->
            viewModelScope.launch {
                try {
                    val singerObservable: suspend () -> BufferedInputStream?
                    val musicObservable: suspend () -> BufferedInputStream?
                    val lyricsObservable: suspend () -> List<LyricsSegment>?

                    resetTempFiles()
                    tempSingerFile?.let { singerFile ->
                        tempMusicFile?.let { musicFile ->
                            singerObservable = { mediaInfo.downloadAndExtractMedia(contentResolver, vocalsUrl, singerFile) }
                            musicObservable = { mediaInfo.downloadAndExtractMedia(contentResolver, accompanimentUrl, musicFile) }

                            val songName = _songFullName.value
                            lyricsObservable = { lyricsProvider?.invokeLyricsLambdaFunction(songName, vocalsUrl.toString()) }

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
                                    mediaInfo.prepareForExecution(bitRate, sampleRate)
                                    _lyricsSegments.value = lyrics

                                    audioProcessor = AudioProcessor(mediaInfo)
                                    val sec: Int = (mediaInfo.timeStampDuration % 1000 % 60).toInt()
                                    val min: Int = (mediaInfo.timeStampDuration % 1000 / 60).toInt()
                                    _durationTime.value = "${if (min / 10 == 0) "0$min" else min}:${if (sec / 10 == 0) "0$sec" else sec}"
                                    setWorkspaceState(WorkspaceState.IDLE)
                                    resetAudio()
                                    notification?.hideNotification()
                                }

                            } ?: run {
                                // Handle the case where the streams and lyrics are null
                            }

                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }



    override fun notifyFailed() {
        //TODO: Notify Failure
        notification?.hideNotification()
    }

    override fun notifyProgressChanged(progress: Int, message: String) {
        notification?.updateProgress(progress, message)
    }

    private fun resetTempFiles() {
        closeTempFiles()
        tempSingerFile = File.createTempFile("media", null)
        tempMusicFile = File.createTempFile("media", null)
    }

    private fun closeTempFiles() {
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
        closeTempFiles()
    }
}