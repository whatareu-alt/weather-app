package com.example.aiweathermonitor.theme

import androidx.compose.ui.unit.dp

// ── SOFT DAYLIGHT SPACING & SHAPE SYSTEM ──────────────────────────────────────
// One source of truth for rhythm and roundness so every surface feels related.

object Spacing {
    val screenHorizontal = 16.dp   // left/right page margin
    val sectionGap = 16.dp         // vertical gap between cards/sections
    val cardPaddingH = 18.dp       // inner card padding (horizontal)
    val cardPaddingV = 16.dp       // inner card padding (vertical)
    val itemGap = 12.dp            // gap between side-by-side tiles / inline rows
    val small = 8.dp
    val tiny = 4.dp
}

object Radii {
    val card = 24.dp   // primary cards
    val pill = 20.dp   // buttons / tab row / search field
    val chip = 14.dp   // small chips / list rows
    val bar = 8.dp     // progress bars / tracks
}

object Elevation {
    val card = 2.dp        // resting soft lift
    val raised = 6.dp      // dropdowns / dialogs (floating above content)
}
