package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.database.entity.FavoriteTipEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for local Room database operations on favorite study tips.
 */
@Dao
interface FavoriteTipDao {

    @Query("SELECT * FROM favorite_study_tips ORDER BY savedAtEpochMillis DESC")
    fun getAllFavoriteTips(): Flow<List<FavoriteTipEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_study_tips WHERE id = :id OR headline = :headline)")
    fun isTipFavorite(id: String, headline: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_study_tips WHERE id = :id)")
    fun isFavoriteById(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteTip(tip: FavoriteTipEntity)

    @Query("DELETE FROM favorite_study_tips WHERE id = :id OR headline = :headline")
    suspend fun deleteFavorite(id: String, headline: String)

    @Query("DELETE FROM favorite_study_tips WHERE id = :id")
    suspend fun deleteFavoriteById(id: String)

    @Query("DELETE FROM favorite_study_tips")
    suspend fun clearAll()
}
