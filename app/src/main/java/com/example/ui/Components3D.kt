package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.SpecialType
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.SolidColor
import com.example.game.PowerUpType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.CircleShape

import kotlin.math.cos
import kotlin.math.sin

import androidx.compose.ui.graphics.drawscope.scale

/**
 * Custom Canvas drawing to render a high-fidelity glossy 3D candy.
 */
@Composable
fun RenderCandy3D(
    type: Int,
    specialType: SpecialType,
    isBlocker: Boolean,
    modifier: Modifier = Modifier,
    isExploding: Boolean = false,
    selected: Boolean = false
) {
    // Dynamic spring-physics physical squish/elastic animations
    val animatedScaleX by animateFloatAsState(
        targetValue = if (selected) 1.22f else if (isExploding) 0f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "candy_squish_x"
    )
    val animatedScaleY by animateFloatAsState(
        targetValue = if (selected) 0.82f else if (isExploding) 0f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "candy_squish_y"
    )

    // Gentle physical breathing loop to make candies feel alive - only for selected or special candies
    val infiniteTransition = rememberInfiniteTransition(label = "candy_breathe")
    val breatheScale by if (selected || specialType != SpecialType.NONE) {
        infiniteTransition.animateFloat(
            initialValue = 0.98f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathe"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        val fScaleX = animatedScaleX * breatheScale
        val fScaleY = animatedScaleY * breatheScale
        if (fScaleX <= 0f || fScaleY <= 0f) return@Canvas

        val boardWidth = size.width
        val boardHeight = size.height
        val candyRadius = (boardWidth.coerceAtMost(boardHeight) / 2f).coerceAtLeast(1f)

        val center = size.center

        scale(scaleX = fScaleX, scaleY = fScaleY, pivot = center) {
            // 1. Draw Drop Shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.28f),
                radius = candyRadius * 0.95f,
                center = center + Offset(0f, candyRadius * 0.15f)
            )

            if (isBlocker) {
                // Render structured chocolate blocker
                drawChocolateBlocker(center, candyRadius)
                return@scale
            }

            // Render Base Glyphs depending on Candy Type
            when (type) {
                0 -> drawCherrySphere(center, candyRadius)
                1 -> drawBlueDiamond(center, candyRadius)
                2 -> drawYellowLemon(center, candyRadius)
                3 -> drawPurpleBead(center, candyRadius)
                4 -> drawJadeStar(center, candyRadius)
                5 -> drawOrangeHexagon(center, candyRadius)
                else -> drawCherrySphere(center, candyRadius) // Default fallback
            }

            // 2. High-intensity subsurface scattering GLOWING internal core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.1f), Color.Transparent),
                    center = center - Offset(candyRadius * 0.1f, candyRadius * 0.1f),
                    radius = (candyRadius * 0.45f).coerceAtLeast(1f)
                ),
                radius = candyRadius * 0.45f,
                center = center,
                blendMode = BlendMode.Screen
            )

            // 3. Frosted rim/matte light-diffusing outer overlay
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.32f)),
                    center = center,
                    radius = candyRadius.coerceAtLeast(1f)
                ),
                radius = candyRadius,
                center = center
            )

            // Draw Special Candy Overlays
            when (specialType) {
                SpecialType.STRIPE_HORIZONTAL -> {
                    drawStripeOverlay(center, candyRadius, isHorizontal = true)
                }
                SpecialType.STRIPE_VERTICAL -> {
                    drawStripeOverlay(center, candyRadius, isHorizontal = false)
                }
                SpecialType.WRAPPED -> {
                    drawWrappedRibbonOverlay(center, candyRadius)
                }
                SpecialType.COLOR_BOMB -> {
                    drawColorBombLayer(center, candyRadius)
                }
                SpecialType.NONE -> { /* No special overlay */ }
            }

            // Highlight/Glossy overlay for all candies
            drawGlobalGlassGloss(center, candyRadius)
        }
    }
}

