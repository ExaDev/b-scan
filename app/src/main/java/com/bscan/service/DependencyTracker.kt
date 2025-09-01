package com.bscan.service

import android.content.Context
import android.util.Log
import com.bscan.model.graph.Entity
import com.bscan.model.graph.entities.*
import com.bscan.repository.CatalogRepository
import java.io.File
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks external dependencies that affect ephemeral entity generation.
 * Detects changes in catalogs, configurations, algorithms, and external data sources
 * to determine when cached derived entities need regeneration.
 */
class DependencyTracker(
    private val context: Context,
    private val catalogRepository: CatalogRepository
) {
    
    companion object {
        private const val TAG = "DependencyTracker"
    }
    
    // Cache dependency fingerprints to detect changes
    private val dependencyCache = ConcurrentHashMap<String, DependencySnapshot>()
    
    /**
     * Extract all dependencies for a derived entity
     */
    fun extractDependencies(entity: Entity, sourceEntity: Entity): DependencySet {
        val dependencies = DependencySet(
            sourceEntityId = sourceEntity.id,
            sourceFingerprint = generateSourceFingerprint(sourceEntity),
            catalogVersion = extractCatalogDependency(entity),
            configHashes = extractConfigDependencies(entity),
            externalDataSources = extractExternalDataSources(entity),
            algorithmFingerprints = extractAlgorithmFingerprints(entity.entityType),
            timestamp = LocalDateTime.now()
        )
        
        Log.v(TAG, "Extracted dependencies for ${entity.entityType}: ${dependencies.getDependencyKeys()}")
        return dependencies
    }
    
    /**
     * Check if dependencies have changed since the entity was cached
     */
    fun hasChanged(cachedDependencies: DependencySet): Boolean {
        val currentDependencies = refreshDependencies(cachedDependencies)
        
        val changed = cachedDependencies != currentDependencies
        
        if (changed) {
            val changes = findChanges(cachedDependencies, currentDependencies)
            Log.d(TAG, "Dependencies changed: $changes")
        }
        
        return changed
    }
    
    /**
     * Get current snapshot of all dependencies for comparison
     */
    private fun refreshDependencies(template: DependencySet): DependencySet {
        return template.copy(
            sourceFingerprint = getCurrentSourceFingerprint(template.sourceEntityId),
            catalogVersion = getCurrentCatalogVersion(),
            configHashes = getCurrentConfigHashes(template.configHashes.keys),
            externalDataSources = getCurrentExternalDataSources(template.externalDataSources),
            algorithmFingerprints = getCurrentAlgorithmFingerprints(template.algorithmFingerprints.keys),
            timestamp = LocalDateTime.now()
        )
    }
    
    /**
     * Generate fingerprint of source entity that affects derivation
     */
    private fun generateSourceFingerprint(sourceEntity: Entity): String {
        val relevantData = buildString {
            append(sourceEntity.id)
            append(sourceEntity.label)
            
            // Include critical properties that affect derivation
            when (sourceEntity) {
                is RawScanData -> {
                    append("rawData:${sourceEntity.rawData?.hashCode()}")
                    append("contentHash:${sourceEntity.contentHash}")
                    append("scanFormat:${sourceEntity.scanFormat}")
                }
                is Information -> {
                    sourceEntity.getProperty<String>("rawData")?.let {
                        append("rawData:${it.hashCode()}")
                    }
                }
            }
            
            // Include metadata that affects processing
            sourceEntity.metadata.let {
                append("created:${it.created}")
                append("modified:${it.lastModified}")
            }
        }
        
        return hashString(relevantData).take(16)
    }
    
    private fun getCurrentSourceFingerprint(sourceEntityId: String): String {
        // This would need to fetch the current source entity
        // For now, return a placeholder that changes when source changes
        return "src_${sourceEntityId.hashCode()}"
    }
    
    /**
     * Extract catalog-related dependencies
     */
    private fun extractCatalogDependency(entity: Entity): String? {
        return when (entity) {
            is DecodedDecrypted -> {
                // If entity uses catalog data, track catalog version
                if (entity.getProperty<String>("catalogData") != null ||
                    entity.productInfo != null) {
                    getCatalogVersion()
                } else null
            }
            is PhysicalComponent -> {
                // Physical components may use catalog for mass, specifications
                if (entity.getProperty<Any>("catalogMass") != null) {
                    getCatalogVersion()
                } else null
            }
            else -> null
        }
    }
    
    private fun getCatalogVersion(): String {
        // Generate version based on catalog content
        try {
            val catalogFingerprint = catalogRepository.getContentFingerprint()
            return "catalog_$catalogFingerprint"
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get catalog version", e)
            return "catalog_unknown_${System.currentTimeMillis()}"
        }
    }
    
    private fun getCurrentCatalogVersion(): String? {
        return getCatalogVersion()
    }
    
    /**
     * Extract configuration file dependencies
     */
    private fun extractConfigDependencies(entity: Entity): Map<String, String> {
        val configFiles = mutableSetOf<String>()
        
        when (entity) {
            is DecodedDecrypted -> {
                // Filament interpretation may depend on material configs
                configFiles.add("filament_mappings.json")
                configFiles.add("temperature_profiles.json")
            }
            is DecodedEncrypted -> {
                // Encryption handling configs
                configFiles.add("encryption_settings.json")
            }
        }
        
        return configFiles.associateWith { fileName ->
            getConfigFileHash(fileName)
        }
    }
    
    private fun getCurrentConfigHashes(configKeys: Set<String>): Map<String, String> {
        return configKeys.associateWith { fileName ->
            getConfigFileHash(fileName)
        }
    }
    
    private fun getConfigFileHash(fileName: String): String {
        return try {
            val configFile = File(context.filesDir, "config/$fileName")
            if (configFile.exists()) {
                val content = configFile.readText()
                hashString(content).take(16)
            } else {
                "missing_$fileName"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to hash config file: $fileName", e)
            "error_$fileName"
        }
    }
    
    /**
     * Extract external data source dependencies
     */
    private fun extractExternalDataSources(entity: Entity): Set<String> {
        val sources = mutableSetOf<String>()
        
        when (entity) {
            is DecodedDecrypted -> {
                // May depend on online product databases
                entity.productInfo?.let { sources.add("product_database") }
            }
            is PhysicalComponent -> {
                // May depend on external component specifications
                entity.getProperty<String>("externalSpec")?.let { 
                    sources.add("component_specs") 
                }
            }
        }
        
        return sources
    }
    
    private fun getCurrentExternalDataSources(sources: Set<String>): Set<String> {
        // For now, return the same sources
        // In practice, this would check if external sources are available/changed
        return sources.map { source ->
            when (source) {
                "product_database" -> "product_db_${getExternalSourceVersion(source)}"
                "component_specs" -> "specs_${getExternalSourceVersion(source)}"
                else -> source
            }
        }.toSet()
    }
    
    private fun getExternalSourceVersion(source: String): String {
        // Placeholder - would check actual external source versions
        return "v1.0"
    }
    
    /**
     * Extract algorithm/parsing logic fingerprints
     */
    private fun extractAlgorithmFingerprints(entityType: String): Map<String, String> {
        val algorithms = mutableMapOf<String, String>()
        
        when (entityType) {
            "decoded_encrypted" -> {
                algorithms["metadata_extraction"] = getAlgorithmFingerprint("MetadataExtractor")
                algorithms["tag_parsing"] = getAlgorithmFingerprint("TagParser")
            }
            "encoded_decrypted" -> {
                algorithms["decryption"] = getAlgorithmFingerprint("BambuDecryptor")
                algorithms["key_derivation"] = getAlgorithmFingerprint("KeyDerivation")
            }
            "decoded_decrypted" -> {
                algorithms["filament_interpretation"] = getAlgorithmFingerprint("FilamentInterpreter")
                algorithms["temperature_calculation"] = getAlgorithmFingerprint("TemperatureCalculator")
            }
        }
        
        return algorithms
    }
    
    private fun getCurrentAlgorithmFingerprints(algorithmKeys: Set<String>): Map<String, String> {
        return algorithmKeys.associateWith { algorithm ->
            getAlgorithmFingerprint(algorithm)
        }
    }
    
    private fun getAlgorithmFingerprint(algorithmName: String): String {
        // This would normally hash the relevant code/logic
        // For now, use a version identifier that changes when logic updates
        return when (algorithmName) {
            "MetadataExtractor" -> "meta_v1.2.0"
            "TagParser" -> "parser_v1.1.0"
            "BambuDecryptor" -> "decrypt_v1.0.0"
            "KeyDerivation" -> "keys_v2.0.0"
            "FilamentInterpreter" -> "interp_v1.3.0"
            "TemperatureCalculator" -> "temp_v1.0.0"
            else -> "unknown_v1.0.0"
        }
    }
    
    /**
     * Find specific changes between dependency sets
     */
    private fun findChanges(old: DependencySet, new: DependencySet): List<String> {
        val changes = mutableListOf<String>()
        
        if (old.sourceFingerprint != new.sourceFingerprint) {
            changes.add("source_entity")
        }
        
        if (old.catalogVersion != new.catalogVersion) {
            changes.add("catalog: ${old.catalogVersion} -> ${new.catalogVersion}")
        }
        
        old.configHashes.forEach { (file, oldHash) ->
            val newHash = new.configHashes[file]
            if (oldHash != newHash) {
                changes.add("config: $file")
            }
        }
        
        if (old.externalDataSources != new.externalDataSources) {
            changes.add("external_data_sources")
        }
        
        old.algorithmFingerprints.forEach { (algo, oldVersion) ->
            val newVersion = new.algorithmFingerprints[algo]
            if (oldVersion != newVersion) {
                changes.add("algorithm: $algo ($oldVersion -> $newVersion)")
            }
        }
        
        return changes
    }
    
    private fun hashString(input: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }
}

/**
 * Snapshot of all dependencies for a derived entity
 */
data class DependencySet(
    val sourceEntityId: String,
    val sourceFingerprint: String,
    val catalogVersion: String? = null,
    val configHashes: Map<String, String> = emptyMap(),
    val externalDataSources: Set<String> = emptySet(),
    val algorithmFingerprints: Map<String, String> = emptyMap(),
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    fun getDependencyKeys(): Set<String> {
        val keys = mutableSetOf<String>()
        keys.add("source:$sourceEntityId")
        catalogVersion?.let { keys.add("catalog:$it") }
        configHashes.forEach { (file, hash) -> keys.add("config:$file:$hash") }
        externalDataSources.forEach { keys.add("external:$it") }
        algorithmFingerprints.forEach { (algo, version) -> keys.add("algorithm:$algo:$version") }
        return keys
    }
}

/**
 * Snapshot of dependency state at a point in time
 */
private data class DependencySnapshot(
    val fingerprint: String,
    val timestamp: LocalDateTime,
    val ttlMinutes: Int = 30
) {
    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(timestamp.plusMinutes(ttlMinutes.toLong()))
    }
}