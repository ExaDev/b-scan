package com.bscan.data.bambu.base

/**
 * Common data class for simple Bambu mappings.
 * Replaces the old MaterialInfo, SeriesInfo, and ColorInfo classes to reduce duplication.
 */
data class MappingInfo(
    val displayName: String,
    val description: String
)