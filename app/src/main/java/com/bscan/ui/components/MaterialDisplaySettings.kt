package com.bscan.ui.components

/**
 * Granular settings for how materials are visually displayed in FilamentColorBox
 */
data class MaterialDisplaySettings(
    val showMaterialShapes: Boolean = true,
    val showMaterialNameInShape: Boolean = false,
    val showMaterialVariantInShape: Boolean = false,
    val showFullVariantNamesInShape: Boolean = false
) {
    companion object {
        /**
         * Default settings that maintain backward compatibility
         */
        val DEFAULT = MaterialDisplaySettings(
            showMaterialShapes = true,
            showMaterialNameInShape = false,
            showMaterialVariantInShape = false,
            showFullVariantNamesInShape = false
        )
        
        /**
         * Legacy MaterialDisplayMode.SHAPES equivalent
         */
        val SHAPES_ONLY = MaterialDisplaySettings(
            showMaterialShapes = true,
            showMaterialNameInShape = false,
            showMaterialVariantInShape = false,
            showFullVariantNamesInShape = false
        )
        
        /**
         * Legacy MaterialDisplayMode.TEXT_LABELS equivalent
         */
        val TEXT_LABELS = MaterialDisplaySettings(
            showMaterialShapes = false,
            showMaterialNameInShape = true,
            showMaterialVariantInShape = false,
            showFullVariantNamesInShape = false
        )
        
        /**
         * Maximum information display
         */
        val FULL_INFO = MaterialDisplaySettings(
            showMaterialShapes = true,
            showMaterialNameInShape = true,
            showMaterialVariantInShape = true,
            showFullVariantNamesInShape = true
        )
    }
}

