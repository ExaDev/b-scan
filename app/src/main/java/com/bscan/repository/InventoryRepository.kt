package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.*
import com.bscan.logic.MassCalculationService
import com.bscan.service.BambuComponentFactory
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Repository for managing inventory items and their weight tracking data.
 * 
 * This repository handles the relationship between FilamentReel items and their
 * physical weight measurements, enabling calculation of remaining filament.
 */
class InventoryRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("inventory_data", Context.MODE_PRIVATE)
    
    private val componentRepository by lazy { ComponentRepository(context) }
    private val calculationService = MassCalculationService()
    private val catalogRepository by lazy { CatalogRepository(context) }
    private val userDataRepository by lazy { UserDataRepository(context) }
    private val unifiedDataAccess by lazy { UnifiedDataAccess(catalogRepository, userDataRepository) }
    private val bambuComponentFactory by lazy { BambuComponentFactory(context) }
    
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
    
    // === Inventory Item Management ===
    
    /**
     * Get all inventory items
     */
    fun getInventoryItems(): List<InventoryItem> {
        val json = sharedPreferences.getString(INVENTORY_ITEMS_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<InventoryItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get inventory item by tray UID
     */
    fun getInventoryItem(trayUid: String): InventoryItem? {
        return getInventoryItems().find { it.trayUid == trayUid }
    }
    
    /**
     * Save or update an inventory item
     */
    fun saveInventoryItem(inventoryItem: InventoryItem) {
        val items = getInventoryItems().toMutableList()
        val existingIndex = items.indexOfFirst { it.trayUid == inventoryItem.trayUid }
        
        val updatedItem = inventoryItem.copy(lastUpdated = LocalDateTime.now())
        
        if (existingIndex >= 0) {
            items[existingIndex] = updatedItem
        } else {
            items.add(updatedItem)
        }
        
        saveInventoryItems(items)
    }
    
    
    /**
     * Try to get filament mass from SKU data with comprehensive fallback strategy
     */
    private fun getFilamentMassFromSku(filamentType: String, colorName: String): Float? {
        try {
            val bestMatch = unifiedDataAccess.findBestProductMatch(filamentType, colorName)
            if (bestMatch?.filamentWeightGrams != null) {
                android.util.Log.d(TAG, "Found SKU mass for $filamentType/$colorName: ${bestMatch.filamentWeightGrams}g")
                return bestMatch.filamentWeightGrams
            }
            
            // Try RFID mapping lookup if no product match
            android.util.Log.d(TAG, "No product match found, checking RFID mappings")
            // Note: RFID mappings don't contain weight info, so this is for logging only
            
            android.util.Log.w(TAG, "No mass data found for $filamentType/$colorName, using default")
            return null
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error looking up SKU mass for $filamentType/$colorName", e)
            return null
        }
    }
    
    /**
     * Get default mass based on material type
     */
    private fun getDefaultMassByMaterial(filamentType: String): Float {
        return when (filamentType.uppercase()) {
            "TPU" -> 500f  // TPU typically comes in 500g spools
            "PVA", "SUPPORT" -> 500f  // Support materials typically 500g
            "PC", "PA", "PAHT" -> 1000f  // Engineering materials typically 1kg
            else -> 1000f  // Standard 1kg for PLA, PETG, ABS, ASA
        }
    }
    
    /**
     * Delete an inventory item
     */
    fun deleteInventoryItem(trayUid: String) {
        val items = getInventoryItems().filterNot { it.trayUid == trayUid }
        saveInventoryItems(items)
    }
    
    private fun saveInventoryItems(items: List<InventoryItem>) {
        val json = gson.toJson(items)
        sharedPreferences.edit()
            .putString(INVENTORY_ITEMS_KEY, json)
            .apply()
    }
    
    // === Mass Measurement Integration ===
    
    /**
     * Add a mass measurement to an inventory item
     */
    fun addMassMeasurement(trayUid: String, measurement: MassMeasurement) {
        val existingItem = getInventoryItem(trayUid)
        val updatedMeasurements = (existingItem?.measurements ?: emptyList()) + measurement
        
        val inventoryItem = existingItem?.copy(
            measurements = updatedMeasurements,
            lastUpdated = LocalDateTime.now()
        ) ?: InventoryItem(
            trayUid = trayUid,
            components = emptyList(), // Will be set separately
            totalMeasuredMass = measurement.measuredMassGrams,
            measurements = listOf(measurement),
            lastUpdated = LocalDateTime.now()
        )
        
        saveInventoryItem(inventoryItem)
    }
    
    /**
     * Update total measured mass and recalculate filament mass
     */
    fun updateTotalMeasuredMass(trayUid: String, totalMassGrams: Float) {
        val existingItem = getInventoryItem(trayUid) ?: return
        
        // Get components for this inventory item
        val components = componentRepository.getComponents()
            .filter { it.id in existingItem.components }
        
        // Update variable mass components based on new total
        val updatedComponents = calculationService.updateVariableComponents(components, totalMassGrams)
        
        // Save updated components
        updatedComponents.forEach { component ->
            componentRepository.saveComponent(component)
        }
        
        // Update inventory item with new total mass
        val updatedItem = existingItem.copy(
            totalMeasuredMass = totalMassGrams,
            lastUpdated = LocalDateTime.now()
        )
        
        saveInventoryItem(updatedItem)
    }
    
    /**
     * Add a component to an inventory item
     */
    fun addComponentToInventoryItem(trayUid: String, componentId: String) {
        // Validate that component exists before adding
        val component = componentRepository.getComponent(componentId)
        if (component == null) {
            android.util.Log.e(TAG, "Cannot add component $componentId: component not found")
            throw IllegalArgumentException("Component with ID '$componentId' not found")
        }
        
        val existingItem = getInventoryItem(trayUid)
        val inventoryItem = existingItem?.withComponent(componentId) ?: InventoryItem(
            trayUid = trayUid,
            components = listOf(componentId),
            totalMeasuredMass = null,
            measurements = emptyList(),
            lastUpdated = LocalDateTime.now()
        )
        
        saveInventoryItem(inventoryItem)
        android.util.Log.d(TAG, "Successfully added component '${component.name}' to inventory item $trayUid")
    }
    
    /**
     * Remove a component from an inventory item
     */
    fun removeComponentFromInventoryItem(trayUid: String, componentId: String) {
        val existingItem = getInventoryItem(trayUid) ?: return
        val updatedItem = existingItem.copy(
            components = existingItem.components.filter { it != componentId },
            lastUpdated = LocalDateTime.now()
        )
        saveInventoryItem(updatedItem)
    }
    
    /**
     * Update component mass with bidirectional synchronisation
     */
    fun updateComponentMass(trayUid: String, componentId: String, newMass: Float, newFullMass: Float? = null) {
        val existingItem = getInventoryItem(trayUid) ?: return
        
        // Get the component and update its mass
        val component = componentRepository.getComponent(componentId) ?: return
        var updatedComponent = component.withUpdatedMass(newMass)
        if (newFullMass != null && component.variableMass) {
            updatedComponent = updatedComponent.withUpdatedFullMass(newFullMass)
        }
        
        // Save the updated component
        componentRepository.saveComponent(updatedComponent)
        
        // Recalculate total mass from all components
        val allComponents = componentRepository.getComponents()
            .filter { it.id in existingItem.components }
            .map { if (it.id == componentId) updatedComponent else it }
        
        val newTotalMass = allComponents.sumOf { (it.massGrams ?: 0f).toDouble() }.toFloat()
        
        // Update inventory item with new total
        val updatedItem = existingItem.copy(
            totalMeasuredMass = newTotalMass,
            lastUpdated = LocalDateTime.now()
        )
        saveInventoryItem(updatedItem)
    }
    
    /**
     * Update total mass with proportional distribution to variable components
     */
    fun updateTotalMassWithDistribution(trayUid: String, newTotalMass: Float) {
        val existingItem = getInventoryItem(trayUid) ?: return
        
        // Get all components for this inventory item
        val components = componentRepository.getComponents()
            .filter { it.id in existingItem.components }
        
        if (components.isEmpty()) {
            // No components, just update total
            val updatedItem = existingItem.copy(
                totalMeasuredMass = newTotalMass,
                lastUpdated = LocalDateTime.now()
            )
            saveInventoryItem(updatedItem)
            return
        }
        
        // Calculate mass distribution
        val fixedMass = components.filter { !it.variableMass }
            .sumOf { (it.massGrams ?: 0f).toDouble() }.toFloat()
        val availableForVariable = newTotalMass - fixedMass
        
        if (availableForVariable < 0) {
            // Not enough mass for fixed components - don't update
            return
        }
        
        val variableComponents = components.filter { it.variableMass }
        if (variableComponents.isNotEmpty()) {
            val currentVariableMass = variableComponents.sumOf { (it.massGrams ?: 0f).toDouble() }.toFloat()
            
            if (currentVariableMass > 0) {
                // Distribute proportionally
                val ratio = availableForVariable / currentVariableMass
                variableComponents.forEach { component ->
                    val newComponentMass = (component.massGrams ?: 0f) * ratio
                    val updatedComponent = component.withUpdatedMass(newComponentMass)
                    componentRepository.saveComponent(updatedComponent)
                }
            } else {
                // Distribute equally among variable components
                val equalShare = availableForVariable / variableComponents.size
                variableComponents.forEach { component ->
                    val updatedComponent = component.withUpdatedMass(equalShare)
                    componentRepository.saveComponent(updatedComponent)
                }
            }
        }
        
        // Update inventory item
        val updatedItem = existingItem.copy(
            totalMeasuredMass = newTotalMass,
            lastUpdated = LocalDateTime.now()
        )
        saveInventoryItem(updatedItem)
    }
    
    /**
     * Get components for an inventory item with current data
     */
    fun getInventoryItemComponents(trayUid: String): List<Component> {
        val inventoryItem = getInventoryItem(trayUid) ?: return emptyList()
        return componentRepository.getComponents()
            .filter { it.id in inventoryItem.components }
    }
    
    /**
     * Set up automatic Bambu component configuration for an inventory item
     * This method is resilient and will ALWAYS create components, even with incomplete data
     */
    fun setupBambuComponents(
        trayUid: String,
        filamentInfo: FilamentInfo,
        includeRefillableSpool: Boolean = false
    ): List<Component> {
        android.util.Log.i(TAG, "Setting up Bambu components for trayUid=$trayUid, type=${filamentInfo.filamentType}, color=${filamentInfo.colorName}")
        
        try {
            // Determine mass with comprehensive fallback strategy
            var filamentMass: Float
            val skuMass = getFilamentMassFromSku(filamentInfo.filamentType, filamentInfo.colorName)
            
            if (skuMass != null) {
                filamentMass = skuMass
                android.util.Log.d(TAG, "Using SKU mass: ${filamentMass}g")
            } else {
                filamentMass = getDefaultMassByMaterial(filamentInfo.filamentType)
                android.util.Log.d(TAG, "Using default mass for ${filamentInfo.filamentType}: ${filamentMass}g")
            }
            
            // Create filament component with resilient data handling
            val finalFilamentComponent = createFilamentComponent(
                filamentType = filamentInfo.filamentType.takeIf { it.isNotBlank() } ?: "PLA_BASIC",
                colorName = filamentInfo.colorName.takeIf { it.isNotBlank() } ?: "Unknown Color",
                estimatedMass = skuMass ?: filamentMass // Use SKU mass as full mass when available, otherwise use current mass
            )
            
            // Save the filament component
            componentRepository.saveComponent(finalFilamentComponent)
            android.util.Log.d(TAG, "Created filament component: ${finalFilamentComponent.id}")
            
            // Get built-in components (these should always work)
            val coreComponent = try {
                getBambuCoreComponent()
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error getting core component, creating fallback", e)
                createBambuCoreComponent().also {
                    componentRepository.saveComponent(it)
                }
            }
            
            val spoolComponent = if (includeRefillableSpool) {
                try {
                    getBambuSpoolComponent()
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error getting spool component, creating fallback", e)
                    createBambuSpoolComponent().also {
                        componentRepository.saveComponent(it)
                    }
                }
            } else null
            
            // Build component list
            val components = listOfNotNull(
                finalFilamentComponent,
                coreComponent,
                spoolComponent
            )
            
            android.util.Log.d(TAG, "Created ${components.size} components: ${components.map { it.name }}")
            
            // Add components to inventory item with error handling
            try {
                val existingItem = getInventoryItem(trayUid)
                val setupNotes = buildString {
                    append("Auto-configured from Bambu scan")
                    if (skuMass != null) {
                        append(" (SKU data: ${skuMass}g)")
                    } else {
                        append(" (default mass: ${filamentMass}g)")
                    }
                    if (includeRefillableSpool) {
                        append(" with refillable spool")
                    }
                }
                
                val updatedItem = existingItem?.copy(
                    components = components.map { it.id },
                    lastUpdated = LocalDateTime.now(),
                    notes = existingItem.notes?.let { "$it; $setupNotes" } ?: setupNotes
                ) ?: InventoryItem(
                    trayUid = trayUid,
                    components = components.map { it.id },
                    totalMeasuredMass = null,
                    measurements = emptyList(),
                    lastUpdated = LocalDateTime.now(),
                    notes = setupNotes
                )
                
                saveInventoryItem(updatedItem)
                android.util.Log.i(TAG, "Successfully set up inventory item for trayUid=$trayUid with ${components.size} components")
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error saving inventory item, but components were created", e)
                // Don't throw - components are created successfully
            }
            
            return components
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Critical error in setupBambuComponents for trayUid=$trayUid", e)
            
            // Emergency fallback - create minimal components
            android.util.Log.w(TAG, "Attempting emergency fallback component creation")
            return createEmergencyFallbackComponents(trayUid, filamentInfo, includeRefillableSpool)
        }
    }
    
    /**
     * Emergency fallback component creation when everything else fails
     */
    private fun createEmergencyFallbackComponents(
        trayUid: String,
        filamentInfo: FilamentInfo,
        includeRefillableSpool: Boolean
    ): List<Component> {
        try {
            android.util.Log.w(TAG, "Creating emergency fallback components")
            
            // Create minimal filament component with hardcoded defaults
            val componentId = "emergency_filament_${System.currentTimeMillis()}"
            val emergencyFilamentComponent = Component(
                id = componentId,
                uniqueIdentifier = "emergency_$trayUid",
                name = "${filamentInfo.filamentType.takeIf { it.isNotBlank() } ?: "PLA"} - ${filamentInfo.colorName.takeIf { it.isNotBlank() } ?: "Unknown"}",
                category = "filament",
                massGrams = 1000f, // Default 1kg
                variableMass = true,
                manufacturer = "Bambu Lab",
                description = "Emergency fallback filament component",
                fullMassGrams = 1000f
            )
            
            // Save emergency component
            componentRepository.saveComponent(emergencyFilamentComponent)
            
            // Create minimal inventory item
            val emergencyItem = InventoryItem(
                trayUid = trayUid,
                components = listOf(componentId),
                totalMeasuredMass = null,
                measurements = emptyList(),
                lastUpdated = LocalDateTime.now(),
                notes = "Emergency fallback configuration - please review component setup"
            )
            
            saveInventoryItem(emergencyItem)
            
            android.util.Log.w(TAG, "Emergency fallback completed for trayUid=$trayUid")
            return listOf(emergencyFilamentComponent)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Even emergency fallback failed for trayUid=$trayUid", e)
            return emptyList()
        }
    }
    
    // === Filament Status Calculation ===
    
    /**
     * Calculate current filament status for an inventory item
     */
    fun calculateFilamentStatus(trayUid: String): FilamentStatus {
        val inventoryItem = getInventoryItem(trayUid)
        if (inventoryItem == null) {
            return FilamentStatus(
                remainingMassGrams = 0f,
                remainingPercentage = 0f,
                consumedMassGrams = 0f,
                lastMeasurement = null,
                components = emptyList(),
                calculationSuccess = false,
                errorMessage = "Inventory item not found"
            )
        }
        
        // Get components for this inventory item
        val components = componentRepository.getComponents()
            .filter { it.id in inventoryItem.components }
        
        if (components.isEmpty()) {
            return FilamentStatus(
                remainingMassGrams = 0f,
                remainingPercentage = 0f,
                consumedMassGrams = 0f,
                lastMeasurement = inventoryItem.latestMeasurement,
                components = emptyList(),
                calculationSuccess = false,
                errorMessage = "No components defined"
            )
        }
        
        // Validate component combination
        val validation = calculationService.validateComponents(components)
        if (!validation.valid) {
            return FilamentStatus(
                remainingMassGrams = 0f,
                remainingPercentage = 0f,
                consumedMassGrams = 0f,
                lastMeasurement = inventoryItem.latestMeasurement,
                components = components,
                calculationSuccess = false,
                errorMessage = validation.message
            )
        }
        
        val latestMeasurement = inventoryItem.latestMeasurement
        val filamentComponents = components.filter { it.variableMass }
        val currentFilamentMass = filamentComponents.sumOf { (it.massGrams ?: 0f).toDouble() }.toFloat()
        
        // If we don't have measurements, return current component mass
        if (latestMeasurement == null || inventoryItem.totalMeasuredMass == null) {
            return FilamentStatus(
                remainingMassGrams = currentFilamentMass,
                remainingPercentage = 1f, // Unknown percentage
                consumedMassGrams = 0f,
                lastMeasurement = latestMeasurement,
                components = components,
                calculationSuccess = true,
                errorMessage = null
            )
        }
        
        // Get initial filament mass (from first measurement or SKU data)
        val initialFilamentMass = findInitialFilamentMass(inventoryItem, components)
        
        if (initialFilamentMass == null || initialFilamentMass <= 0) {
            return FilamentStatus(
                remainingMassGrams = currentFilamentMass,
                remainingPercentage = 1f, // Unknown percentage
                consumedMassGrams = 0f,
                lastMeasurement = latestMeasurement,
                components = components,
                calculationSuccess = true,
                errorMessage = null
            )
        }
        
        val consumedMass = maxOf(0f, initialFilamentMass - currentFilamentMass)
        val remainingPercentage = if (initialFilamentMass > 0) {
            maxOf(0f, minOf(1f, currentFilamentMass / initialFilamentMass))
        } else 1f
        
        return FilamentStatus(
            remainingMassGrams = currentFilamentMass,
            remainingPercentage = remainingPercentage,
            consumedMassGrams = consumedMass,
            lastMeasurement = latestMeasurement,
            components = components,
            calculationSuccess = true,
            errorMessage = null
        )
    }
    
    /**
     * Find the initial filament mass from measurements or SKU data
     */
    private fun findInitialFilamentMass(
        inventoryItem: InventoryItem, 
        components: List<Component>
    ): Float? {
        // Try to get from earliest measurement
        val earliestMeasurement = inventoryItem.measurements.minByOrNull { it.measuredAt }
        if (earliestMeasurement != null && earliestMeasurement.measurementType == MeasurementType.FULL_WEIGHT) {
            val fixedComponentsMass = components.filter { !it.variableMass }
                .sumOf { (it.massGrams ?: 0f).toDouble() }.toFloat()
            return earliestMeasurement.measuredMassGrams - fixedComponentsMass
        }
        
        // Fallback to current filament component mass (from SKU or default)
        return components.filter { it.variableMass }
            .sumOf { (it.massGrams ?: 0f).toDouble() }.toFloat()
            .takeIf { it > 0 }
    }
    
    
    /**
     * Get all inventory items with calculated status
     */
    fun getInventoryItemsWithStatus(): List<Pair<InventoryItem, FilamentStatus>> {
        return getInventoryItems().map { item ->
            item to calculateFilamentStatus(item.trayUid)
        }
    }
    
    companion object {
        private const val INVENTORY_ITEMS_KEY = "inventory_items"
        private const val TAG = "InventoryRepository"
    }
}