private fun DrawScope.drawCherrySphere(center: Offset, radius: Float) {
    // Red Cherry Sphere
    val baseColor = Color(0xFFD32F2F)
    val highlightColor = Color(0xFFFF5252)
    val shadowColor = Color(0xFF7F0000)

    val bodyBrush = Brush.radialGradient(
        colors = listOf(highlightColor, baseColor, shadowColor),
        center = center - Offset(radius * 0.3f, radius * 0.3f),
        radius = radius * 1.4f
    )

    // Main circle
    drawCircle(brush = bodyBrush, radius = radius, center = center)

    // Red Cherry leaf/stem details in 3D
    val path = Path().apply {
        moveTo(center.x, center.y - radius * 0.8f)
        quadraticTo(
            center.x + radius * 0.3f, center.y - radius * 1.1f,
            center.x + radius * 0.6f, center.y - radius * 1.2f
        )
        quadraticTo(
            center.x + radius * 0.4f, center.y - radius * 0.8f,
            center.x, center.y - radius * 0.8f
        )
    }
    drawPath(path = path, color = Color(0xFF388E3C))
}

private fun DrawScope.drawBlueDiamond(center: Offset, radius: Float) {
    // Blue Diamond Cut Gem
    val topColor = Color(0xFF40C4FF)
    val baseColor = Color(0xFF00B0FF)
    val shadowColor = Color(0xFF0077C2)

    val path = Path().apply {
        moveTo(center.x, center.y - radius)          // Top point
        lineTo(center.x + radius, center.y - radius * 0.2f) // Right edge
        lineTo(center.x + radius * 0.6f, center.y + radius) // Bottom right bevel
        lineTo(center.x - radius * 0.6f, center.y + radius) // Bottom left bevel
        lineTo(center.x - radius, center.y - radius * 0.2f) // Left edge
        close()
    }

    val brush = Brush.radialGradient(
        colors = listOf(topColor, baseColor, shadowColor),
        center = center - Offset(radius * 0.2f, radius * 0.4f),
        radius = radius * 1.5f
    )
    drawPath(path = path, brush = brush)

    // Facet lines to reflect 3D cut
    drawPath(
        path = path,
        color = Color.White.copy(alpha = 0.4f),
        style = Stroke(width = 3f)
    )

    // Draw inner facet lines for reflection
    drawLine(
        color = Color.White.copy(alpha = 0.3f),
        start = center - Offset(0f, radius),
        end = center + Offset(0f, radius),
        strokeWidth = 2f
    )
    drawLine(
        color = Color.White.copy(alpha = 0.3f),
        start = center - Offset(radius, radius * 0.2f),
        end = center + Offset(radius, -radius * 0.2f),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawYellowLemon(center: Offset, radius: Float) {
    // Yellow Lemon Droplet
    val topColor = Color(0xFFFFF176)
    val baseColor = Color(0xFFFDD835)
    val shadowColor = Color(0xFFF57F17)

    val path = Path().apply {
        // Pointy top teardrop shape
        moveTo(center.x, center.y - radius * 1.1f)
        cubicTo(
            center.x + radius * 0.9f, center.y - radius * 0.8f,
            center.x + radius * 0.9f, center.y + radius * 0.9f,
            center.x, center.y + radius
        )
        cubicTo(
            center.x - radius * 0.9f, center.y + radius * 0.9f,
            center.x - radius * 0.9f, center.y - radius * 0.8f,
            center.x, center.y - radius * 1.1f
        )
        close()
    }

    val brush = Brush.radialGradient(
        colors = listOf(topColor, baseColor, shadowColor),
        center = center - Offset(radius * 0.2f, radius * 0.3f),
        radius = radius * 1.3f
    )
    drawPath(path = path, brush = brush)
}

private fun DrawScope.drawPurpleBead(center: Offset, radius: Float) {
    // Purple beaded candy
    val topColor = Color(0xFFE040FB)
    val baseColor = Color(0xFF9C27B0)
    val shadowColor = Color(0xFF4A148C)

    val brush = Brush.radialGradient(
        colors = listOf(topColor, baseColor, shadowColor),
        center = center - Offset(radius * 0.2f, radius * 0.2f),
        radius = radius * 1.3f
    )

    // Double bubble bead design (draw a larger background berry and smaller top)
    drawCircle(brush = brush, radius = radius, center = center)

    // Inner glossy bead
    drawCircle(
        color = Color.White.copy(alpha = 0.15f),
        radius = radius * 0.5f,
        center = center + Offset(radius * 0.1f, radius * 0.1f)
    )
}

private fun DrawScope.drawJadeStar(center: Offset, radius: Float) {
    // Jade Green 3D Star
    val topColor = Color(0xFF69F0AE)
    val baseColor = Color(0xFF00E676)
    val shadowColor = Color(0xFF1B5E20)

    val starPath = Path()
    val numPoints = 5
    val outerRadius = radius
    val innerRadius = radius * 0.45f
    var angle = -Math.PI / 2

    for (i in 0 until numPoints * 2) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = (center.x + r * cos(angle)).toFloat()
        val y = (center.y + r * sin(angle)).toFloat()
        if (i == 0) {
            starPath.moveTo(x, y)
        } else {
            starPath.lineTo(x, y)
        }
        angle += Math.PI / numPoints
    }
    starPath.close()

    val brush = Brush.radialGradient(
        colors = listOf(topColor, baseColor, shadowColor),
        center = center - Offset(radius * 0.2f, radius * 0.2f),
        radius = radius * 1.3f
    )
    drawPath(path = starPath, brush = brush)

    // Draw internal beveled star facets
    for (i in 0 until numPoints) {
        val outerAngle = -Math.PI / 2 + i * (2 * Math.PI / numPoints)
        val x = (center.x + outerRadius * cos(outerAngle)).toFloat()
        val y = (center.y + outerRadius * sin(outerAngle)).toFloat()
        drawLine(
            color = Color.White.copy(alpha = 0.25f),
            start = center,
            end = Offset(x, y),
            strokeWidth = 3f
        )
    }
}

private fun DrawScope.drawOrangeHexagon(center: Offset, radius: Float) {
    // Orange crystal candy
    val topColor = Color(0xFFFFAB40)
    val baseColor = Color(0xFFFF9100)
    val shadowColor = Color(0xFFE65100)

    val path = Path()
    for (i in 0 until 6) {
        val angle = i * Math.PI / 3
        val x = (center.x + radius * cos(angle)).toFloat()
        val y = (center.y + radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()

    val brush = Brush.radialGradient(
        colors = listOf(topColor, baseColor, shadowColor),
        center = center - Offset(radius * 0.2f, radius * 0.2f),
        radius = radius * 1.4f
    )
    drawPath(path = path, brush = brush)

    // Inner offset hexagon for beautiful beveled look
    val innerPath = Path()
    for (i in 0 until 6) {
        val angle = i * Math.PI / 3
        val x = (center.x + radius * 0.65f * cos(angle)).toFloat()
        val y = (center.y + radius * 0.65f * sin(angle)).toFloat()
        if (i == 0) innerPath.moveTo(x, y) else innerPath.lineTo(x, y)
    }
    innerPath.close()
    drawPath(path = innerPath, color = Color.White.copy(alpha = 0.2f))
}

private fun DrawScope.drawChocolateBlocker(center: Offset, radius: Float) {
    // High-fidelity textured chocolate blocker
    val topBrown = Color(0xFF8D6E63)
    val midBrown = Color(0xFF5D4037)
    val darkBrown = Color(0xFF3E2723)

    // Main rounded chocolate box
    drawRoundRect(
        color = midBrown,
        topLeft = center - Offset(radius, radius),
        size = Size(radius * 2, radius * 2),
        cornerRadius = CornerRadius(radius * 0.25f, radius * 0.25f)
    )

    // 3D bottom bevel shadow
    drawRoundRect(
        color = darkBrown,
        topLeft = center - Offset(radius - 4f, radius - radius * 0.6f),
        size = Size(radius * 2 - 8f, radius * 1.6f - 4f),
        cornerRadius = CornerRadius(radius * 0.2f, radius * 0.2f)
    )

    // Inside grid representing a split chocolate bar
    val dividerLeft = center.x - 3f
    val dividerTop = center.y - radius + 8f
    val dividerWidth = 6f
    val dividerHeight = radius * 2 - 16f
    drawRect(color = darkBrown, topLeft = Offset(dividerLeft, dividerTop), size = Size(dividerWidth, dividerHeight))
    drawRect(color = darkBrown, topLeft = Offset(dividerTop, dividerLeft), size = Size(dividerHeight, dividerWidth))

    // Inner chocolate wells highlights
    val wellSize = radius * 0.65f
    val offset1 = radius * 0.75f
    val corners = CornerRadius(radius * 0.1f, radius * 0.1f)

    // Highlight squares
    listOf(
        center - Offset(offset1, offset1),
        center - Offset(-8f, offset1),
        center - Offset(offset1, -8f),
        center - Offset(-8f, -8f)
    ).forEach { pos ->
        drawRoundRect(
            color = topBrown,
            topLeft = pos,
            size = Size(wellSize, wellSize),
            cornerRadius = corners
        )
    }
}

private fun DrawScope.drawStripeOverlay(center: Offset, radius: Float, isHorizontal: Boolean) {
    val stripeBrush = Brush.linearGradient(
        colors = listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.9f)),
        start = if (isHorizontal) Offset(0f, center.y - radius) else Offset(center.x - radius, 0f),
        end = if (isHorizontal) Offset(0f, center.y + radius) else Offset(center.x + radius, 0f)
    )

    if (isHorizontal) {
        drawRect(
            brush = stripeBrush,
            topLeft = Offset(center.x - radius, center.y - radius * 0.2f),
            size = Size(radius * 2, radius * 0.4f)
        )
    } else {
        drawRect(
            brush = stripeBrush,
            topLeft = Offset(center.x - radius * 0.2f, center.y - radius),
            size = Size(radius * 0.4f, radius * 2)
        )
    }
}

private fun DrawScope.drawWrappedRibbonOverlay(center: Offset, radius: Float) {
    // White glowing folded cellophane wrapper corners
    val ribbonColor = Color.White.copy(alpha = 0.65f)

    // Left folded ribbon wing
    val leftPath = Path().apply {
        moveTo(center.x - radius * 0.5f, center.y)
        lineTo(center.x - radius * 1.2f, center.y - radius * 0.4f)
        lineTo(center.x - radius * 1.2f, center.y + radius * 0.4f)
        close()
    }
    // Right folded ribbon wing
    val rightPath = Path().apply {
        moveTo(center.x + radius * 0.5f, center.y)
        lineTo(center.x + radius * 1.2f, center.y - radius * 0.4f)
        lineTo(center.x + radius * 1.2f, center.y + radius * 0.4f)
        close()
    }

    drawPath(path = leftPath, color = ribbonColor)
    drawPath(path = rightPath, color = ribbonColor)

    // Draw cellophane creases
    drawPath(path = leftPath, color = Color.White.copy(alpha = 0.8f), style = Stroke(width = 3f))
    drawPath(path = rightPath, color = Color.White.copy(alpha = 0.8f), style = Stroke(width = 3f))
}

private fun DrawScope.drawColorBombLayer(center: Offset, radius: Float) {
    // Rainbow Color Bomb ball
    val sweepBrush = Brush.sweepGradient(
        colors = listOf(
            Color(0xFFFF1744), // Red
            Color(0xFFFF9100), // Orange
            Color(0xFFFFEA00), // Yellow
            Color(0xFF00E676), // Green
            Color(0xFF00B0FF), // Blue
            Color(0xFFD500F9), // Purple
            Color(0xFFFF1744)  // Loop Red
        ),
        center = center
    )
    drawCircle(brush = sweepBrush, radius = radius, center = center)

    // Rainbow sparkles sprinkled over Color Bomb
    listOf(
        center + Offset(-radius * 0.4f, -radius * 0.4f),
        center + Offset(radius * 0.4f, -radius * 0.3f),
        center + Offset(-radius * 0.3f, radius * 0.5f),
        center + Offset(radius * 0.5f, radius * 0.4f),
        center + Offset(0f, 0f)
    ).forEach { pos ->
        drawCircle(color = Color.White, radius = radius * 0.12f, center = pos)
        drawCircle(color = Color(0xFFFFD700), radius = radius * 0.06f, center = pos) // golden core
    }
}

private fun DrawScope.drawGlobalGlassGloss(center: Offset, radius: Float) {
    // Specular reflective highlight dot to simulate glossy spherical 3D glass
    val highlightBrush = Brush.verticalGradient(
        colors = listOf(Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.0f)),
        startY = center.y - radius * 0.8f,
        endY = center.y - radius * 0.1f
    )

    // Oval highlight on upper left
    drawOval(
        brush = highlightBrush,
        topLeft = center - Offset(radius * 0.6f, radius * 0.8f),
        size = Size(radius * 1.0f, radius * 0.5f)
    )

    // Sparkle reflection on lower right
    drawCircle(
        color = Color.White.copy(alpha = 0.4f),
        radius = radius * 0.1f,
        center = center + Offset(radius * 0.5f, radius * 0.5f)
    )
}

