package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.audio.SoundSynthesizer
import com.example.data.DifficultyLevel
import com.example.ui.components.CelebrationDialog
import com.example.ui.components.ConfettiBurstOverlay
import com.example.ui.components.GameTopBar
import com.example.ui.components.ToyButton
import com.example.ui.theme.WonderPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private data class XylophoneBar(
    val noteName: String,
    val color: Color,
    val shadowColor: Color,
    val widthFraction: Float
)

private val RainbowBars = listOf(
    XylophoneBar("DO", WonderPalette.CoralPop, WonderPalette.CoralPopDark, 1.00f),
    XylophoneBar("RE", WonderPalette.Tangerine, WonderPalette.TangerineDark, 0.95f),
    XylophoneBar("MI", WonderPalette.SunnyYellow, WonderPalette.SunnyYellowDark, 0.90f),
    XylophoneBar("FA", WonderPalette.MintMeadow, WonderPalette.MintMeadowDark, 0.85f),
    XylophoneBar("SO", WonderPalette.AquaTeal, WonderPalette.AquaTealDark, 0.80f),
    XylophoneBar("LA", WonderPalette.SkyBlue, WonderPalette.SkyBlueDark, 0.75f),
    XylophoneBar("TI", WonderPalette.BerryPurple, WonderPalette.BerryPurpleDark, 0.70f),
    XylophoneBar("DO!", WonderPalette.BubblePink, WonderPalette.BubblePinkDark, 0.65f)
)

@Composable
fun XylophoneScreen(
    difficulty: DifficultyLevel,
    sound: SoundSynthesizer,
    onGameComplete: (score: Int, stars: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isEchoMode by remember { mutableStateOf(false) }
    var roundKey by remember { mutableIntStateOf(0) }
    var activeBar by remember { mutableIntStateOf(-1) }
    var score by remember(roundKey, isEchoMode) { mutableIntStateOf(0) }
    var level by remember(roundKey, isEchoMode) { mutableIntStateOf(1) }
    val maxLevels = 4
    var sequence by remember(roundKey, isEchoMode) { mutableStateOf<List<Int>>(emptyList()) }
    var userStep by remember(roundKey, isEchoMode) { mutableIntStateOf(0) }
    var isPlayingBack by remember(roundKey, isEchoMode) { mutableStateOf(false) }
    var statusText by remember(roundKey, isEchoMode) {
        mutableStateOf("Tap any rainbow key to play music!")
    }
    var confettiTrigger by remember(roundKey, isEchoMode) { mutableIntStateOf(0) }
    var isFinished by remember(roundKey, isEchoMode) { mutableStateOf(false) }

    suspend fun playSequence(notes: List<Int>) {
        isPlayingBack = true
        statusText = "Listen & watch closely…"
        delay(400)
        for (barIdx in notes) {
            activeBar = barIdx
            sound.playXylophoneNote(barIdx)
            delay(460)
            activeBar = -1
            delay(160)
        }
        isPlayingBack = false
        statusText = "Your turn! Repeat ${notes.size} notes!"
    }

    LaunchedEffect(isEchoMode, roundKey) {
        if (isEchoMode) {
            val baseLen = if (difficulty == DifficultyLevel.EASY) 2 else 3
            val initial = List(baseLen) { Random.nextInt(RainbowBars.size) }
            sequence = initial
            userStep = 0
            level = 1
            playSequence(initial)
        } else {
            statusText = "Tap any rainbow key to play music!"
        }
    }

    fun handleKeyTap(idx: Int) {
        if (isPlayingBack || isFinished) return
        sound.playXylophoneNote(idx)
        scope.launch {
            activeBar = idx
            delay(200)
            if (activeBar == idx) activeBar = -1
        }

        if (!isEchoMode) {
            score += 5
            return
        }

        val expected = sequence.getOrNull(userStep) ?: return
        if (idx == expected) {
            userStep++
            score += 15
            if (userStep >= sequence.size) {
                confettiTrigger++
                if (level >= maxLevels) {
                    isFinished = true
                    val stars = 3
                    scope.launch {
                        delay(300)
                        sound.playFanfare()
                    }
                    onGameComplete(score, stars)
                } else {
                    level++
                    val nextSeq = sequence + Random.nextInt(RainbowBars.size)
                    sequence = nextSeq
                    userStep = 0
                    scope.launch {
                        delay(450)
                        playSequence(nextSeq)
                    }
                }
            }
        } else {
            sound.playBoing()
            userStep = 0
            statusText = "Oops! Let's hear it again!"
            scope.launch {
                delay(450)
                playSequence(sequence)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        WonderPalette.BerryLight,
                        MaterialTheme.colorScheme.background,
                        WonderPalette.SunnyLight.copy(alpha = 0.45f)
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
                title = "Music Rainbow",
                score = score,
                onBack = onBack,
                accentColor = WonderPalette.BerryPurple
            )

            Column(
                modifier = Modifier
                    .widthIn(max = 580.dp)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !isEchoMode,
                        onClick = { isEchoMode = false },
                        label = { Text("Free Play") },
                        leadingIcon = {
                            Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("xylophone_free_mode")
                    )
                    FilterChip(
                        selected = isEchoMode,
                        onClick = {
                            isEchoMode = true
                            roundKey++
                        },
                        label = { Text("Melody Echo") },
                        leadingIcon = {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("xylophone_echo_mode")
                    )
                }

                Spacer(Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = if (isEchoMode) "Level $level/$maxLevels • $statusText" else statusText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // 8 Rainbow Xylophone Bars
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RainbowBars.forEachIndexed { idx, bar ->
                        val isLit = activeBar == idx
                        val barScale by animateFloatAsState(
                            targetValue = if (isLit) 1.05f else 1f,
                            animationSpec = spring(dampingRatio = 0.45f),
                            label = "bar_scale_$idx"
                        )
                        val shape = RoundedCornerShape(20.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(bar.widthFraction)
                                .scale(barScale)
                                .clip(shape)
                                .background(if (isLit) bar.color else bar.color.copy(alpha = 0.92f))
                                .border(
                                    width = if (isLit) 4.dp else 2.dp,
                                    color = if (isLit) Color.White else bar.shadowColor.copy(alpha = 0.5f),
                                    shape = shape
                                )
                                .clickable { handleKeyTap(idx) }
                                .padding(horizontal = 18.dp)
                                .testTag("xylophone_bar_$idx"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.7f))
                                )
                                Text(
                                    text = bar.noteName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (bar.color == WonderPalette.SunnyYellow) WonderPalette.CocoaText else Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.7f))
                                )
                            }
                        }
                    }
                }

                if (!isEchoMode && score >= 25) {
                    Spacer(Modifier.height(10.dp))
                    ToyButton(
                        text = "Save Music Stars (+1 ★)",
                        onClick = {
                            sound.playFanfare()
                            confettiTrigger++
                            onGameComplete(score, 1)
                            score = 0
                            statusText = "Music stars saved!"
                        },
                        containerColor = WonderPalette.SunnyYellow,
                        shadowColor = WonderPalette.SunnyYellowDark,
                        contentColor = WonderPalette.CocoaText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        testTag = "xylophone_save_stars_button"
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        ConfettiBurstOverlay(triggerKey = confettiTrigger)

        if (isFinished) {
            CelebrationDialog(
                title = "Melody Star!",
                subtitle = "You echoed all $maxLevels rainbow tunes!",
                score = score,
                starsEarned = 3,
                onPlayAgain = { roundKey++ },
                onBackHome = onBack
            )
        }
    }
}
