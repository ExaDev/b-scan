package com.bscan.service

import android.content.Context
import android.util.Log
import com.bscan.model.*
import com.bscan.repository.ComponentRepository
import com.bscan.repository.MappingsRepository
import java.time.LocalDateTime
import java.util.UUID

/**
 * Factory service for creating hierarchical component structures from Bambu RFID scans.
 * Automatically creates tray components with child components for RFID tags, filament, and core.
 */
class BambuComponentFactory(private val context: Context) {
    
    private val componentRepository = ComponentRepository(context)
    private val mappingsRepository = MappingsRepository(context)
    
    /**
     * Process an RFID scan and create/update the component hierarchy
     */
    fun processBambuRfidScan(
        tagUid: String,
        trayUid: String,
        filamentInfo: FilamentInfo
    ): Component? {
        try {
            Log.d(TAG, "Processing Bambu RFID scan for tray: $trayUid, tag: $tagUid")
            
            // Create RFID tag component
            val rfidTagComponent = createRfidTagComponent(tagUid, trayUid, filamentInfo)
            componentRepository.saveComponent(rfidTagComponent)
            
            // Find or create tray component
            var trayComponent = componentRepository.findInventoryByUniqueId(trayUid)
            
            if (trayComponent == null) {
                // Create new tray with all standard components
                trayComponent = createCompleteTrayComponent(trayUid, filamentInfo, rfidTagComponent.id)
                Log.i(TAG, "Created new tray component: ${trayComponent.name}")
            } else {
                // Add this RFID tag to existing tray
                trayComponent = trayComponent.withChildComponent(rfidTagComponent.id)
                componentRepository.saveComponent(trayComponent)
                Log.i(TAG, "Added RFID tag ${tagUid} to existing tray: ${trayComponent.name}")
            }
            
            return trayComponent
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing Bambu RFID scan", e)
            return null
        }
    }
    
    /**
     * Create a complete tray component with all standard child components
     */
    private fun createCompleteTrayComponent(
        trayUid: String,
        filamentInfo: FilamentInfo,
        rfidTagComponentId: String
    ): Component {
        
        // Create filament component
        val filamentComponent = createFilamentComponent(filamentInfo)
        componentRepository.saveComponent(filamentComponent)
        
        // Create core component
        val coreComponent = createCoreComponent()
        componentRepository.saveComponent(coreComponent)
        
        // Create spool component
        val spoolComponent = createSpoolComponent()
        componentRepository.saveComponent(spoolComponent)
        
        // Create tray component containing all others
        val trayComponent = Component(
            id = "tray_${System.currentTimeMillis()}",
            uniqueIdentifier = trayUid,
            name = "Bambu ${filamentInfo.filamentType} - ${filamentInfo.colorName}",
            category = "filament-tray",
            tags = listOf("bambu", "composite", "inventory-item"),
            childComponents = listOf(
                rfidTagComponentId,
                filamentComponent.id,
                coreComponent.id,
                spoolComponent.id
            ),
            massGrams = null, // Will be calculated from children
            manufacturer = "Bambu Lab",
            description = "Bambu filament tray with RFID tags, filament, and core",
            metadata = mapOf(
                "trayUid" to trayUid,
                "filamentType" to filamentInfo.filamentType,
                "colorName" to filamentInfo.colorName,
                "colorHex" to filamentInfo.colorHex,
                "source" to "bambu-scan"
            ),
            createdAt = System.currentTimeMillis(),
            lastUpdated = LocalDateTime.now()
        )
        
        // Update child components to reference this tray as parent
        listOf(filamentComponent, coreComponent, spoolComponent).forEach { child ->
            val updatedChild = child.copy(parentComponentId = trayComponent.id)
            componentRepository.saveComponent(updatedChild)
        }
        
        componentRepository.saveComponent(trayComponent)
        return trayComponent
    }
    
    /**
     * Create an RFID tag component
     */
    private fun createRfidTagComponent(
        tagUid: String,
        trayUid: String,
        filamentInfo: FilamentInfo
    ): Component {
        return Component(
            id = "rfid_tag_${tagUid}_${System.currentTimeMillis()}",
            uniqueIdentifier = tagUid,
            name = "RFID Tag $tagUid",
            category = "rfid-tag",
            tags = listOf("bambu", "identifier", "fixed-mass"),
            massGrams = 0.5f, // Negligible mass for RFID tag
            variableMass = false,
            manufacturer = "Bambu Lab",
            description = "Bambu Lab RFID tag for filament identification",
            metadata = mapOf(
                "tagUid" to tagUid,
                "trayUid" to trayUid,
                "filamentType" to filamentInfo.filamentType,
                "colorName" to filamentInfo.colorName,
                "scanTimestamp" to LocalDateTime.now().toString()
            )
        )
    }
    
