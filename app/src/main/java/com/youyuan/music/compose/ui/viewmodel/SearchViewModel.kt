package com.youyuan.music.compose.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youyuan.music.compose.data.model.SongItem
import com.youyuan.music.compose.data.repo.RepoRiskException
import com.youyuan.music.compose.data.repo.SearchRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 搜索 ViewModel —— 精简版。
 * 只存状态 + 调 SearchRepo，网络逻辑全部在 Repo 层。
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepo: SearchRepo,
) : ViewModel() {

    private val _searchQueryText = MutableStateFlow("")
    val searchQueryText: StateFlow<String> = _searchQueryText.asStateFlow()

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SongItem>>(emptyList())
    val searchResults: StateFlow<List<SongItem>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun searchSuggestions(keywords: String) {
        _searchQueryText.value = keywords
        if (keywords.isBlank()) {
            _searchSuggestions.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                _searchSuggestions.value = searchRepo.searchSuggestions(keywords)
            } catch (e: RepoRiskException) {
                _error.value = e.message
            } catch (_: Exception) {
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
                _searchResults.value = searchRepo.searchSongs(keywords)
            } catch (e: RepoRiskException) {
                _error.value = e.message
            } catch (_: Exception) {
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