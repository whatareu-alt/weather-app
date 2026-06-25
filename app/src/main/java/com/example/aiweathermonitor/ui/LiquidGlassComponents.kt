package com.example.aiweathermonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiweathermonitor.GeocodingResult
import com.example.aiweathermonitor.theme.Elevation
import com.example.aiweathermonitor.theme.Radii
import com.example.aiweathermonitor.theme.Spacing

// ─── COMPOSITION LOCAL ────────────────────────────────────────────────────────

val LocalWeatherCode = compositionLocalOf { 0 }

// ─── WEATHER MODE (kept for source compatibility) ─────────────────────────────

enum class OrganicWeatherMode { SUNNY, RAINY, SNOWY, STORMY, NEUTRAL }

fun getWeatherMode(code: Int): OrganicWeatherMode = when (code) {
    0, 1, 2, 3 -> OrganicWeatherMode.SUNNY
    45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> OrganicWeatherMode.RAINY
    71, 73, 75, 77, 85, 86 -> OrganicWeatherMode.SNOWY
    95, 96, 99 -> OrganicWeatherMode.STORMY
    else -> OrganicWeatherMode.NEUTRAL
}

/** No-op — removes all per-card infinite-transition overhead. */
@Composable
fun Modifier.organicMotion(mode: OrganicWeatherMode): Modifier = this

@Composable
fun glassRefractionBorder(): Brush = Brush.linearGradient(
    colors = listOf(
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    )
)

fun glassSubtleBorder(): Brush = Brush.linearGradient(
    colors = listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.06f))
)

// ─── LIQUID GLASS CARD ───────────────────────────────────────────────────────

@Composable
fun LiquidGlassCard(
    title: String = "",
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Radii.card,
    glassAlpha: Float = 0.08f,      // kept for API compat
    showShimmer: Boolean = true,     // kept for API compat
    content: @Composable () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Elevation.card),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = Spacing.cardPaddingH, vertical = Spacing.cardPaddingV)) {
            if (title.isNotBlank()) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.small))
            }
            content()
        }
    }
}

// ─── LIQUID GLASS METRIC CARD ─────────────────────────────────────────────────

@Composable
fun LiquidGlassMetricCard(
    title: String,
    value: String,
    desc: String,
    modifier: Modifier = Modifier,
    isDark: Boolean = false,         // kept for API compat
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    var showDetail by remember { mutableStateOf(false) }

    ElevatedCard(
        onClick = { showDetail = true },
        shape = RoundedCornerShape(Radii.card),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Elevation.card),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.cardPaddingH, vertical = Spacing.cardPaddingV)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.tiny)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Capped to one line so a long value can't blow up the card height —
            // the full text is available in the tap-to-open detail dialog.
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (content != null) {
                Column(modifier = Modifier.fillMaxWidth()) { content() }
            }
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (showDetail) {
        AlertDialog(
            onDismissRequest = { showDetail = false },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W600,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (desc.isNotBlank()) {
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetail = false }) { Text("Close") }
            },
            shape = RoundedCornerShape(Radii.card),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

// ─── TOOLBAR BUTTON ───────────────────────────────────────────────────────────

@Composable
fun LiquidGlassToolbarButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.90f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier.size(42.dp)
    ) { content() }
}

// ─── BUTTON ───────────────────────────────────────────────────────────────────

@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(Radii.pill),
        modifier = modifier.fillMaxWidth(),
        content = content
    )
}

// ─── TAB ROW ──────────────────────────────────────────────────────────────────

@Composable
fun LiquidGlassTabRow(
    selectedTabIndex: Int,
    tabTitles: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    SecondaryTabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.pill))
    ) {
        tabTitles.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (selectedTabIndex == index) FontWeight.W700 else FontWeight.W500
                    )
                }
            )
        }
    }
}

// ─── TEXT FIELD ───────────────────────────────────────────────────────────────

@Composable
fun LiquidGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    textStyle: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        label = label,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        leadingIcon = leadingIcon,
        shape = RoundedCornerShape(Radii.pill),
        textStyle = textStyle,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// ─── SEARCH BAR ───────────────────────────────────────────────────────────────

@Composable
fun LiquidGlassSearchBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    LiquidGlassTextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = "Search city (e.g. Paris, Tokyo)",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        },
        leadingIcon = leadingIcon,
        modifier = modifier.fillMaxWidth()
    )
}

// ─── SEARCH RESULTS DROPDOWN ──────────────────────────────────────────────────

@Composable
fun LiquidGlassSearchResults(
    searchResults: List<GeocodingResult>,
    onSelectCity: (GeocodingResult) -> Unit,
    modifier: Modifier = Modifier
) {
    if (searchResults.isEmpty()) return

    ElevatedCard(
        shape = RoundedCornerShape(Radii.pill),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Elevation.raised),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
            items(searchResults) { result ->
                ListItem(
                    headlineContent = {
                        Text(result.name, fontSize = 14.sp, fontWeight = FontWeight.W500)
                    },
                    supportingContent = {
                        val parts = listOfNotNull(result.admin1, result.country)
                        if (parts.isNotEmpty()) {
                            Text(
                                text = parts.joinToString(", "),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                            )
                        }
                    },
                    modifier = Modifier.clickable { onSelectCity(result) }
                )
                if (result != searchResults.last()) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// ─── ALERT CARD ───────────────────────────────────────────────────────────────

@Composable
fun LiquidGlassAlertCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(Radii.card),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.cardPaddingH, vertical = Spacing.cardPaddingV),
            verticalArrangement = Arrangement.spacedBy(Spacing.tiny),
            content = content
        )
    }
}

// ─── DIALOG ───────────────────────────────────────────────────────────────────

@Composable
fun LiquidGlassDialog(
    onDismissRequest: () -> Unit,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        text = text,
        confirmButton = { confirmButton?.invoke() },
        dismissButton = { dismissButton?.invoke() },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}
