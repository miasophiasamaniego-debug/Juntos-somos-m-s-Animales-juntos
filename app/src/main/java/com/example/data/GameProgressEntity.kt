package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_progress")
data class GameProgressEntity(
    @PrimaryKey val id: Int = 1,
    val highScore: Int = 0,
    val totalBottles: Int = 0,
    val totalCardboard: Int = 0,
    val selectedAnimalId: String = "bunny",
    val unlockedAnimalIds: String = "bunny", // Comma separated: "bunny,puppy"
    val completedLevelMax: Int = 1,
    val houseRoofLevel: Int = 1,
    val houseWallLevel: Int = 1,
    val houseGardenLevel: Int = 0,
    val houseSolarLevel: Int = 0,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)
