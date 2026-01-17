package com.youyuan.music.compose.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.SearchApi
import com.youyuan.music.compose.api.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val apiClient: ApiClient
) : ViewModel() {

    private val searchApi: SearchApi = apiClient.createService(SearchApi::class.java)

    private val _searchQueryText = MutableStateFlow("")
    val searchQueryText : StateFlow<String> = _searchQueryText.asStateFlow()

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun searchSuggestions(keywords: String) {
        _searchQueryText.value = keywords
        if (keywords.isBlank()) {
            _searchSuggestions.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                val response = searchApi.searchSuggestions(
                    keywords = keywords,
                    type = "mobile"
                )
                _searchSuggestions.value = response.result?.allMatch?.mapNotNull { it.keyword } ?: emptyList()
            } catch (e: Exception) {
                _searchSuggestions.value = emptyList()
            }
        }
    }

    fun searchSongs(keywords: String) {
        _searchQueryText.value = keywords
        if (keywords.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = searchApi.searchSongs(
                    keywords = keywords,
                    limit = 50,
                    type = 1
                )
                _searchResults.value = response.result?.songs ?: emptyList()
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSearchSuggestions() {
        _searchQueryText.value = ""
        _searchSuggestions.value = emptyList()
    }
}