package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.security.ImportedSourceDraft
import com.example.ui.theme.BgCharcoal
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HdrGold
import com.example.ui.theme.SurfaceBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun ImportSourcesPreviewDialog(
    sources: List<ImportedSourceDraft>,
    isDuplicateCheck: (ImportedSourceDraft) -> Boolean,
    cancelLabel: String,
    onConfirmSelection: (List<ImportedSourceDraft>) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedStates = remember(sources) {
        mutableStateListOf<Boolean>().apply {
            sources.forEach { add(true) }
        }
    }
    val selectedCount = selectedStates.count { it }
    val firstRowFocusRequester = remember { FocusRequester() }

    LaunchedEffect(sources) {
        if (sources.isNotEmpty()) {
            withFrameNanos { }
            try {
                firstRowFocusRequester.requestFocus()
            } catch (_: Throwable) {
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.FileUpload, contentDescription = null, tint = ElectricCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Preview Sources (${sources.size})",
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select sources to test & import:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val selectAllHover = rememberHoverFocusState()
                        Surface(
                            onClick = {
                                val allSelected = selectedCount == sources.size
                                for (i in selectedStates.indices) {
                                    selectedStates[i] = !allSelected
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectAllHover.isHighlighted) ElectricCyan.copy(alpha = 0.24f) else SurfaceCardVariant,
                            border = BorderStroke(
                                if (selectAllHover.isHighlighted) 2.dp else 1.dp,
                                if (selectAllHover.isHighlighted) ElectricCyan else SurfaceBorderSubtle
                            ),
                            modifier = Modifier
                                .trackHoverFocus(selectAllHover)
                                .thickBlackHoverOutline(
                                    highlighted = selectAllHover.isHighlighted,
                                    shape = RoundedCornerShape(8.dp),
                                    blackWidth = 3.dp,
                                    contrastHaloColor = ElectricCyan.copy(alpha = 0.9f),
                                    haloPadding = 1.2.dp
                                )
                                .minimumInteractiveComponentSize()
                        ) {
                            Text(
                                text = if (selectedCount == sources.size) "Uncheck All" else "Check All",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    itemsIndexed(sources) { index, item ->
                        val checked = selectedStates.getOrElse(index) { true }
                        val isDuplicate = remember(item) { isDuplicateCheck(item) }
                        var rowFocused by remember { mutableStateOf(false) }

                        Surface(
                            onClick = {
                                if (index in selectedStates.indices) {
                                    selectedStates[index] = !checked
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                rowFocused -> ElectricCyan.copy(alpha = 0.20f)
                                checked -> SurfaceCardVariant
                                else -> BgCharcoal.copy(alpha = 0.6f)
                            },
                            border = BorderStroke(
                                width = if (rowFocused) 2.dp else 1.dp,
                                color = when {
                                    rowFocused -> ElectricCyan
                                    checked -> ElectricCyan.copy(alpha = 0.45f)
                                    else -> SurfaceBorderSubtle
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (index == 0) Modifier.focusRequester(firstRowFocusRequester) else Modifier)
                                .onFocusChanged { rowFocused = it.isFocused }
                                .minimumInteractiveComponentSize()
                                .testTag("import_preview_item_$index")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = ElectricCyan,
                                        checkmarkColor = BgCharcoal,
                                        uncheckedColor = TextMuted
                                    ),
                                    modifier = Modifier.focusProperties { canFocus = false }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.sourceName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (isDuplicate) {
                                            Surface(
                                                color = WarningAmber.copy(alpha = 0.16f),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = "Duplicate",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = WarningAmber,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = item.serverUrl.removePrefix("https://").removePrefix("http://").trimEnd('/'),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val expDisplay = item.expiryDate.ifBlank { "N/A" }
                                    Text(
                                        text = "User: ${item.username} • Exp: $expDisplay",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            NextVuButton(
                onClick = {
                    val chosen = sources.filterIndexed { index, _ ->
                        selectedStates.getOrElse(index) { false }
                    }
                    if (chosen.isNotEmpty()) {
                        onConfirmSelection(chosen)
                    }
                },
                enabled = selectedCount > 0,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("import_preview_confirm_btn")
            ) {
                Text("Import ($selectedCount)", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            NextVuTextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(
                    text = cancelLabel,
                    color = TextSecondary
                )
            }
        }
    )
}

@Composable
fun ImportCredentialsWarningDialog(
    selectedCount: Int,
    cancelLabel: String,
    onConfirmImport: () -> Unit,
    onDismiss: () -> Unit
) {
    val confirmFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        try {
            confirmFocusRequester.requestFocus()
        } catch (_: Throwable) {
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = HdrGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Security Warning", color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "WARNING - This JSON file contains plain-text login credentials, handle carefully!",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = WarningAmber
                )
                Text(
                    text = "NextVu will test connectivity for $selectedCount selected source(s), skip duplicates already saved, and encrypt valid sources into local secure storage. You can activate a source afterwards in Sources Manager.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            NextVuButton(
                onClick = onConfirmImport,
                modifier = Modifier
                    .focusRequester(confirmFocusRequester)
                    .minimumInteractiveComponentSize()
                    .testTag("import_warning_confirm_btn")
            ) {
                Text("Proceed & Test", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            NextVuTextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(
                    text = cancelLabel,
                    color = TextSecondary
                )
            }
        }
    )
}

@Composable
fun ImportProgressDialog(
    current: Int,
    total: Int,
    currentSourceName: String
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = SurfaceCard,
        title = { Text("Validating Sources...", color = TextPrimary) },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    color = ElectricCyan,
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Testing connection ($current / $total)",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan
                    )
                    if (currentSourceName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentSourceName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}
