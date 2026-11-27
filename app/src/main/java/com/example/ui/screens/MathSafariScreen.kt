package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.audio.SoundSynthesizer
import com.example.data.DifficultyLevel
import com.example.ui.components.CelebrationDialog
import com.example.ui.components.ConfettiBurstOverlay
import com.example.ui.components.GameTopBar
import com.example.ui.components.ToyButton
import com.example.ui.theme.WonderPalette
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class MathVisualItem { APPLE, STAR, BERRY }

private data class MathQuestion(
    val leftCount: Int,
    val rightCount: Int,
    val isAddition: Boolean,
    val isCountOnly: Boolean,
    val answer: Int,
    val options: List<Int>,
    val visualItem: MathVisualItem
)

private fun generateMathQuestion(difficulty: DifficultyLevel): MathQuestion {
    val visual = MathVisualItem.entries.random()
    val countOnly = difficulty == DifficultyLevel.EASY && Random.nextFloat() < 0.6f
    if (countOnly) {
        val count = Random.nextInt(2, 9)
        val options = buildOptions(count, min = 1, max = 10)
        return MathQuestion(count, 0, true, true, count, options, visual)
    }
    val maxOperand = if (difficulty == DifficultyLevel.EASY) 5 else 8
    val isAdd = difficulty == DifficultyLevel.EASY || Random.nextBoolean()
    val a: Int
    val b: Int
    val ans: Int
    if (isAdd) {
        a = Random.nextInt(1, maxOperand + 1)
        b = Random.nextInt(1, maxOperand + 1)
        ans = a + b
    } else {
        val x = Random.nextInt(3, 12)
        val y = Random.nextInt(1, x)
        a = x
        b = y
        ans = x - y
    }
    return MathQuestion(a, b, isAdd, false, ans, buildOptions(ans, 1, 16), visual)
}

