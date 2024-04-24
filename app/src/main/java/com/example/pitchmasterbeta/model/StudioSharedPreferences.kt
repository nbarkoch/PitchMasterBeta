package com.example.pitchmasterbeta.model
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StudioSharedPreferences(context: Context) {

    data class KaraokeRef(val vocal: String, val music: String, val lyrics: List<LyricsTimestampedSegment>)

    private val sharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val onPreferenceChangeListener = OnPreferenceChangeListener()

    fun saveKaraoke(audioPath: String, karaokeRef: KaraokeRef) {
        sharedPreferences.edit().putString(audioPath, gson.toJson(karaokeRef)).apply()
    }

    fun remove(audioPath: String) {
        sharedPreferences.edit().remove(audioPath).apply()
    }

    fun getKaraoke(audioPath: String): KaraokeRef? {
        val jsonString = sharedPreferences.getString(audioPath, null)
        return if (jsonString != null) {
            gson.fromJson(jsonString, object : TypeToken<KaraokeRef>() {}.type)
        } else {
            null
        }
    }

    data class AudioPrev(val name: String, val path: String)
    fun saveAudioPrev(audioPrev: AudioPrev) {
        val array = getAllAudioPreviews()
        array.removeIf { aP -> aP.path == audioPrev.path }
        array.add(0, audioPrev)
        sharedPreferences.edit().putString(AUDIO_PREVIEWS_KEY, gson.toJson(array)).apply()
    }
    fun removeAudioPrev(audioPath: String) {
        val array = getAllAudioPreviews()
        array.removeIf { aP -> aP.path == audioPath }
        sharedPreferences.edit().putString(AUDIO_PREVIEWS_KEY, gson.toJson(array)).apply()
    }
    private fun getAllAudioPreviews(): ArrayList<AudioPrev> {
        val jsonString = sharedPreferences.getString(AUDIO_PREVIEWS_KEY, "[]")
        return gson.fromJson(jsonString, object : TypeToken<List<AudioPrev>>() {}.type)
    }

    inner class OnPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
            if (key == AUDIO_PREVIEWS_KEY) {
                _audioPreviews.value = getAllAudioPreviews()
            }
        }
    }

    private val _audioPreviews = MutableStateFlow<List<AudioPrev>>(getAllAudioPreviews())
    val audioPreviews: StateFlow<List<AudioPrev>> = _audioPreviews

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(onPreferenceChangeListener)
    }

    companion object {
        const val AUDIO_PREVIEWS_KEY = "audios"
        const val NAME = "StudioSharedPreferences"
    }

}