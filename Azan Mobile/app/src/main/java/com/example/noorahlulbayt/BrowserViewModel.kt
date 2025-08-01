package com.example.noorahlulbayt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.noorahlulbayt.database.BrowserDatabase
import com.example.noorahlulbayt.managers.DownloadManager
import com.example.noorahlulbayt.managers.FavoritesManager
import com.example.noorahlulbayt.managers.HistoryManager
import com.example.noorahlulbayt.models.Download
import com.example.noorahlulbayt.models.Favorite
import com.example.noorahlulbayt.models.HistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()
    
    private val _isBlocked = MutableStateFlow(false)
    val isBlocked: StateFlow<Boolean> = _isBlocked.asStateFlow()
    
    // Managers
    private val favoritesManager = FavoritesManager(application)
    private val downloadManager = DownloadManager(application)
    private val historyManager = HistoryManager(application)
    
    // State flows for new features
    private val _favorites = MutableStateFlow<List<Favorite>>(emptyList())
    val favorites: StateFlow<List<Favorite>> = _favorites.asStateFlow()
    
    private val _downloads = MutableStateFlow<List<Download>>(emptyList())
    val downloads: StateFlow<List<Download>> = _downloads.asStateFlow()
    
    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()
    
    init {
        // Add initial tab
        addTab()
        
        // Load data
        loadFavorites()
        loadDownloads()
        loadHistory()
    }
    
    fun addTab() {
        if (_tabs.value.size < 5) { // Max 5 tabs
            val newTab = BrowserTab(
                id = System.currentTimeMillis(),
                url = "https://www.google.com",
                title = "New Tab"
            )
            _tabs.value = _tabs.value + newTab
        }
    }
    
    fun closeTab(index: Int) {
        if (_tabs.value.size > 1) {
            val updatedTabs = _tabs.value.toMutableList()
            updatedTabs.removeAt(index)
            _tabs.value = updatedTabs
        }
    }
    
    fun updateTabUrl(index: Int, url: String) {
        val updatedTabs = _tabs.value.toMutableList()
        if (index < updatedTabs.size) {
            updatedTabs[index] = updatedTabs[index].copy(url = url)
            _tabs.value = updatedTabs
        }
    }
    
    fun updateTabTitle(index: Int, title: String) {
        val updatedTabs = _tabs.value.toMutableList()
        if (index < updatedTabs.size) {
            updatedTabs[index] = updatedTabs[index].copy(title = title)
            _tabs.value = updatedTabs
        }
    }
    
    fun blockAllTabs() {
        _isBlocked.value = true
        viewModelScope.launch {
            // Block all tabs by loading about:blank
            val updatedTabs = _tabs.value.map { tab ->
                tab.copy(url = "about:blank", title = "Blocked")
            }
            _tabs.value = updatedTabs
        }
    }
    
    fun unblockAllTabs() {
        _isBlocked.value = false
    }
    
    fun navigateToUrl(index: Int, url: String) {
        val updatedTabs = _tabs.value.toMutableList()
        if (index < updatedTabs.size) {
            updatedTabs[index] = updatedTabs[index].copy(url = url)
            _tabs.value = updatedTabs
        }
    }
    
    // Favorites management
    fun addFavorite(url: String, title: String) {
        viewModelScope.launch {
            favoritesManager.addFavorite(url, title)
        }
    }
    
    fun removeFavorite(url: String) {
        viewModelScope.launch {
            favoritesManager.removeFavorite(url)
        }
    }
    
    fun isFavorite(url: String): Boolean {
        return _favorites.value.any { it.url == url }
    }
    
    private fun loadFavorites() {
        viewModelScope.launch {
            favoritesManager.getAllFavorites().collect { favorites ->
                _favorites.value = favorites
            }
        }
    }
    
    // Downloads management
    fun startDownload(url: String, filename: String? = null) {
        viewModelScope.launch {
            downloadManager.startDownload(url, filename)
        }
    }
    
    fun cancelDownload(downloadId: String) {
        viewModelScope.launch {
            downloadManager.cancelDownload(downloadId)
        }
    }
    
    fun deleteDownload(download: Download) {
        viewModelScope.launch {
            downloadManager.deleteDownload(download)
        }
    }
    
    private fun loadDownloads() {
        viewModelScope.launch {
            downloadManager.getAllDownloads().collect { downloads ->
                _downloads.value = downloads
            }
        }
    }
    
    // History management
    fun addHistoryEntry(url: String, title: String) {
        viewModelScope.launch {
            historyManager.addHistoryEntry(url, title)
        }
    }
    
    fun removeHistoryEntry(entry: HistoryEntry) {
        viewModelScope.launch {
            historyManager.removeHistoryEntry(entry)
        }
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            historyManager.clearHistory()
        }
    }
    
    fun searchHistory(query: String) {
        viewModelScope.launch {
            historyManager.searchHistory(query).collect { history ->
                _history.value = history
            }
        }
    }
    
    private fun loadHistory() {
        viewModelScope.launch {
            historyManager.getAllHistory().collect { history ->
                _history.value = history
            }
        }
    }
}

data class BrowserTab(
    val id: Long,
    val url: String,
    val title: String,
    val isBlocked: Boolean = false
) 