package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.AnimalKind
import com.example.ui.theme.WonderPalette
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ToyButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    shadowColor: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String = "toy_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val topOffset by animateDpAsState(
        targetValue = if (isPressed || !enabled) 4.dp else 0.dp,
        animationSpec = spring(stiffness = 700f),
        label = "toy_btn_offset"
    )
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .height(58.dp)
            .minimumInteractiveComponentSize()
            .testTag(testTag)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        // Bottom 3D toy lip
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 5.dp)
                .background(if (enabled) shadowColor else shadowColor.copy(alpha = 0.4f), shape)
        )
        // Top tactile face
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(53.dp)
                .offset(y = topOffset)
                .background(if (enabled) containerColor else containerColor.copy(alpha = 0.5f), shape)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StarPillBadge(
    stars: Long,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val pulse by rememberInfiniteTransition(label = "star_pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_scale"
    )
    Surface(
        shape = RoundedCornerShape(50),
        color = WonderPalette.SunnyLight,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = modifier
            .border(2.dp, WonderPalette.SunnyYellow, RoundedCornerShape(50))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .minimumInteractiveComponentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = "Stars",
                tint = WonderPalette.SunnyYellowDark,
                modifier = Modifier
                    .size(22.dp)
                    .scale(pulse)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stars.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = WonderPalette.CocoaText,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun GameTopBar(
    title: String,
    score: Int,
    onBack: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp,
            modifier = Modifier.size(48.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("game_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Surface(
            shape = RoundedCornerShape(50),
            color = accentColor.copy(alpha = 0.16f),
            modifier = Modifier.border(1.5.dp, accentColor, RoundedCornerShape(50))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = "Score",
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = score.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

private data class ConfettiPiece(
    val startX: Float,
    val startY: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val color: Color
)

@Composable
fun ConfettiBurstOverlay(
    triggerKey: Int,
    modifier: Modifier = Modifier
) {
    if (triggerKey <= 0) return
    val progress = remember(triggerKey) { Animatable(0f) }
    val pieces = remember(triggerKey) {
        val rng = Random(triggerKey * 997)
        val colors = listOf(
            WonderPalette.CoralPop,
            WonderPalette.SunnyYellow,
            WonderPalette.SkyBlue,
            WonderPalette.MintMeadow,
            WonderPalette.BerryPurple,
            WonderPalette.BubblePink
        )
        List(36) {
            val angle = rng.nextDouble(0.0, Math.PI * 2.0)
            val speed = rng.nextFloat() * 0.45f + 0.15f
            ConfettiPiece(
                startX = 0.5f + (rng.nextFloat() - 0.5f) * 0.25f,
                startY = 0.35f + (rng.nextFloat() - 0.5f) * 0.15f,
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed - 0.15f).toFloat(),
                radius = rng.nextFloat() * 8f + 5f,
                color = colors[it % colors.size]
            )
        }
    }

    LaunchedEffect(triggerKey) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 1200, easing = LinearEasing))
    }

    if (progress.value < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val t = progress.value
            val alpha = (1f - t).coerceIn(0f, 1f)
            pieces.forEach { p ->
                val x = (p.startX + p.vx * t) * size.width
                val y = (p.startY + p.vy * t + 0.55f * t * t) * size.height
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = p.radius,
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
fun CelebrationDialog(
    title: String,
    subtitle: String,
    score: Int,
    starsEarned: Int,
    onPlayAgain: () -> Unit,
    onBackHome: () -> Unit
) {
    Dialog(onDismissRequest = onBackHome) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(3.dp, WonderPalette.SunnyYellow, RoundedCornerShape(32.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { idx ->
                        val earned = idx < starsEarned
                        val scale by animateFloatAsState(
                            targetValue = if (earned) 1.1f else 0.85f,
                            animationSpec = spring(dampingRatio = 0.45f),
                            label = "celebration_star_$idx"
                        )
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = if (earned) WonderPalette.SunnyYellow else WonderPalette.CocoaMuted.copy(alpha = 0.22f),
                            modifier = Modifier
                                .size(if (idx == 1) 56.dp else 44.dp)
                                .scale(scale)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = WonderPalette.SunnyLight
                ) {
                    Text(
                        text = "Score: $score  •  +$starsEarned Stars",
                        style = MaterialTheme.typography.titleMedium,
                        color = WonderPalette.CocoaText,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }

                Spacer(Modifier.height(22.dp))
                ToyButton(
                    text = stringResource(R.string.play_again),
                    onClick = onPlayAgain,
                    containerColor = WonderPalette.MintMeadow,
                    shadowColor = WonderPalette.MintMeadowDark,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "celebration_play_again_button"
                )
                Spacer(Modifier.height(12.dp))
                ToyButton(
                    text = stringResource(R.string.back_home),
                    onClick = onBackHome,
                    containerColor = WonderPalette.SkyBlue,
                    shadowColor = WonderPalette.SkyBlueDark,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "celebration_home_button"
                )
            }
        }
    }
}

@Composable
fun AnimalAvatarCanvas(
    animal: AnimalKind,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 64.dp
) {
    Canvas(modifier = modifier.size(sizeDp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(w, h) * 0.36f

        when (animal) {
            AnimalKind.LION -> {
                // Scalloped mane
                for (i in 0 until 10) {
                    val angle = (i * 36.0) * Math.PI / 180.0
                    val mx = cx + (r * 0.92f * cos(angle)).toFloat()
                    val my = cy + (r * 0.92f * sin(angle)).toFloat()
                    drawCircle(WonderPalette.TangerineDark, radius = r * 0.34f, center = Offset(mx, my))
                }
                drawCircle(WonderPalette.SunnyYellow, radius = r * 0.85f, center = Offset(cx, cy))
                drawCircle(WonderPalette.SunnyLight, radius = r * 0.42f, center = Offset(cx, cy + r * 0.22f))
                drawCuteFace(cx, cy, r)
            }
            AnimalKind.PANDA -> {
                drawCircle(WonderPalette.CocoaText, radius = r * 0.32f, center = Offset(cx - r * 0.68f, cy - r * 0.62f))
                drawCircle(WonderPalette.CocoaText, radius = r * 0.32f, center = Offset(cx + r * 0.68f, cy - r * 0.62f))
                drawCircle(Color.White, radius = r * 0.88f, center = Offset(cx, cy))
                drawCircle(WonderPalette.CocoaText, radius = r * 0.24f, center = Offset(cx - r * 0.32f, cy - r * 0.1f))
                drawCircle(WonderPalette.CocoaText, radius = r * 0.24f, center = Offset(cx + r * 0.32f, cy - r * 0.1f))
                drawCircle(Color.White, radius = r * 0.08f, center = Offset(cx - r * 0.32f, cy - r * 0.12f))
                drawCircle(Color.White, radius = r * 0.08f, center = Offset(cx + r * 0.32f, cy - r * 0.12f))
                drawCircle(WonderPalette.CocoaText, radius = r * 0.12f, center = Offset(cx, cy + r * 0.16f))
            }
            AnimalKind.BUNNY -> {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(cx - r * 0.56f, cy - r * 1.28f),
                    size = Size(r * 0.38f, r * 0.9f),
                    cornerRadius = CornerRadius(r * 0.2f, r * 0.2f)
                )
                drawRoundRect(
                    color = WonderPalette.BubblePink.copy(alpha = 0.55f),
                    topLeft = Offset(cx - r * 0.46f, cy - r * 1.15f),
                    size = Size(r * 0.18f, r * 0.6f),
                    cornerRadius = CornerRadius(r * 0.1f, r * 0.1f)
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(cx + r * 0.18f, cy - r * 1.28f),
                    size = Size(r * 0.38f, r * 0.9f),
                    cornerRadius = CornerRadius(r * 0.2f, r * 0.2f)
                )
                drawRoundRect(
                    color = WonderPalette.BubblePink.copy(alpha = 0.55f),
                    topLeft = Offset(cx + r * 0.28f, cy - r * 1.15f),
                    size = Size(r * 0.18f, r * 0.6f),
                    cornerRadius = CornerRadius(r * 0.1f, r * 0.1f)
                )
                drawCircle(Color.White, radius = r * 0.85f, center = Offset(cx, cy + r * 0.08f))
                drawCuteFace(cx, cy + r * 0.08f, r, noseColor = WonderPalette.BubblePink)
            }
            AnimalKind.FROG -> {
                drawCircle(WonderPalette.MintMeadow, radius = r * 0.36f, center = Offset(cx - r * 0.52f, cy - r * 0.64f))
                drawCircle(WonderPalette.MintMeadow, radius = r * 0.36f, center = Offset(cx + r * 0.52f, cy - r * 0.64f))
                drawCircle(Color.White, radius = r * 0.22f, center = Offset(cx - r * 0.52f, cy - r * 0.64f))
                drawCircle(Color.White, radius = r * 0.22f, center = Offset(cx + r * 0.52f, cy - r * 0.64f))
                drawCircle(WonderPalette.CocoaText, radius = r * 0.11f, center = Offset(cx - r * 0.52f, cy - r * 0.64f))
                drawCircle(WonderPalette.CocoaText, radius = r * 0.11f, center = Offset(cx + r * 0.52f, cy - r * 0.64f))
                drawOval(
                    color = WonderPalette.MintMeadow,
                    topLeft = Offset(cx - r * 0.95f, cy - r * 0.55f),
                    size = Size(r * 1.9f, r * 1.35f)
                )
                drawCircle(WonderPalette.BubblePink.copy(alpha = 0.45f), radius = r * 0.17f, center = Offset(cx - r * 0.55f, cy + r * 0.15f))
                drawCircle(WonderPalette.BubblePink.copy(alpha = 0.45f), radius = r * 0.17f, center = Offset(cx + r * 0.55f, cy + r * 0.15f))
                drawArc(
                    color = WonderPalette.CocoaText,
                    startAngle = 15f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(cx - r * 0.4f, cy - r * 0.05f),
                    size = Size(r * 0.8f, r * 0.45f),
                    style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                )
            }
            AnimalKind.OWL -> {
                drawCircle(WonderPalette.BerryPurple, radius = r * 0.9f, center = Offset(cx, cy + r * 0.05f))
                drawCircle(WonderPalette.BerryLight, radius = r * 0.38f, center = Offset(cx - r * 0.35f, cy - r * 0.05f))
                drawCircle(WonderPalette.BerryLight, radius = r * 0.38f, center = Offset(cx + r * 0.35f, cy - r * 0.05f))
                drawCircle(WonderPalette.CocoaText, radius = r * 0.14f, center = Offset(cx - r * 0.35f, cy - r * 0.05f))
                drawCircle(WonderPalette.CocoaText, radius = r * 0.14f, center = Offset(cx + r * 0.35f, cy - r * 0.05f))
                val beak = Path().apply {
                    moveTo(cx - r * 0.15f, cy + r * 0.1f)
                    lineTo(cx + r * 0.15f, cy + r * 0.1f)
                    lineTo(cx, cy + r * 0.36f)
                    close()
                }
                drawPath(beak, WonderPalette.SunnyYellow)
            }
            AnimalKind.ELEPHANT -> {
                val earColor = WonderPalette.SkyBlue.copy(alpha = 0.75f)
                drawCircle(earColor, radius = r * 0.55f, center = Offset(cx - r * 0.82f, cy))
                drawCircle(earColor, radius = r * 0.55f, center = Offset(cx + r * 0.82f, cy))
                drawCircle(WonderPalette.SkyLight, radius = r * 0.82f, center = Offset(cx, cy))
                drawCuteFace(cx, cy - r * 0.08f, r, showNose = false)
                drawRoundRect(
                    color = WonderPalette.SkyBlue,
                    topLeft = Offset(cx - r * 0.16f, cy + r * 0.06f),
                    size = Size(r * 0.32f, r * 0.7f),
                    cornerRadius = CornerRadius(r * 0.16f, r * 0.16f)
                )
            }
            AnimalKind.FOX -> {
                val leftEar = Path().apply {
                    moveTo(cx - r * 0.78f, cy - r * 0.25f)
                    lineTo(cx - r * 0.55f, cy - r * 1.05f)
                    lineTo(cx - r * 0.1f, cy - r * 0.55f)
                    close()
                }
                val rightEar = Path().apply {
                    moveTo(cx + r * 0.78f, cy - r * 0.25f)
                    lineTo(cx + r * 0.55f, cy - r * 1.05f)
                    lineTo(cx + r * 0.1f, cy - r * 0.55f)
                    close()
                }
                drawPath(leftEar, WonderPalette.CoralPop)
                drawPath(rightEar, WonderPalette.CoralPop)
                drawCircle(WonderPalette.Tangerine, radius = r * 0.85f, center = Offset(cx, cy + r * 0.05f))
                drawOval(
                    color = Color.White,
                    topLeft = Offset(cx - r * 0.65f, cy + r * 0.05f),
                    size = Size(r * 1.3f, r * 0.75f)
                )
                drawCuteFace(cx, cy + r * 0.02f, r)
            }
            AnimalKind.WHALE -> {
                drawOval(
                    color = WonderPalette.AquaTeal,
                    topLeft = Offset(cx - r * 0.95f, cy - r * 0.65f),
                    size = Size(r * 1.9f, r * 1.35f)
                )
                drawOval(
                    color = Color.White.copy(alpha = 0.88f),
                    topLeft = Offset(cx - r * 0.65f, cy + r * 0.12f),
                    size = Size(r * 1.3f, r * 0.52f)
                )
                // Water spout
                drawCircle(WonderPalette.SkyLight, radius = r * 0.16f, center = Offset(cx - r * 0.18f, cy - r * 0.88f))
                drawCircle(WonderPalette.SkyLight, radius = r * 0.16f, center = Offset(cx + r * 0.18f, cy - r * 0.88f))
                drawCuteFace(cx, cy - r * 0.06f, r, showNose = false)
            }
        }
    }
}

private fun DrawScope.drawCuteFace(
    cx: Float,
    cy: Float,
    r: Float,
    noseColor: Color = WonderPalette.CocoaText,
    showNose: Boolean = true
) {
    drawCircle(WonderPalette.CocoaText, radius = r * 0.1f, center = Offset(cx - r * 0.32f, cy - r * 0.1f))
    drawCircle(WonderPalette.CocoaText, radius = r * 0.1f, center = Offset(cx + r * 0.32f, cy - r * 0.1f))
    drawCircle(Color.White, radius = r * 0.035f, center = Offset(cx - r * 0.35f, cy - r * 0.13f))
    drawCircle(Color.White, radius = r * 0.035f, center = Offset(cx + r * 0.29f, cy - r * 0.13f))
    drawCircle(WonderPalette.BubblePink.copy(alpha = 0.45f), radius = r * 0.14f, center = Offset(cx - r * 0.48f, cy + r * 0.12f))
    drawCircle(WonderPalette.BubblePink.copy(alpha = 0.45f), radius = r * 0.14f, center = Offset(cx + r * 0.48f, cy + r * 0.12f))
    if (showNose) {
        drawCircle(noseColor, radius = r * 0.1f, center = Offset(cx, cy + r * 0.1f))
    }
    drawArc(
        color = WonderPalette.CocoaText,
        startAngle = 15f,
        sweepAngle = 150f,
        useCenter = false,
        topLeft = Offset(cx - r * 0.22f, cy + r * 0.14f),
        size = Size(r * 0.44f, r * 0.26f),
        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
    )
}