    /**
     * Create a filament component with mass from SKU data
     */
    private fun createFilamentComponent(filamentInfo: FilamentInfo): Component {
        val filamentMass = getFilamentMassFromSku(filamentInfo.filamentType, filamentInfo.colorName)
        
        return Component(
            id = "filament_${System.currentTimeMillis()}",
            name = "${filamentInfo.filamentType} ${filamentInfo.colorName} Filament",
            category = "filament",
            tags = listOf("consumable", "variable-mass", "bambu"),
            massGrams = filamentMass,
            fullMassGrams = filamentMass,
            variableMass = true,
            manufacturer = "Bambu Lab",
            description = "Bambu Lab ${filamentInfo.filamentType} filament in ${filamentInfo.colorName}",
            metadata = mapOf(
                "filamentType" to filamentInfo.filamentType,
                "colorName" to filamentInfo.colorName,
                "colorHex" to filamentInfo.colorHex,
                "diameter" to filamentInfo.filamentDiameter.toString(),
                "source" to "sku-data"
            )
        )
    }
    
    /**
     * Create a cardboard core component
     */
    private fun createCoreComponent(): Component {
        return Component(
            id = "core_${System.currentTimeMillis()}",
            name = "Bambu Cardboard Core",
            category = "core",
            tags = listOf("reusable", "fixed-mass", "bambu"),
            massGrams = 33f,
            variableMass = false,
            manufacturer = "Bambu Lab",
            description = "Standard Bambu Lab cardboard core (33g)",
            metadata = mapOf(
                "material" to "cardboard",
                "standardWeight" to "33g"
            )
        )
    }
    
    /**
     * Create a refillable spool component
     */
    private fun createSpoolComponent(): Component {
        return Component(
            id = "spool_${System.currentTimeMillis()}",
            name = "Bambu Refillable Spool",
            category = "spool", 
            tags = listOf("reusable", "fixed-mass", "bambu"),
            massGrams = 212f,
            variableMass = false,
            manufacturer = "Bambu Lab",
            description = "Standard Bambu Lab refillable spool (212g)",
            metadata = mapOf(
                "material" to "plastic",
                "standardWeight" to "212g",
                "type" to "refillable"
            )
        )
    }
    
    /**
     * Get filament mass from SKU data with fallback to defaults
     */
    private fun getFilamentMassFromSku(filamentType: String, colorName: String): Float {
        return try {
            val bestMatch = mappingsRepository.findBestProductMatch(filamentType, colorName)
            if (bestMatch?.filamentWeightGrams != null) {
                Log.d(TAG, "Found SKU mass for $filamentType/$colorName: ${bestMatch.filamentWeightGrams}g")
                bestMatch.filamentWeightGrams
            } else {
                val defaultMass = getDefaultMassByMaterial(filamentType)
                Log.d(TAG, "Using default mass for $filamentType: ${defaultMass}g")
                defaultMass
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up SKU mass for $filamentType/$colorName", e)
            getDefaultMassByMaterial(filamentType)
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
     * Add additional components to an existing tray (e.g., bag, adapter)
     */
    fun addComponentToTray(trayUid: String, component: Component): Boolean {
        val tray = componentRepository.findInventoryByUniqueId(trayUid) ?: return false
        
        // Save the new component first
        componentRepository.saveComponent(component.copy(parentComponentId = tray.id))
        
        // Add to tray's children
        val updatedTray = tray.withChildComponent(component.id)
        componentRepository.saveComponent(updatedTray)
        
        Log.d(TAG, "Added component ${component.name} to tray $trayUid")
        return true
    }
    
    /**
     * Infer and add a component based on total weight measurement
     */
    fun inferAndAddComponent(
        trayUid: String,
        componentName: String,
        componentCategory: String,
        totalMeasuredMass: Float
    ): Component? {
        val tray = componentRepository.findInventoryByUniqueId(trayUid) ?: return null
        
        // Calculate known mass from existing children
        val knownMass = tray.childComponents.sumOf { childId ->
            componentRepository.getComponent(childId)?.massGrams?.toDouble() ?: 0.0
        }.toFloat()
        
        val inferredMass = totalMeasuredMass - knownMass
        if (inferredMass < 0) {
            Log.w(TAG, "Cannot infer component mass - total ($totalMeasuredMass) less than known mass ($knownMass)")
            return null
        }
        
        // Create inferred component
        val inferredComponent = Component(
            id = "${componentCategory}_${System.currentTimeMillis()}",
            name = componentName,
            category = componentCategory,
            tags = listOf("inferred", "fixed-mass"),
            massGrams = inferredMass,
            variableMass = false,
            inferredMass = true,
            description = "Component mass inferred from total measurement",
            metadata = mapOf(
                "inferredFrom" to "total_measurement",
                "totalMass" to totalMeasuredMass.toString(),
                "knownMass" to knownMass.toString()
            ),
            parentComponentId = tray.id
        )
        
        // Save and add to tray
        componentRepository.saveComponent(inferredComponent)
        val updatedTray = tray.withChildComponent(inferredComponent.id)
        componentRepository.saveComponent(updatedTray)
        
        Log.i(TAG, "Inferred and added component: $componentName (${inferredMass}g) to tray $trayUid")
        return inferredComponent
    }
    
    companion object {
        private const val TAG = "BambuComponentFactory"
    }
}