/**
 * Beautiful 3D progress bar with dynamic dual-layered fluid sloshing wave action.
 */
@Composable
fun ScoreProgressBar3D(
    currentScore: Int,
    targetScore: Int,
    starsEarned: Int,
    modifier: Modifier = Modifier
) {
    val progress = (currentScore.toFloat() / targetScore.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "progress"
    )

    // Fluid slosh coordinate transition
    val infiniteTransition = rememberInfiniteTransition(label = "slosh")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF261042), Color(0xFF10041F))
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Core sloshing fluid body
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
        ) {
            val barWidth = size.width
            val barHeight = size.height
            val currentWidth = barWidth * animatedProgress

            if (currentWidth > 0f) {
                // Wave 1: Lavender highlight
                val wavePath1 = Path().apply {
                    moveTo(0f, barHeight)
                    for (x in 0..currentWidth.toInt()) {
                        val relativeX = x.toFloat() / 42f
                        val waveY = (sin(relativeX + waveOffset) * 4f + (barHeight * (1f - animatedProgress))).toFloat()
                        lineTo(x.toFloat(), waveY.coerceIn(0f, barHeight))
                    }
                    lineTo(currentWidth, barHeight)
                    close()
                }
                drawPath(
                    path = wavePath1,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF2A85).copy(alpha = 0.45f), Color(0xFF7C4DFF).copy(alpha = 0.65f))
                    )
                )

                // Wave 2: Neon Cyan Foreground
                val wavePath2 = Path().apply {
                    moveTo(0f, barHeight)
                    for (x in 0..currentWidth.toInt()) {
                        val relativeX = x.toFloat() / 32f
                        val waveY = (cos(relativeX - waveOffset) * 3f + 1f + (barHeight * (1f - animatedProgress))).toFloat()
                        lineTo(x.toFloat(), waveY.coerceIn(0f, barHeight))
                    }
                    lineTo(currentWidth, barHeight)
                    close()
                }
                drawPath(
                    path = wavePath2,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.85f), Color(0xFF2979FF).copy(alpha = 0.95f))
                    )
                )

                // High Specular Gloss Overlay
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.4f), Color.Transparent),
                        startY = 0f,
                        endY = barHeight * 0.45f
                    ),
                    size = Size(currentWidth, barHeight)
                )
            }
        }

        // Milepost Star locations overlay (33%, 66%, 100%)
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(2.dp))

            // Star 1
            StarMilestoneIndicator(achieved = currentScore >= targetScore * 0.33f)

            // Star 2
            StarMilestoneIndicator(achieved = currentScore >= targetScore * 0.66f)

            // Star 3
            StarMilestoneIndicator(achieved = currentScore >= targetScore)

            Spacer(modifier = Modifier.width(2.dp))
        }
    }
}

