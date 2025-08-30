package com.bscan.model

/**
 * Component creation results for the factory pattern
 */
sealed class ComponentCreationResult {
    data class Success(
        val rootComponent: Component,
        val factoryType: String,
        val tagFormat: TagFormat
    ) : ComponentCreationResult()
    
    data class FormatDetected(
        val format: TagFormat,
        val confidence: Float,
        val manufacturerName: String?
    ) : ComponentCreationResult()
    
    data class FormatDetectionFailed(
        val reason: String,
        val confidence: Float
    ) : ComponentCreationResult()
    
    data class ComponentCreationFailed(
        val reason: String
    ) : ComponentCreationResult()
    
    data class ProcessingError(
        val message: String
    ) : ComponentCreationResult()
}