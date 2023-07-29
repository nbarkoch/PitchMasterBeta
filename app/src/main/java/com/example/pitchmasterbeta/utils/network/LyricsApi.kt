package com.example.pitchmasterbeta.utils.network

import com.example.pitchmasterbeta.model.LyricsSegment
import retrofit2.http.GET

interface LyricsApi {
    @GET("your-endpoint")
    suspend fun getItems(): List<LyricsSegment>
}