package com.youyuan.music.compose.api.apis

import com.youyuan.music.compose.api.model.SearchSongsResponse
import com.youyuan.music.compose.api.model.SearchSuggestResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchApi {

    @GET("/search")
    suspend fun searchSongs(
        @Query("keywords") keywords: String,
        @Query("limit") limit: Int = 30,
        @Query("type") type: Int = 1,
    ) : SearchSongsResponse

    @GET("/search/suggest")
    suspend fun searchSuggestions(
        @Query("keywords") keywords: String,
        @Query("type") type: String = "mobile"
    ) : SearchSuggestResponse

}