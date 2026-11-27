package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.audio.SoundSynthesizer
import com.example.auth.attemptAutoSignIn
import com.example.data.GameType
import com.example.data.SettingsRepository
import com.example.data.WonderPlayRepository
import com.example.ui.WonderPlayViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BalloonPopScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MathSafariScreen
import com.example.ui.screens.MemoryMatchScreen
import com.example.ui.screens.StickerAlbumScreen
import com.example.ui.screens.XylophoneScreen
import com.example.ui.theme.WonderPlayTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private object WonderRoutes {
    const val HOME = "home"
    const val BALLOON_POP = "balloon_pop"
    const val MEMORY_MATCH = "memory_match"
    const val MATH_SAFARI = "math_safari"
    const val XYLOPHONE = "xylophone"
    const val STICKERS = "stickers"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WonderPlayTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = this
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        val uid = user.uid
        val displayName = user.displayName
        key(uid) {
            WonderPlayAuthenticatedApp(
                currentUserId = uid,
                initialDisplayName = displayName
            )
        }
    }
}

@Composable
private fun WonderPlayAuthenticatedApp(
    currentUserId: String,
    initialDisplayName: String?
) {
    val viewModel: WonderPlayViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                WonderPlayViewModel(
                    repository = WonderPlayRepository(db),
                    settingsRepository = SettingsRepository(app),
                    sound = SoundSynthesizer(),
                    currentUserId = currentUserId,
                    initialDisplayName = initialDisplayName
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val sessionsState by viewModel.recentSessionsState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = WonderRoutes.HOME,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(WonderRoutes.HOME) {
            HomeScreen(
                profileState = profileState,
                sessionsState = sessionsState,
                settings = settings,
                actionMessage = actionMessage,
                onClearMessage = viewModel::clearMessage,
                onOpenGame = { type ->
                    val route = when (type) {
                        GameType.BALLOON_POP -> WonderRoutes.BALLOON_POP
                        GameType.MEMORY_MATCH -> WonderRoutes.MEMORY_MATCH
                        GameType.MATH_SAFARI -> WonderRoutes.MATH_SAFARI
                        GameType.XYLOPHONE -> WonderRoutes.XYLOPHONE
                    }
                    navController.navigate(route)
                },
                onOpenStickers = { navController.navigate(WonderRoutes.STICKERS) },
                onSetSound = viewModel::setSoundEnabled,
                onSetDifficulty = viewModel::setDifficulty,
                onUpdateName = viewModel::updatePlayerName,
                onPlayPopSound = { viewModel.sound.playPop() }
            )
        }

        composable(WonderRoutes.BALLOON_POP) {
            BalloonPopScreen(
                difficulty = settings.difficulty,
                sound = viewModel.sound,
                onGameComplete = { score, stars ->
                    viewModel.recordGameResult(GameType.BALLOON_POP, score, stars)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(WonderRoutes.MEMORY_MATCH) {
            MemoryMatchScreen(
                difficulty = settings.difficulty,
                sound = viewModel.sound,
                onGameComplete = { score, stars ->
                    viewModel.recordGameResult(GameType.MEMORY_MATCH, score, stars)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(WonderRoutes.MATH_SAFARI) {
            MathSafariScreen(
                difficulty = settings.difficulty,
                sound = viewModel.sound,
                onGameComplete = { score, stars ->
                    viewModel.recordGameResult(GameType.MATH_SAFARI, score, stars)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(WonderRoutes.XYLOPHONE) {
            XylophoneScreen(
                difficulty = settings.difficulty,
                sound = viewModel.sound,
                onGameComplete = { score, stars ->
                    viewModel.recordGameResult(GameType.XYLOPHONE, score, stars)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(WonderRoutes.STICKERS) {
            StickerAlbumScreen(
                profileState = profileState,
                actionMessage = actionMessage,
                onClearMessage = viewModel::clearMessage,
                onUnlockSticker = viewModel::unlockSticker,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
