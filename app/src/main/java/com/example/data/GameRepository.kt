package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(private val dao: GameDao) {

    val gameProgress: Flow<GameProgressEntity> = dao.getGameProgress().map {
        it ?: GameProgressEntity()
    }

    suspend fun getProgressSync(): GameProgressEntity {
        return dao.getGameProgressSync() ?: GameProgressEntity()
    }

    suspend fun saveProgress(progress: GameProgressEntity) {
        dao.saveGameProgress(progress)
    }

    suspend fun recordGameResult(
        score: Int,
        bottlesCollected: Int,
        cardboardCollected: Int,
        levelCompleted: Int?
    ) {
        val current = getProgressSync()
        val newHighScore = maxOf(current.highScore, score)
        val newBottles = current.totalBottles + bottlesCollected
        val newCardboard = current.totalCardboard + cardboardCollected
        val newMaxLevel = if (levelCompleted != null && levelCompleted >= current.completedLevelMax) {
            levelCompleted + 1
        } else {
            current.completedLevelMax
        }

        val updated = current.copy(
            highScore = newHighScore,
            totalBottles = newBottles,
            totalCardboard = newCardboard,
            completedLevelMax = newMaxLevel
        )
        dao.saveGameProgress(updated)
    }

    suspend fun setSelectedAnimal(animalId: String) {
        val current = getProgressSync()
        dao.saveGameProgress(current.copy(selectedAnimalId = animalId))
    }

    suspend fun unlockAnimal(animalId: String, bottleCost: Int, cardboardCost: Int): Boolean {
        val current = getProgressSync()
        if (current.totalBottles >= bottleCost && current.totalCardboard >= cardboardCost) {
            val unlockedList = current.unlockedAnimalIds.split(",").toMutableList()
            if (!unlockedList.contains(animalId)) {
                unlockedList.add(animalId)
            }
            val updated = current.copy(
                totalBottles = current.totalBottles - bottleCost,
                totalCardboard = current.totalCardboard - cardboardCost,
                unlockedAnimalIds = unlockedList.joinToString(","),
                selectedAnimalId = animalId
            )
            dao.saveGameProgress(updated)
            return true
        }
        return false
    }

    suspend fun upgradeHousePart(
        part: HousePart,
        bottleCost: Int,
        cardboardCost: Int
    ): Boolean {
        val current = getProgressSync()
        if (current.totalBottles >= bottleCost && current.totalCardboard >= cardboardCost) {
            val updated = when (part) {
                HousePart.ROOF -> current.copy(
                    houseRoofLevel = current.houseRoofLevel + 1,
                    totalBottles = current.totalBottles - bottleCost,
                    totalCardboard = current.totalCardboard - cardboardCost
                )
                HousePart.WALLS -> current.copy(
                    houseWallLevel = current.houseWallLevel + 1,
                    totalBottles = current.totalBottles - bottleCost,
                    totalCardboard = current.totalCardboard - cardboardCost
                )
                HousePart.GARDEN -> current.copy(
                    houseGardenLevel = current.houseGardenLevel + 1,
                    totalBottles = current.totalBottles - bottleCost,
                    totalCardboard = current.totalCardboard - cardboardCost
                )
                HousePart.SOLAR -> current.copy(
                    houseSolarLevel = current.houseSolarLevel + 1,
                    totalBottles = current.totalBottles - bottleCost,
                    totalCardboard = current.totalCardboard - cardboardCost
                )
            }
            dao.saveGameProgress(updated)
            return true
        }
        return false
    }

    suspend fun toggleSound(enabled: Boolean) {
        val current = getProgressSync()
        dao.saveGameProgress(current.copy(soundEnabled = enabled))
    }

    suspend fun toggleVibration(enabled: Boolean) {
        val current = getProgressSync()
        dao.saveGameProgress(current.copy(vibrationEnabled = enabled))
    }
}

enum class HousePart {
    ROOF, WALLS, GARDEN, SOLAR
}
