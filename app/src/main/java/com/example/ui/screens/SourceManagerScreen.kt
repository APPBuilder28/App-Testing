package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.core.i18n.LocalAppStrings
import com.example.core.security.XtreamSourceProfile
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LegalDisclaimerBanner
import com.example.ui.components.NextVuButton
import com.example.ui.components.NextVuFloatingActionButton
import com.example.ui.components.NextVuIconButton
import com.example.ui.components.NextVuInteractiveCard
import com.example.ui.components.NextVuOutlinedButton
import com.example.ui.components.NextVuTextButton
import com.example.ui.components.NextVuTopBar
import com.example.ui.theme.BgCharcoal
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.TestConnectionUiState
import kotlin.math.roundToInt

@Composable
fun SourceManagerScreen(
    sources: List<XtreamSourceProfile>,
    activeSource: XtreamSourceProfile?,
    isOfflineMode: Boolean,
    isReloadingPlaylists: Boolean,
    testConnectionState: TestConnectionUiState,
    openAddDialogInitially: Boolean = false,
    onOpenQuickSwitch: () -> Unit,
    onReloadPlaylists: () -> Unit,
    onTestConnection: (serverUrl: String, username: String, password: String) -> Unit,
    onResetTestState: () -> Unit,
    onSaveSource: (
        existingId: String?,
        sourceName: String,
        serverUrl: String,
        username: String,
        password: String,
        makeActive: Boolean,
        onSaved: () -> Unit
    ) -> Unit,
    onSwitchSource: (String) -> Unit,
    onRenameSource: (String, String) -> Unit,
    onDeleteSource: (String) -> Unit,
    onSourceReadyNavigateHome: () -> Unit
) {
    val strings = LocalAppStrings.current
    var showAddEditDialog by remember { mutableStateOf(openAddDialogInitially) }
    var editingSource by remember { mutableStateOf<XtreamSourceProfile?>(null) }
    var confirmSwitchTarget by remember { mutableStateOf<XtreamSourceProfile?>(null) }
    var contextMenuTarget by remember { mutableStateOf<XtreamSourceProfile?>(null) }
    var renameTarget by remember { mutableStateOf<XtreamSourceProfile?>(null) }
    var deleteTarget by remember { mutableStateOf<XtreamSourceProfile?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCharcoal)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NextVuTopBar(
                title = strings.sourceManagerTitle,
                activeSource = activeSource,
                isOfflineMode = isOfflineMode,
                onActiveSourceClick = onOpenQuickSwitch
            )

            if (activeSource != null) {
                Surface(
                    color = SurfaceCard,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SurfaceBorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active: ${activeSource.sourceName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Refresh live channels, movies, series & stream links",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        NextVuButton(
                            onClick = onReloadPlaylists,
                            enabled = !isReloadingPlaylists,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = BgCharcoal,
                                disabledContainerColor = SurfaceCardVariant,
                                disabledContentColor = TextMuted
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("reload_playlists_button")
                        ) {
                            if (isReloadingPlaylists) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = ElectricCyan
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reloading...", fontWeight = FontWeight.SemiBold)
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Reload Playlist",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reload Playlist", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (sources.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Rounded.Dns,
                        title = strings.noSourceTitle,
                        subtitle = strings.noSourceSubtitle,
                        actionLabel = strings.addNewSource,
                        onAction = {
                            editingSource = null
                            onResetTestState()
                            showAddEditDialog = true
                        }
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sources, key = { it.id }) { source ->
                            SourceProfileCard(
                                source = source,
                                isActive = source.id == activeSource?.id,
                                onClick = {
                                    if (source.id != activeSource?.id) {
                                        confirmSwitchTarget = source
                                    }
                                },
                                onLongClick = { contextMenuTarget = source },
                                onMenuClick = { contextMenuTarget = source }
                            )
                        }
                    }
                }

                NextVuFloatingActionButton(
                    onClick = {
                        editingSource = null
                        onResetTestState()
                        showAddEditDialog = true
                    },
                    containerColor = ElectricCyan,
                    contentColor = BgCharcoal,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .testTag("add_source_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = strings.addNewSource)
                }
            }

            LegalDisclaimerBanner(
                text = strings.legalDisclaimerShort,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("source_manager_legal_disclaimer")
            )
        }
    }

    // Switch Active Source Confirmation Popup
    confirmSwitchTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmSwitchTarget = null },
            containerColor = SurfaceCard,
            title = { Text(strings.setActiveSourceTitle, color = TextPrimary) },
            text = {
                Text(
                    "${target.sourceName}\n${target.truncatedUrl()}\n\n${strings.setActiveSourceConfirm}",
                    color = TextSecondary
                )
            },
            confirmButton = {
                NextVuButton(
                    onClick = {
                        onSwitchSource(target.id)
                        confirmSwitchTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = BgCharcoal),
                    modifier = Modifier.testTag("confirm_switch_source_btn")
                ) {
                    Text(strings.switchSource)
                }
            },
            dismissButton = {
                NextVuTextButton(onClick = { confirmSwitchTarget = null }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }

    // Long-press / Context Options Dialog (TV D-Pad + Touch friendly)
    contextMenuTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { contextMenuTarget = null },
            containerColor = SurfaceCard,
            title = { Text(target.sourceName, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (target.id == activeSource?.id) {
                        NextVuOutlinedButton(
                            onClick = {
                                contextMenuTarget = null
                                onReloadPlaylists()
                            },
                            enabled = !isReloadingPlaylists,
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("action_reload_playlists")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = ElectricCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reload Playlist & Links", color = ElectricCyan)
                        }
                    }

                    NextVuOutlinedButton(
                        onClick = {
                            contextMenuTarget = null
                            renameTarget = target
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("action_rename_source")
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.renameSource, color = TextPrimary)
                    }

                    NextVuOutlinedButton(
                        onClick = {
                            contextMenuTarget = null
                            editingSource = target
                            onResetTestState()
                            showAddEditDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("action_edit_source")
                    ) {
                        Icon(Icons.Rounded.Dns, contentDescription = null, tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.editCredentials, color = TextPrimary)
                    }

                    NextVuOutlinedButton(
                        onClick = {
                            contextMenuTarget = null
                            deleteTarget = target
                        },
                        border = BorderStroke(1.dp, ErrorRose.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("action_delete_source")
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = ErrorRose)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.deleteSource, color = ErrorRose)
                    }
                }
            },
            confirmButton = {
                NextVuTextButton(onClick = { contextMenuTarget = null }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }

    // Rename Source Dialog
    renameTarget?.let { target ->
        var newName by remember(target.id) { mutableStateOf(target.sourceName) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = SurfaceCard,
            title = { Text(strings.renameSource, color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(strings.sourceNameLabel) },
                    singleLine = true,
                    colors = nextVuTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                NextVuButton(
                    onClick = {
                        onRenameSource(target.id, newName)
                        renameTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = BgCharcoal)
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                NextVuTextButton(onClick = { renameTarget = null }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = SurfaceCard,
            title = { Text(strings.deleteSource, color = ErrorRose) },
            text = {
                Text(
                    "Remove '${target.sourceName}' and its local watch history, progress, and watchlist?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                NextVuButton(
                    onClick = {
                        onDeleteSource(target.id)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRose, contentColor = TextPrimary)
                ) {
                    Text(strings.deleteSource)
                }
            },
            dismissButton = {
                NextVuTextButton(onClick = { deleteTarget = null }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }

    if (showAddEditDialog) {
        AddOrEditSourceDialog(
            existing = editingSource,
            testConnectionState = testConnectionState,
            onTestConnection = onTestConnection,
            onSave = { name, url, user, pass, makeActive ->
                val wasFirstSource = sources.isEmpty()
                onSaveSource(editingSource?.id, name, url, user, pass, makeActive) {
                    showAddEditDialog = false
                    editingSource = null
                    if (wasFirstSource) {
                        onSourceReadyNavigateHome()
                    }
                }
            },
            onDismiss = {
                showAddEditDialog = false
                editingSource = null
                onResetTestState()
            }
        )
    }
}

@Composable
private fun SourceProfileCard(
    source: XtreamSourceProfile,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    NextVuInteractiveCard(
        isActive = isActive,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("source_card_${source.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isActive) ElectricCyan.copy(alpha = 0.16f) else SurfaceCardVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Dns,
                        contentDescription = source.sourceName,
                        tint = if (isActive) ElectricCyan else TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = source.sourceName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (isActive) ElectricCyan.copy(alpha = 0.18f) else SurfaceCardVariant,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isActive) ElectricCyan.copy(alpha = 0.6f) else SurfaceBorderSubtle
                            )
                        ) {
                            Text(
                                text = if (isActive) strings.activeBadge else strings.inactiveBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isActive) ElectricCyan else TextMuted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = source.truncatedUrl(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!source.expDate.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "User: ${source.username} • Exp: ${source.expDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            NextVuIconButton(
                onClick = onMenuClick,
                modifier = Modifier.testTag("source_menu_${source.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Source options",
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun AddOrEditSourceDialog(
    existing: XtreamSourceProfile?,
    testConnectionState: TestConnectionUiState,
    onTestConnection: (String, String, String) -> Unit,
    onSave: (String, String, String, String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var sourceName by remember { mutableStateOf(existing?.sourceName ?: "") }
    var serverUrl by remember { mutableStateOf(existing?.serverUrl ?: "") }
    var username by remember { mutableStateOf(existing?.username ?: "") }
    var password by remember { mutableStateOf(existing?.password ?: "") }
    var showPassword by remember { mutableStateOf(false) }
    var makeActive by remember { mutableStateOf(existing?.isActive ?: true) }

    // Horizontal shake animation on Test Connection error
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(testConnectionState) {
        if (testConnectionState is TestConnectionUiState.Error) {
            val deltas = listOf(-18f, 18f, -14f, 14f, -8f, 8f, 0f)
            deltas.forEach { target ->
                shakeOffset.animateTo(target, animationSpec = tween(durationMillis = 45))
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f)),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (existing == null) strings.addNewSource else strings.editSource,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = sourceName,
                    onValueChange = { sourceName = it },
                    label = { Text(strings.sourceNameLabel) },
                    placeholder = { Text("Home Provider") },
                    singleLine = true,
                    colors = nextVuTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("source_name_input")
                )

                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text(strings.serverUrlLabel) },
                    placeholder = { Text("http://example.com:8080") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = nextVuTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("server_url_input")
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(strings.usernameLabel) },
                    singleLine = true,
                    colors = nextVuTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(strings.passwordLabel) },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        NextVuIconButton(
                            onClick = { showPassword = !showPassword },
                            modifier = Modifier.testTag("password_visibility_toggle")
                        ) {
                            Icon(
                                imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password",
                                tint = ElectricCyan
                            )
                        }
                    },
                    colors = nextVuTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Set as Active Source",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    Switch(
                        checked = makeActive,
                        onCheckedChange = { makeActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BgCharcoal,
                            checkedTrackColor = ElectricCyan
                        )
                    )
                }

                // Validation Result Feedback (Green Check Animation OR Error Banner)
                AnimatedVisibility(
                    visible = testConnectionState is TestConnectionUiState.Success,
                    enter = fadeIn() + scaleIn(animationSpec = spring(dampingRatio = 0.55f))
                ) {
                    val info = (testConnectionState as? TestConnectionUiState.Success)?.result
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_connection_success")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Connection verified",
                                tint = SuccessGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Connected • Status: ${info?.status ?: "Active"}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SuccessGreen
                                )
                                if (!info?.expDate.isNullOrBlank()) {
                                    Text(
                                        text = "Expires: ${info?.expDate} • Max Conn: ${info?.maxConnections ?: "-"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                if (testConnectionState is TestConnectionUiState.Error) {
                    Surface(
                        color = ErrorRose.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ErrorRose.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_connection_error")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = "Connection failed",
                                tint = ErrorRose
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = testConnectionState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ErrorRose
                            )
                        }
                    }
                }

                val fieldsFilled = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
                val isTesting = testConnectionState is TestConnectionUiState.Testing

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NextVuOutlinedButton(
                        onClick = { onTestConnection(serverUrl, username, password) },
                        enabled = fieldsFilled && !isTesting,
                        border = BorderStroke(1.dp, DeepViolet),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("test_connection_button")
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = ElectricCyan
                            )
                        } else {
                            Text(strings.testConnection, color = TextPrimary, maxLines = 1)
                        }
                    }

                    NextVuButton(
                        onClick = {
                            onSave(
                                sourceName.ifBlank { "My Xtream" },
                                serverUrl,
                                username,
                                password,
                                makeActive
                            )
                        },
                        enabled = fieldsFilled && !isTesting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = BgCharcoal
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("save_source_button")
                    ) {
                        Text(strings.saveSource, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }

                LegalDisclaimerBanner(text = strings.legalDisclaimerShort)

                NextVuTextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun nextVuTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ElectricCyan,
    unfocusedBorderColor = SurfaceBorderSubtle,
    focusedLabelColor = ElectricCyan,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = ElectricCyan
)
