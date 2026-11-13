package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalAppStrings
import com.example.core.security.ImportedSourceDraft
import com.example.core.security.JsonParseResult
import com.example.core.security.JsonSourceImporter
import com.example.core.security.XtreamSourceProfile
import com.example.core.update.AppUpdateUiState
import com.example.core.update.GitHubAppUpdater
import com.example.data.local.SubtitleColorOption
import com.example.data.local.UserSettings
import com.example.data.local.VideoQualityPreset
import com.example.ui.components.ImportCredentialsWarningDialog
import com.example.ui.components.ImportProgressDialog
import com.example.ui.components.ImportSourcesPreviewDialog
import com.example.ui.components.LegalDisclaimerBanner
import com.example.ui.components.NextVuButton
import com.example.ui.components.NextVuFilterChip
import com.example.ui.components.NextVuOutlinedButton
import com.example.ui.components.NextVuTextButton
import com.example.ui.components.NextVuTopBar
import com.example.ui.components.rememberHoverFocusState
import com.example.ui.components.thickBlackHoverOutline
import com.example.ui.components.trackHoverFocus
import com.example.ui.theme.BgCharcoal
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.HdrGold
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.SourceImportSummary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    activeSource: XtreamSourceProfile?,
    isOfflineMode: Boolean,
    settings: UserSettings,
    hasParentalPin: Boolean,
    updateCheckState: AppUpdateUiState,
    onOpenQuickSwitch: () -> Unit,
    onSelectVideoQuality: (VideoQualityPreset) -> Unit,
    onSetSubtitleSize: (Float) -> Unit,
    onSetSubtitleColor: (SubtitleColorOption) -> Unit,
    onSetSubtitleBgOpacity: (Float) -> Unit,
    onSetShowBitrate: (Boolean) -> Unit,
    onSetAutoNextEpisode: (Boolean) -> Unit,
    onSetScreenshotBlocking: (Boolean) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetUkLivePlaylistsOnly: (Boolean) -> Unit,
    onSetEnglishVodOnly: (Boolean) -> Unit,
    onSetLiveTvUiScale: (Int) -> Unit,
    onSetGithubUpdateRepo: (String) -> Unit,
    onCheckForAppUpdate: (String?) -> Unit,
    onEnableParentalPin: (String) -> Unit,
    onVerifyAndDisableParentalPin: (String) -> Boolean,
    onClearActiveSourceHistory: () -> Unit,
    onClearAllSourcesHistory: () -> Unit,
    onResetAllSources: () -> Unit,
    onCheckDuplicateSource: (ImportedSourceDraft) -> Boolean,
    onImportSourcesBatch: suspend (List<ImportedSourceDraft>, (Int, Int, String) -> Unit) -> SourceImportSummary
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinDialogIsDisabling by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showResetSourcesDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showGithubRepoDialog by remember { mutableStateOf(false) }

    var parsedSourcesForPreview by remember { mutableStateOf<List<ImportedSourceDraft>?>(null) }
    var pendingSourcesAfterWarning by remember { mutableStateOf<List<ImportedSourceDraft>?>(null) }
    var importProgress by remember { mutableStateOf<Triple<Int, Int, String>?>(null) }

    val jsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        when (val result = JsonSourceImporter.readFromUri(context, uri)) {
            is JsonParseResult.Success -> {
                parsedSourcesForPreview = result.sources
            }
            is JsonParseResult.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCharcoal)
    ) {
        NextVuTopBar(
            title = strings.settingsTitle,
            activeSource = activeSource,
            isOfflineMode = isOfflineMode,
            onActiveSourceClick = onOpenQuickSwitch
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_list")
        ) {
            // Video Quality Presets
            item {
                SettingsCard(icon = Icons.Rounded.Tune, title = strings.videoQualityPreset) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VideoQualityPreset.entries.forEach { preset ->
                            NextVuFilterChip(
                                selected = settings.videoQuality == preset,
                                onClick = { onSelectVideoQuality(preset) },
                                label = { Text(preset.label, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricCyan
                                ),
                                modifier = Modifier.testTag("quality_preset_${preset.name}")
                            )
                        }
                    }
                }
            }

            // Region & Language Content Filters (UK Playlists, English / ENG Movies & Series)
            item {
                SettingsCard(icon = Icons.Rounded.FilterList, title = strings.regionContentFilters) {
                    SettingsToggleRow(
                        label = strings.ukPlaylistsOnly,
                        subtitle = strings.ukPlaylistsOnlyDesc,
                        checked = settings.ukLivePlaylistsOnly,
                        onCheckedChange = onSetUkLivePlaylistsOnly,
                        testTag = "settings_uk_playlists_toggle"
                    )
                    SettingsToggleRow(
                        label = strings.englishVodOnly,
                        subtitle = strings.englishVodOnlyDesc,
                        checked = settings.englishVodOnly,
                        onCheckedChange = onSetEnglishVodOnly,
                        testTag = "settings_english_vod_toggle"
                    )
                }
            }

            // Live TV UI Scale (0% - 100%)
            item {
                SettingsCard(icon = Icons.Rounded.Tune, title = "Live TV UI Scale") {
                    Text(
                        text = "Scale: ${settings.liveTvUiScalePercent}% (0% = Compact / More Rows, 100% = Large)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Slider(
                        value = settings.liveTvUiScalePercent.toFloat(),
                        onValueChange = { onSetLiveTvUiScale(it.toInt().coerceIn(0, 100)) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan),
                        modifier = Modifier.testTag("settings_live_ui_scale_slider")
                    )
                }
            }

            // Subtitle Customization
            item {
                SettingsCard(icon = Icons.Rounded.Subtitles, title = strings.subtitleCustomization) {
                    Text(
                        text = "${strings.subtitleFontSize}: ${settings.subtitleFontSizeSp.toInt()} sp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Slider(
                        value = settings.subtitleFontSizeSp,
                        onValueChange = onSetSubtitleSize,
                        valueRange = 12f..30f,
                        colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                    )

                    Text(
                        text = strings.subtitleColor,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SubtitleColorOption.entries.forEach { opt ->
                            NextVuFilterChip(
                                selected = settings.subtitleColor == opt,
                                onClick = { onSetSubtitleColor(opt) },
                                label = { Text(opt.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${strings.subtitleBgOpacity}: ${(settings.subtitleBgOpacity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Slider(
                        value = settings.subtitleBgOpacity,
                        onValueChange = onSetSubtitleBgOpacity,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                    )

                    // Live Subtitle Preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .background(SurfaceCardVariant, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sample Subtitle Preview",
                            fontSize = settings.subtitleFontSizeSp.sp,
                            color = Color(settings.subtitleColor.argb),
                            modifier = Modifier
                                .background(
                                    Color.Black.copy(alpha = settings.subtitleBgOpacity),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Playback, Parental Lock & TV Security Toggles
            item {
                SettingsCard(icon = Icons.Rounded.Security, title = "Playback & Privacy Controls") {
                    SettingsToggleRow(
                        label = strings.bitrateOverlay,
                        checked = settings.showBitrateOverlay,
                        onCheckedChange = onSetShowBitrate,
                        testTag = "settings_bitrate_toggle"
                    )
                    SettingsToggleRow(
                        label = strings.autoNextEpisode,
                        checked = settings.autoNextEpisode,
                        onCheckedChange = onSetAutoNextEpisode,
                        testTag = "settings_auto_next_toggle"
                    )
                    SettingsToggleRow(
                        label = strings.screenshotBlocking,
                        checked = settings.screenshotBlockingEnabled,
                        onCheckedChange = onSetScreenshotBlocking,
                        testTag = "settings_screenshot_block_toggle"
                    )
                    SettingsToggleRow(
                        label = strings.parentalLock,
                        checked = settings.parentalLockEnabled,
                        onCheckedChange = { enable ->
                            pinDialogIsDisabling = !enable && hasParentalPin
                            showPinDialog = true
                        },
                        testTag = "settings_parental_lock_toggle"
                    )
                }
            }

            // Multi-Language UI Support
            item {
                SettingsCard(icon = Icons.Rounded.Language, title = strings.languageLabel) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            NextVuFilterChip(
                                selected = settings.language == lang,
                                onClick = { onSetLanguage(lang) },
                                label = { Text(lang.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricCyan
                                ),
                                modifier = Modifier.testTag("language_chip_${lang.code}")
                            )
                        }
                    }
                }
            }

            // Data & Source Management Actions
            item {
                SettingsCard(icon = Icons.Rounded.DeleteSweep, title = "History & Source Data") {
                    SettingsActionButton(
                        icon = Icons.Rounded.FileUpload,
                        label = "Import Sources (JSON File)",
                        tint = ElectricCyan,
                        borderColor = ElectricCyan.copy(alpha = 0.55f),
                        onClick = {
                            try {
                                jsonPickerLauncher.launch(arrayOf("application/json", "text/json"))
                            } catch (_: SecurityException) {
                                Toast.makeText(
                                    context,
                                    "Permission denied: Unable to open device storage picker.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(
                                    context,
                                    "No file picker application is available on this device.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context,
                                    "Failed to launch JSON file picker.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        modifier = Modifier.testTag("settings_import_sources_json_button")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsActionButton(
                        icon = Icons.Rounded.DeleteSweep,
                        label = strings.clearHistoryProgress,
                        tint = ElectricCyan,
                        borderColor = ElectricCyan.copy(alpha = 0.55f),
                        onClick = { showClearHistoryDialog = true },
                        modifier = Modifier.testTag("settings_clear_history_button")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsActionButton(
                        icon = Icons.Rounded.DeleteForever,
                        label = strings.resetAllSources,
                        tint = ErrorRose,
                        labelColor = ErrorRose,
                        borderColor = ErrorRose.copy(alpha = 0.65f),
                        onClick = { showResetSourcesDialog = true },
                        modifier = Modifier.testTag("settings_reset_sources_button")
                    )
                }
            }

            // Application Updater (GitHub Sync)
            item {
                SettingsCard(icon = Icons.Rounded.SystemUpdate, title = "App Updates (GitHub Sync)") {
                    Text(
                        text = "Installed: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.titleSmall,
                        color = ElectricCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val effectiveRepo = settings.githubUpdateRepo.ifBlank { GitHubAppUpdater.DEFAULT_GITHUB_REPO }
                    Text(
                        text = "GitHub Repo: $effectiveRepo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NextVuOutlinedButton(
                            onClick = { showGithubRepoDialog = true },
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_set_github_repo_btn")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Repo")
                        }

                        NextVuButton(
                            onClick = { onCheckForAppUpdate(effectiveRepo) },
                            enabled = updateCheckState !is AppUpdateUiState.Checking,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_check_updates_btn")
                        ) {
                            if (updateCheckState is AppUpdateUiState.Checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = BgCharcoal
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Checking...")
                            } else {
                                Icon(Icons.Rounded.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check Updates", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    when (updateCheckState) {
                        AppUpdateUiState.Idle -> Unit
                        is AppUpdateUiState.Checking -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Checking ${updateCheckState.repoSlug} on GitHub...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        is AppUpdateUiState.UpdateAvailable -> {
                            val info = updateCheckState.info
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = ElectricCyan.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_update_available_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val buildSuffix = info.latestVersionCode?.let { " (Build $it)" } ?: ""
                                    Text(
                                        text = "New Version Available: ${info.latestVersionName}$buildSuffix",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricCyan
                                    )
                                    Text(
                                        text = "Source: ${info.sourceLabel}" + (info.publishedOrSyncedAt?.let { " • $it" } ?: ""),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HdrGold
                                    )
                                    if (info.summaryText.isNotBlank()) {
                                        Text(
                                            text = info.summaryText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val downloadTarget = info.directApkAssetUrl ?: info.releaseOrBuildUrl
                                    NextVuButton(
                                        onClick = { openExternalUrl(context, downloadTarget) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("settings_open_github_update_btn")
                                    ) {
                                        Icon(Icons.Rounded.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (info.directApkAssetUrl != null) {
                                                "Download Latest APK (GitHub)"
                                            } else {
                                                "Open Latest Build on GitHub"
                                            },
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (info.directApkAssetUrl != null && info.releaseOrBuildUrl != info.directApkAssetUrl) {
                                        NextVuTextButton(
                                            onClick = { openExternalUrl(context, info.releaseOrBuildUrl) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("View Release Notes on GitHub", color = ElectricCyan)
                                        }
                                    }
                                }
                            }
                        }

                        is AppUpdateUiState.UpToDate -> {
                            val info = updateCheckState.info
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = SuccessGreen.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_update_uptodate_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val buildSuffix = info.latestVersionCode?.let { " (Build $it)" } ?: ""
                                        Text(
                                            text = "Up to date • ${info.latestVersionName}$buildSuffix",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SuccessGreen
                                        )
                                    }
                                    if (info.summaryText.isNotBlank()) {
                                        Text(
                                            text = info.summaryText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    NextVuTextButton(
                                        onClick = { openExternalUrl(context, info.releaseOrBuildUrl) }
                                    ) {
                                        Icon(Icons.Rounded.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open GitHub Repo / Builds", color = ElectricCyan)
                                    }
                                }
                            }
                        }

                        is AppUpdateUiState.Error -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = updateCheckState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = ErrorRose,
                                modifier = Modifier.testTag("settings_update_error_text")
                            )
                        }
                    }
                }
            }

            // About & Legal Disclaimer
            item {
                SettingsCard(icon = Icons.Rounded.Info, title = strings.aboutLegalTitle) {
                    Text(
                        text = "${strings.appName} • v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.legalDisclaimerFull,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    NextVuTextButton(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.testTag("settings_about_dialog_btn")
                    ) {
                        Text("View Full Legal & Codec Info", color = ElectricCyan)
                    }
                }
            }

            item {
                LegalDisclaimerBanner(
                    text = strings.legalDisclaimerShort,
                    modifier = Modifier.testTag("settings_legal_disclaimer")
                )
            }
        }
    }

    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            containerColor = SurfaceCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = ElectricCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (pinDialogIsDisabling) "Verify PIN to Disable" else "Set 4-Digit Parental PIN",
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pinInput = it },
                        label = { Text("4-Digit PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(pinError!!, color = ErrorRose, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                NextVuButton(
                    onClick = {
                        if (pinInput.length != 4) {
                            pinError = "Enter a 4-digit PIN."
                        } else if (pinDialogIsDisabling) {
                            if (onVerifyAndDisableParentalPin(pinInput)) {
                                showPinDialog = false
                            } else {
                                pinError = "Incorrect PIN."
                            }
                        } else {
                            onEnableParentalPin(pinInput)
                            showPinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = BgCharcoal)
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                NextVuTextButton(onClick = { showPinDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            containerColor = SurfaceCard,
            title = { Text(strings.clearHistoryProgress, color = TextPrimary) },
            text = {
                Text(
                    "Choose whether to clear watch history & playback progress for the active source (${activeSource?.sourceName ?: "None"}) or across all saved sources.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NextVuButton(
                        onClick = {
                            onClearActiveSourceHistory()
                            showClearHistoryDialog = false
                        },
                        enabled = activeSource != null,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = BgCharcoal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.clearCurrentSource)
                    }
                    NextVuButton(
                        onClick = {
                            onClearAllSourcesHistory()
                            showClearHistoryDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepViolet, contentColor = TextPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.clearAllSourcesData)
                    }
                }
            },
            dismissButton = {
                NextVuTextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }

    if (showResetSourcesDialog) {
        AlertDialog(
            onDismissRequest = { showResetSourcesDialog = false },
            containerColor = SurfaceCard,
            title = { Text(strings.resetAllSources, color = ErrorRose) },
            text = {
                Text(
                    "This will permanently delete all encrypted Xtream source credentials and all per-source watch history, progress, and watchlist data.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                NextVuButton(
                    onClick = {
                        onResetAllSources()
                        showResetSourcesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRose, contentColor = TextPrimary),
                    modifier = Modifier.testTag("confirm_reset_all_sources_button")
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                NextVuTextButton(onClick = { showResetSourcesDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }

    if (showGithubRepoDialog) {
        var repoInput by remember(settings.githubUpdateRepo) {
            mutableStateOf(settings.githubUpdateRepo.ifBlank { GitHubAppUpdater.DEFAULT_GITHUB_REPO })
        }
        var repoError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showGithubRepoDialog = false },
            containerColor = SurfaceCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = ElectricCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GitHub Update Repository", color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter your synced GitHub repository (owner/repo or full GitHub URL) to check Releases and synced main/master builds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = repoInput,
                        onValueChange = {
                            repoInput = it
                            repoError = null
                        },
                        label = { Text("GitHub Repository") },
                        placeholder = { Text("e.g. APPBuilder28/App-Testing") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_github_repo_input")
                    )
                    if (repoError != null) {
                        Text(repoError!!, color = ErrorRose, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                NextVuButton(
                    onClick = {
                        val slug = GitHubAppUpdater.normalizeRepoSlug(repoInput)
                        if (slug == null) {
                            repoError = "Enter a valid owner/repo or https://github.com/owner/repo URL."
                        } else {
                            onSetGithubUpdateRepo(slug)
                            showGithubRepoDialog = false
                            onCheckForAppUpdate(slug)
                        }
                    },
                    modifier = Modifier.testTag("settings_save_github_repo_btn")
                ) {
                    Text("Save & Check")
                }
            },
            dismissButton = {
                NextVuTextButton(onClick = { showGithubRepoDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = SurfaceCard,
            title = { Text(strings.aboutLegalTitle, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "NextVu Player v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                    Text(strings.legalDisclaimerFull, color = TextSecondary)
                    Text(
                        "Engine: Jetpack Media3 ExoPlayer (HEVC H.265 hardware + software fallback, 4K 2160p UHD, HDR10/HLG/DV detection, AC3/E-AC3 passthrough & PCM fallback).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            },
            confirmButton = {
                NextVuButton(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = BgCharcoal)
                ) {
                    Text("OK")
                }
            }
        )
    }

    parsedSourcesForPreview?.let { parsed ->
        ImportSourcesPreviewDialog(
            sources = parsed,
            isDuplicateCheck = onCheckDuplicateSource,
            cancelLabel = strings.cancel,
            onConfirmSelection = { chosen ->
                parsedSourcesForPreview = null
                pendingSourcesAfterWarning = chosen
            },
            onDismiss = { parsedSourcesForPreview = null }
        )
    }

    pendingSourcesAfterWarning?.let { chosen ->
        ImportCredentialsWarningDialog(
            selectedCount = chosen.size,
            cancelLabel = strings.cancel,
            onConfirmImport = {
                pendingSourcesAfterWarning = null
                coroutineScope.launch {
                    importProgress = Triple(1, chosen.size, chosen.firstOrNull()?.sourceName.orEmpty())
                    val summary = onImportSourcesBatch(chosen) { current, total, name ->
                        importProgress = Triple(current, total, name)
                    }
                    importProgress = null

                    val feedbackParts = mutableListOf<String>()
                    if (summary.importedCount > 0) {
                        feedbackParts.add(
                            "Imported ${summary.importedCount} source(s). Go to Sources Manager to Set Active Source."
                        )
                    }
                    if (summary.skippedDuplicatesCount > 0) {
                        feedbackParts.add("Skipped ${summary.skippedDuplicatesCount} duplicate source(s).")
                    }
                    if (summary.failedConnectionCount > 0) {
                        feedbackParts.add("${summary.failedConnectionCount} source(s) failed connection test.")
                    }
                    val message = if (feedbackParts.isEmpty()) {
                        "No sources imported."
                    } else {
                        feedbackParts.joinToString(" ")
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = { pendingSourcesAfterWarning = null }
        )
    }

    importProgress?.let { (current, total, name) ->
        ImportProgressDialog(
            current = current,
            total = total,
            currentSourceName = name
        )
    }
}

@Composable
private fun SettingsActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelColor: Color = TextPrimary
) {
    val hoverState = rememberHoverFocusState()
    val highlighted = hoverState.isHighlighted
    val btnShape = RoundedCornerShape(999.dp)

    Surface(
        onClick = onClick,
        shape = btnShape,
        color = if (highlighted) SurfaceCardVariant else Color.Transparent,
        border = if (highlighted) null else BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .trackHoverFocus(hoverState)
            .thickBlackHoverOutline(
                highlighted = highlighted,
                shape = btnShape,
                contrastHaloColor = ElectricCyan.copy(alpha = 0.9f)
            )
            .minimumInteractiveComponentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) ElectricCyan else tint
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Medium,
                color = if (highlighted && labelColor == TextPrimary) Color.White else labelColor
            )
        }
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SurfaceBorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BgCharcoal,
                checkedTrackColor = ElectricCyan
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

private fun openExternalUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No browser app found to open GitHub link.", Toast.LENGTH_LONG).show()
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to open link: $url", Toast.LENGTH_LONG).show()
    }
}