private fun buildOptions(answer: Int, min: Int, max: Int): List<Int> {
    val set = mutableSetOf(answer)
    while (set.size < 4) {
        val offset = Random.nextInt(-3, 4)
        val candidate = (answer + offset).coerceIn(min, max)
        set.add(candidate)
    }
    return set.toList().shuffled()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MathSafariScreen(
    difficulty: DifficultyLevel,
    sound: SoundSynthesizer,
    onGameComplete: (score: Int, stars: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalQuestions = 8
    var roundKey by remember { mutableIntStateOf(0) }
    var currentIndex by remember(roundKey, difficulty) { mutableIntStateOf(0) }
    var score by remember(roundKey, difficulty) { mutableIntStateOf(0) }
    var correctCount by remember(roundKey, difficulty) { mutableIntStateOf(0) }
    var confettiTrigger by remember(roundKey, difficulty) { mutableIntStateOf(0) }
    var question by remember(roundKey, difficulty) { mutableStateOf(generateMathQuestion(difficulty)) }
    var hintMessage by remember(roundKey, difficulty) { mutableStateOf("Tap the right number!") }
    var isFinished by remember(roundKey, difficulty) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        WonderPalette.SkyLight,
                        MaterialTheme.colorScheme.background,
                        WonderPalette.SunnyLight.copy(alpha = 0.5f)
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
                title = "Math Safari",
                score = score,
                onBack = onBack,
                accentColor = WonderPalette.SkyBlue
            )

            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        progress = { currentIndex / totalQuestions.toFloat() },
                        color = WonderPalette.SkyBlue,
                        trackColor = WonderPalette.SkyLight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Puzzle ${currentIndex + 1} of $totalQuestions  •  $hintMessage",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Visual Puzzle Card
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(3.dp, WonderPalette.SkyBlue.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = when {
                                question.isCountOnly -> "How many do you see?"
                                question.isAddition -> "${question.leftCount}  +  ${question.rightCount}  =  ?"
                                else -> "${question.leftCount}  -  ${question.rightCount}  =  ?"
                            },
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        if (question.isCountOnly) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                repeat(question.leftCount) {
                                    VisualItemBadge(item = question.visualItem)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = WonderPalette.SunnyLight,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        repeat(question.leftCount) {
                                            VisualItemBadge(item = question.visualItem, compact = true)
                                        }
                                    }
                                }

                                Text(
                                    text = if (question.isAddition) "+" else "−",
                                    style = MaterialTheme.typography.displayLarge,
                                    color = WonderPalette.CoralPop
                                )

                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (question.isAddition) WonderPalette.MintLight else WonderPalette.CoralLight,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        repeat(question.rightCount) {
                                            VisualItemBadge(
                                                item = question.visualItem,
                                                compact = true,
                                                faded = !question.isAddition
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2x2 Big Bouncy Answer Buttons
                val buttonColors = listOf(
                    WonderPalette.CoralPop to WonderPalette.CoralPopDark,
                    WonderPalette.MintMeadow to WonderPalette.MintMeadowDark,
                    WonderPalette.SunnyYellow to WonderPalette.SunnyYellowDark,
                    WonderPalette.BerryPurple to WonderPalette.BerryPurpleDark
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp)
                ) {
                    question.options.chunked(2).forEachIndexed { rowIdx, rowOpts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowOpts.forEachIndexed { colIdx, optionVal ->
                                val idx = rowIdx * 2 + colIdx
                                val (bg, shadow) = buttonColors[idx % buttonColors.size]
                                val textColor = if (bg == WonderPalette.SunnyYellow) WonderPalette.CocoaText else Color.White
                                ToyButton(
                                    text = optionVal.toString(),
                                    onClick = {
                                        if (isFinished) return@ToyButton
                                        if (optionVal == question.answer) {
                                            sound.playChime()
                                            confettiTrigger++
                                            score += 15
                                            correctCount++
                                            hintMessage = "Yay! $optionVal is right!"
                                            if (currentIndex + 1 >= totalQuestions) {
                                                isFinished = true
                                                val stars = when {
                                                    correctCount >= 7 -> 3
                                                    correctCount >= 5 -> 2
                                                    else -> 1
                                                }
                                                sound.playFanfare()
                                                onGameComplete(score, stars)
                                            } else {
                                                currentIndex++
                                                question = generateMathQuestion(difficulty)
                                            }
                                        } else {
                                            sound.playBoing()
                                            score = (score - 2).coerceAtLeast(0)
                                            hintMessage = "Almost! Try counting again!"
                                        }
                                    },
                                    containerColor = bg,
                                    shadowColor = shadow,
                                    contentColor = textColor,
                                    modifier = Modifier.weight(1f),
                                    testTag = "math_option_$idx"
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
                correctCount >= 7 -> 3
                correctCount >= 5 -> 2
                else -> 1
            }
            CelebrationDialog(
                title = "Math Explorer!",
                subtitle = "You solved all $totalQuestions sunny puzzles!",
                score = score,
                starsEarned = stars,
                onPlayAgain = { roundKey++ },
                onBackHome = onBack
            )
        }
    }
}

@Composable
private fun VisualItemBadge(
    item: MathVisualItem,
    compact: Boolean = false,
    faded: Boolean = false
) {
    val sizeDp = if (compact) 30.dp else 40.dp
    val alpha = if (faded) 0.45f else 1f
    Canvas(modifier = Modifier.size(sizeDp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.42f
        when (item) {
            MathVisualItem.APPLE -> {
                drawCircle(WonderPalette.CoralPop.copy(alpha = alpha), radius = r, center = Offset(cx, cy + r * 0.12f))
                drawCircle(WonderPalette.MintMeadow.copy(alpha = alpha), radius = r * 0.34f, center = Offset(cx + r * 0.28f, cy - r * 0.85f))
            }
            MathVisualItem.STAR -> {
                val starPath = Path()
                for (i in 0 until 10) {
                    val rad = if (i % 2 == 0) r else r * 0.46f
                    val angle = Math.toRadians((i * 36.0) - 90.0)
                    val px = cx + (rad * cos(angle)).toFloat()
                    val py = cy + (rad * sin(angle)).toFloat()
                    if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                }
                starPath.close()
                drawPath(starPath, WonderPalette.SunnyYellow.copy(alpha = alpha))
            }
            MathVisualItem.BERRY -> {
                drawCircle(WonderPalette.BerryPurple.copy(alpha = alpha), radius = r * 0.9f, center = Offset(cx, cy + r * 0.1f))
                drawCircle(WonderPalette.BubblePink.copy(alpha = alpha * 0.7f), radius = r * 0.3f, center = Offset(cx - r * 0.25f, cy - r * 0.15f))
            }
        }
    }
}
