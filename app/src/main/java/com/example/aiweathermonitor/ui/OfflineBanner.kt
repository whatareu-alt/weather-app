package com.example.aiweathermonitor.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shows a banner when:
 *  - device is offline (always shown), OR
 *  - data is stale (lastRefreshedEpoch > 0 and age > 1 hour)
 *
 * Disappears when online and data is fresh.
 */
@Composable
fun LiquidGlassOfflineBanner(
    isOnline: Boolean,
    lastRefreshedTime: String,
    lastRefreshedEpoch: Long = 0L,
    modifier: Modifier = Modifier
) {
    val staleThresholdMs = 60 * 60 * 1000L  // 1 hour
    val isStale = lastRefreshedEpoch > 0L &&
            (System.currentTimeMillis() - lastRefreshedEpoch) > staleThresholdMs

    val shouldShow = !isOnline || isStale

    AnimatedVisibility(
        visible = shouldShow,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (!isOnline) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.secondaryContainer
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            val onBanner = if (!isOnline) MaterialTheme.colorScheme.onErrorContainer
                           else MaterialTheme.colorScheme.onSecondaryContainer
            Icon(
                imageVector = if (!isOnline) Icons.Outlined.WifiOff else Icons.Outlined.AccessTime,
                contentDescription = null,
                tint = onBanner,
                modifier = Modifier.size(16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (!isOnline) "No internet connection" else "Weather data may be outdated",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W500,
                    color = onBanner
                )
                if (lastRefreshedTime.isNotBlank()) {
                    Text(
                        text = "Last updated $lastRefreshedTime",
                        fontSize = 11.sp,
                        color = onBanner
                    )
                }
            }
        }
    }
}
