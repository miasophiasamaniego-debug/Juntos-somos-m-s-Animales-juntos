package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM game_progress WHERE id = 1")
    fun getGameProgress(): Flow<GameProgressEntity?>

    @Query("SELECT * FROM game_progress WHERE id = 1")
    suspend fun getGameProgressSync(): GameProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGameProgress(progress: GameProgressEntity)

    @Query("UPDATE game_progress SET highScore = :highScore WHERE id = 1 AND :highScore > highScore")
    suspend fun updateHighScore(highScore: Int)

    @Query("UPDATE game_progress SET totalBottles = totalBottles + :bottles, totalCardboard = totalCardboard + :cardboard WHERE id = 1")
    suspend fun addResources(bottles: Int, cardboard: Int)
}
