package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.*
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Repository for managing spool weight data including components, configurations,
 * presets, and weight measurements.
 */
class SpoolWeightRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("spool_weight_data", Context.MODE_PRIVATE)
    
    // Custom LocalDateTime adapter for Gson
    private val localDateTimeAdapter = object : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        
        override fun serialize(src: LocalDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src?.format(formatter))
        }
        
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDateTime? {
            return try {
                json?.asString?.let { LocalDateTime.parse(it, formatter) }
            } catch (e: Exception) {
                LocalDateTime.now() // Fallback to current time
            }
        }
    }
    
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, localDateTimeAdapter)
        .create()
    
    init {
        // Initialize with factory defaults if first run
        if (getComponents().isEmpty()) {
            initializeFactoryDefaults()
        }
    }
    
    // === Component Management ===
    
    /**
     * Get all spool components
     */
    fun getComponents(): List<SpoolComponent> {
        val json = sharedPreferences.getString(COMPONENTS_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SpoolComponent>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get component by ID
     */
    fun getComponent(id: String): SpoolComponent? {
        return getComponents().find { it.id == id }
    }
    
    /**
     * Save or update a component
     */
    fun saveComponent(component: SpoolComponent) {
        val components = getComponents().toMutableList()
        val existingIndex = components.indexOfFirst { it.id == component.id }
        
        if (existingIndex >= 0) {
            components[existingIndex] = component
        } else {
            components.add(component)
        }
        
        saveComponents(components)
    }
    
    /**
     * Delete a component
     */
    fun deleteComponent(componentId: String) {
        val components = getComponents().filterNot { it.id == componentId }
        saveComponents(components)
    }
    
    private fun saveComponents(components: List<SpoolComponent>) {
        val json = gson.toJson(components)
        sharedPreferences.edit()
            .putString(COMPONENTS_KEY, json)
            .apply()
    }
    
    // === Configuration Management ===
    
    /**
     * Get all spool configurations
     */
    fun getConfigurations(): List<SpoolConfiguration> {
        val json = sharedPreferences.getString(CONFIGURATIONS_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SpoolConfiguration>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get configuration by ID
     */
    fun getConfiguration(id: String): SpoolConfiguration? {
        return getConfigurations().find { it.id == id }
    }
    
    /**
     * Save or update a configuration
     */
    fun saveConfiguration(configuration: SpoolConfiguration) {
        val configurations = getConfigurations().toMutableList()
        val existingIndex = configurations.indexOfFirst { it.id == configuration.id }
        
        if (existingIndex >= 0) {
            configurations[existingIndex] = configuration
        } else {
            configurations.add(configuration)
        }
        
        saveConfigurations(configurations)
    }
    
    /**
     * Delete a configuration
     */
    fun deleteConfiguration(configurationId: String) {
        val configurations = getConfigurations().filterNot { it.id == configurationId }
        saveConfigurations(configurations)
    }
    
    private fun saveConfigurations(configurations: List<SpoolConfiguration>) {
        val json = gson.toJson(configurations)
        sharedPreferences.edit()
            .putString(CONFIGURATIONS_KEY, json)
            .apply()
    }
    
    // === Preset Management ===
    
    /**
     * Get all spool weight presets
     */
    fun getPresets(): List<SpoolWeightPreset> {
        val json = sharedPreferences.getString(PRESETS_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SpoolWeightPreset>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get preset by ID
     */
    fun getPreset(id: String): SpoolWeightPreset? {
        return getPresets().find { it.id == id }
    }
    
    /**
     * Save or update a preset
     */
    fun savePreset(preset: SpoolWeightPreset) {
        val presets = getPresets().toMutableList()
        val existingIndex = presets.indexOfFirst { it.id == preset.id }
        
        if (existingIndex >= 0) {
            presets[existingIndex] = preset
        } else {
            presets.add(preset)
        }
        
        savePresets(presets)
    }
    
    /**
     * Reset a factory preset to its default values
     */
    fun resetPresetToDefault(presetId: String) {
        val factoryDefaults = createFactoryPresets()
        val defaultPreset = factoryDefaults.find { it.id == presetId }
        
        if (defaultPreset != null) {
            savePreset(defaultPreset.copy(isModified = false))
        }
    }
    
    private fun savePresets(presets: List<SpoolWeightPreset>) {
        val json = gson.toJson(presets)
        sharedPreferences.edit()
            .putString(PRESETS_KEY, json)
            .apply()
    }
    
    // === Measurement Management ===
    
    /**
     * Get all weight measurements
     */
    fun getMeasurements(): List<FilamentWeightMeasurement> {
        val json = sharedPreferences.getString(MEASUREMENTS_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<FilamentWeightMeasurement>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get measurements for a specific tray UID
     */
    fun getMeasurementsForTray(trayUid: String): List<FilamentWeightMeasurement> {
        return getMeasurements().filter { it.trayUid == trayUid }
    }
    
    /**
     * Save or update a measurement
     */
    fun saveMeasurement(measurement: FilamentWeightMeasurement) {
        val measurements = getMeasurements().toMutableList()
        val existingIndex = measurements.indexOfFirst { it.id == measurement.id }
        
        if (existingIndex >= 0) {
            measurements[existingIndex] = measurement
        } else {
            measurements.add(measurement)
        }
        
        saveMeasurements(measurements)
    }
    
    /**
     * Delete a measurement
     */
    fun deleteMeasurement(measurementId: String) {
        val measurements = getMeasurements().filterNot { it.id == measurementId }
        saveMeasurements(measurements)
    }
    
    private fun saveMeasurements(measurements: List<FilamentWeightMeasurement>) {
        val json = gson.toJson(measurements)
        sharedPreferences.edit()
            .putString(MEASUREMENTS_KEY, json)
            .apply()
    }
    
    // === Factory Defaults Initialization ===
    
    /**
     * Initialize repository with factory default components, configurations, and presets
     */
    private fun initializeFactoryDefaults() {
        // Initialize components
        val factoryComponents = createFactoryComponents()
        saveComponents(factoryComponents)
        
        // Initialize configurations
        val factoryConfigurations = createFactoryConfigurations()
        saveConfigurations(factoryConfigurations)
        
        // Initialize presets
        val factoryPresets = createFactoryPresets()
        savePresets(factoryPresets)
    }
    
    /**
     * Reset all data to factory defaults
     */
    fun resetToFactoryDefaults() {
        sharedPreferences.edit().clear().apply()
        initializeFactoryDefaults()
    }
    
    // === Factory Default Data ===
    
    private fun createFactoryComponents(): List<SpoolComponent> {
        return listOf(
            SpoolComponent(
                id = "bambu_cardboard_core",
                name = "Bambu Cardboard Core",
                type = SpoolComponentType.CORE_RING,
                weightGrams = 33f,
                manufacturer = "Bambu Lab",
                description = "Standard cardboard core ring for Bambu refills"
            ),
            SpoolComponent(
                id = "bambu_spool_empty",
                name = "Bambu Spool (Empty)",
                type = SpoolComponentType.BASE_SPOOL,
                weightGrams = 212f,
                manufacturer = "Bambu Lab",
                description = "Empty Bambu spool without cardboard core"
            ),
            SpoolComponent(
                id = "sunlu_v2_spool",
                name = "Sunlu Reusable Spool v2",
                type = SpoolComponentType.BASE_SPOOL,
                weightGrams = 0f, // TBD - user can measure and update
                manufacturer = "Sunlu",
                description = "Sunlu reusable spool version 2 (weight TBD)"
            ),
            SpoolComponent(
                id = "sunlu_v3_spool",
                name = "Sunlu Reusable Spool v3",
                type = SpoolComponentType.BASE_SPOOL,
                weightGrams = 0f, // TBD - user can measure and update
                manufacturer = "Sunlu",
                description = "Sunlu reusable spool version 3 (weight TBD)"
            )
            // Note: Bambu HT spool weight is currently unknown, can be added later
        )
    }
    
    private fun createFactoryConfigurations(): List<SpoolConfiguration> {
        val components = createFactoryComponents()
        
        return listOf(
            SpoolConfiguration(
                id = "bambu_refill_config",
                name = "Bambu Refill",
                components = listOf("bambu_cardboard_core"),
                totalWeightGrams = calculateConfigurationWeight(listOf("bambu_cardboard_core"), components),
                packageType = PackageType.OPEN_REFILL,
                isPreset = true,
                description = "Bambu refill with cardboard core only"
            ),
            SpoolConfiguration(
                id = "bambu_spool_config",
                name = "Bambu Spool",
                components = listOf("bambu_spool_empty", "bambu_cardboard_core"),
                totalWeightGrams = calculateConfigurationWeight(listOf("bambu_spool_empty", "bambu_cardboard_core"), components),
                packageType = PackageType.OPEN_SPOOL,
                isPreset = true,
                description = "Complete Bambu spool with cardboard core"
            )
        )
    }
    
    private fun calculateConfigurationWeight(componentIds: List<String>, components: List<SpoolComponent>): Float {
        return componentIds.sumOf { componentId ->
            components.find { it.id == componentId }?.weightGrams?.toDouble() ?: 0.0
        }.toFloat()
    }
    
    private fun createFactoryPresets(): List<SpoolWeightPreset> {
        return listOf(
            SpoolWeightPreset(
                id = "bambu_boxed_refill",
                name = "Boxed Refill",
                packageType = PackageType.BOXED_REFILL,
                configurationId = "bambu_refill_config",
                supportedCapacities = listOf(0.5f, 0.75f, 1.0f),
                description = "Bambu refill in retail box"
            ),
            SpoolWeightPreset(
                id = "bambu_bagged_refill",
                name = "Bagged Refill", 
                packageType = PackageType.BAGGED_REFILL,
                configurationId = "bambu_refill_config",
                supportedCapacities = listOf(0.5f, 0.75f, 1.0f),
                description = "Bambu refill in plastic bag"
            ),
            SpoolWeightPreset(
                id = "bambu_open_refill",
                name = "Open Refill",
                packageType = PackageType.OPEN_REFILL,
                configurationId = "bambu_refill_config", 
                supportedCapacities = listOf(0.5f, 0.75f, 1.0f),
                description = "Bambu refill without packaging"
            ),
            SpoolWeightPreset(
                id = "bambu_boxed_spool",
                name = "Boxed Spool",
                packageType = PackageType.BOXED_SPOOL,
                configurationId = "bambu_spool_config",
                supportedCapacities = listOf(0.5f, 0.75f, 1.0f),
                description = "Complete Bambu spool in retail box"
            ),
            SpoolWeightPreset(
                id = "bambu_bagged_spool",
                name = "Bagged Spool",
                packageType = PackageType.BAGGED_SPOOL,
                configurationId = "bambu_spool_config",
                supportedCapacities = listOf(0.5f, 0.75f, 1.0f),
                description = "Complete Bambu spool in plastic bag"
            ),
            SpoolWeightPreset(
                id = "bambu_open_spool",
                name = "Open Spool",
                packageType = PackageType.OPEN_SPOOL,
                configurationId = "bambu_spool_config",
                supportedCapacities = listOf(0.5f, 0.75f, 1.0f),
                description = "Complete Bambu spool without packaging"
            )
        )
    }
    
    companion object {
        private const val COMPONENTS_KEY = "spool_components"
        private const val CONFIGURATIONS_KEY = "spool_configurations"
        private const val PRESETS_KEY = "spool_presets"
        private const val MEASUREMENTS_KEY = "weight_measurements"
    }
}