package com.bscan.ui.components

/**
 * Display mode for material representation in FilamentColorBox
 */
enum class MaterialDisplayMode(
    val displayName: String,
    val description: String
) {
    SHAPES(
        displayName = "Geometric Shapes",
        description = "Use distinct shapes for each material type (circle, hexagon, etc.)"
    ),
    TEXT_LABELS(
        displayName = "Text Labels", 
        description = "Show material abbreviations as text overlays (PLA, PETG, etc.)"
    )
}