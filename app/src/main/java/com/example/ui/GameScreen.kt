package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.CandyItem
import com.example.game.GameUiState
import com.example.game.SpecialType
import kotlinx.coroutines.delay

import com.example.game.PowerUpType
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.Canvas

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.translate

import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

@Composable
fun RoyalMascot(
    movesLeft: Int,
    scoreScale: Float,
    modifier: Modifier = Modifier
) {
    // Determine state
    val isPanic = movesLeft in 1..4
    val isHappy = scoreScale > 1.05f

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "mascot")

    val breatheY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPanic) 5f else 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPanic) 150 else if (isHappy) 200 else 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val shakeX by infiniteTransition.animateFloat(
        initialValue = if (isPanic) -4f else 0f,
        targetValue = if (isPanic) 4f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween( if (isPanic) 80 else 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

    val jumpY = if (isHappy) -breatheY * 2f else breatheY

    val density = LocalDensity.current
    val crownPath = remember(density) {
        val sizePx = with(density) { 72.dp.toPx() }
        val center = Offset(sizePx / 2, sizePx / 2)
        Path().apply {
            moveTo(center.x - 22f, center.y - 12f)
            lineTo(center.x - 28f, center.y - 36f)
            lineTo(center.x - 10f, center.y - 20f)
            lineTo(center.x, center.y - 42f)
            lineTo(center.x + 10f, center.y - 20f)
            lineTo(center.x + 28f, center.y - 36f)
            lineTo(center.x + 22f, center.y - 12f)
            close()
        }
    }
    val mouthPath = remember(density) {
        val sizePx = with(density) { 72.dp.toPx() }
        val center = Offset(sizePx / 2, sizePx / 2)
        Path().apply {
            moveTo(center.x - 7f, center.y + 6f)
            quadraticBezierTo(center.x, center.y + 14f, center.x + 7f, center.y + 6f)
        }
    }
    val happyMouthPath = remember(density) {
        val sizePx = with(density) { 72.dp.toPx() }
        val center = Offset(sizePx / 2, sizePx / 2)
        Path().apply {
            moveTo(center.x - 5f, center.y + 8f)
            quadraticBezierTo(center.x, center.y + 10f, center.x + 5f, center.y + 8f)
        }
    }

    Canvas(modifier = modifier.size(72.dp)) {
        val center = size.center

        translate(left = shakeX, top = jumpY) {
            // Draw Cape or Royal Robe fluff shoulderpads
            drawCircle(color = Color(0xFFE53935), radius = 16f, center = Offset(center.x - 18f, center.y + 18f))
            drawCircle(color = Color(0xFFE53935), radius = 16f, center = Offset(center.x + 18f, center.y + 18f))
            drawRoundRect(color = Color(0xFFB71C1C), topLeft = Offset(center.x - 18f, center.y + 10f), size = Size(36f, 20f), cornerRadius = CornerRadius(10f))

            // White fur collar
            drawRoundRect(color = Color.White, topLeft = Offset(center.x - 20f, center.y + 12f), size = Size(40f, 10f), cornerRadius = CornerRadius(5f))
            drawCircle(color = Color.Black.copy(alpha=0.6f), radius = 1.5f, center = Offset(center.x - 10f, center.y + 17f))
            drawCircle(color = Color.Black.copy(alpha=0.6f), radius = 1.5f, center = Offset(center.x + 10f, center.y + 17f))

            // Draw Head
            drawCircle(color = Color(0xFFFFD180), radius = 24f, center = Offset(center.x, center.y - 2f))

            // Draw Crown
            drawPath(crownPath, color = Color(0xFFFFD700)) // Gold Crown
            // Crown jewels
            drawCircle(color = Color.Red, radius = 3.5f, center = Offset(center.x - 28f, center.y - 36f))
            drawCircle(color = Color.Red, radius = 4f, center = Offset(center.x, center.y - 42f))
            drawCircle(color = Color.Red, radius = 3.5f, center = Offset(center.x + 28f, center.y - 36f))
            
            // Crown base rim
            drawRoundRect(color = Color(0xFFFBC02D), topLeft = Offset(center.x - 24f, center.y - 14f), size = Size(48f, 6f), cornerRadius = CornerRadius(3f))

            // Eyes
            val eyeRadius = if (isPanic) 5f else if (isHappy) 3f else 3.5f
            val eyeOffsetY = if (isHappy) -2f else 0f
            drawCircle(color = Color.Black, radius = eyeRadius, center = Offset(center.x - 8f, center.y - 4f + eyeOffsetY))
            drawCircle(color = Color.Black, radius = eyeRadius, center = Offset(center.x + 8f, center.y - 4f + eyeOffsetY))

            // Mouth
            if (isPanic) {
                drawCircle(color = Color.Black, radius = 4.5f, center = Offset(center.x, center.y + 8f))
            } else if (isHappy) {
                drawPath(mouthPath, color = Color.Black, style = Stroke(width = 3f, cap = StrokeCap.Round))
                // Tongue
                drawCircle(color = Color.Red.copy(alpha=0.8f), radius = 2.5f, center = Offset(center.x, center.y + 10f))
            } else {
                drawPath(happyMouthPath, color = Color.Black, style = Stroke(width = 2f, cap = StrokeCap.Round))
            }

            // Flush / Cheeks
            drawCircle(color = Color.Red.copy(alpha = 0.35f), radius = 3.5f, center = Offset(center.x - 14f, center.y + 3f))
            drawCircle(color = Color.Red.copy(alpha = 0.35f), radius = 3.5f, center = Offset(center.x + 14f, center.y + 3f))
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GameScreen(
    state: GameUiState.Playing,
    onCellSelected: (Int, Int) -> Unit,
    onSwipe: (Int, Int, Int, Int) -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    onAdvanceLevel: () -> Unit,
    onSelectPowerUp: (PowerUpType) -> Unit,
    modifier: Modifier = Modifier
) {
    val level = state.level
    val board = state.board
    val score = state.score
    val movesLeft = state.movesLeft
    val selectedCell = state.selectedCell

    // High fidelity elastic springing Pop when Score is incremented
    var popTrigger by remember { mutableStateOf(1f) }
    LaunchedEffect(score) {
        popTrigger = 1.18f
        delay(120L)
        popTrigger = 1f
    }
    val scoreScale by animateFloatAsState(
        targetValue = popTrigger,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "score_pop_bounce"
    )

    // Animation values for board border cycling
    val infiniteRotation = rememberInfiniteTransition(label = "board_border_rotation")
    val rotationAngle by infiniteRotation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_angle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // High Definition shifting gradient liquid sunset backdrop
        HDMovingMeshBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Stats HUD Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .shadow(16.dp, RoundedCornerShape(20.dp)),
                color = Color.Black.copy(alpha = 0.35f), // Dark acrylic frosting
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Top Bar back & target labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onExit,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                                .size(36.dp)
                                .testTag("back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit to levels",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RoyalMascot(
                                movesLeft = movesLeft,
                                scoreScale = scoreScale,
                                modifier = Modifier.padding(bottom = 4.dp).offset(y = (-4).dp)
                            )
                            Text(
                                text = "LEVEL ${level.id}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFEB3B),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Target: ${level.targetScore} pts",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onRestart,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                                .size(36.dp)
                                .testTag("restart_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Restart game",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dashboard stats indicators row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Moves Left Large Display
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "MOVES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "$movesLeft",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (movesLeft <= 5) Color(0xFFFF5252) else Color(0xFF00E676),
                                modifier = Modifier.testTag("moves_indicator")
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Score progress with dynamic rating stars info
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1.5f)
                                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SCORE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                // Financial-grade pop floating pill shaped bubble container
                                Box(
                                    modifier = Modifier
                                        .scale(scoreScale)
                                        .shadow(6.dp, RoundedCornerShape(12.dp))
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color(0xFFFF2A85), Color(0xFF7C4DFF))
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$score",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        modifier = Modifier.testTag("score_indicator")
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            ScoreProgressBar3D(
                                currentScore = score,
                                targetScore = level.targetScore,
                                starsEarned = state.stars
                            )
                        }
                    }
                }
            }

            // Central Play Area Board Dispenser Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                // Frosted acrylic glassmorphic board plate with slow cycling gradient boundary lines
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .shadow(24.dp, RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                        .drawWithContent {
                            drawContent()
                            val angleRad = Math.toRadians(rotationAngle.toDouble())
                            val cosA = cos(angleRad).toFloat()
                            val sinA = sin(angleRad).toFloat()
                            
                            val startOffset = Offset(cosA * 1200f, sinA * 1200f)
                            val endOffset = Offset(-cosA * 1200f, -sinA * 1200f)
                            
                            val brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFFF2A85), Color(0xFF00E5FF), Color(0xFFFF2A85)),
                                start = startOffset,
                                end = endOffset
                            )
                            
                            drawRoundRect(
                                brush = brush,
                                cornerRadius = CornerRadius(24.dp.toPx()),
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing grid cells
                        val gridSize = level.gridSize
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (r in 0 until gridSize) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (c in 0 until gridSize) {
                                        val candyItem = board.getOrNull(r)?.getOrNull(c) ?: CandyItem(type = -1, row = r, col = c)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                        ) {
                                            CandyGridCell(
                                                item = candyItem,
                                                isSelected = selectedCell == Pair(r, c),
                                                onClick = { onCellSelected(r, c) },
                                                onSwipe = { dRow, dCol -> onSwipe(r, c, dRow, dCol) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // High-performance overlay for floating scores and particles
                        Box(modifier = Modifier.fillMaxSize()) {
                            state.floatingScores.forEach { float ->
                                key(float.id) {
                                    AnimateFloatingScores(float)
                                }
                            }
                        }
                    }
                }
            }

            // Premium Reflective 3D Power-up dock displaying Stripe Hammer, Color Bomb, and Choco Smasher
            PowerUpReflectiveDock(
                stripeCount = state.stripeHammerCount,
                rainbowCount = state.rainbowBrushCount,
                smasherCount = state.chocoSmasherCount,
                activePowerUp = state.activePowerUp,
                onSelectPowerUp = onSelectPowerUp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Overlay game sheets: Victory/Defeat dialogs
        if (state.isVictory) {
            GameOutcomeDialog(
                title = "VICTORY!",
                message = "Sweet! You crushed it!",
                score = score,
                stars = state.stars,
                buttonText = "NEXT LEVEL",
                onAction = onAdvanceLevel, // Advancing or restarting level handles DB sync
                onExit = onExit,
                isVictory = true
            )
        } else if (state.isGameOver) {
            GameOutcomeDialog(
                title = "OUT OF MOVES!",
                message = "Oh no! No worries, try again!",
                score = score,
                stars = 0,
                buttonText = "RETRY",
                onAction = onRestart,
                onExit = onExit,
                isVictory = false
            )
        }
    }
}

@Composable
fun CandyGridCell(
    item: CandyItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSwipe: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (item.type == -1) {
        // Render Empty spacing placeholder
        Box(modifier = modifier.fillMaxSize())
        return
    }

    var swipeTriggered by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .shadow(if (isSelected) 6.dp else 0.dp, RoundedCornerShape(10.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF37474F).copy(alpha = 0.5f), Color(0xFF212121).copy(alpha = 0.5f))
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .pointerInput(item.id) {
                detectDragGestures(
                    onDragStart = { swipeTriggered = false },
                    onDragEnd = { swipeTriggered = false },
                    onDrag = { change, dragAmount ->
                        if (!swipeTriggered) {
                            val threshold = 35f
                            val absX = Math.abs(dragAmount.x)
                            val absY = Math.abs(dragAmount.y)
                            if (absX > threshold || absY > threshold) {
                                swipeTriggered = true
                                val dRow = if (absY > absX) (if (dragAmount.y > 0) 1 else -1) else 0
                                val dCol = if (absX > absY) (if (dragAmount.x > 0) 1 else -1) else 0
                                if (dRow != 0 || dCol != 0) {
                                    onSwipe(dRow, dCol)
                                }
                            }
                        }
                    }
                )
            }
            .testTag("cell_${item.row}_${item.col}")
    ) {
        RenderCandy3D(
            type = item.type,
            specialType = item.specialType,
            isBlocker = item.isBlocker,
            isExploding = item.isExploding,
            selected = isSelected
        )
    }
}

@Composable
fun AnimateFloatingScores(floatScore: com.example.game.FloatingScore) {
    val duration = 800
    val offsetTransition = remember { Animatable(0f) }
    val alphaTransition = remember { Animatable(1f) }

    // High fidelity particle nodes flying outward using trigonometry offsets
    val particles = remember {
        List(10) { // Reduced count for performance
            val angle = Math.random() * 2 * Math.PI
            val distance = (30 + Math.random() * 80).toFloat() // Reduced spread
            val dx = (cos(angle) * distance).toFloat()
            val dy = (sin(angle) * distance - 40).toFloat() 
            val randColor = listOf(
                Color(0xFFFF2A85),
                Color(0xFF00E5FF),
                Color(0xFFFFEE58),
                Color(0xFF7C4DFF)
            ).random()
            Triple(dx, dy, randColor.copy(alpha = 0.9f))
        }
    }

    val particleProgress = remember { Animatable(0f) }

    LaunchedEffect(floatScore.id) {
        // Parallel animations
        launch {
            offsetTransition.animateTo(-120f, animationSpec = tween(duration, easing = LinearOutSlowInEasing))
        }
        launch {
            delay((duration / 2).toLong())
            alphaTransition.animateTo(0f, animationSpec = tween(duration / 2, easing = FastOutLinearInEasing))
        }
        launch {
            particleProgress.animateTo(1.0f, animationSpec = tween(duration, easing = LinearOutSlowInEasing))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = alphaTransition.value
            },
        contentAlignment = Alignment.Center
    ) {
        // Particle canvas using static size where possible
        val density = LocalDensity.current
        Canvas(modifier = Modifier.size(150.dp)) {
            val centerPos = size.center + Offset(0f, offsetTransition.value)
            particles.forEach { (dx, dy, color) ->
                val currentOffset = Offset(
                    x = centerPos.x + dx * particleProgress.value,
                    y = centerPos.y + dy * particleProgress.value
                )
                drawCircle(
                    color = color.copy(alpha = color.alpha * (1f - particleProgress.value)),
                    radius = (4f + 6f * (1f - particleProgress.value)),
                    center = currentOffset
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f * (1f - particleProgress.value)),
                    radius = (1.5f + 2f * (1f - particleProgress.value)),
                    center = currentOffset
                )
            }
        }

        // Frosted Pill Dialog Bubble
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationY = offsetTransition.value
                }
                .shadow(8.dp, RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF2A85), Color(0xFF7C4DFF))
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                floatScore.message?.let {
                    Text(
                        text = it.uppercase(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    text = "+${floatScore.score}",
                    color = Color(0xFFFFEB3B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun GameOutcomeDialog(
    title: String,
    message: String,
    score: Int,
    stars: Int,
    buttonText: String,
    onAction: () -> Unit,
    onExit: () -> Unit,
    isVictory: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .shadow(24.dp, RoundedCornerShape(28.dp)),
            color = Color(0xFF311B92), // Radiant mystical purple
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(3.dp, Color(0xFFFFD700)) // Golden metallic rim
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Victory Trophy or Defeat Icon
                Text(
                    text = if (isVictory) "🏆" else "🍭😢",
                    fontSize = 60.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = title,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isVictory) Color(0xFFFFD700) else Color(0xFFFF5252),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Score Achievement Details
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "FINAL SCORE",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$score",
                            fontSize = 24.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    }

                    if (isVictory) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "STARS",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                            Row {
                                repeat(3) { index ->
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (index < stars) Color(0xFFFFD700) else Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Dynamic Flow Button selections
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onExit,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f))
                    ) {
                        Text(text = "LEVELS", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onAction,
                        modifier = Modifier.weight(1.5f).testTag("action_dialog_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                    ) {
                        Text(
                            text = buttonText,
                            color = Color(0xFF1A237E),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
