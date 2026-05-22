package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "candy_levels")
data class CandyLevel(
    @PrimaryKey val id: Int,
    val isUnlocked: Boolean,
    val bestScore: Int = 0,
    val stars: Int = 0,
    val targetScore: Int,
    val maxMoves: Int,
    val gridSize: Int = 8,
    val candyTypesCount: Int = 4,
    val blockersCount: Int = 0
)
