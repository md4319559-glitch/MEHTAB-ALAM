package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.PlayerProfile
import com.example.data.StickerCatalog
import com.example.data.StickerItem
import com.example.data.UiState
import com.example.ui.components.AnimalAvatarCanvas
import com.example.ui.components.StarPillBadge
import com.example.ui.components.ToyButton
import com.example.ui.theme.WonderPalette

@Composable
fun StickerAlbumScreen(
    profileState: UiState<PlayerProfile>,
    actionMessage: String?,
    onClearMessage: () -> Unit,
    onUnlockSticker: (StickerItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        if (actionMessage != null) {
            snackbarHostState.showSnackbar(actionMessage)
            onClearMessage()
        }
    }

    val profile = (profileState as? UiState.Success)?.data ?: PlayerProfile()
    val unlockedSet = profile.unlockedStickers.toSet()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            WonderPalette.BerryLight.copy(alpha = 0.65f),
                            MaterialTheme.colorScheme.background,
                            WonderPalette.SunnyLight.copy(alpha = 0.45f)
                        )
                    )
                )
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 800.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                    .testTag("stickers_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Text(
                            text = "Sticker Album",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        StarPillBadge(stars = profile.totalStars)
                    }
                }

                item {
                    TrophySummaryCard(profile = profile)
                }

                item {
                    Text(
                        text = "Animal Friends (${unlockedSet.size} / ${StickerCatalog.allStickers.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                StickerCatalog.allStickers.chunked(2).forEach { pair ->
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            pair.forEach { sticker ->
                                val isUnlocked = unlockedSet.contains(sticker.id)
                                val canAfford = profile.totalStars >= sticker.starCost
                                StickerCard(
                                    sticker = sticker,
                                    isUnlocked = isUnlocked,
                                    canAfford = canAfford,
                                    onUnlock = { onUnlockSticker(sticker) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrophySummaryCard(profile: PlayerProfile) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Personal Best Scores",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScoreChip("Balloons", profile.balloonBestScore, WonderPalette.CoralLight, Modifier.weight(1f))
                ScoreChip("Memory", profile.memoryBestScore, WonderPalette.MintLight, Modifier.weight(1f))
                ScoreChip("Math", profile.mathBestScore, WonderPalette.SkyLight, Modifier.weight(1f))
                ScoreChip("Music", profile.xylophoneBestScore, WonderPalette.BerryLight, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ScoreChip(
    label: String,
    value: Long,
    bgColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = WonderPalette.CocoaText
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = WonderPalette.CocoaMuted
            )
        }
    }
}

@Composable
private fun StickerCard(
    sticker: StickerItem,
    isUnlocked: Boolean,
    canAfford: Boolean,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        shape = shape,
        color = if (isUnlocked) WonderPalette.SunnyLight else MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = modifier
            .border(
                width = 2.5.dp,
                color = if (isUnlocked) WonderPalette.SunnyYellow else MaterialTheme.colorScheme.surfaceVariant,
                shape = shape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .alpha(if (isUnlocked) 1f else 0.45f),
                contentAlignment = Alignment.Center
            ) {
                AnimalAvatarCanvas(animal = sticker.animal, sizeDp = 82.dp)
                if (!isUnlocked) {
                    Surface(
                        shape = CircleShape,
                        color = WonderPalette.CocoaText.copy(alpha = 0.72f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Locked",
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = sticker.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = sticker.tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Spacer(Modifier.height(12.dp))

            if (isUnlocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Unlocked",
                        tint = WonderPalette.MintMeadowDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = " Collected!",
                        style = MaterialTheme.typography.labelMedium,
                        color = WonderPalette.MintMeadowDark
                    )
                }
            } else {
                ToyButton(
                    text = "Unlock (${sticker.starCost} ★)",
                    onClick = onUnlock,
                    enabled = canAfford,
                    containerColor = WonderPalette.BerryPurple,
                    shadowColor = WonderPalette.BerryPurpleDark,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "unlock_sticker_${sticker.id}"
                )
            }
        }
    }
}
