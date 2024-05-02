package com.example.pitchmasterbeta.ui.workspace

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Constraints.Companion.Infinity
import androidx.core.net.toFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pitchmasterbeta.MainActivity.Companion.appContext
import com.example.pitchmasterbeta.MainActivity.Companion.isPreview
import com.example.pitchmasterbeta.model.AudioProcessor
import com.example.pitchmasterbeta.model.AudioRecorder
import com.example.pitchmasterbeta.model.LyricsSegment
import com.example.pitchmasterbeta.model.LyricsTimestampedSegment
import com.example.pitchmasterbeta.model.MediaInfo
import com.example.pitchmasterbeta.model.SongAudioDispatcher
import com.example.pitchmasterbeta.model.StudioSharedPreferences
import com.example.pitchmasterbeta.model.StudioSharedPreferences.AudioPrev
import com.example.pitchmasterbeta.model.StudioSharedPreferences.KaraokeRef
import com.example.pitchmasterbeta.model.VocalAudioDispatcher
import com.example.pitchmasterbeta.model.getColor
import com.example.pitchmasterbeta.services.LyricsProvider
import com.example.pitchmasterbeta.services.SpleeterService
import com.example.pitchmasterbeta.utils.Mocks
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
import java.io.BufferedInputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class WorkspaceViewModel : ViewModel(), SpleeterService.ServiceNotifier {

    private var initialized = false
    private var mediaInfo = MediaInfo()
    private var audioProcessor = AudioProcessor(mediaInfo)

    //    private var lyricsProvider: LyricsProvider? = null
    private lateinit var sharedKaraokePreferences: StudioSharedPreferences
    private var audioUri: Uri? = null
    private var loadKaraokeJob: Job? = null

    private val devTestMode: Boolean = false

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
        _lyricsSegments.value = LyricsProvider.extractData(Mocks.DIFFERENT_LOVE)
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
        context: Context, uri: Uri?
    ) {
        uri?.let {
            context.contentResolver?.let { contentResolver ->
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
                        setSongName(context, uri)
                        val sec: Int = (mediaInfo.timeStampDuration % 1000 % 60).toInt()
                        val min: Int = (mediaInfo.timeStampDuration % 1000 / 60).toInt()
                        _durationTime.value =
                            "${if (min / 10 == 0) "0$min" else min}:${if (sec / 10 == 0) "0$sec" else sec}"
                        setWorkspaceState(WorkspaceState.IDLE)
                        resetAudio()
                        mockupLyrics()
                    }
                } else {
                    setSongName(context, uri)
                    audioUri = uri
                    _notificationMessage.value = LyricsSegment("Loading Karaoke", 0.0, 0.75)
                    setWorkspaceState(WorkspaceState.WAITING)
                    if (sharedKaraokePreferences.getKaraoke(uri.toString()) != null) {
                        loadKaraokeFromStorage()
                    } else {
                        beginGenerateKaraoke(uri, _songFullName.value)
                    }
                }
            }
        }

    }

    private fun beginGenerateKaraoke(fileUri: Uri, songName: String) {
        val context = appContext ?: return
        try {
            stopGenerateKaraoke()
            val serviceSpleeterIntent = Intent(context, SpleeterService::class.java)
            serviceSpleeterIntent.putExtra(SpleeterService.KEYS.EXTRA_FILE_URI, fileUri)
            val fileName = "mashu"//UUID.randomUUID().toString() // "mashu"
            serviceSpleeterIntent.putExtra(SpleeterService.KEYS.EXTRA_OBJECT_KEY, fileName)
            serviceSpleeterIntent.putExtra(SpleeterService.KEYS.EXTRA_FILE_NAME, songName)
            context.startService(serviceSpleeterIntent)
            serviceConnection.bindService()
        } catch (e: Exception) {
            e.printStackTrace()
            stopGenerateKaraoke()
        }
    }

    fun stopGenerateKaraoke() {
        val context = appContext ?: return
        serviceConnection.unbindService()
        val serviceSpleeterIntent = Intent(context, SpleeterService::class.java)
        context.stopService(serviceSpleeterIntent)
    }

    private fun finishGenerateKaraoke() {
        stopGenerateKaraoke()
    }


    private var microphoneAudioDispatcher: VocalAudioDispatcher? = null
    private var musicAudioDispatcher: SongAudioDispatcher? = null
    private var audioRecorder: AudioRecorder? = null
    private var lastWindowPosition: Int = 0
    private var goodForThisWindowWatch: Boolean = false
    private var goodForOpinionWatch: Boolean = false
    private var lyricsNowActive: Boolean = false
    private var expectedScore = 0
    fun getExpectedScore(): Int {
        return expectedScore
    }

    private var _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score


    data class NoteState(var noteF: Float, val volume: Float)

    private val _micNoteActive = MutableStateFlow(false)
    val micNoteActive: StateFlow<Boolean> = _micNoteActive
    private val _micNote = MutableStateFlow(NoteState(0f, 0f))
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
            audioProcessor.volumeFactor = volume
        } else {
            audioProcessor.volumeFactor = 0f
        }
    }

    fun getSingerVolume(): Float {
        return audioProcessor.volumeFactor
    }

    private val _isRecording = MutableStateFlow(true)
    val isRecording: StateFlow<Boolean> = _isRecording
    private val _isRecordingDisabled = MutableStateFlow(false)
    val isRecordingDisabled: StateFlow<Boolean> = _isRecordingDisabled
    private val _recordSaved = MutableStateFlow(false)
    val recordSaved: StateFlow<Boolean> = _recordSaved
    fun setRecording(trigger: Boolean) {
        _isRecording.value = trigger
    }


    private val _displayPitchFactor = MutableStateFlow(false)
    val displayPitchFactor: StateFlow<Boolean> = _displayPitchFactor
    fun displayPitchFactor(b: Boolean) {
        _displayPitchFactor.value = b
    }

    fun getPitchFactor(): Float {
        return (audioProcessor.pitchFactor) - 0.5f
    }

    companion object {
        private const val FACTOR_EFFECT = 0.5f
        private const val DEFAULT_FACTOR_VALUE = 0.5f
    }

    fun setPitchFactor(pitchFactor: Float) {
        audioProcessor.pitchFactor =
            pitchFactor * FACTOR_EFFECT + (1 - FACTOR_EFFECT * DEFAULT_FACTOR_VALUE)
    }