@Composable
private fun StarMilestoneIndicator(achieved: Boolean) {
    val starColor by animateColorAsState(
        targetValue = if (achieved) Color(0xFFFFD700) else Color(0xFF90A4AE),
        label = "star_color"
    )
    val scale by animateFloatAsState(
        targetValue = if (achieved) 1.25f else 1.0f,
        label = "star_scale"
    )

    Box(
        modifier = Modifier
            .size(24.dp)
            .scale(scale)
            .shadow(if (achieved) 4.dp else 0.dp, RoundedCornerShape(12.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = if (achieved) listOf(Color(0xFFFFF9C4), Color(0xFFFBC02D))
                    else listOf(Color(0xFFECEFF1), Color(0xFFCFD8DC)),
                    radius = (with(LocalDensity.current) { 12.dp.toPx() }).coerceAtLeast(1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Star milestone",
            tint = starColor,
            modifier = Modifier.size(13.dp)
        )
    }
}

/**
 * HD shifting gradient liquid landscape backdrop.
 */
@Composable
fun HDMovingMeshBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh_bg")
    
    val shiftX1 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gx1"
    )
    val shiftY1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(12500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gy1"
    )
    val shiftX2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(10200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gx2"
    )
    val shiftY2 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(14100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gy2"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        // Deep twilight purple backdrop
        drawRect(Color(0xFF0C0416))

        // Moving Mesh Red-Sunset Glowing node
        val center1 = Offset(width * shiftX1, height * shiftY1)
        val radius1 = (width * 0.95f).coerceAtLeast(1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF2A85).copy(alpha = 0.38f),
                    Color(0xFFFF7E40).copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = center1,
                radius = radius1
            ),
            radius = radius1,
            center = center1
        )

        // Shifting Electric Cyan node
        val center2 = Offset(width * shiftX2, height * shiftY2)
        val radius2 = (width * 1.1f).coerceAtLeast(1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.3f),
                    Color(0xFF7C4DFF).copy(alpha = 0.1f),
                    Color.Transparent
                ),
                center = center2,
                radius = radius2
            ),
            radius = radius2,
            center = center2
        )

        // Light translucent grid layout
        val gridLineCount = 8
        val cellHeight = size.height / gridLineCount
        val cellWidth = size.width / gridLineCount
        for (i in 1 until gridLineCount) {
            drawLine(
                color = Color.White.copy(alpha = 0.01f),
                start = Offset(0f, i * cellHeight),
                end = Offset(size.width, i * cellHeight),
                strokeWidth = 1f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.01f),
                start = Offset(i * cellWidth, 0f),
                end = Offset(i * cellWidth, size.height),
                strokeWidth = 1f
            )
        }
    }
}

