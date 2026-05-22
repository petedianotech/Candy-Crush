package com.example.game

import java.util.UUID

enum class SpecialType {
    NONE,
    STRIPE_HORIZONTAL, // Clears the entire row when matched
    STRIPE_VERTICAL,   // Clears the entire column when matched
    WRAPPED,           // Explodes a 3x3 surrounding region when matched
    COLOR_BOMB         // Explodes all candies of matched color when swapped
}

data class CandyItem(
    val id: String = UUID.randomUUID().toString(),
    val type: Int,
    val row: Int,
    val col: Int,
    val specialType: SpecialType = SpecialType.NONE,
    val isBlocker: Boolean = false,
    val isMatched: Boolean = false,
    val isExploding: Boolean = false
) {
    // Copy item with updated grid position
    fun at(newRow: Int, newCol: Int): CandyItem {
        return this.copy(row = newRow, col = newCol)
    }
}

enum class PowerUpType {
    STRIPE_HAMMER,     // turns candy into striped candy
    RAINBOW_BRUSH,     // turns candy into Color Bomb
    CHOCO_SMASHER      // smashes/clears candy or blocker instantly
}

sealed interface GameUiState {
    object Idle : GameUiState
    data class LevelSelect(val levels: List<com.example.data.CandyLevel>) : GameUiState
    data class Playing(
        val level: com.example.data.CandyLevel,
        val board: List<List<CandyItem>>,
        val score: Int,
        val movesLeft: Int,
        val stars: Int,
        val currentCombo: Int = 1,
        val selectedCell: Pair<Int, Int>? = null,
        val isAnimating: Boolean = false,
        val isVictory: Boolean = false,
        val isGameOver: Boolean = false,
        val floatingScores: List<FloatingScore> = emptyList(),
        val activePowerUp: PowerUpType? = null,
        val stripeHammerCount: Int = 3,
        val rainbowBrushCount: Int = 3,
        val chocoSmasherCount: Int = 3
    ) : GameUiState
}

data class FloatingScore(
    val id: String = UUID.randomUUID().toString(),
    val score: Int,
    val row: Int,
    val col: Int,
    val message: String? = null
)
