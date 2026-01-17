package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

data class SearchSuggestResponse(
    @SerializedName("result")
    val result: SearchSuggestResult? = null,
    @SerializedName("code")
    val code: Long? = null
)

data class SearchSuggestResult(
    @SerializedName("allMatch")
    val allMatch: List<SearchSuggestItem>? = null
)

data class SearchSuggestItem(
    @SerializedName("keyword")
    val keyword: String? = null,
    @SerializedName("type")
    val type: Long? = null,
    @SerializedName("alg")
    val alg: String? = null,
    @SerializedName("lastKeyword")
    val lastKeyword: String? = null,
    @SerializedName("feature")
    val feature: String? = null
)