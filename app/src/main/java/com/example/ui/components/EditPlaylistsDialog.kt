package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.XtreamCategory
import com.example.ui.theme.BgCharcoal
import com.example.ui.theme.BgCharcoalElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SurfaceBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun EditPlaylistsChipButton(
    selectedCount: Int,
    totalCount: Int,
    isCustomActive: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val hoverState = rememberHoverFocusState()
    val highlighted = hoverState.isHighlighted
    val chipShape = RoundedCornerShape(10.dp)

    Surface(
        onClick = onClick,
        shape = chipShape,
        color = when {
            highlighted -> ElectricCyan.copy(alpha = 0.28f)
            isCustomActive -> ElectricCyan.copy(alpha = 0.16f)
            else -> SurfaceCard
        },
        border = if (highlighted) null else BorderStroke(
            width = 1.dp,
            color = if (isCustomActive) ElectricCyan.copy(alpha = 0.72f) else SurfaceBorderSubtle
        ),
        modifier = modifier
            .heightIn(min = 36.dp)
            .trackHoverFocus(hoverState)
            .thickBlackHoverOutline(
                highlighted = highlighted,
                shape = chipShape,
                contrastHaloColor = ElectricCyan.copy(alpha = 0.9f)
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Checklist,
                contentDescription = "Edit Playlists",
                tint = if (highlighted || isCustomActive) ElectricCyan else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isCustomActive) {
                    "Edit Playlists ($selectedCount/$totalCount)"
                } else {
                    "Edit Playlists"
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (highlighted || isCustomActive) ElectricCyan else TextPrimary
            )
        }
    }
}

@Composable
fun EditPlaylistsDialog(
    title: String,
    categories: List<XtreamCategory>,
    itemCountsByCategory: Map<String, Int>,
    initialSelectedIds: Set<String>?,
    activeGlobalFilterNote: String? = null,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    val allCategoryIds = remember(categories) { categories.map { it.categoryId }.toSet() }
    var draftSelectedIds by remember(categories, initialSelectedIds) {
        mutableStateOf(
            if (initialSelectedIds == null) {
                allCategoryIds
            } else {
                initialSelectedIds.intersect(allCategoryIds)
            }
        )
    }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) categories
        else categories.filter { it.categoryName.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = BgCharcoalElevated,
            border = BorderStroke(1.2.dp, ElectricCyan.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .fillMaxHeight(0.86f)
                .testTag("edit_playlists_dialog")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Checklist,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${draftSelectedIds.size} of ${categories.size} playlists selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (draftSelectedIds.isEmpty()) TextMuted else ElectricCyan
                            )
                        }
                    }

                    NextVuIconButton(
                        onClick = onDismiss,
                        containerColor = SurfaceCardVariant,
                        hoverContainerColor = ElectricCyan.copy(alpha = 0.25f),
                        modifier = Modifier.testTag("edit_playlists_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                if (!activeGlobalFilterNote.isNullOrBlank()) {
                    Surface(
                        color = SurfaceCard,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SurfaceBorderSubtle)
                    ) {
                        Text(
                            text = activeGlobalFilterNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Click-to-edit Search Filter
                ClickToEditSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search playlists...",
                    height = 46.dp,
                    shape = RoundedCornerShape(12.dp),
                    testTag = "edit_playlists_search_input",
                    modifier = Modifier.fillMaxWidth()
                )

                // Select All / Deselect All Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PlaylistBulkActionButton(
                        label = "Select All",
                        icon = Icons.Rounded.SelectAll,
                        isActive = draftSelectedIds.size == allCategoryIds.size && allCategoryIds.isNotEmpty(),
                        onClick = { draftSelectedIds = allCategoryIds },
                        testTag = "edit_playlists_select_all",
                        modifier = Modifier.weight(1f)
                    )
                    PlaylistBulkActionButton(
                        label = "Deselect All",
                        icon = Icons.Rounded.Deselect,
                        isActive = draftSelectedIds.isEmpty(),
                        onClick = { draftSelectedIds = emptySet() },
                        testTag = "edit_playlists_deselect_all",
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = SurfaceBorderSubtle.copy(alpha = 0.6f))

                // Playlists Checklist
                if (filteredCategories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (categories.isEmpty()) {
                                "No playlists available in this source."
                            } else {
                                "No playlists match \"$searchQuery\"."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(
                            filteredCategories,
                            key = { idx, cat -> "edit_pl_${cat.categoryId}_$idx" }
                        ) { _, cat ->
                            val isChecked = draftSelectedIds.contains(cat.categoryId)
                            val count = itemCountsByCategory[cat.categoryId] ?: 0
                            EditPlaylistRow(
                                name = cat.categoryName,
                                count = count,
                                checked = isChecked,
                                onToggle = {
                                    draftSelectedIds = if (isChecked) {
                                        draftSelectedIds - cat.categoryId
                                    } else {
                                        draftSelectedIds + cat.categoryId
                                    }
                                },
                                testTag = "edit_playlist_row_${cat.categoryId}"
                            )
                        }
                    }
                }

                HorizontalDivider(color = SurfaceBorderSubtle.copy(alpha = 0.6f))

                // Footer Cancel / Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NextVuOutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, SurfaceBorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("edit_playlists_cancel_btn")
                    ) {
                        Text("Cancel", color = TextPrimary)
                    }

                    NextVuButton(
                        onClick = { onSave(draftSelectedIds) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = BgCharcoal
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("edit_playlists_save_btn")
                    ) {
                        Text(
                            text = "Save (${draftSelectedIds.size})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistBulkActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val hoverState = rememberHoverFocusState()
    val highlighted = hoverState.isHighlighted
    val btnShape = RoundedCornerShape(10.dp)

    Surface(
        onClick = onClick,
        shape = btnShape,
        color = when {
            highlighted -> ElectricCyan.copy(alpha = 0.28f)
            isActive -> ElectricCyan.copy(alpha = 0.14f)
            else -> SurfaceCard
        },
        border = if (highlighted) null else BorderStroke(
            width = 1.dp,
            color = if (isActive) ElectricCyan.copy(alpha = 0.65f) else SurfaceBorderSubtle
        ),
        modifier = modifier
            .heightIn(min = 48.dp)
            .trackHoverFocus(hoverState)
            .thickBlackHoverOutline(
                highlighted = highlighted,
                shape = btnShape,
                contrastHaloColor = ElectricCyan.copy(alpha = 0.9f)
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted || isActive) ElectricCyan else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (highlighted || isActive) ElectricCyan else TextPrimary
            )
        }
    }
}

@Composable
private fun EditPlaylistRow(
    name: String,
    count: Int,
    checked: Boolean,
    onToggle: () -> Unit,
    testTag: String
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(10.dp),
        color = when {
            isFocused -> ElectricCyan.copy(alpha = 0.20f)
            checked -> SurfaceCardVariant
            else -> SurfaceCard.copy(alpha = 0.55f)
        },
        border = BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = when {
                isFocused -> ElectricCyan
                checked -> ElectricCyan.copy(alpha = 0.45f)
                else -> SurfaceBorderSubtle.copy(alpha = 0.6f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = ElectricCyan,
                    uncheckedColor = TextMuted,
                    checkmarkColor = BgCharcoal
                )
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal,
                color = if (checked) TextPrimary else TextSecondary,
                softWrap = true,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = BgCharcoal
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (checked) ElectricCyan else TextMuted,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    }
}
