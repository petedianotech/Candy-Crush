package com.example.game

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CandyDatabase
import com.example.data.CandyLevel
import com.example.data.LevelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val levelDao = CandyDatabase.getDatabase(application).levelDao
    private val repository = LevelRepository(levelDao)

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Idle)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
        } catch (e: Exception) {
            Log.e("GameViewModel", "Failed to init ToneGenerator", e)
        }
        loadLevels()
    }

    private fun loadLevels() {
        viewModelScope.launch {
            // First ensure levels are in DB
            repository.ensureLevelsPrepopulated()
            repository.allLevels.collect { levels ->
                if (_uiState.value is GameUiState.Idle || _uiState.value is GameUiState.LevelSelect) {
                    _uiState.value = GameUiState.LevelSelect(levels)
                }
            }
        }
    }

    fun selectLevel(level: CandyLevel) {
        val rows = level.gridSize
        val cols = level.gridSize
        val colorsCount = level.candyTypesCount

        // 1. Generate an initial board without any initial matches-of-3
        var board = generateValidInitialBoard(rows, cols, colorsCount, level.blockersCount)

        _uiState.value = GameUiState.Playing(
            level = level,
            board = board,
            score = 0,
            movesLeft = level.maxMoves,
            stars = 0,
            currentCombo = 1
        )
    }

    fun goBackToLevelSelect() {
        _uiState.value = GameUiState.Idle
        loadLevels()
    }

    fun selectNextLevel(currentLevelId: Int) {
        viewModelScope.launch {
            val nextLevel = repository.getLevelById(currentLevelId + 1)
            if (nextLevel != null && nextLevel.isUnlocked) {
                selectLevel(nextLevel)
            } else {
                goBackToLevelSelect()
            }
        }
    }

    private fun generateValidInitialBoard(
        rows: Int,
        cols: Int,
        colorsCount: Int,
        blockersCount: Int
    ): List<List<CandyItem>> {
        var board = List(rows) { r ->
            List(cols) { c ->
                CandyItem(type = Random.nextInt(colorsCount), row = r, col = c)
            }
        }

        // Add blocker square obstacles at random but symmetric grid spots
        var placedBlockers = 0
        val random = Random(42) // Seeded for deterministic but random blockers
        while (placedBlockers < blockersCount) {
            val r = random.nextInt(1, rows - 1)
            val c = random.nextInt(1, cols - 1)
            if (!board[r][c].isBlocker) {
                board = board.mapIndexed { ri, rowItems ->
                    rowItems.mapIndexed { ci, item ->
                        if (ri == r && ci == c) item.copy(type = -99, isBlocker = true) else item
                    }
                }
                placedBlockers++
            }
        }

        // Make sure we have no matching-3 at start
        var hasMatches = true
        var escapeCounter = 0
        while (hasMatches && escapeCounter < 100) {
            hasMatches = false
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val current = board[r][c]
                    if (current.isBlocker) continue

                    // Horizontal check
                    if (c >= 2) {
                        val left1 = board[r][c - 1]
                        val left2 = board[r][c - 2]
                        if (!left1.isBlocker && !left2.isBlocker && left1.type == current.type && left2.type == current.type) {
                            board = changeCandyTypeAt(board, r, c, (current.type + 1) % colorsCount)
                            hasMatches = true
                        }
                    }

                    // Vertical check
                    if (r >= 2) {
                        val top1 = board[r - 1][c]
                        val top2 = board[r - 2][c]
                        if (!top1.isBlocker && !top2.isBlocker && top1.type == current.type && top2.type == current.type) {
                            board = changeCandyTypeAt(board, r, c, (current.type + 2) % colorsCount)
                            hasMatches = true
                        }
                    }
                }
            }
            escapeCounter++
        }

        return board
    }

    private fun changeCandyTypeAt(
        board: List<List<CandyItem>>,
        r: Int,
        c: Int,
        newType: Int
    ): List<List<CandyItem>> {
        return board.mapIndexed { ri, rowItems ->
            rowItems.mapIndexed { ci, item ->
                if (ri == r && ci == c) item.copy(type = newType) else item
            }
        }
    }

    fun handleCellSelection(r: Int, c: Int) {
        val state = _uiState.value as? GameUiState.Playing ?: return
        if (state.isAnimating || state.isGameOver || state.isVictory) return

        if (state.activePowerUp != null) {
            playBeepTone(400, 40)
            usePowerUpOnCell(r, c)
            return
        }

        val board = state.board
        if (board[r][c].isBlocker) return

        playBeepTone(400, 40)

        val selected = state.selectedCell
        if (selected == null) {
            // First cell select
            _uiState.value = state.copy(selectedCell = Pair(r, c))
        } else {
            val (sr, sc) = selected
            // Check if adjacent (sharing an edge)
            val isAdjacent = (Math.abs(sr - r) == 1 && sc == c) || (Math.abs(sc - c) == 1 && sr == r)

            if (isAdjacent) {
                // Try swipe / swap
                swapAndMatch(sr, sc, r, c)
            } else {
                // Select different cell
                _uiState.value = state.copy(selectedCell = Pair(r, c))
            }
        }
    }

    fun handleSwipe(fromRow: Int, fromCol: Int, dRow: Int, dCol: Int) {
        val state = _uiState.value as? GameUiState.Playing ?: return
        if (state.isAnimating || state.isGameOver || state.isVictory) return

        val rows = state.level.gridSize
        val cols = state.level.gridSize
        val targetRow = fromRow + dRow
        val targetCol = fromCol + dCol

        if (targetRow in 0 until rows && targetCol in 0 until cols) {
            if (!state.board[fromRow][fromCol].isBlocker && !state.board[targetRow][targetCol].isBlocker) {
                swapAndMatch(fromRow, fromCol, targetRow, targetCol)
            }
        }
    }

    fun selectPowerUp(powerUpType: PowerUpType) {
        val state = _uiState.value as? GameUiState.Playing ?: return
        if (state.isAnimating || state.isGameOver || state.isVictory) return
        
        val nextPowerUp = if (state.activePowerUp == powerUpType) null else powerUpType
        _uiState.value = state.copy(activePowerUp = nextPowerUp)
    }

    private fun usePowerUpOnCell(r: Int, c: Int) {
        val state = _uiState.value as? GameUiState.Playing ?: return
        val powerUp = state.activePowerUp ?: return
        val board = state.board
        
        val hasStock = when (powerUp) {
            PowerUpType.STRIPE_HAMMER -> state.stripeHammerCount > 0
            PowerUpType.RAINBOW_BRUSH -> state.rainbowBrushCount > 0
            PowerUpType.CHOCO_SMASHER -> state.chocoSmasherCount > 0
        }
        if (!hasStock) {
            _uiState.value = state.copy(activePowerUp = null)
            return
        }

        // Stripe Hammer / Rainbow Brush cannot be used on blockers
        if ((powerUp == PowerUpType.STRIPE_HAMMER || powerUp == PowerUpType.RAINBOW_BRUSH) && board[r][c].isBlocker) {
            _uiState.value = state.copy(activePowerUp = null)
            return
        }
        
        viewModelScope.launch {
            _uiState.value = state.copy(isAnimating = true, selectedCell = null, activePowerUp = null)
            
            val nextStripeCount = if (powerUp == PowerUpType.STRIPE_HAMMER) state.stripeHammerCount - 1 else state.stripeHammerCount
            val nextRainbowCount = if (powerUp == PowerUpType.RAINBOW_BRUSH) state.rainbowBrushCount - 1 else state.rainbowBrushCount
            val nextSmasherCount = if (powerUp == PowerUpType.CHOCO_SMASHER) state.chocoSmasherCount - 1 else state.chocoSmasherCount
            
            val updatedBoard = board.mapIndexed { ri, rowItems ->
                rowItems.mapIndexed { ci, colItem ->
                    if (ri == r && ci == c) {
                        when (powerUp) {
                            PowerUpType.STRIPE_HAMMER -> {
                                val isHorizontal = Math.random() < 0.5
                                val sType = if (isHorizontal) SpecialType.STRIPE_HORIZONTAL else SpecialType.STRIPE_VERTICAL
                                colItem.copy(specialType = sType, isBlocker = false)
                            }
                            PowerUpType.RAINBOW_BRUSH -> {
                                colItem.copy(specialType = SpecialType.COLOR_BOMB, isBlocker = false)
                            }
                            PowerUpType.CHOCO_SMASHER -> {
                                colItem.copy(isExploding = true)
                            }
                        }
                    } else {
                        colItem
                    }
                }
            }
            
            var earnedPoints = 0
            val floatScores = mutableListOf<FloatingScore>()
            when (powerUp) {
                PowerUpType.STRIPE_HAMMER -> {
                    floatScores.add(FloatingScore(score = 200, row = r, col = c, message = "STRIPE!"))
                    earnedPoints += 200
                }
                PowerUpType.RAINBOW_BRUSH -> {
                    floatScores.add(FloatingScore(score = 500, row = r, col = c, message = "COLOR BOMB!"))
                    earnedPoints += 500
                }
                PowerUpType.CHOCO_SMASHER -> {
                    floatScores.add(FloatingScore(score = 100, row = r, col = c, message = "SMASHED!"))
                    earnedPoints += 100
                }
            }
            
            _uiState.value = state.copy(
                board = updatedBoard,
                score = state.score + earnedPoints,
                stripeHammerCount = nextStripeCount,
                rainbowBrushCount = nextRainbowCount,
                chocoSmasherCount = nextSmasherCount,
                floatingScores = floatScores
            )
            
            playBeepTone(900, 150)
            delay(150L)
            
            runCascadeSequence(
                startingBoard = updatedBoard,
                startingScore = state.score + earnedPoints,
                movesLeft = state.movesLeft,
                isColorBombSwap = false,
                nextStripeCount = nextStripeCount,
                nextRainbowCount = nextRainbowCount,
                nextSmasherCount = nextSmasherCount
            )
        }
    }

    private fun swapAndMatch(r1: Int, c1: Int, r2: Int, c2: Int) {
        viewModelScope.launch {
            val state = _uiState.value as? GameUiState.Playing ?: return@launch
            _uiState.value = state.copy(isAnimating = true, selectedCell = null)

            // Perform visual swap animation locally
            var currentBoard = swapCandiesOnBoard(state.board, r1, c1, r2, c2)
            _uiState.value = state.copy(board = currentBoard)
            delay(200L)

            // Detect matches on swap
            val matches = findMatches(currentBoard)

            // Handle Multicolor Candy specials
            val isColorBombSwap = currentBoard[r1][c1].specialType == SpecialType.COLOR_BOMB || currentBoard[r2][c2].specialType == SpecialType.COLOR_BOMB

            if (matches.isNotEmpty() || isColorBombSwap) {
                // Move validated! Reduce moves left
                val nextMoves = state.movesLeft - 1
                runCascadeSequence(
                    startingBoard = currentBoard,
                    startingScore = state.score,
                    movesLeft = nextMoves,
                    isColorBombSwap = isColorBombSwap,
                    bombR1 = r1, bombC1 = c1, bombR2 = r2, bombC2 = c2
                )
            } else {
                // Invalid move - Swap back
                playBeepTone(150, 100)
                currentBoard = swapCandiesOnBoard(currentBoard, r1, c1, r2, c2)
                _uiState.value = state.copy(board = currentBoard, isAnimating = false)
            }
        }
    }

    private suspend fun runCascadeSequence(
        startingBoard: List<List<CandyItem>>,
        startingScore: Int,
        movesLeft: Int,
        isColorBombSwap: Boolean = false,
        bombR1: Int = -1, bombC1: Int = -1, bombR2: Int = -1, bombC2: Int = -1,
        nextStripeCount: Int = -1,
        nextRainbowCount: Int = -1,
        nextSmasherCount: Int = -1
    ) {
        val state = _uiState.value as? GameUiState.Playing ?: return
        
        var currentScore = startingScore
        var isFirstCycle = true
        var combo = 1
        var activeBoard = startingBoard

        var finalStripe = if (nextStripeCount >= 0) nextStripeCount else state.stripeHammerCount
        var finalRainbow = if (nextRainbowCount >= 0) nextRainbowCount else state.rainbowBrushCount
        var finalSmasher = if (nextSmasherCount >= 0) nextSmasherCount else state.chocoSmasherCount

        // Resolve Color Bomb swap immediately if present
        if (isColorBombSwap && bombR1 >= 0) {
            val (boardAfterBomb, earnedPoints) = resolveColorBombExplosion(activeBoard, bombR1, bombC1, bombR2, bombC2)
            activeBoard = boardAfterBomb
            currentScore += earnedPoints
            playBeepTone(800, 150)
            delay(300L)
        }

        var cascadeActive = true
        while (cascadeActive) {
            val cycleMatches = findMatches(activeBoard)
            if (cycleMatches.isEmpty() && (!isFirstCycle || !isColorBombSwap)) {
                cascadeActive = false
            } else {
                isFirstCycle = false

                // Mark elements as exploding
                activeBoard = markMatchesForExplosion(activeBoard, cycleMatches)
                _uiState.value = state.copy(
                    board = activeBoard,
                    score = currentScore,
                    movesLeft = movesLeft,
                    stripeHammerCount = finalStripe,
                    rainbowBrushCount = finalRainbow,
                    chocoSmasherCount = finalSmasher
                )
                playBeepTone(400 + combo * 100, 80)
                delay(250L) // visual explosion speed

                // Apply standard match scores & resolve special creation
                val outcome = applyMatchesAndScore(activeBoard, cycleMatches, combo, state.level.candyTypesCount)
                currentScore += outcome.pointsEarned
                activeBoard = outcome.board
                
                outcome.rewardedPowerUps.forEach { pu ->
                    if (pu == PowerUpType.STRIPE_HAMMER) finalStripe++
                    if (pu == PowerUpType.RAINBOW_BRUSH) finalRainbow++
                    if (pu == PowerUpType.CHOCO_SMASHER) finalSmasher++
                }

                // Visual flash after resolve
                _uiState.value = state.copy(
                    board = activeBoard,
                    score = currentScore,
                    stripeHammerCount = finalStripe,
                    rainbowBrushCount = finalRainbow,
                    chocoSmasherCount = finalSmasher
                )
                delay(100L)

                // Float score highlights
                val newFloatingScores = outcome.newFloats
                _uiState.value = state.copy(
                    floatingScores = newFloatingScores,
                    stripeHammerCount = finalStripe,
                    rainbowBrushCount = finalRainbow,
                    chocoSmasherCount = finalSmasher
                )

                // Apply gravity drop
                activeBoard = dropCandiesAndRefill(activeBoard, state.level.candyTypesCount)
                _uiState.value = state.copy(
                    board = activeBoard,
                    stripeHammerCount = finalStripe,
                    rainbowBrushCount = finalRainbow,
                    chocoSmasherCount = finalSmasher
                )
                delay(350L) // gravity settle speed

                combo++
            }
        }

        // Check endgame conditions
        val isWin = currentScore >= state.level.targetScore
        val outOfMoves = movesLeft <= 0
        val isLoss = !isWin && outOfMoves

        var stars = 0
        if (isWin) {
            stars = when {
                currentScore >= state.level.targetScore * 1.5 -> 3
                currentScore >= state.level.targetScore * 1.2 -> 2
                else -> 1
            }
        }

        if (isWin) {
            saveCompletedLevel(state.level.id, currentScore, stars)
        }

        _uiState.value = state.copy(
            board = activeBoard,
            score = currentScore,
            movesLeft = movesLeft,
            stars = stars,
            isAnimating = false,
            isVictory = isWin,
            isGameOver = isLoss,
            floatingScores = emptyList(),
            stripeHammerCount = finalStripe,
            rainbowBrushCount = finalRainbow,
            chocoSmasherCount = finalSmasher
        )

        if (isWin) {
            playBeepTone(900, 200)
            delay(150L)
            playBeepTone(1200, 400)
        } else if (isLoss) {
            playBeepTone(250, 400)
        }
    }

    private fun swapCandiesOnBoard(
        board: List<List<CandyItem>>,
        r1: Int, c1: Int, r2: Int, c2: Int
    ): List<List<CandyItem>> {
        val mutable = board.map { it.toMutableList() }.toMutableList()
        val temp = mutable[r1][c1]
        mutable[r1][c1] = mutable[r2][c2].copy(row = r1, col = c1)
        mutable[r2][c2] = temp.copy(row = r2, col = c2)
        return mutable
    }

    private data class MatchGroup(
        val type: Int,
        val cells: Set<Pair<Int, Int>>
    )

    private fun findMatches(board: List<List<CandyItem>>): List<MatchGroup> {
        val rows = board.size
        val cols = board[0].size
        val confirmedMatches = mutableListOf<MatchGroup>()

        // 1. Horizontal Scans
        for (r in 0 until rows) {
            var colStart = 0
            while (colStart < cols) {
                val currentType = board[r][colStart].type
                if (board[r][colStart].isBlocker) {
                    colStart++
                    continue
                }

                var matchLen = 1
                while (colStart + matchLen < cols &&
                    !board[r][colStart + matchLen].isBlocker &&
                    board[r][colStart + matchLen].type == currentType
                ) {
                    matchLen++
                }

                if (matchLen >= 3) {
                    val cells = (0 until matchLen).map { Pair(r, colStart + it) }.toSet()
                    confirmedMatches.add(MatchGroup(currentType, cells))
                    colStart += matchLen
                } else {
                    colStart++
                }
            }
        }

        // 2. Vertical Scans
        for (c in 0 until cols) {
            var rowStart = 0
            while (rowStart < rows) {
                val currentType = board[rowStart][c].type
                if (board[rowStart][c].isBlocker) {
                    rowStart++
                    continue
                }

                var matchLen = 1
                while (rowStart + matchLen < rows &&
                    !board[rowStart + matchLen][c].isBlocker &&
                    board[rowStart + matchLen][c].type == currentType
                ) {
                    matchLen++
                }

                if (matchLen >= 3) {
                    val cells = (0 until matchLen).map { Pair(rowStart + it, c) }.toSet()
                    confirmedMatches.add(MatchGroup(currentType, cells))
                    rowStart += matchLen
                } else {
                    rowStart++
                }
            }
        }

        return confirmedMatches
    }

    private fun markMatchesForExplosion(
        board: List<List<CandyItem>>,
        matches: List<MatchGroup>
    ): List<List<CandyItem>> {
        val matchedCells = matches.flatMap { it.cells }.toSet()
        return board.mapIndexed { ri, rowItems ->
            rowItems.mapIndexed { ci, item ->
                if (matchedCells.contains(Pair(ri, ci))) {
                    item.copy(isExploding = true, isMatched = true)
                } else {
                    item
                }
            }
        }
    }

    private data class ScoreOutcome(
        val board: List<List<CandyItem>>,
        val pointsEarned: Int,
        val newFloats: List<FloatingScore>,
        val rewardedPowerUps: List<PowerUpType> = emptyList()
    )

    private fun applyMatchesAndScore(
        board: List<List<CandyItem>>,
        matches: List<MatchGroup>,
        combo: Int,
        candyTypes: Int
    ): ScoreOutcome {
        val rows = board.size
        val cols = board[0].size

        val matchedCells = matches.flatMap { it.cells }.toSet()
        val mutable = board.map { it.toMutableList() }.toMutableList()

        val newFloats = mutableListOf<FloatingScore>()
        val rewardedPowerUps = mutableListOf<PowerUpType>()
        var points = 0

        // 1. Trigger blocker destruction if adjacent to any matched cells
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (mutable[r][c].isBlocker) {
                    // Check neighbors
                    val hasAdjacentMatch = listOf(
                        Pair(r - 1, c), Pair(r + 1, c), Pair(r, c - 1), Pair(r, c + 1)
                    ).any { (nr, nc) ->
                        nr in 0 until rows && nc in 0 until cols && matchedCells.contains(Pair(nr, nc))
                    }
                    if (hasAdjacentMatch) {
                        // Destroy Blocker/Chocolate square! Give 200 bonus
                        mutable[r][c] = CandyItem(type = -1, row = r, col = c) // Empty space
                        points += 200
                        newFloats.add(FloatingScore(score = 200, row = r, col = c, message = "Crunch!"))
                    }
                }
            }
        }

        // 2. Figure out Special Candies to spawn!
        // We look for match quantities size in groups
        val horizontalSplits = matches.filter { g -> g.cells.map { it.first }.toSet().size == 1 }
        val verticalSplits = matches.filter { g -> g.cells.map { it.second }.toSet().size == 1 }

        val specialSpawnReservations = mutableMapOf<Pair<Int, Int>, SpecialType>()

        // Match-5 in line creates Rainbow Color Bomb
        matches.forEach { group ->
            val size = group.cells.size
            if (size >= 5) {
                // Find a central cell of selection to spawn Color Bomb
                val centerCell = group.cells.first()
                specialSpawnReservations[centerCell] = SpecialType.COLOR_BOMB
            } else if (size == 4) {
                // Match 4 creates a Striped Candy
                val isVert = group.cells.map { it.second }.toSet().size == 1
                val centerCell = group.cells.elementAt(1)
                specialSpawnReservations[centerCell] = if (isVert) SpecialType.STRIPE_HORIZONTAL else SpecialType.STRIPE_VERTICAL
            } else if (size == 3 && matches.size >= 2) {
                // Dual matches intersecting (T or L shapes) create Wrapped Exploding candies
                val isIntersecting = matches.any { other ->
                    other != group && other.cells.intersect(group.cells).isNotEmpty()
                }
                if (isIntersecting) {
                    val intersection = matches[0].cells.intersect(matches[0].cells).firstOrNull() ?: group.cells.first()
                    specialSpawnReservations[intersection] = SpecialType.WRAPPED
                }
            }
        }

        // 3. Clear normal matched items or elevate them to specials
        matches.forEach { group ->
            val size = group.cells.size
            // Reward combo points capping at 100 per match!
            val rawMatchPoints = size * 12 * combo
            val matchPoints = rawMatchPoints.coerceAtMost(100)
            points += matchPoints
            
            // Add massive combo bursts!
            if (size >= 8 || combo >= 6) {
                rewardedPowerUps.add(PowerUpType.CHOCO_SMASHER)
                rewardedPowerUps.add(PowerUpType.RAINBOW_BRUSH)
            } else if (size >= 6 || combo >= 4) {
                rewardedPowerUps.add(PowerUpType.RAINBOW_BRUSH)
            } else if (size >= 5 || combo >= 3) {
                rewardedPowerUps.add(PowerUpType.STRIPE_HAMMER)
            }

            group.cells.forEach { (r, c) ->
                val alreadyExploded = mutable[r][c].type == -1
                if (!alreadyExploded) {
                    // Check if there is a special candy triggered during matched clear (e.g. Row/Col striped, Wrapped)
                    val wasSpecial = mutable[r][c].specialType
                    if (wasSpecial != SpecialType.NONE) {
                        points += resolveSpecialCandyImpact(r, c, wasSpecial, mutable, rows, cols).coerceAtMost(100)
                    }

                    val toSpawn = specialSpawnReservations[Pair(r, c)]
                    if (toSpawn != null) {
                        // Upgrade this cell into a Special Candy rather than deleting it
                        mutable[r][c] = CandyItem(
                            type = group.type,
                            row = r,
                            col = c,
                            specialType = toSpawn
                        )
                        newFloats.add(FloatingScore(score = matchPoints, row = r, col = c, message = toSpawn.name))
                    } else {
                        // Empty it
                        mutable[r][c] = CandyItem(type = -1, row = r, col = c)
                    }
                }
            }
        }

        // Add float for combo feedback if multiplier is sweet
        if (combo > 1 && matches.isNotEmpty()) {
            val anchor = matches.first().cells.first()
            val text = when (combo) {
                2 -> "Sweet!"
                3 -> "Delicious!"
                4 -> "Tasty!!"
                else -> "SPECTACULAR!!!"
            }
            newFloats.add(FloatingScore(score = points, row = anchor.first, col = anchor.second, message = text))
        }

        return ScoreOutcome(mutable, points, newFloats, rewardedPowerUps)
    }

    private fun resolveSpecialCandyImpact(
        r: Int,
        c: Int,
        special: SpecialType,
        mutable: MutableList<MutableList<CandyItem>>,
        rows: Int,
        cols: Int
    ): Int {
        var addedPoints = 0
        when (special) {
            SpecialType.STRIPE_HORIZONTAL -> {
                // Clear entire row
                for (ci in 0 until cols) {
                    if (!mutable[r][ci].isBlocker && mutable[r][ci].type != -1) {
                        mutable[r][ci] = CandyItem(type = -1, row = r, col = ci)
                        addedPoints += 50
                    }
                }
            }
            SpecialType.STRIPE_VERTICAL -> {
                // Clear entire column
                for (ri in 0 until rows) {
                    if (!mutable[ri][c].isBlocker && mutable[ri][c].type != -1) {
                        mutable[ri][c] = CandyItem(type = -1, row = ri, col = c)
                        addedPoints += 50
                    }
                }
            }
            SpecialType.WRAPPED -> {
                // 3x3 blast footprint
                for (ri in (r - 1)..(r + 1)) {
                    for (ci in (c - 1)..(c + 1)) {
                        if (ri in 0 until rows && ci in 0 until cols) {
                            if (!mutable[ri][ci].isBlocker && mutable[ri][ci].type != -1) {
                                mutable[ri][ci] = CandyItem(type = -1, row = ri, col = ci)
                                addedPoints += 70
                            }
                        }
                    }
                }
            }
            else -> { /* Color bomb resolved differently */ }
        }
        return addedPoints
    }

    private fun resolveColorBombExplosion(
        board: List<List<CandyItem>>,
        r1: Int, c1: Int, r2: Int, c2: Int
    ): Pair<List<List<CandyItem>>, Int> {
        val rows = board.size
        val cols = board[0].size
        val mutable = board.map { it.toMutableList() }.toMutableList()

        // Detect which candy color the rainbow was swapped with
        val bombIs1 = board[r1][c1].specialType == SpecialType.COLOR_BOMB
        val swapPos = if (bombIs1) Pair(r2, c2) else Pair(r1, c1)
        val bombPos = if (bombIs1) Pair(r1, c1) else Pair(r2, c2)

        val targetColor = board[swapPos.first][swapPos.second].type
        var cleared = 0

        // If matched color is valid normal candy, wipe all of them
        if (targetColor >= 0) {
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val item = mutable[r][c]
                    if (!item.isBlocker && (item.type == targetColor || (r == bombPos.first && c == bombPos.second))) {
                        mutable[r][c] = CandyItem(type = -1, row = r, col = c)
                        cleared++
                    }
                }
            }
        } else {
            // Swapped with empty space or blocker, delete bomb itself
            mutable[bombPos.first][bombPos.second] = CandyItem(type = -1, row = bombPos.first, col = bombPos.second)
            cleared++
        }

        return Pair(mutable, cleared * 150)
    }

    private fun dropCandiesAndRefill(
        board: List<List<CandyItem>>,
        candyColors: Int
    ): List<List<CandyItem>> {
        val rows = board.size
        val cols = board[0].size
        val mutable = board.map { it.toMutableList() }.toMutableList()

        // Apply column gravitation bottom-to-top
        for (c in 0 until cols) {
            // Gather non-empty/non-blocker entities from bottom up
            val elements = mutableListOf<CandyItem>()
            for (r in rows - 1 downTo 0) {
                if (!mutable[r][c].isBlocker && mutable[r][c].type != -1) {
                    elements.add(mutable[r][c])
                }
            }

            var elementIndex = 0
            for (r in rows - 1 downTo 0) {
                if (mutable[r][c].isBlocker) {
                    // Do not move blocker tiles!
                    continue
                }

                if (elementIndex < elements.size) {
                    // Pull verified element down
                    mutable[r][c] = elements[elementIndex].copy(row = r, col = c)
                    elementIndex++
                } else {
                    // Fill blank slots above by generating new shiny 3D candies
                    mutable[r][c] = CandyItem(
                        type = Random.nextInt(candyColors),
                        row = r,
                        col = c
                    )
                }
            }
        }
        return mutable
    }

    private fun saveCompletedLevel(levelId: Int, score: Int, stars: Int) {
        viewModelScope.launch {
            val currentLevel = repository.getLevelById(levelId) ?: return@launch
            val nextLevel = repository.getLevelById(levelId + 1)

            // Save best score
            val updatedCurrent = currentLevel.copy(
                bestScore = Math.max(currentLevel.bestScore, score),
                stars = Math.max(currentLevel.stars, stars)
            )
            repository.updateLevel(updatedCurrent)

            // Advance unlocked progress
            if (nextLevel != null && !nextLevel.isUnlocked) {
                repository.updateLevel(nextLevel.copy(isUnlocked = true))
            }
        }
    }

    private fun playBeepTone(freq: Int, duration: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, duration)
            } catch (e: Exception) {
                // ignore audio failures
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            // ignore
        }
    }
}