/**
 * Bottom interactive Power-up Dock with structural physical boundaries and vertical glossy reflecting mirror-floor.
 */
@Composable
fun PowerUpReflectiveDock(
    stripeCount: Int,
    rainbowCount: Int,
    smasherCount: Int,
    activePowerUp: PowerUpType?,
    onSelectPowerUp: (PowerUpType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PowerUpItemWidget(
                type = PowerUpType.STRIPE_HAMMER,
                count = stripeCount,
                isActive = activePowerUp == PowerUpType.STRIPE_HAMMER,
                onClick = { onSelectPowerUp(PowerUpType.STRIPE_HAMMER) }
            )
            PowerUpItemWidget(
                type = PowerUpType.RAINBOW_BRUSH,
                count = rainbowCount,
                isActive = activePowerUp == PowerUpType.RAINBOW_BRUSH,
                onClick = { onSelectPowerUp(PowerUpType.RAINBOW_BRUSH) }
            )
            PowerUpItemWidget(
                type = PowerUpType.CHOCO_SMASHER,
                count = smasherCount,
                isActive = activePowerUp == PowerUpType.CHOCO_SMASHER,
                onClick = { onSelectPowerUp(PowerUpType.CHOCO_SMASHER) }
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Glossy Reflective Floor panel mirroring colors
        Box(
            modifier = Modifier
                .width(230.dp)
                .height(28.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.02f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top
            ) {
                // Faded reflections mirroring docks
                Box(modifier = Modifier.size(36.dp).background(Brush.radialGradient(colors = listOf(Color(0xFFFF2A85).copy(alpha = 0.22f), Color.Transparent), radius = (with(LocalDensity.current) { 18.dp.toPx() }).coerceAtLeast(1f)), CircleShape))
                Spacer(modifier = Modifier.width(42.dp))
                Box(modifier = Modifier.size(36.dp).background(Brush.radialGradient(colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.22f), Color.Transparent), radius = (with(LocalDensity.current) { 18.dp.toPx() }).coerceAtLeast(1f)), CircleShape))
                Spacer(modifier = Modifier.width(42.dp))
                Box(modifier = Modifier.size(36.dp).background(Brush.radialGradient(colors = listOf(Color(0xFFFFEE58).copy(alpha = 0.22f), Color.Transparent), radius = (with(LocalDensity.current) { 18.dp.toPx() }).coerceAtLeast(1f)), CircleShape))
            }
        }
    }
}

