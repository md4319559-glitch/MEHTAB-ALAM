package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class GameType(val wireValue: String, val displayTitle: String) {
    BALLOON_POP("balloon_pop", "Balloon Pop"),
    MEMORY_MATCH("memory_match", "Animal Match"),
    MATH_SAFARI("math_safari", "Math Safari"),
    XYLOPHONE("xylophone", "Music Rainbow");

    companion object {
        fun fromWire(raw: String): GameType =
            entries.firstOrNull { it.wireValue == raw } ?: BALLOON_POP
    }
}

enum class DifficultyLevel(val wireValue: String, val label: String) {
    EASY("easy", "Easy (Ages 3–5)"),
    MEDIUM("medium", "Explorer (Ages 6+)");

    companion object {
        fun fromWire(raw: String): DifficultyLevel =
            entries.firstOrNull { it.wireValue == raw } ?: EASY
    }
}

@Keep
@IgnoreExtraProperties
data class PlayerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Little Explorer",
    val totalStars: Long = 0L,
    val gamesPlayed: Long = 0L,
    val balloonBestScore: Long = 0L,
    val memoryBestScore: Long = 0L,
    val mathBestScore: Long = 0L,
    val xylophoneBestScore: Long = 0L,
    val unlockedStickers: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GameSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val gameType: String = GameType.BALLOON_POP.wireValue,
    val score: Long = 0L,
    val starsEarned: Long = 0L,
    val difficulty: String = DifficultyLevel.EASY.wireValue,
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class AnimalKind {
    LION, PANDA, BUNNY, FROG, OWL, ELEPHANT, FOX, WHALE
}

data class StickerItem(
    val id: String,
    val name: String,
    val tagline: String,
    val starCost: Int,
    val animal: AnimalKind
)

object StickerCatalog {
    val allStickers: List<StickerItem> = listOf(
        StickerItem("sunny_lion", "Sunny Lion", "King of the meadow!", 3, AnimalKind.LION),
        StickerItem("happy_bunny", "Hoppy Bunny", "Loves rainbow carrots!", 4, AnimalKind.BUNNY),
        StickerItem("space_panda", "Space Panda", "Counts twinkling stars!", 5, AnimalKind.PANDA),
        StickerItem("music_frog", "Music Frog", "Jumps on xylophone keys!", 6, AnimalKind.FROG),
        StickerItem("wise_owl", "Wise Owl", "Knows every number!", 8, AnimalKind.OWL),
        StickerItem("baby_elephant", "Pip Elephant", "Paints with water splashes!", 10, AnimalKind.ELEPHANT),
        StickerItem("rocket_fox", "Rocket Fox", "Fastest balloon popper!", 12, AnimalKind.FOX),
        StickerItem("rainbow_whale", "Rainbow Whale", "Swims in candy clouds!", 15, AnimalKind.WHALE)
    )
}
