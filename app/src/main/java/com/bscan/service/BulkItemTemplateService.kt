package com.bscan.service

import com.bscan.model.graph.entities.InventoryItem
import com.bscan.model.graph.entities.Virtual
import com.bscan.model.graph.entities.PhysicalComponent
import com.bscan.model.graph.entities.TrackingMode
import com.bscan.repository.GraphRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Service for creating and managing bulk item templates and presets
 * Provides ready-to-use templates for common bulk items like screws, nuts, etc.
 */
class BulkItemTemplateService(
    private val graphRepository: GraphRepository
) {
    
    /**
     * Create inventory item from bulk template
     */
    suspend fun createFromTemplate(
        template: BulkItemTemplate,
        label: String,
        initialQuantity: Int = 0,
        containerWeight: Float? = null
    ): CreateFromTemplateResult = withContext(Dispatchers.IO) {
        
        try {
            // Create the inventory item
            val inventoryItem = InventoryItem(
                label = label,
                trackingMode = TrackingMode.DISCRETE // Bulk items are usually discrete units
            ).apply {
                // Copy template properties
                unitWeight = template.estimatedUnitWeight
                calibrationMethod = "TEMPLATE_ESTIMATED"
                calibrationConfidence = template.confidenceLevel
                isConsumable = true
                currentQuantity = initialQuantity.toFloat()
                
                // Container setup
                if (containerWeight != null) {
                    this.containerWeight = containerWeight
                    calibrationConfidence = (template.confidenceLevel + 15f).coerceAtMost(85f)
                }
                
                lastCalibratedAt = LocalDateTime.now().toString()
                
                // Set template-specific properties
                setProperty("material", template.material)
                setProperty("size", template.size)
                setProperty("category", template.category)
                setProperty("density", template.density)
                setProperty("templateId", template.id)
                setProperty("estimatedWeight", template.estimatedUnitWeight)
            }
            
            graphRepository.addEntity(inventoryItem)
            
            // Create template category entity if it doesn't exist
            val categoryEntity = getOrCreateCategory(template.category)
            
            // Create physical component representation
            val physicalComponent = PhysicalComponent(
                label = "$label (Physical)"
            ).apply {
                setProperty("material", template.material)
                setProperty("size", template.size)
                setProperty("weight", template.estimatedUnitWeight)
                setProperty("category", template.category)
                setProperty("templateId", template.id)
            }
            
            graphRepository.addEntity(physicalComponent)
            
            // Link inventory item to physical component and category
            // (Note: Using manual edge creation for explicit relationships)
            
            CreateFromTemplateResult(
                success = true,
                inventoryItem = inventoryItem,
                physicalComponent = physicalComponent,
                category = categoryEntity,
                template = template
            )
            
        } catch (e: Exception) {
            CreateFromTemplateResult(
                success = false,
                error = "Failed to create from template: ${e.message}"
            )
        }
    }
    
    /**
     * Get all available bulk item templates
     */
    fun getAllTemplates(): List<BulkItemTemplate> {
        return getBulkItemTemplates()
    }
    
    /**
     * Get templates by category
     */
    fun getTemplatesByCategory(category: String): List<BulkItemTemplate> {
        return getBulkItemTemplates().filter { it.category == category }
    }
    
    /**
     * Get template by ID
     */
    fun getTemplateById(templateId: String): BulkItemTemplate? {
        return getBulkItemTemplates().find { it.id == templateId }
    }
    
    /**
     * Get all available categories
     */
    fun getAvailableCategories(): List<String> {
        return getBulkItemTemplates().map { it.category }.distinct().sorted()
    }
    
    /**
     * Create or get existing category entity
     */
    private suspend fun getOrCreateCategory(categoryName: String): Virtual {
        // Try to find existing category
        val existingCategories = graphRepository.getEntitiesByType("virtual")
        val existingCategory = existingCategories.find { 
            it.label == categoryName && it.getProperty<String>("virtualType") == "bulk_item_category"
        }
        
        if (existingCategory != null) {
            return existingCategory as Virtual
        }
        
        // Create new category
        val category = Virtual(
            virtualType = "bulk_item_category",
            label = categoryName
        ).apply {
            setProperty("description", "Category for bulk items: $categoryName")
        }
        
        graphRepository.addEntity(category)
        return category
    }
    
    /**
     * Built-in bulk item templates
     */
    private fun getBulkItemTemplates(): List<BulkItemTemplate> {
        return listOf(
            // Metric screws - commonly used
            BulkItemTemplate(
                id = "m3_screw_8mm",
                name = "M3 x 8mm Button Head Cap Screw",
                category = "Screws",
                material = "Stainless Steel",
                size = "M3 x 8mm",
                estimatedUnitWeight = 0.65f, // grams
                confidenceLevel = 70f,
                density = 7.9f, // g/cm³ for stainless steel
                description = "Standard M3 button head cap screw, 8mm length"
            ),
            
            BulkItemTemplate(
                id = "m3_screw_12mm",
                name = "M3 x 12mm Button Head Cap Screw",
                category = "Screws",
                material = "Stainless Steel",
                size = "M3 x 12mm",
                estimatedUnitWeight = 0.85f,
                confidenceLevel = 70f,
                density = 7.9f,
                description = "Standard M3 button head cap screw, 12mm length"
            ),
            
            BulkItemTemplate(
                id = "m3_screw_16mm",
                name = "M3 x 16mm Button Head Cap Screw",
                category = "Screws",
                material = "Stainless Steel",
                size = "M3 x 16mm",
                estimatedUnitWeight = 1.1f,
                confidenceLevel = 70f,
                density = 7.9f,
                description = "Standard M3 button head cap screw, 16mm length"
            ),
            
            BulkItemTemplate(
                id = "m4_screw_8mm",
                name = "M4 x 8mm Button Head Cap Screw",
                category = "Screws",
                material = "Stainless Steel",
                size = "M4 x 8mm",
                estimatedUnitWeight = 1.2f,
                confidenceLevel = 70f,
                density = 7.9f,
                description = "Standard M4 button head cap screw, 8mm length"
            ),
            
            BulkItemTemplate(
                id = "m4_screw_12mm",
                name = "M4 x 12mm Button Head Cap Screw",
                category = "Screws",
                material = "Stainless Steel",
                size = "M4 x 12mm",
                estimatedUnitWeight = 1.6f,
                confidenceLevel = 70f,
                density = 7.9f,
                description = "Standard M4 button head cap screw, 12mm length"
            ),
            
            BulkItemTemplate(
                id = "m5_screw_12mm",
                name = "M5 x 12mm Button Head Cap Screw",
                category = "Screws",
                material = "Stainless Steel",
                size = "M5 x 12mm",
                estimatedUnitWeight = 2.4f,
                confidenceLevel = 70f,
                density = 7.9f,
                description = "Standard M5 button head cap screw, 12mm length"
            ),
            
            // Nuts
            BulkItemTemplate(
                id = "m3_hex_nut",
                name = "M3 Hex Nut",
                category = "Nuts",
                material = "Stainless Steel",
                size = "M3",
                estimatedUnitWeight = 0.4f,
                confidenceLevel = 75f,
                density = 7.9f,
                description = "Standard M3 hex nut"
            ),
            
            BulkItemTemplate(
                id = "m4_hex_nut",
                name = "M4 Hex Nut",
                category = "Nuts",
                material = "Stainless Steel",
                size = "M4",
                estimatedUnitWeight = 0.8f,
                confidenceLevel = 75f,
                density = 7.9f,
                description = "Standard M4 hex nut"
            ),
            
            BulkItemTemplate(
                id = "m5_hex_nut",
                name = "M5 Hex Nut",
                category = "Nuts",
                material = "Stainless Steel",
                size = "M5",
                estimatedUnitWeight = 1.4f,
                confidenceLevel = 75f,
                density = 7.9f,
                description = "Standard M5 hex nut"
            ),
            
            // Washers
            BulkItemTemplate(
                id = "m3_flat_washer",
                name = "M3 Flat Washer",
                category = "Washers",
                material = "Stainless Steel",
                size = "M3",
                estimatedUnitWeight = 0.12f,
                confidenceLevel = 65f, // Lower confidence for very light items
                density = 7.9f,
                description = "Standard M3 flat washer"
            ),
            
            BulkItemTemplate(
                id = "m4_flat_washer",
                name = "M4 Flat Washer",
                category = "Washers",
                material = "Stainless Steel",
                size = "M4",
                estimatedUnitWeight = 0.2f,
                confidenceLevel = 65f,
                density = 7.9f,
                description = "Standard M4 flat washer"
            ),
            
            BulkItemTemplate(
                id = "m5_flat_washer",
                name = "M5 Flat Washer",
                category = "Washers",
                material = "Stainless Steel",
                size = "M5",
                estimatedUnitWeight = 0.35f,
                confidenceLevel = 65f,
                density = 7.9f,
                description = "Standard M5 flat washer"
            ),
            
            // Electronic components
            BulkItemTemplate(
                id = "resistor_1_4w",
                name = "1/4W Resistor",
                category = "Electronics",
                material = "Carbon Film",
                size = "1/4W",
                estimatedUnitWeight = 0.04f,
                confidenceLevel = 60f, // Very light, harder to weigh accurately
                density = 1.5f,
                description = "Standard 1/4 watt through-hole resistor"
            ),
            
            BulkItemTemplate(
                id = "led_5mm",
                name = "5mm LED",
                category = "Electronics",
                material = "Plastic/Metal",
                size = "5mm",
                estimatedUnitWeight = 0.15f,
                confidenceLevel = 65f,
                density = 2.0f,
                description = "Standard 5mm through-hole LED"
            ),
            
            // Hardware - general
            BulkItemTemplate(
                id = "heat_insert_m3",
                name = "M3 Heat Insert",
                category = "Inserts",
                material = "Brass",
                size = "M3",
                estimatedUnitWeight = 0.8f,
                confidenceLevel = 75f,
                density = 8.5f, // g/cm³ for brass
                description = "M3 threaded heat insert for 3D printing"
            ),
            
            BulkItemTemplate(
                id = "heat_insert_m4",
                name = "M4 Heat Insert",
                category = "Inserts",
                material = "Brass",
                size = "M4",
                estimatedUnitWeight = 1.5f,
                confidenceLevel = 75f,
                density = 8.5f,
                description = "M4 threaded heat insert for 3D printing"
            ),
            
            // Generic templates for unknown items
            BulkItemTemplate(
                id = "generic_small",
                name = "Generic Small Item",
                category = "Generic",
                material = "Mixed",
                size = "Small",
                estimatedUnitWeight = 0.5f,
                confidenceLevel = 50f,
                density = 3.0f,
                description = "Generic template for small bulk items (0.1g - 1g)"
            ),
            
            BulkItemTemplate(
                id = "generic_medium",
                name = "Generic Medium Item",
                category = "Generic",
                material = "Mixed",
                size = "Medium",
                estimatedUnitWeight = 3.0f,
                confidenceLevel = 50f,
                density = 3.0f,
                description = "Generic template for medium bulk items (1g - 10g)"
            ),
            
            BulkItemTemplate(
                id = "generic_large",
                name = "Generic Large Item",
                category = "Generic",
                material = "Mixed",
                size = "Large",
                estimatedUnitWeight = 15.0f,
                confidenceLevel = 50f,
                density = 3.0f,
                description = "Generic template for large bulk items (10g+)"
            )
        )
    }
}

/**
 * Bulk item template definition
 */
data class BulkItemTemplate(
    val id: String,
    val name: String,
    val category: String,
    val material: String,
    val size: String,
    val estimatedUnitWeight: Float, // in grams
    val confidenceLevel: Float, // percentage (0-100)
    val density: Float, // g/cm³
    val description: String
)

/**
 * Result of creating inventory item from template
 */
data class CreateFromTemplateResult(
    val success: Boolean,
    val inventoryItem: InventoryItem? = null,
    val physicalComponent: PhysicalComponent? = null,
    val category: Virtual? = null,
    val template: BulkItemTemplate? = null,
    val error: String? = null
)