@Composable
fun PowerUpItemWidget(
    type: PowerUpType,
    count: Int,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "powerup_scale"
    )

    val frameColor = if (isActive) Color(0xFFFF2A85) else Color.White.copy(alpha = 0.15f)

    Box(
        modifier = Modifier
            .size(62.dp)
            .scale(scale)
            .shadow(if (isActive) 12.dp else 3.dp, RoundedCornerShape(14.dp), spotColor = frameColor)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isActive) listOf(Color(0xFF38125C), Color(0xFF140520))
                    else listOf(Color(0xFF220E3E), Color(0xFF0F041D))
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = if (isActive) 2.dp else 1.dp,
                brush = if (isActive) Brush.linearGradient(listOf(Color(0xFFFF2A85), Color(0xFF00E5FF)))
                        else SolidColor(Color.White.copy(alpha = 0.16f)),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(11.dp)) {
            val center = size.center
            val radius = size.width / 2.2f

            when (type) {
                PowerUpType.STRIPE_HAMMER -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFE040FB), Color(0xFF6A1B9A)),
                            center = center,
                            radius = radius.coerceAtLeast(1f)
                        ),
                        radius = radius,
                        center = center
                    )
                    // Structural Stripe
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(center.x - radius, center.y - radius * 0.15f),
                        size = Size(radius * 2f, radius * 0.3f)
                    )
                }
                PowerUpType.RAINBOW_BRUSH -> {
                    val sweep = Brush.sweepGradient(
                        colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Magenta, Color.Red),
                        center = center
                    )
                    drawCircle(brush = sweep, radius = radius, center = center)
                    drawCircle(color = Color.White, radius = radius * 0.35f, center = center)
                }
                PowerUpType.CHOCO_SMASHER -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFE082), Color(0xFFE65100)),
                            center = center,
                            radius = radius.coerceAtLeast(1f)
                        ),
                        radius = radius,
                        center = center
                    )
                    // High shine gloss edge
                    drawCircle(
                        color = Color.White.copy(alpha = 0.7f),
                        radius = radius * 0.2f,
                        center = center - Offset(radius * 0.3f, radius * 0.3f)
                    )
                }
            }
        }

        // Active selection or inventory count
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 5.dp, y = (-5).dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = if (count > 0) listOf(Color(0xFFFF2A85), Color(0xFFFF7E40))
                                 else listOf(Color(0xFF78909C), Color(0xFF455A64))
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
