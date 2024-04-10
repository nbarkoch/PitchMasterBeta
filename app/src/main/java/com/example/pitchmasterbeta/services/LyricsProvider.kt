package com.example.pitchmasterbeta.services

import android.content.Context
import android.util.Log
import com.example.pitchmasterbeta.model.LyricsTimestampedSegment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject

class LyricsProvider(context: Context?) {
    private var context: Context? = null

    init {
        if (context != null) {
            this.context = context
        } else throw IllegalArgumentException("Context must be NoNull!")
    }


    companion object {
        fun extractData(payloadString: String): List<LyricsTimestampedSegment> {
            val responseJson = JSONObject(payloadString)
            val gson = Gson()
            val jsonBody = JSONObject(responseJson.getString("body"))

            val timestampedSegments: MutableList<LyricsTimestampedSegment> = gson.fromJson(
                jsonBody.getString("timestamped_segments"),
                object : TypeToken<List<LyricsTimestampedSegment>>() {}.type
            )
            if (timestampedSegments.isEmpty()) {
                Log.e("LyricsProvider", " - bad response - lyrics are empty :(")
            } else {
                Log.i("LyricsProvider", " - response: \n $timestampedSegments")
            }
            return timestampedSegments
        }
    }
}