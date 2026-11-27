package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.signOut
import com.example.data.AppSettings
import com.example.data.DifficultyLevel
import com.example.data.GameSession
import com.example.data.GameType
import com.example.data.PlayerProfile
import com.example.data.UiState
import com.example.ui.components.StarPillBadge
import com.example.ui.components.ToyButton
import com.example.ui.theme.WonderPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    profileState: UiState<PlayerProfile>,
    sessionsState: UiState<List<GameSession>>,
    settings: AppSettings,
    actionMessage: String?,
    onClearMessage: () -> Unit,
    onOpenGame: (GameType) -> Unit,
    onOpenStickers: () -> Unit,
    onSetSound: (Boolean) -> Unit,
    onSetDifficulty: (DifficultyLevel) -> Unit,
    onUpdateName: (String) -> Unit,
    onPlayPopSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showSettingsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(actionMessage) {
        if (actionMessage != null) {
            snackbarHostState.showSnackbar(actionMessage)
            onClearMessage()
        }
    }

    val profile = (profileState as? UiState.Success)?.data ?: PlayerProfile()
    val errorMsg = (profileState as? UiState.Error)?.message

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
                            WonderPalette.SkyLight.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.background,
                            WonderPalette.SunnyLight.copy(alpha = 0.35f)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                val isWide = maxWidth >= 600.dp

                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 920.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item {
                        HomeHeaderRow(
                            displayName = profile.displayName,
                            totalStars = profile.totalStars,
                            unlockedStickersCount = profile.unlockedStickers.size,
                            onOpenStickers = {
                                onPlayPopSound()
                                onOpenStickers()
                            },
                            onOpenSettings = {
                                onPlayPopSound()
                                showSettingsSheet = true
                            }
                        )
                    }

                    if (errorMsg != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = WonderPalette.CoralLight,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Cloud sync note: $errorMsg",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WonderPalette.CoralPopDark,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }

                    item {
                        HeroPlayBanner(
                            gamesPlayed = profile.gamesPlayed,
                            onSurpriseGame = {
                                onPlayPopSound()
                                onOpenGame(GameType.entries.random())
                            }
                        )
                    }

                    item {
                        Text(
                            text = "Choose Your Adventure",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    item {
                        val cards = listOf(
                            GameCardSpec(
                                type = GameType.BALLOON_POP,
                                title = stringResource(R.string.game_balloon_title),
                                desc = stringResource(R.string.game_balloon_desc),
                                bestScore = profile.balloonBestScore,
                                primaryColor = WonderPalette.CoralPop,
                                shadowColor = WonderPalette.CoralPopDark,
                                icon = Icons.Rounded.Celebration,
                                illustrationRes = R.drawable.img_game_balloons,
                                tag = "card_balloon_pop"
                            ),
                            GameCardSpec(
                                type = GameType.MEMORY_MATCH,
                                title = stringResource(R.string.game_memory_title),
                                desc = stringResource(R.string.game_memory_desc),
                                bestScore = profile.memoryBestScore,
                                primaryColor = WonderPalette.MintMeadow,
                                shadowColor = WonderPalette.MintMeadowDark,
                                icon = Icons.Rounded.Pets,
                                illustrationRes = null,
                                tag = "card_memory_match"
                            ),
                            GameCardSpec(
                                type = GameType.MATH_SAFARI,
                                title = stringResource(R.string.game_math_title),
                                desc = stringResource(R.string.game_math_desc),
                                bestScore = profile.mathBestScore,
                                primaryColor = WonderPalette.SkyBlue,
                                shadowColor = WonderPalette.SkyBlueDark,
                                icon = Icons.Rounded.Calculate,
                                illustrationRes = R.drawable.img_game_safari,
                                tag = "card_math_safari"
                            ),
                            GameCardSpec(
                                type = GameType.XYLOPHONE,
                                title = stringResource(R.string.game_xylophone_title),
                                desc = stringResource(R.string.game_xylophone_desc),
                                bestScore = profile.xylophoneBestScore,
                                primaryColor = WonderPalette.BerryPurple,
                                shadowColor = WonderPalette.BerryPurpleDark,
                                icon = Icons.Rounded.MusicNote,
                                illustrationRes = null,
                                tag = "card_xylophone"
                            )
                        )

                        val columns = if (isWide) 2 else 2
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            cards.chunked(columns).forEach { rowCards ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    rowCards.forEach { spec ->
                                        GameHubCard(
                                            spec = spec,
                                            onClick = {
                                                onPlayPopSound()
                                                onOpenGame(spec.type)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
                        if (sessions.isNotEmpty()) {
                            RecentAdventuresCard(sessions = sessions.take(3))
                        }
                    }
                }
            }
        }
    }

    if (showSettingsSheet) {
        SettingsBottomSheet(
            profile = profile,
            settings = settings,
            onDismiss = { showSettingsSheet = false },
            onSetSound = onSetSound,
            onSetDifficulty = onSetDifficulty,
            onUpdateName = onUpdateName
        )
    }
}

@Composable
private fun HomeHeaderRow(
    displayName: String,
    totalStars: Long,
    unlockedStickersCount: Int,
    onOpenStickers: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_greeting, displayName.substringBefore(" ")),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Ready for sunny playtime?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StarPillBadge(
                stars = totalStars,
                onClick = onOpenStickers,
                modifier = Modifier.testTag("home_stars_badge")
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = WonderPalette.BerryLight,
                modifier = Modifier
                    .border(2.dp, WonderPalette.BerryPurple, RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onOpenStickers)
                    .minimumInteractiveComponentSize()
                    .testTag("home_stickers_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = stringResource(R.string.sticker_album),
                        tint = WonderPalette.BerryPurpleDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$unlockedStickersCount/8",
                        style = MaterialTheme.typography.titleMedium,
                        color = WonderPalette.CocoaText
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
                modifier = Modifier.size(48.dp)
            ) {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("home_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroPlayBanner(
    gamesPlayed: Long,
    onSurpriseGame: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 6.dp,
        color = WonderPalette.WarmSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_wonderplay_hero),
                contentDescription = "WonderPlay Storybook Meadow",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                WonderPalette.CocoaText.copy(alpha = 0.68f),
                                WonderPalette.CocoaText.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = WonderPalette.SunnyYellow
                    ) {
                        Text(
                            text = if (gamesPlayed == 0L) "NEW ADVENTURE" else "$gamesPlayed GAMES PLAYED",
                            style = MaterialTheme.typography.labelMedium,
                            color = WonderPalette.CocoaText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.home_banner_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.home_banner_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                    Spacer(Modifier.height(4.dp))
                    ToyButton(
                        text = stringResource(R.string.play_surprise),
                        onClick = onSurpriseGame,
                        containerColor = WonderPalette.SunnyYellow,
                        shadowColor = WonderPalette.SunnyYellowDark,
                        contentColor = WonderPalette.CocoaText,
                        icon = Icons.Rounded.AutoAwesome,
                        modifier = Modifier.width(195.dp),
                        testTag = "surprise_game_button"
                    )
                }
            }
        }
    }
}

