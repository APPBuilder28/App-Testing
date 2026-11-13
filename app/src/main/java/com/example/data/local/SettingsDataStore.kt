package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.i18n.AppLanguage
import com.example.core.update.GitHubAppUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.settingsDataStore by preferencesDataStore(name = "nextvu_settings")

enum class PlaylistSection(val keySuffix: String) {
    LIVE("live"),
    MOVIES("movies"),
    SERIES("series")
}

@Serializable
data class PlaylistSelectionConfig(
    val isCustomActive: Boolean = false,
    val selectedCategoryIds: Set<String> = emptySet()
)

fun playlistSelectionKey(sourceId: String, section: PlaylistSection): String =
    "${sourceId}::${section.keySuffix}"

enum class VideoQualityPreset(val label: String, val maxHeightPx: Int?) {
    AUTO("Auto", null),
    HD_720("720p", 720),
    FHD_1080("1080p", 1080),
    UHD_4K("2160p (4K)", 2160)
}

enum class SubtitleColorOption(val label: String, val argb: Int) {
    WHITE("White", 0xFFFFFFFF.toInt()),
    CYAN("Electric Cyan", 0xFF00E5FF.toInt()),
    YELLOW("Cinema Gold", 0xFFFFD54F.toInt()),
    GREEN("Soft Mint", 0xFF69F0AE.toInt())
}

data class UserSettings(
    val videoQuality: VideoQualityPreset = VideoQualityPreset.AUTO,
    val subtitleFontSizeSp: Float = 18f,
    val subtitleColor: SubtitleColorOption = SubtitleColorOption.WHITE,
    val subtitleBgOpacity: Float = 0.65f,
    val parentalLockEnabled: Boolean = false,
    val showBitrateOverlay: Boolean = true,
    val autoNextEpisode: Boolean = true,
    val screenshotBlockingEnabled: Boolean = false,
    val language: AppLanguage = AppLanguage.EN,
    val ukLivePlaylistsOnly: Boolean = false,
    val englishVodOnly: Boolean = false,
    val liveTvUiScalePercent: Int = 40,
    val githubUpdateRepo: String = GitHubAppUpdater.DEFAULT_GITHUB_REPO,
    val playlistSelections: Map<String, PlaylistSelectionConfig> = emptyMap()
) {
    fun playlistSelectionFor(sourceId: String?, section: PlaylistSection): PlaylistSelectionConfig {
        if (sourceId.isNullOrBlank()) return PlaylistSelectionConfig()
        return playlistSelections[playlistSelectionKey(sourceId, section)] ?: PlaylistSelectionConfig()
    }

    fun visibleCategoryIdsFor(sourceId: String?, section: PlaylistSection): Set<String>? {
        val config = playlistSelectionFor(sourceId, section)
        return if (config.isCustomActive) config.selectedCategoryIds else null
    }
}

class SettingsRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private object Keys {
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val SUBTITLE_FONT_SIZE = floatPreferencesKey("subtitle_font_size")
        val SUBTITLE_COLOR = stringPreferencesKey("subtitle_color")
        val SUBTITLE_BG_OPACITY = floatPreferencesKey("subtitle_bg_opacity")
        val PARENTAL_LOCK = booleanPreferencesKey("parental_lock")
        val SHOW_BITRATE = booleanPreferencesKey("show_bitrate")
        val AUTO_NEXT_EPISODE = booleanPreferencesKey("auto_next_episode")
        val SCREENSHOT_BLOCKING = booleanPreferencesKey("screenshot_blocking")
        val LANGUAGE = stringPreferencesKey("app_language")
        val UK_LIVE_PLAYLISTS_ONLY = booleanPreferencesKey("uk_live_playlists_only")
        val ENGLISH_VOD_ONLY = booleanPreferencesKey("english_vod_only")
        val LIVE_TV_UI_SCALE_PERCENT = intPreferencesKey("live_tv_ui_scale_percent")
        val GITHUB_UPDATE_REPO = stringPreferencesKey("github_update_repo")
        val PLAYLIST_SELECTIONS_JSON = stringPreferencesKey("playlist_selections_json")
    }

    val settingsFlow: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        val decodedSelections = prefs[Keys.PLAYLIST_SELECTIONS_JSON]?.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching { json.decodeFromString<Map<String, PlaylistSelectionConfig>>(raw) }.getOrDefault(emptyMap())
        } ?: emptyMap()

        UserSettings(
            videoQuality = prefs[Keys.VIDEO_QUALITY]?.let { name ->
                VideoQualityPreset.entries.firstOrNull { it.name == name }
            } ?: VideoQualityPreset.AUTO,
            subtitleFontSizeSp = prefs[Keys.SUBTITLE_FONT_SIZE] ?: 18f,
            subtitleColor = prefs[Keys.SUBTITLE_COLOR]?.let { name ->
                SubtitleColorOption.entries.firstOrNull { it.name == name }
            } ?: SubtitleColorOption.WHITE,
            subtitleBgOpacity = prefs[Keys.SUBTITLE_BG_OPACITY] ?: 0.65f,
            parentalLockEnabled = prefs[Keys.PARENTAL_LOCK] ?: false,
            showBitrateOverlay = prefs[Keys.SHOW_BITRATE] ?: true,
            autoNextEpisode = prefs[Keys.AUTO_NEXT_EPISODE] ?: true,
            screenshotBlockingEnabled = prefs[Keys.SCREENSHOT_BLOCKING] ?: false,
            language = prefs[Keys.LANGUAGE]?.let { code ->
                AppLanguage.entries.firstOrNull { it.code == code }
            } ?: AppLanguage.EN,
            ukLivePlaylistsOnly = prefs[Keys.UK_LIVE_PLAYLISTS_ONLY] ?: false,
            englishVodOnly = prefs[Keys.ENGLISH_VOD_ONLY] ?: false,
            liveTvUiScalePercent = (prefs[Keys.LIVE_TV_UI_SCALE_PERCENT] ?: 40).coerceIn(0, 100),
            githubUpdateRepo = prefs[Keys.GITHUB_UPDATE_REPO]?.takeIf { it.isNotBlank() } ?: GitHubAppUpdater.DEFAULT_GITHUB_REPO,
            playlistSelections = decodedSelections
        )
    }

    suspend fun setVideoQuality(preset: VideoQualityPreset) {
        context.settingsDataStore.edit { it[Keys.VIDEO_QUALITY] = preset.name }
    }

    suspend fun setSubtitleFontSize(sizeSp: Float) {
        context.settingsDataStore.edit { it[Keys.SUBTITLE_FONT_SIZE] = sizeSp.coerceIn(12f, 32f) }
    }

    suspend fun setSubtitleColor(color: SubtitleColorOption) {
        context.settingsDataStore.edit { it[Keys.SUBTITLE_COLOR] = color.name }
    }

    suspend fun setSubtitleBgOpacity(opacity: Float) {
        context.settingsDataStore.edit { it[Keys.SUBTITLE_BG_OPACITY] = opacity.coerceIn(0f, 1f) }
    }

    suspend fun setParentalLockEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.PARENTAL_LOCK] = enabled }
    }

    suspend fun setShowBitrateOverlay(show: Boolean) {
        context.settingsDataStore.edit { it[Keys.SHOW_BITRATE] = show }
    }

    suspend fun setAutoNextEpisode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTO_NEXT_EPISODE] = enabled }
    }

    suspend fun setScreenshotBlocking(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SCREENSHOT_BLOCKING] = enabled }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setUkLivePlaylistsOnly(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.UK_LIVE_PLAYLISTS_ONLY] = enabled }
    }

    suspend fun setEnglishVodOnly(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.ENGLISH_VOD_ONLY] = enabled }
    }

    suspend fun setLiveTvUiScalePercent(percent: Int) {
        context.settingsDataStore.edit { it[Keys.LIVE_TV_UI_SCALE_PERCENT] = percent.coerceIn(0, 100) }
    }

    suspend fun setGithubUpdateRepo(repo: String) {
        val cleaned = repo.trim().ifBlank { GitHubAppUpdater.DEFAULT_GITHUB_REPO }
        context.settingsDataStore.edit { it[Keys.GITHUB_UPDATE_REPO] = cleaned }
    }

    suspend fun setPlaylistSelection(
        sourceId: String,
        section: PlaylistSection,
        allCategoryIds: Set<String>,
        selectedIds: Set<String>
    ) {
        if (sourceId.isBlank()) return
        context.settingsDataStore.edit { prefs ->
            val currentMap = prefs[Keys.PLAYLIST_SELECTIONS_JSON]?.takeIf { it.isNotBlank() }?.let { raw ->
                runCatching { json.decodeFromString<Map<String, PlaylistSelectionConfig>>(raw) }.getOrDefault(emptyMap())
            }?.toMutableMap() ?: mutableMapOf()

            val key = playlistSelectionKey(sourceId, section)
            val normalizedSelected = selectedIds.intersect(allCategoryIds)
            if (allCategoryIds.isNotEmpty() && normalizedSelected.size == allCategoryIds.size) {
                currentMap.remove(key)
            } else {
                currentMap[key] = PlaylistSelectionConfig(
                    isCustomActive = true,
                    selectedCategoryIds = normalizedSelected
                )
            }

            if (currentMap.isEmpty()) {
                prefs.remove(Keys.PLAYLIST_SELECTIONS_JSON)
            } else {
                prefs[Keys.PLAYLIST_SELECTIONS_JSON] = json.encodeToString(currentMap)
            }
        }
    }
}
