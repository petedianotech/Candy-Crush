package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LevelRepository(private val levelDao: LevelDao) {
    val allLevels: Flow<List<CandyLevel>> = levelDao.getAllLevels()

    suspend fun updateLevel(level: CandyLevel) = withContext(Dispatchers.IO) {
        levelDao.updateLevel(level)
    }

    suspend fun getLevelById(id: Int): CandyLevel? = withContext(Dispatchers.IO) {
        levelDao.getLevelById(id)
    }

    suspend fun ensureLevelsPrepopulated() = withContext(Dispatchers.IO) {
        // Prepopulate with 500 levels mapping out a smooth dynamic difficulty ramp
        val levels = mutableListOf<CandyLevel>()
        for (i in 1..500) {
            val gridSize = when {
                i <= 5 -> 6
                i <= 12 -> 7
                else -> 8
            }
            val candyTypesCount = when {
                i <= 10 -> 4
                i <= 25 -> 5
                else -> 6
            }
            val blockersCount = when {
                i <= 3 -> 0
                i <= 8 -> 2
                i <= 15 -> 4
                i <= 30 -> 8
                else -> 12
            }
            levels.add(
                CandyLevel(
                    id = i,
                    isUnlocked = (i == 1),
                    targetScore = (100 + (i * 20)).coerceAtMost(2000),
                    maxMoves = (26 - (i / 4)).coerceAtLeast(12),
                    gridSize = gridSize,
                    candyTypesCount = candyTypesCount,
                    blockersCount = blockersCount
                )
            )
        }
        levelDao.insertLevels(levels)
    }
}
