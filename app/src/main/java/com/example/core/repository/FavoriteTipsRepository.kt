package com.example.core.repository

import android.content.Context
import com.example.core.database.NexoraDatabase
import com.example.core.database.dao.FavoriteTipDao
import com.example.core.database.entity.FavoriteTipEntity
import com.example.core.model.DailyStudyTip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Repository to manage locally saved favorite study tips via Room database.
 */
object FavoriteTipsRepository {

    private var dao: FavoriteTipDao? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _favoritesList = MutableStateFlow<List<FavoriteTipEntity>>(emptyList())
    val favoritesList: StateFlow<List<FavoriteTipEntity>> = _favoritesList.asStateFlow()

    fun initialize(context: Context) {
        if (dao == null) {
            val db = NexoraDatabase.getInstance(context)
            dao = db.favoriteTipDao()
            observeFavorites()
        }
    }

    fun initializeWithDao(customDao: FavoriteTipDao) {
        dao = customDao
        observeFavorites()
    }

    private fun observeFavorites() {
        scope.launch {
            dao?.getAllFavoriteTips()?.collect { list ->
                _favoritesList.value = list
            }
        }
    }

    fun getAllFavorites(): Flow<List<FavoriteTipEntity>> {
        return dao?.getAllFavoriteTips() ?: flowOf(emptyList())
    }

    fun isTipFavorite(id: String, headline: String): Flow<Boolean> {
        val currentDao = dao ?: return flowOf(false)
        return currentDao.isTipFavorite(id, headline)
    }

    suspend fun toggleFavorite(tip: DailyStudyTip, currentlyFavorite: Boolean): Boolean {
        val currentDao = dao ?: return false
        return if (currentlyFavorite) {
            currentDao.deleteFavorite(tip.id, tip.headline)
            false
        } else {
            currentDao.insertFavoriteTip(FavoriteTipEntity.fromDailyStudyTip(tip))
            true
        }
    }

    suspend fun saveFavorite(tip: DailyStudyTip) {
        dao?.insertFavoriteTip(FavoriteTipEntity.fromDailyStudyTip(tip))
    }

    suspend fun removeFavorite(id: String, headline: String) {
        dao?.deleteFavorite(id, headline)
    }

    suspend fun clearAll() {
        dao?.clearAll()
    }
}
