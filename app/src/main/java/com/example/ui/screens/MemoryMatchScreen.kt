package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.audio.SoundSynthesizer
import com.example.data.AnimalKind
import com.example.data.DifficultyLevel
import com.example.ui.components.AnimalAvatarCanvas
import com.example.ui.components.CelebrationDialog
import com.example.ui.components.ConfettiBurstOverlay
import com.example.ui.components.GameTopBar
import com.example.ui.theme.WonderPalette
import kotlinx.coroutines.delay

private data class MemoryCard(
    val uid: Int,
    val animal: AnimalKind,
    val revealed: Boolean = false,
    val matched: Boolean = false
)

@Composable
fun MemoryMatchScreen(
    difficulty: DifficultyLevel,
    sound: SoundSynthesizer,
    onGameComplete: (score: Int, stars: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pairCount = if (difficulty == DifficultyLevel.EASY) 3 else 6
    val columns = if (difficulty == DifficultyLevel.EASY) 2 else 3

    var roundKey by remember { mutableIntStateOf(0) }
    var cards by remember(roundKey, difficulty) {
        val chosen = AnimalKind.entries.shuffled().take(pairCount)
        val deck = (chosen + chosen).shuffled().mapIndexed { index, kind ->
            MemoryCard(uid = index, animal = kind)
        }
        mutableStateOf(deck)
    }
    var firstIndex by remember(roundKey, difficulty) { mutableStateOf<Int?>(null) }
    var secondIndex by remember(roundKey, difficulty) { mutableStateOf<Int?>(null) }
    var isChecking by remember(roundKey, difficulty) { mutableStateOf(false) }
    var moves by remember(roundKey, difficulty) { mutableIntStateOf(0) }
    var score by remember(roundKey, difficulty) { mutableIntStateOf(0) }
    var confettiTrigger by remember(roundKey, difficulty) { mutableIntStateOf(0) }
    var isFinished by remember(roundKey, difficulty) { mutableStateOf(false) }

    LaunchedEffect(secondIndex) {
        val first = firstIndex
        val second = secondIndex
        if (first != null && second != null) {
            isChecking = true
            moves++
            val c1 = cards[first]
            val c2 = cards[second]
            if (c1.animal == c2.animal) {
                sound.playChime()
                confettiTrigger++
                score += 25
                cards = cards.mapIndexed { idx, card ->
                    if (idx == first || idx == second) card.copy(matched = true) else card
                }
                firstIndex = null
                secondIndex = null
                isChecking = false

                if (cards.all { it.matched }) {
                    val efficiencyBonus = ((pairCount * 3 - moves).coerceAtLeast(0)) * 5
                    val finalScore = score + efficiencyBonus
                    score = finalScore
                    val stars = when {
                        moves <= pairCount + 2 -> 3
                        moves <= pairCount + 5 -> 2
                        else -> 1
                    }
                    delay(350)
                    sound.playFanfare()
                    isFinished = true
                    onGameComplete(finalScore, stars)
                }
            } else {
                sound.playBoing()
                delay(700)
                cards = cards.mapIndexed { idx, card ->
                    if (idx == first || idx == second) card.copy(revealed = false) else card
                }
                firstIndex = null
                secondIndex = null
                isChecking = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        WonderPalette.MintLight,
                        MaterialTheme.colorScheme.background,
                        WonderPalette.SunnyLight.copy(alpha = 0.4f)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GameTopBar(
                title = "Animal Match",
                score = score,
                onBack = onBack,
                accentColor = WonderPalette.MintMeadowDark
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                val matchedPairs = cards.count { it.matched } / 2
                Text(
                    text = "Pairs Found: $matchedPairs / $pairCount   •   Moves: $moves",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                val rows = cards.chunked(columns)
                Column(
                    modifier = Modifier.widthIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    rows.forEachIndexed { rowIdx, rowCards ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowCards.forEachIndexed { colIdx, card ->
                                val flatIndex = rowIdx * columns + colIdx
                                FlipAnimalCard(
                                    card = card,
                                    onClick = {
                                        if (isChecking || card.revealed || card.matched || isFinished) return@FlipAnimalCard
                                        sound.playPop()
                                        cards = cards.mapIndexed { idx, item ->
                                            if (idx == flatIndex) item.copy(revealed = true) else item
                                        }
                                        if (firstIndex == null) {
                                            firstIndex = flatIndex
                                        } else {
                                            secondIndex = flatIndex
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.95f)
                                        .testTag("memory_card_$flatIndex")
                                )
                            }
                        }
                    }
                }
            }
        }

        ConfettiBurstOverlay(triggerKey = confettiTrigger)

        if (isFinished) {
            val stars = when {
                moves <= pairCount + 2 -> 3
                moves <= pairCount + 5 -> 2
                else -> 1
            }
            CelebrationDialog(
                title = "Animal Friend!",
                subtitle = "You matched all $pairCount pairs in $moves moves!",
                score = score,
                starsEarned = stars,
                onPlayAgain = { roundKey++ },
                onBackHome = onBack
            )
        }
    }
}

@Composable
private fun FlipAnimalCard(
    card: MemoryCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFaceUp = card.revealed || card.matched
    val rotation by animateFloatAsState(
        targetValue = if (isFaceUp) 180f else 0f,
        animationSpec = tween(durationMillis = 360),
        label = "card_flip"
    )
    val shape = RoundedCornerShape(22.dp)

    Surface(
        shape = shape,
        shadowElevation = if (card.matched) 2.dp else 6.dp,
        color = Color.Transparent,
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        if (rotation <= 90f) {
            // Card Back
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(WonderPalette.MintMeadow, WonderPalette.AquaTeal)
                        )
                    )
                    .border(3.dp, Color.White.copy(alpha = 0.7f), shape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = "Hidden card",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(40.dp)
                )
            }
        } else {
            // Card Front (mirrored horizontally back to normal)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
                    .background(
                        if (card.matched) WonderPalette.SunnyLight else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        width = 3.dp,
                        color = if (card.matched) WonderPalette.SunnyYellow else WonderPalette.MintMeadow,
                        shape = shape
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimalAvatarCanvas(
                        animal = card.animal,
                        sizeDp = 64.dp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = card.animal.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = WonderPalette.CocoaText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
