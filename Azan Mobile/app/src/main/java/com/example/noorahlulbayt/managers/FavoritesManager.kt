package com.example.noorahlulbayt.managers

import android.content.Context
import com.example.noorahlulbayt.database.BrowserDatabase
import com.example.noorahlulbayt.models.Favorite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.*

class FavoritesManager(private val context: Context) {
    
    private val database = BrowserDatabase.getDatabase(context)
    
    fun getAllFavorites(): Flow<List<Favorite>> {
        return database.favoriteDao().getAllFavorites()
    }
    
    suspend fun addFavorite(url: String, title: String, icon: String = "") {
        val favorite = Favorite(
            url = url,
            title = title,
            icon = icon,
            dateAdded = Date()
        )
        database.favoriteDao().insertFavorite(favorite)
    }
    
    suspend fun removeFavorite(url: String) {
        database.favoriteDao().deleteFavoriteByUrl(url)
    }
    
    suspend fun isFavorite(url: String): Boolean {
        return database.favoriteDao().getFavorite(url) != null
    }
    
    suspend fun getFavorite(url: String): Favorite? {
        return database.favoriteDao().getFavorite(url)
    }
    
    suspend fun updateFavorite(favorite: Favorite) {
        database.favoriteDao().insertFavorite(favorite)
    }
    
    suspend fun getFavoritesCount(): Int {
        return database.favoriteDao().getFavoritesCount()
    }
    
    suspend fun clearAllFavorites() {
        // Note: Room doesn't have a direct clear method, so we need to implement this differently
        // For now, we'll use a workaround by getting all favorites and deleting them
        val favorites = database.favoriteDao().getAllFavorites().first()
        favorites.forEach { favorite: Favorite ->
            database.favoriteDao().deleteFavorite(favorite)
        }
    }
} 