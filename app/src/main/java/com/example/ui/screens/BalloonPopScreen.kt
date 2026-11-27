package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundSynthesizer
import com.example.data.DifficultyLevel
import com.example.ui.components.CelebrationDialog
import com.example.ui.components.ConfettiBurstOverlay
import com.example.ui.components.GameTopBar
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.WonderPalette
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private enum class BalloonColor(val label: String, val fill: Color, val highlight: Color) {
    RED("RED", WonderPalette.CoralPop, Color(0xFFFFB2B2)),
    YELLOW("YELLOW", WonderPalette.SunnyYellow, Color(0xFFFFF0A8)),
    BLUE("BLUE", WonderPalette.SkyBlue, Color(0xFFA6D4FF)),
    GREEN("GREEN", WonderPalette.MintMeadow, Color(0xFFA8F5D8)),
    PURPLE("PURPLE", WonderPalette.BerryPurple, Color(0xFFD5C2FF))
}

private data class FloatingBalloon(
    val id: Int,
    val xNorm: Float,
    val yNorm: Float,
    val speed: Float,
    val swayPhase: Float,
    val color: BalloonColor,
    val number: Int
)

@Composable
fun BalloonPopScreen(
    difficulty: DifficultyLevel,
    sound: SoundSynthesizer,
    onGameComplete: (score: Int, stars: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalTargets = 10
    var roundKey by remember { mutableIntStateOf(0) }
    var score by remember(roundKey) { mutableIntStateOf(0) }
    var poppedCount by remember(roundKey) { mutableIntStateOf(0) }
    var streak by remember(roundKey) { mutableIntStateOf(0) }
    var confettiTrigger by remember(roundKey) { mutableIntStateOf(0) }
    var targetColor by remember(roundKey) { mutableStateOf(BalloonColor.entries.random()) }
    var targetNumber by remember(roundKey) { mutableIntStateOf(Random.nextInt(1, 6)) }
    var useNumberPrompt by remember(roundKey) { mutableStateOf(false) }
    var isFinished by remember(roundKey) { mutableStateOf(false) }
    var nextId by remember(roundKey) { mutableIntStateOf(1) }
    var feedbackText by remember(roundKey) { mutableStateOf("Tap the matching balloon!") }

    val balloons = remember(roundKey) { mutableStateListOf<FloatingBalloon>() }
    val textMeasurer: TextMeasurer = rememberTextMeasurer()

    fun pickNextTarget() {
        val askNumber = difficulty == DifficultyLevel.MEDIUM && Random.nextBoolean()
        useNumberPrompt = askNumber
        if (askNumber) {
            val existingNums = balloons.map { it.number }
            targetNumber = if (existingNums.isNotEmpty() && Random.nextFloat() < 0.7f) {
                existingNums.random()
            } else {
                Random.nextInt(1, 7)
            }
        } else {
            val existingColors = balloons.map { it.color }
            targetColor = if (existingColors.isNotEmpty() && Random.nextFloat() < 0.7f) {
                existingColors.random()
            } else {
                BalloonColor.entries.random()
            }
        }
    }

    fun spawnBalloon(startY: Float = 1.12f, forceTarget: Boolean = false): FloatingBalloon {
        val color = if (forceTarget && !useNumberPrompt) targetColor else BalloonColor.entries.random()
        val num = if (forceTarget && useNumberPrompt) targetNumber else Random.nextInt(1, 7)
        val speedBase = if (difficulty == DifficultyLevel.EASY) 0.16f else 0.24f
        return FloatingBalloon(
            id = nextId++,
            xNorm = Random.nextFloat() * 0.74f + 0.13f,
            yNorm = startY,
            speed = speedBase + Random.nextFloat() * 0.08f,
            swayPhase = Random.nextFloat() * 6.28f,
            color = color,
            number = num
        )
    }

    LaunchedEffect(roundKey) {
        balloons.clear()
        repeat(6) { idx ->
            balloons.add(spawnBalloon(startY = 0.28f + idx * 0.14f, forceTarget = idx == 0))
        }
        pickNextTarget()

        var lastTime = 0L
        while (!isFinished) {
            withFrameMillis { frameTime ->
                if (lastTime == 0L) {
                    lastTime = frameTime
                    return@withFrameMillis
                }
                val dt = ((frameTime - lastTime) / 1000f).coerceAtMost(0.05f)
                lastTime = frameTime

                for (i in balloons.indices) {
                    val b = balloons[i]
                    val newY = b.yNorm - b.speed * dt
                    if (newY < -0.18f) {
                        val needsTarget = balloons.none {
                            if (useNumberPrompt) it.number == targetNumber else it.color == targetColor
                        }
                        balloons[i] = spawnBalloon(startY = 1.12f, forceTarget = needsTarget)
                    } else {
                        balloons[i] = b.copy(
                            yNorm = newY,
                            swayPhase = b.swayPhase + dt * 2.4f
                        )
                    }
                }
                while (balloons.size < 6) {
                    val needsTarget = balloons.none {
                        if (useNumberPrompt) it.number == targetNumber else it.color == targetColor
                    }
                    balloons.add(spawnBalloon(startY = 1.1f, forceTarget = needsTarget))
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        WonderPalette.SkyLight,
                        Color(0xFFF3FAFF),
                        WonderPalette.MintLight.copy(alpha = 0.45f)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GameTopBar(
                title = "Balloon Pop",
                score = score,
                onBack = onBack,
                accentColor = WonderPalette.CoralPop
            )

            // Prompt banner
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .border(2.dp, WonderPalette.SkyBlue.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (!useNumberPrompt) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(targetColor.fill)
                                    .border(2.dp, Color.White, CircleShape)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Pop the ${targetColor.label} balloon!",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = WonderPalette.SunnyYellow,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = targetNumber.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = WonderPalette.CocoaText
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Pop number $targetNumber!",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { poppedCount / totalTargets.toFloat() },
                        color = WonderPalette.MintMeadow,
                        trackColor = WonderPalette.MintLight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$feedbackText ($poppedCount / $totalTargets)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Playfield
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("balloon_playfield")
            ) {
                val density = LocalDensity.current
                val balloonRadiusPx = with(density) { 46.dp.toPx() }
                val widthPx = with(density) { maxWidth.toPx() }
                val heightPx = with(density) { maxHeight.toPx() }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(roundKey, useNumberPrompt, targetColor, targetNumber, isFinished) {
                            detectTapGestures { tapOffset ->
                                if (isFinished) return@detectTapGestures
                                val hitIndex = balloons.indexOfLast { b ->
                                    val bx = (b.xNorm * widthPx) + sin(b.swayPhase) * 18f
                                    val by = b.yNorm * heightPx
                                    hypot(tapOffset.x - bx, tapOffset.y - by) <= balloonRadiusPx * 1.25f
                                }
                                if (hitIndex >= 0) {
                                    val tapped = balloons[hitIndex]
                                    val isMatch = if (useNumberPrompt) {
                                        tapped.number == targetNumber
                                    } else {
                                        tapped.color == targetColor
                                    }
                                    if (isMatch) {
                                        sound.playPop()
                                        confettiTrigger++
                                        streak++
                                        score += 10 + (streak.coerceAtMost(4) * 2)
                                        poppedCount++
                                        feedbackText = if (streak >= 3) "Super Streak x$streak!" else "Pop! Great job!"
                                        balloons.removeAt(hitIndex)
                                        if (poppedCount >= totalTargets) {
                                            isFinished = true
                                            val stars = when {
                                                score >= 125 -> 3
                                                score >= 95 -> 2
                                                else -> 1
                                            }
                                            sound.playFanfare()
                                            onGameComplete(score, stars)
                                        } else {
                                            pickNextTarget()
                                        }
                                    } else {
                                        sound.playBoing()
                                        streak = 0
                                        feedbackText = if (useNumberPrompt) {
                                            "Oops! Look for number $targetNumber"
                                        } else {
                                            "Oops! Look for ${targetColor.label}"
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    // Decorative soft clouds
                    drawCircle(Color.White.copy(alpha = 0.65f), radius = 58f, center = Offset(size.width * 0.18f, size.height * 0.16f))
                    drawCircle(Color.White.copy(alpha = 0.65f), radius = 42f, center = Offset(size.width * 0.25f, size.height * 0.18f))
                    drawCircle(Color.White.copy(alpha = 0.55f), radius = 64f, center = Offset(size.width * 0.80f, size.height * 0.28f))

                    balloons.forEach { b ->
                        val bx = (b.xNorm * size.width) + sin(b.swayPhase) * 18f
                        val by = b.yNorm * size.height
                        val rx = balloonRadiusPx * 0.88f
                        val ry = balloonRadiusPx * 1.06f

                        // String
                        val stringPath = Path().apply {
                            moveTo(bx, by + ry)
                            quadraticTo(
                                bx + sin(b.swayPhase + 1f) * 16f,
                                by + ry + 32f,
                                bx,
                                by + ry + 64f
                            )
                        }
                        drawPath(
                            path = stringPath,
                            color = WonderPalette.CocoaMuted.copy(alpha = 0.5f),
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                        )

                        // Balloon body
                        drawOval(
                            color = b.color.fill,
                            topLeft = Offset(bx - rx, by - ry),
                            size = Size(rx * 2f, ry * 2f)
                        )
                        // Gloss highlight
                        drawOval(
                            color = b.color.highlight.copy(alpha = 0.65f),
                            topLeft = Offset(bx - rx * 0.55f, by - ry * 0.65f),
                            size = Size(rx * 0.36f, ry * 0.5f)
                        )
                        // Knot
                        val knot = Path().apply {
                            moveTo(bx - 10f, by + ry + 6f)
                            lineTo(bx + 10f, by + ry + 6f)
                            lineTo(bx, by + ry - 3f)
                            close()
                        }
                        drawPath(knot, b.color.fill)

                        if (difficulty == DifficultyLevel.MEDIUM) {
                            val measured = textMeasurer.measure(
                                text = b.number.toString(),
                                style = TextStyle(
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 26.sp,
                                    color = Color.White
                                )
                            )
                            drawText(
                                textLayoutResult = measured,
                                topLeft = Offset(
                                    bx - measured.size.width / 2f,
                                    by - measured.size.height / 2f
                                )
                            )
                        }
                    }
                }
            }
        }

        ConfettiBurstOverlay(triggerKey = confettiTrigger)

        if (isFinished) {
            val stars = when {
                score >= 125 -> 3
                score >= 95 -> 2
                else -> 1
            }
            CelebrationDialog(
                title = "Balloon Master!",
                subtitle = "You popped all $totalTargets balloons!",
                score = score,
                starsEarned = stars,
                onPlayAgain = { roundKey++ },
                onBackHome = onBack
            )
        }
    }
}