//    private val _displayPitchControls = MutableStateFlow(false)
//    val displayPitchControls: StateFlow<Boolean> = _displayPitchControls
//    fun displayPitchControls(b: Boolean) {
//        _displayPitchControls.value = b
//    }


    private val _isComputingPitchSinger = MutableStateFlow(false)
    val isComputingPitchSinger: StateFlow<Boolean> = _isComputingPitchSinger
    fun isComputingPitchSinger(b: Boolean) {
        audioProcessor.computeAndPlaySingerSoundMode = b
        _isComputingPitchSinger.value = b
    }

    private val _isComputingPitchMic = MutableStateFlow(false)
    val isComputingPitchMic: StateFlow<Boolean> = _isComputingPitchMic
    fun isComputingPitchMic(b: Boolean) {
        audioProcessor.computeAndPlayRecordedSoundMode = b
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
                    if (noteI > 0 && lyricsNowActive) {
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
            microphoneAudioDispatcher = audioProcessor.buildMicrophoneAudioDispatcher(handlePitch)
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

                updateActiveSegmentIndex(
                    musicTimeStamp, _lyricsScrollToPosition.value, _lyricsActiveWordIndex.value
                )
            }
            val onCompletion: () -> Unit = {
                if (_playingState.value == PlayerState.PLAYING) {
                    setPlayingState(PlayerState.END)
                }
                resetAudio()
            }
            setPlayingState(PlayerState.PLAYING)
            musicAudioDispatcher =
                audioProcessor.buildMusicAudioDispatcher(handlePitch, onCompletion)
            // Start the audio dispatcher
            musicAudioDispatcher?.run()
        }

        appContext?.let { context ->
            if (_isRecording.value) {
                audioRecorder = AudioRecorder(context = context)
                audioRecorder?.startRecording()
            }
        }

        jobsCompleted = false
    }

    private fun updateActiveSegmentIndex(musicTimeStamp: Double, currentPosition: Int, index: Int) {
        _lyricsSegments.value.takeIf { it.isNotEmpty() }?.let { thisLyricsSegments ->
            updateActiveWordIndex(thisLyricsSegments[currentPosition], index, musicTimeStamp)
            val nextSegmentPosition = (currentPosition + 1) % thisLyricsSegments.size
            val nextSegmentBoundaries = thisLyricsSegments[nextSegmentPosition].text
            if (musicTimeStamp in nextSegmentBoundaries.first().start..nextSegmentBoundaries.last().end) {
                _lyricsScrollToPosition.value = nextSegmentPosition
                _lyricsActiveWordIndex.value = -1
            }
        }
    }

    private fun updateActiveWordIndex(
        currentSegment: LyricsTimestampedSegment,
        currentActiveWordIndex: Int,
        musicTimeStamp: Double
    ) {
        if (currentActiveWordIndex != -1) {
            val nextActiveIndex = currentSegment.text.indexOfLast { word ->
                musicTimeStamp in word.start..word.end
            }
            if (nextActiveIndex != -1 && nextActiveIndex != _lyricsActiveWordIndex.value) {
                _lyricsActiveWordIndex.value = nextActiveIndex
                lyricsNowActive = true
            }
        } else {
            val firstWord = currentSegment.text.first()
            if (musicTimeStamp in firstWord.start..firstWord.end) {
                _lyricsActiveWordIndex.value = 0
                lyricsNowActive = true
            }
        }
        // Set lyricsNowActive to false if the conditions are not met
        if (currentActiveWordIndex == -1 || musicTimeStamp !in currentSegment.text.first().start..currentSegment.text.last().end) {
            lyricsNowActive = false
        }
    }

    fun pauseAudioDispatchers() {
        microphoneAudioDispatcher?.pause()
        audioRecorder?.pauseRecording()
        musicAudioDispatcher?.pause()
        setPlayingState(PlayerState.PAUSE)
    }

    fun continueAudioDispatchers() {
        if (!jumpInProgress) {
            musicAudioDispatcher?.resume()
        }
        microphoneAudioDispatcher?.resume()
        audioRecorder?.resumeRecording()
        setPlayingState(PlayerState.PLAYING)
    }

    private fun resetStreams() {
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
    }

    private fun resetDispatchers() {
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
        audioRecorder?.stopRecording()
    }

    private fun resetAudio() {
        _lyricsScrollToPosition.value = 0
        _lyricsActiveWordIndex.value = -1
        _progress.value = 0f
        _currentTime.value = "00:00"
        goodForThisWindowWatch = false
        jumpInProgress = false
        _isRecordingDisabled.value = false
        _micNote.value = NoteState(0f, 0f)
        _sinNote.value = NoteState(0f, 0f)
        _micNoteActive.value = false
        _sinNoteActive.value = false
        _recordSaved.value = false
        try {
            resetDispatchers()
            resetStreams()
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
        _isRecordingDisabled.value = true // since the audio is ruined, we cannot record, for now..
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
                        resetStreams()
                        it.skipBytes(bytesToSkip)
                        if (_playingState.value == PlayerState.PLAYING) {
                            it.resume()
                        }

                        _lyricsSegments.value.let { lyricsSegments ->
                            if (lyricsSegments.isNotEmpty()) {
                                val index = (0 until lyricsSegments.size - 1).indexOfLast { i ->
                                    val currentStart = lyricsSegments[i].text.first().start
                                    val nextStart = lyricsSegments[i + 1].text.first().start
                                    currentStart <= time && time < nextStart
                                }
                                val segmentIndex =
                                    if (index > -1) index else lyricsSegments.size - 1
                                _lyricsScrollToPosition.value = segmentIndex
                                _lyricsActiveWordIndex.value =
                                    _lyricsSegments.value[segmentIndex].text.indexOfLast { word ->
                                        word.start <= time && word.end > time
                                    }
                                lyricsNowActive = true
                            }
                        }
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
    private var shouldSaveData = true

    override fun notifyCompletion(
        vocalsFile: File,
        accompanimentFile: File,
        lyrics: List<LyricsTimestampedSegment>,
    ): Boolean {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (shouldSaveData) {
                    _showSaveAudioDialog.value = true
                    saveKaraoke(
                        KaraokeRef(
                            vocal = Uri.fromFile(vocalsFile).toString(),
                            music = Uri.fromFile(accompanimentFile).toString(),
                            lyrics = lyrics
                        )
                    )
                }
                val streams = transformMp3FilesToStreams(vocalsFile, accompanimentFile)
                setStudioData(streams.vocalStream, streams.musicStream, lyrics)
                finishGenerateKaraoke()
            } catch (e: Exception) {
                e.printStackTrace()
                // we had an error, sorry..
                setWorkspaceState(WorkspaceState.PICK)
            }
        }
        return serviceConnection.isServiceBound()
    }


    // Service connection object
    private val serviceConnection = object : ServiceConnection {
        private var isBound = false
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as SpleeterService.LocalBinder
            binder.service.setServiceNotifier(this@WorkspaceViewModel)
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }

        fun isServiceBound(): Boolean {
            return isBound
        }

        fun bindService() {
            val context = appContext ?: return
            if (isServiceRunning(context, SpleeterService::class.java) && !isBound) {
                context.bindService(
                    Intent(context, SpleeterService::class.java), this, Context.BIND_AUTO_CREATE
                )
                isBound = true
            }
        }

        fun unbindService() {
            val context = appContext ?: return
            if (isServiceRunning(context, SpleeterService::class.java) && isBound) {
                context.unbindService(this)
            }
            isBound = false
        }
    }

    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun setStudioData(
        vocalStream: BufferedInputStream,
        musicStream: BufferedInputStream,
        lyrics: List<LyricsTimestampedSegment>
    ) {
        mediaInfo.singerInputStream = vocalStream
        mediaInfo.bgMusicInputStream = musicStream
        _lyricsSegments.value = lyrics
        val sec: Int = (mediaInfo.timeStampDuration % 1000 % 60).toInt()
        val min: Int = (mediaInfo.timeStampDuration % 1000 / 60).toInt()
        _durationTime.value =
            "${if (min / 10 == 0) "0$min" else min}:${if (sec / 10 == 0) "0$sec" else sec}"
        setWorkspaceState(WorkspaceState.IDLE)
        resetAudio()
        audioUri?.let {
            sharedKaraokePreferences.saveAudioPrev(
                AudioPrev(
                    _songFullName.value, it.toString()
                )
            )
        }
    }


    data class GeneratedStreams(
        val vocalStream: BufferedInputStream, val musicStream: BufferedInputStream
    )

    private suspend fun transformMp3FilesToStreams(
        vocalsFile: File, accompanimentFile: File
    ): GeneratedStreams {
        val singerFile = tempSingerFile
        val musicFile = tempMusicFile
        if (singerFile == null || musicFile == null) {
            throw Exception("files doesn't exists")
        }
        return suspendCoroutine { continuation ->
            viewModelScope.launch(Dispatchers.IO) {
                appContext?.let { context ->
                    try {
                        val vocalStream = mediaInfo.getAudioBufferedInputStream(
                            context, vocalsFile, singerFile
                        )
                        val musicStream = mediaInfo.getAudioBufferedInputStream(
                            context, accompanimentFile, musicFile
                        )
                        continuation.resume(GeneratedStreams(vocalStream, musicStream))
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        }
    }

    private fun loadKaraokeFromStorage() {
        val uri = audioUri ?: return
        var exceptionWasThrown = false
        loadKaraokeJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val karaokeRef = sharedKaraokePreferences.getKaraoke(audioPath = uri.toString())
                    ?: throw Exception("loadKaraoke - karaokeStudio not exists")
                loadFromKaraokeRef(karaokeRef)
            } catch (e: Exception) {
                e.printStackTrace()
                exceptionWasThrown = true
            }
        }
        loadKaraokeJob?.run {
            invokeOnCompletion {
                if (exceptionWasThrown && !isCancelled) {
                    forgetKaraoke(uri.toString())
                    beginGenerateKaraoke(uri, _songFullName.value)
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
        _displaySingerVolume.value = false
        _displayPitchFactor.value = false
    }

    fun stopLoadKaraoke() {
        loadKaraokeJob?.cancel(CancellationException("interrupted"))
    }

    private val _notificationMessage = MutableStateFlow(LyricsSegment("", 0.0, 0.0))
    val notificationMessage: StateFlow<LyricsSegment> = _notificationMessage

    override fun notifyProgressChanged(progress: Int, message: String) {
        _notificationMessage.value = LyricsSegment(message, 0.0, 0.0)
    }

    override fun notifyProgressChanged(progress: Int, message: String, duration: Double) {
        _notificationMessage.value = LyricsSegment(message, 0.0, duration)
    }

    fun init(context: Context) {
        sharedKaraokePreferences = StudioSharedPreferences(context)
        initTempFiles(context)
        resetWorkspace()
        initialized = true
    }

    private fun initTempFiles(context: Context) {
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
        serviceConnection.unbindService()
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

    private val _showNotificationDialog = MutableStateFlow(false)
    val showNotificationDialog: StateFlow<Boolean> = _showNotificationDialog

    fun showNotificationDialog() {
        _showNotificationDialog.value = true
    }

    fun hideNotificationDialog() {
        _showNotificationDialog.value = false
    }

    private val _showSaveAudioDialog = MutableStateFlow(false)
    val showSaveAudioDialog: StateFlow<Boolean> = _showSaveAudioDialog

    fun hideSaveAudioDialog() {
        _showSaveAudioDialog.value = false
    }

    fun getTimestampFromProgress(progress: Float): Double {
        return progress * mediaInfo.timeStampDuration
    }

    private fun saveKaraoke(karaokeRef: KaraokeRef) {
        audioUri?.apply {
            sharedKaraokePreferences.saveKaraoke(audioUri.toString(), karaokeRef)
        }
    }

    fun forgetKaraoke() {
        audioUri?.let {
            forgetKaraoke(it.toString())
        }
    }

    fun forgetKaraoke(audioPath: String) {
        sharedKaraokePreferences.getKaraoke(audioPath)?.apply {
            deleteFile(this.vocal)
            deleteFile(this.music)
        }
        sharedKaraokePreferences.remove(audioPath)
        sharedKaraokePreferences.removeAudioPrev(audioPath)
    }

    private fun deleteFile(path: String) {
        try {
            val fileToDelete = Uri.parse(path).toFile()
            if (fileToDelete.exists()) {
                fileToDelete.delete()
            }
        } catch (e: Exception) {
            // Handle specific exceptions as needed
            e.printStackTrace()
        }
    }

    fun getIsInitialized(): Boolean {
        return initialized
    }

    fun loadKaraokeFromIntent(jsonString: String, audioPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val gson = Gson()
                val karaokeRef: KaraokeRef = gson.fromJson(
                    jsonString, object : TypeToken<KaraokeRef>() {}.type
                )
                audioUri = Uri.parse(audioPath)
                loadFromKaraokeRef(karaokeRef)
                if (shouldSaveData) {
                    _showSaveAudioDialog.value = true
                    saveKaraoke(karaokeRef)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // sorry, we had a problem..
                setWorkspaceState(WorkspaceState.PICK)
            }
        }
    }

    fun loadProgressFromIntent(
        progress: Int, message: String, duration: Double, audioPath: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = appContext ?: return@launch
                val uri = Uri.parse(audioPath) ?: return@launch
                serviceConnection.bindService()
                setSongName(context, uri)
                audioUri = uri
                setWorkspaceState(WorkspaceState.WAITING)
                notifyProgressChanged(progress, message, duration)
            } catch (e: Exception) {
                e.printStackTrace()
                setWorkspaceState(WorkspaceState.INTRO)
            }
        }
    }

    private suspend fun loadFromKaraokeRef(karaokeRef: KaraokeRef) {
        val vocalsFile = Uri.parse(karaokeRef.vocal).toFile()
        val accompanimentFile = Uri.parse(karaokeRef.music).toFile()
        val lyrics = karaokeRef.lyrics
        if (!vocalsFile.exists() || !accompanimentFile.exists()) {
            throw Exception("loadKaraoke - one or more of the files doesn't exist")
        }
        val streams = transformMp3FilesToStreams(vocalsFile, accompanimentFile)
        setStudioData(streams.vocalStream, streams.musicStream, lyrics)
    }

    fun audioPickList(): StateFlow<List<AudioPrev>> {
        return if (isPreview) MutableStateFlow(Mocks.AUDIO_PREVS)
               else sharedKaraokePreferences.audioPreviews
    }

    fun onPickAudio(audioPrev: AudioPrev) {
        audioUri = Uri.parse(audioPrev.path)
        _songFullName.value = audioPrev.name
        _notificationMessage.value = LyricsSegment("Loading Karaoke", 0.0, 0.75)
        setWorkspaceState(WorkspaceState.WAITING)
        loadKaraokeFromStorage()
    }

    private fun setSongName(context: Context, uri: Uri) {
        mediaInfo.getSongInfo(context, uri)
        _songFullName.value = "${mediaInfo.sponsorTitle}${
            if (mediaInfo.sponsorArtist.isNotEmpty()) " - ${mediaInfo.sponsorArtist}" else ""
        }"
    }

    fun saveRecording() {
        val recordDate = SimpleDateFormat("ddMMyyyyHHmm", Locale.getDefault()).format(Date())
        viewModelScope.launch(Dispatchers.IO) {
            _recordSaved.value = true
            audioRecorder?.saveRecording(fileName = "record${recordDate}")
        }
    }
}