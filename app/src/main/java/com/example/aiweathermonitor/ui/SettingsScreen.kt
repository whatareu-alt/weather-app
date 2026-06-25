package com.example.aiweathermonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiweathermonitor.WeatherState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.text.font.FontWeight

// ── Enums ────────────────────────────────────────────────────────────────────

enum class WindUnit(val label: String) { KMH("km/h"), MPH("mph"), MS("m/s") }
enum class PressureUnit(val label: String) { HPA("hPa"), INHG("inHg") }
enum class VisibilityUnit(val label: String) { KM("km"), MI("mi") }
enum class AppTheme(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

// ── Settings state ────────────────────────────────────────────────────────────

data class AppSettings(
    val windUnit: WindUnit = WindUnit.KMH,
    val pressureUnit: PressureUnit = PressureUnit.HPA,
    val visibilityUnit: VisibilityUnit = VisibilityUnit.KM,
    val theme: AppTheme = AppTheme.SYSTEM,
    val notifySevere: Boolean = true,
    val notifyRain: Boolean = true,
    val notifyDailyBriefing: Boolean = false,
    val notifyHighUv: Boolean = false,
    val backgroundRefresh: Boolean = true,
    val soundEnabled: Boolean = false
)

// ── Main composable ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: WeatherState,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onSaveWeatherApiKey: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

    var showPrivacy by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings", fontSize = 17.sp, color = contentColor, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = settingsBackIcon(),
                                contentDescription = "Back",
                                tint = contentColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = contentColor,
                        navigationIconContentColor = contentColor
                    )
                )
            },
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
            Spacer(Modifier.height(8.dp))

            // ── Units ────────────────────────────────────────────────────────
            SettingsGroupLabel("Units")
            SettingsCard {
                SegmentedRow(
                    icon = icons.temperature,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    label = "Temperature",
                    options = listOf("°C", "°F"),
                    selected = if (state.isCelsius) 0 else 1,
                    onSelect = { /* isCelsius toggled via WeatherState — see caller */ }
                )
                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                SegmentedRow(
                    icon = icons.wind,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = "Wind speed",
                    options = WindUnit.entries.map { it.label },
                    selected = settings.windUnit.ordinal,
                    onSelect = { onSettingsChange(settings.copy(windUnit = WindUnit.entries[it])) }
                )
                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                SegmentedRow(
                    icon = icons.pressure,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    label = "Pressure",
                    options = PressureUnit.entries.map { it.label },
                    selected = settings.pressureUnit.ordinal,
                    onSelect = { onSettingsChange(settings.copy(pressureUnit = PressureUnit.entries[it])) }
                )
                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                SegmentedRow(
                    icon = icons.visibility,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    label = "Visibility",
                    options = VisibilityUnit.entries.map { it.label },
                    selected = settings.visibilityUnit.ordinal,
                    onSelect = { onSettingsChange(settings.copy(visibilityUnit = VisibilityUnit.entries[it])) }
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Appearance ───────────────────────────────────────────────────
            SettingsGroupLabel("Appearance")
            SettingsCard {
                SegmentedRow(
                    icon = icons.theme,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    label = "Theme",
                    options = AppTheme.entries.map { it.label },
                    selected = settings.theme.ordinal,
                    onSelect = { onSettingsChange(settings.copy(theme = AppTheme.entries[it])) }
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Sound ─────────────────────────────────────────────────────────
            SettingsGroupLabel("Sound")
            SettingsCard {
                ToggleRow(
                    icon = icons.sound,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = "Weather ambience",
                    subtitle = "Play background sound matching the weather",
                    checked = settings.soundEnabled,
                    onCheckedChange = { onSettingsChange(settings.copy(soundEnabled = it)) }
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Notifications ─────────────────────────────────────────────────
            SettingsGroupLabel("Notifications")
            SettingsCard {
                ToggleRow(
                    icon = icons.alert,
                    iconBgColor = MaterialTheme.colorScheme.errorContainer,
                    iconTint = MaterialTheme.colorScheme.error,
                    label = "Severe weather alerts",
                    subtitle = "Storms, heat advisories, floods",
                    checked = settings.notifySevere,
                    onCheckedChange = { onSettingsChange(settings.copy(notifySevere = it)) }
                )
                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                ToggleRow(
                    icon = icons.rain,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = "Rain alerts",
                    subtitle = "Notify 30 min before rain",
                    checked = settings.notifyRain,
                    onCheckedChange = { onSettingsChange(settings.copy(notifyRain = it)) }
                )
                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                ToggleRow(
                    icon = icons.sun,
                    iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    label = "Daily briefing",
                    subtitle = "Morning summary at 7:00 AM",
                    checked = settings.notifyDailyBriefing,
                    onCheckedChange = { onSettingsChange(settings.copy(notifyDailyBriefing = it)) }
                )
                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                ToggleRow(
                    icon = icons.uv,
                    iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    label = "High UV alert",
                    subtitle = "When UV index ≥ 8",
                    checked = settings.notifyHighUv,
                    onCheckedChange = { onSettingsChange(settings.copy(notifyHighUv = it)) }
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Data sources ──────────────────────────────────────────────────
            SettingsGroupLabel("Data sources")
            SettingsCard {
                // Open-Meteo row (always active, no toggle)
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RowIconBox(icon = icons.globe, iconTint = contentColor, bgColor = MaterialTheme.colorScheme.secondary)
                    Column(Modifier.weight(1f)) {
                        Text("Open-Meteo", fontSize = 14.sp, color = contentColor.copy(alpha = 0.90f))
                        Text("Always active · no key required", fontSize = 12.sp, color = contentColor.copy(alpha = 0.60f))
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Active", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                ToggleRow(
                    icon = icons.refresh,
                    iconBgColor = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "Background refresh",
                    subtitle = "Every 30 minutes",
                    checked = settings.backgroundRefresh,
                    onCheckedChange = { onSettingsChange(settings.copy(backgroundRefresh = it)) }
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── About ─────────────────────────────────────────────────────────
            SettingsGroupLabel("About")
            SettingsCard {
                NavRow(icon = icons.info, label = "Version", trailingText = "1.0.0", onClick = {})
                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                NavRow(icon = icons.privacy, label = "Privacy policy", onClick = { showPrivacy = true })
                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                NavRow(icon = icons.licenses, label = "Open source licenses", onClick = { showLicenses = true })
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Friday · Open-Meteo + WeatherAPI.com",
                fontSize = 12.sp,
                color = contentColor.copy(alpha = 0.50f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(32.dp))
        }
    }

        PrivacyPolicyDialog(visible = showPrivacy, onDismiss = { showPrivacy = false })
        LicensesDialog(visible = showLicenses, onDismiss = { showLicenses = false })
    }
}

// ── About dialogs ─────────────────────────────────────────────────────────────

@Composable
private fun InfoDialog(
    visible: Boolean,
    title: String,
    onDismiss: () -> Unit,
    body: @Composable ColumnScope.() -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = body
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun PolicyHeading(text: String) =
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.W700,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 4.dp)
    )

@Composable
private fun PolicyBody(text: String) =
    Text(
        text = text,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

@Composable
private fun PrivacyPolicyDialog(visible: Boolean, onDismiss: () -> Unit) {
    InfoDialog(visible = visible, title = "Privacy Policy", onDismiss = onDismiss) {
        PolicyBody("Last updated: June 2026")
        PolicyBody(
            "This app is built privacy-first. We do not collect, store, or share any " +
                "personal information, and there are no accounts or sign-ins."
        )
        PolicyHeading("Location")
        PolicyBody(
            "If you grant location permission, your coordinates are used only on your " +
                "device to fetch local weather and are sent solely to the weather provider " +
                "to return a forecast. Your location is never stored on a server or shared " +
                "with third parties. You can also use the app by searching for cities manually."
        )
        PolicyHeading("Data sources")
        PolicyBody(
            "Weather comes from Open-Meteo and (optionally) WeatherAPI.com. News headlines " +
                "come from the Google News RSS feed. Requests contain only the city or " +
                "coordinates needed to return results."
        )
        PolicyHeading("On-device storage")
        PolicyBody(
            "Your selected city, saved favourites, units, and last fetched weather are cached " +
                "locally so the app works offline. This data never leaves your device, and you " +
                "can clear it any time from the system app settings."
        )
        PolicyHeading("No tracking or ads")
        PolicyBody("The app contains no analytics, advertising, or third-party trackers.")
        PolicyHeading("Permissions")
        PolicyBody(
            "Location (optional) — current-location weather.\n" +
                "Notifications (optional) — severe weather, rain, and UV alerts."
        )
    }
}

@Composable
private fun LicensesDialog(visible: Boolean, onDismiss: () -> Unit) {
    InfoDialog(visible = visible, title = "Open-Source Licenses", onDismiss = onDismiss) {
        PolicyBody("This app is built with these open-source projects:")
        val libraries = listOf(
            "Jetpack Compose & AndroidX" to "Apache License 2.0",
            "Material 3 & Material Icons" to "Apache License 2.0",
            "Kotlin & Coroutines" to "Apache License 2.0",
            "kotlinx.serialization" to "Apache License 2.0",
            "OkHttp" to "Apache License 2.0",
            "Koin (dependency injection)" to "Apache License 2.0",
            "Lottie for Android (Airbnb)" to "Apache License 2.0",
            "DataStore & WorkManager" to "Apache License 2.0",
            "Meteocons (Bas Milius)" to "MIT License"
        )
        libraries.forEach { (name, license) ->
            Column {
                Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W600,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = license,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        PolicyHeading("Data")
        PolicyBody(
            "Weather: Open-Meteo (CC BY 4.0) and WeatherAPI.com.\n" +
                "News: Google News RSS.\n" +
                "All trademarks belong to their respective owners."
        )
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun SettingsGroupLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(com.example.aiweathermonitor.theme.Radii.card),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = com.example.aiweathermonitor.theme.Elevation.card
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
private fun RowIconBox(icon: ImageVector, iconTint: androidx.compose.ui.graphics.Color, bgColor: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SegmentedRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    label: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconTint.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
        Text(label, fontSize = 14.sp, color = contentColor.copy(alpha = 0.90f), modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(contentColor.copy(alpha = 0.06f))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            options.forEachIndexed { idx, opt ->
                val isActive = idx == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isActive) contentColor.copy(alpha = 0.18f)
                            else Color.Transparent
                        )
                        .clickable { onSelect(idx) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        opt,
                        fontSize = 12.sp,
                        color = if (isActive) contentColor else contentColor.copy(alpha = 0.45f),
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    iconBgColor: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RowIconBox(icon = icon, iconTint = iconTint, bgColor = iconBgColor)
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, color = contentColor.copy(alpha = 0.90f))
            Text(subtitle, fontSize = 12.sp, color = contentColor.copy(alpha = 0.60f))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = contentColor.copy(alpha = 0.6f),
                uncheckedTrackColor = contentColor.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
private fun NavRow(
    icon: ImageVector,
    label: String,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RowIconBox(
            icon = icon,
            iconTint = MaterialTheme.colorScheme.primary,
            bgColor = MaterialTheme.colorScheme.primaryContainer
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = contentColor.copy(alpha = 0.90f),
            modifier = Modifier.weight(1f)
        )
        if (trailingText != null) {
            Text(
                text = trailingText,
                fontSize = 13.sp,
                color = contentColor.copy(alpha = 0.55f)
            )
        }
    }
}

// ── Icon set (outlined Material symbols) ──────────────────────────────────────

private object icons {
    val temperature = Icons.Outlined.Thermostat
    val wind        = Icons.Outlined.Air
    val pressure    = Icons.Outlined.Speed
    val visibility  = Icons.Outlined.Visibility
    val theme       = Icons.Outlined.DarkMode
    val alert       = Icons.Outlined.Warning
    val rain        = Icons.Outlined.WaterDrop
    val uv          = Icons.Outlined.WbSunny
    val sun         = Icons.Outlined.WbSunny
    val refresh     = Icons.Outlined.Refresh
    val sound       = Icons.Outlined.MusicNote
    val globe       = Icons.Outlined.Public
    val key         = Icons.Outlined.Key
    val info        = Icons.Outlined.Info
    val licenses    = Icons.Outlined.Description
    val privacy     = Icons.Outlined.Shield
}

private fun settingsBackIcon(): ImageVector = Icons.Outlined.ArrowBackIosNew
