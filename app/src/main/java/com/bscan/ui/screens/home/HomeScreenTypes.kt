package com.bscan.ui.screens.home

enum class ViewMode { SPOOLS, SKUS, TAGS, SCANS }
enum class SortOption { MOST_RECENT, OLDEST, NAME, SUCCESS_RATE, COLOR, MATERIAL_TYPE }
enum class GroupByOption { NONE, COLOR, BASE_MATERIAL, MATERIAL_SERIES }

data class FilterState(
    val filamentTypes: Set<String> = emptySet(), // Detailed types (PLA Basic, PLA Silk+, etc.)
    val colors: Set<String> = emptySet(),
    val baseMaterials: Set<String> = emptySet(), // PLA, PETG, ABS, etc.
    val materialSeries: Set<String> = emptySet(), // Basic, Silk+, Matte, etc.
    val minSuccessRate: Float = 0f,
    val showSuccessOnly: Boolean = false,
    val showFailuresOnly: Boolean = false,
    val dateRangeDays: Int? = null // null = all time, 7 = last week, 30 = last month, etc.
)