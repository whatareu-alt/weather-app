package com.example.aiweathermonitor.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Pages ─────────────────────────────────────────────────────────────────────

private sealed class OnboardingPage {
    data object Welcome  : OnboardingPage()
    data object Features : OnboardingPage()
    data object Location : OnboardingPage()
}

private val pages = listOf(
    OnboardingPage.Welcome,
    OnboardingPage.Features,
    OnboardingPage.Location
)

// ── Entry composable ──────────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(
    onRequestLocation: () -> Unit,
    onSkipToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableIntStateOf(0) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp)
        ) {
            // Step dots
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.forEachIndexed { idx, _ ->
                    StepDot(active = idx == currentPage)
                    if (idx < pages.lastIndex) Spacer(Modifier.width(6.dp))
                }
            }
            Spacer(Modifier.height(8.dp))

            // Page content
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState > initialState)
                        slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                    else
                        slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                },
                modifier = Modifier.weight(1f),
                label = "onboarding_page"
            ) { page ->
                when (pages[page]) {
                    OnboardingPage.Welcome  -> WelcomePage()
                    OnboardingPage.Features -> FeaturesPage()
                    OnboardingPage.Location -> LocationPage()
                }
            }

            // Actions
            when (currentPage) {
                0 -> {
                    PrimaryButton("Get started") { currentPage = 1 }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Powered by Open-Meteo · WeatherAPI.com",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                1 -> PrimaryButton("Continue") { currentPage = 2 }
                2 -> {
                    PrimaryButton("Allow location") { onRequestLocation() }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onSkipToSearch,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Search city manually", fontSize = 14.sp) }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "You can change this later in Settings",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Page composables ──────────────────────────────────────────────────────────

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HeroIconBox(Icons.Outlined.WbCloudy)
        Spacer(Modifier.height(28.dp))
        Text(
            "Your weather,\nbeautifully clear",
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Real-time conditions, hourly and 14-day forecasts, air quality, UV index, and alerts — all in one place.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun FeaturesPage() {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Everything you need to know",
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "All conditions, no subscriptions.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
        )
        val features = listOf(
            Triple(Icons.Outlined.LightMode,   "UV & AQI",           "Hourly UV index and EPA air quality with health guidance"),
            Triple(Icons.Outlined.CalendarMonth,"14-day forecast",    "Daily highs, lows, precip chance, and UV per day"),
            Triple(Icons.Outlined.Warning,      "Severe alerts",      "Push notifications for storms, heat advisories, and heavy rain"),
            Triple(Icons.Outlined.Refresh,      "Background refresh", "Conditions update every 30 min, even when the app is closed"),
        )
        features.forEach { (icon, title, sub) ->
            FeatureRow(icon = icon, title = title, subtitle = sub)
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun LocationPage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        HeroIconBox(Icons.Outlined.LocationOn)
        Spacer(Modifier.height(20.dp))
        Text(
            "Allow location access",
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "To show your local weather automatically.\nYou can also search any city manually.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(20.dp))
        // Permission card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(24.dp))
                        }
                    }
                    Column {
                        Text("Location", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text("While using the app", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(14.dp))
                PermissionBullet(icon = Icons.Outlined.Check, positive = true,  text = "Automatic weather for your current city")
                Spacer(Modifier.height(8.dp))
                PermissionBullet(icon = Icons.Outlined.Check, positive = true,  text = "Never shared or stored on any server")
                Spacer(Modifier.height(8.dp))
                PermissionBullet(icon = Icons.Outlined.Close, positive = false, text = "Not used in the background for tracking")
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

// ── Small components ──────────────────────────────────────────────────────────

@Composable
private fun StepDot(active: Boolean) {
    Box(
        modifier = Modifier
            .height(6.dp)
            .clip(CircleShape)
            .background(
                if (active) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.outlineVariant
            )
            .animateContentSize()
            .width(if (active) 20.dp else 6.dp)
    )
}

@Composable
private fun HeroIconBox(icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(88.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(44.dp))
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun PermissionBullet(icon: ImageVector, positive: Boolean, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
