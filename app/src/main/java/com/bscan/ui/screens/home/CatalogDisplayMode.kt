package com.bscan.ui.screens.home

/**
 * Display mode for catalog product information
 */
enum class CatalogDisplayMode(
    val displayName: String,
    val description: String
) {
    COMPLETE_TITLE(
        displayName = "Complete Material Name",
        description = "Show full material name in title (e.g. 'Basic Cyan PLA')"
    ),
    COLOR_FOCUSED(
        displayName = "Color-Focused Display",
        description = "Show only color name in title, material details in properties"
    )
}