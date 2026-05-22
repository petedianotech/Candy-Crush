package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CandyLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelectScreen(
    levels: List<CandyLevel>,
    onLevelSelected: (CandyLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4A148C), // Beautiful dark candy purple
                        Color(0xFF311B92),
                        Color(0xFF0D47A1)  // Deeper background
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant Playful Header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(18.dp)),
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CANDY MATCH 3D",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFEB3B), // Glowing lemon gold
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Pop matching candies & clear chocolate barriers!",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Grid of 20 Levels
            Text(
                text = "SELECT LEVEL",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Start
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(levels, key = { it.id }) { level ->
                    LevelGridNode(
                        level = level,
                        onClick = { if (level.isUnlocked) onLevelSelected(level) }
                    )
                }
            }

            // Decorative Bottom Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                color = Color.Black.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Collect up to 3 Stars on each of the 20 levels!",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun LevelGridNode(
    level: CandyLevel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nodeBrush = if (level.isUnlocked) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE040FB), // Fuchsia neon
                Color(0xFF9C27B0),
                Color(0xFF7B1FA2)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF90A4AE),
                Color(0xFF546E7A)
            )
        )
    }

    Box(
        modifier = modifier
            .aspectRatio(1.1f)
            .shadow(if (level.isUnlocked) 8.dp else 2.dp, RoundedCornerShape(16.dp))
            .background(brush = nodeBrush, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = level.isUnlocked, onClick = onClick)
            .testTag("level_node_${level.id}"),
        contentAlignment = Alignment.Center
    ) {
        // Highlight shine on top half
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .align(Alignment.TopCenter)
                .background(Color.White.copy(alpha = 0.15f))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Lvl ${level.id}",
                fontSize = 18.sp,
                color = if (level.isUnlocked) Color(0xFFFFEB3B) else Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (level.isUnlocked) {
                // Show stars
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val active = index < level.stars
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (active) Color(0xFFFFD700) else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                if (level.bestScore > 0) {
                    Text(
                        text = "${level.bestScore} pts",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "play",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "PLAY",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked level",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