private data class GameCardSpec(
    val type: GameType,
    val title: String,
    val desc: String,
    val bestScore: Long,
    val primaryColor: Color,
    val shadowColor: Color,
    val icon: ImageVector,
    val illustrationRes: Int?,
    val tag: String
)

@Composable
private fun GameHubCard(
    spec: GameCardSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = modifier
            .height(216.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag(spec.tag)
    ) {
        // Tactile toybox shadow lip
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 6.dp)
                .background(spec.shadowColor, shape)
        )
        Surface(
            shape = shape,
            color = spec.primaryColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (spec.illustrationRes != null) {
                        Image(
                            painter = painterResource(id = spec.illustrationRes),
                            contentDescription = spec.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(62.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .border(2.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = spec.icon,
                                contentDescription = spec.title,
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.26f)
                    ) {
                        Text(
                            text = stringResource(R.string.best_score_format, spec.bestScore),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = spec.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = spec.desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentAdventuresCard(sessions: List<GameSession>) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.GridView,
                    contentDescription = null,
                    tint = WonderPalette.SkyBlue
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Recent Playtime Wins",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            sessions.forEach { session ->
                val type = GameType.fromWire(session.gameType)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = type.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Score ${session.score} • +${session.starsEarned} ★",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WonderPalette.SunnyYellowDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsBottomSheet(
    profile: PlayerProfile,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSetSound: (Boolean) -> Unit,
    onSetDifficulty: (DifficultyLevel) -> Unit,
    onUpdateName: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var nameInput by remember(profile.displayName) { mutableStateOf(profile.displayName) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = nameInput,
                onValueChange = { if (it.length <= 40) nameInput = it },
                label = { Text("Kid's Nickname") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_name_input")
            )

            if (nameInput.trim().isNotEmpty() && nameInput.trim() != profile.displayName) {
                ToyButton(
                    text = "Save Nickname",
                    onClick = {
                        onUpdateName(nameInput.trim())
                        onDismiss()
                    },
                    containerColor = WonderPalette.MintMeadow,
                    shadowColor = WonderPalette.MintMeadowDark,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "save_nickname_button"
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = WonderPalette.SkyBlue
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.sound_effects_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Switch(
                    checked = settings.soundEnabled,
                    onCheckedChange = onSetSound,
                    modifier = Modifier.testTag("settings_sound_switch")
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.difficulty_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DifficultyLevel.entries.forEach { level ->
                        FilterChip(
                            selected = settings.difficulty == level,
                            onClick = { onSetDifficulty(level) },
                            label = { Text(level.label) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("difficulty_${level.wireValue}_chip")
                        )
                    }
                }
            }

            ToyButton(
                text = stringResource(R.string.sign_out),
                onClick = {
                    onDismiss()
                    signOut(credentialManager = credentialManager, scope = scope)
                },
                containerColor = WonderPalette.CoralPop,
                shadowColor = WonderPalette.CoralPopDark,
                icon = Icons.AutoMirrored.Rounded.Logout,
                modifier = Modifier.fillMaxWidth(),
                testTag = "settings_sign_out_button"
            )

            Spacer(Modifier.height(10.dp))
        }
    }
}
