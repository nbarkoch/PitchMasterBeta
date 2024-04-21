package com.example.pitchmasterbeta.model
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StudioSharedPreferences(context: Context) {

    data class KaraokeRef(val vocal: String, val music: String, val lyrics: List<LyricsTimestampedSegment>)

    private val sharedPreferences = context.getSharedPreferences("StudioSharedPreferences", Context.MODE_PRIVATE)
    private val gson = Gson()

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
}