package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "nextvu_settings")

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
    val englishVodOnly: Boolean = false
)

class SettingsRepository(private val context: Context) {

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
    }

    val settingsFlow: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
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
            englishVodOnly = prefs[Keys.ENGLISH_VOD_ONLY] ?: false
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
}
