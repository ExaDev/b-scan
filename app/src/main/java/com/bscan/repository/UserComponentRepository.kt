package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.bscan.model.UserComponentOverlay
import com.bscan.model.UserModificationRecord
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Repository for persisting user modifications and customisations to components.
 * This is separate from generated components, allowing user data to persist
 * while generation logic can be updated freely.
 */
class UserComponentRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, JsonSerializer<LocalDateTime> { src, _, _ ->
            JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        })
        .registerTypeAdapter(LocalDateTime::class.java, JsonDeserializer<LocalDateTime> { json, _, _ ->
            LocalDateTime.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        })
        .create()
    
    companion object {
        private const val PREFS_NAME = "user_component_overlays"
        private const val OVERLAYS_KEY = "overlays"
        private const val MODIFICATIONS_KEY = "modifications"
        private const val TAG = "UserComponentRepository"
    }
    
    /**
     * Get all user component overlays
     */
    fun getAllOverlays(): List<UserComponentOverlay> {
        val json = sharedPreferences.getString(OVERLAYS_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<UserComponentOverlay>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user component overlays", e)
            emptyList()
        }
    }
    
    /**
     * Get overlay for a specific component ID
     */
    fun getOverlay(componentId: String): UserComponentOverlay? {
        return getAllOverlays().find { it.componentId == componentId && !it.isDeleted }
    }
    
    /**
     * Save or update a user component overlay
     */
    fun saveOverlay(overlay: UserComponentOverlay) {
        val overlays = getAllOverlays().toMutableList()
        val existingIndex = overlays.indexOfFirst { it.componentId == overlay.componentId }
        
        if (existingIndex >= 0) {
            // Update existing overlay
            overlays[existingIndex] = overlay.withUpdate()
            Log.d(TAG, "Updated overlay for component: ${overlay.componentId}")
        } else {
            // Add new overlay
            overlays.add(overlay)
            Log.d(TAG, "Created new overlay for component: ${overlay.componentId}")
        }
        
        saveAllOverlays(overlays)
    }
    
    /**
     * Delete an overlay (soft delete by default)
     */
    fun deleteOverlay(componentId: String, hardDelete: Boolean = false) {
        val overlays = getAllOverlays().toMutableList()
        
        if (hardDelete) {
            // Remove completely from storage
            val removed = overlays.removeAll { it.componentId == componentId }
            if (removed) {
                Log.d(TAG, "Hard deleted overlay for component: $componentId")
                saveAllOverlays(overlays)
            }
        } else {
            // Soft delete - mark as deleted
            val existingIndex = overlays.indexOfFirst { it.componentId == componentId }
            if (existingIndex >= 0) {
                overlays[existingIndex] = overlays[existingIndex].markDeleted()
                Log.d(TAG, "Soft deleted overlay for component: $componentId")
                saveAllOverlays(overlays)
            }
        }
    }
    
    /**
     * Archive an overlay
     */
    fun archiveOverlay(componentId: String) {
        val overlays = getAllOverlays().toMutableList()
        val existingIndex = overlays.indexOfFirst { it.componentId == componentId }
        
        if (existingIndex >= 0) {
            overlays[existingIndex] = overlays[existingIndex].markArchived()
            Log.d(TAG, "Archived overlay for component: $componentId")
            saveAllOverlays(overlays)
        }
    }
    
    /**
     * Get all active overlays (not deleted)
     */
    fun getActiveOverlays(): List<UserComponentOverlay> {
        return getAllOverlays().filter { !it.isDeleted }
    }
    
    /**
     * Get all user-created components (fully user-created, not modifications)
     */
    fun getUserCreatedOverlays(): List<UserComponentOverlay> {
        return getActiveOverlays().filter { it.isUserCreated }
    }
    
    /**
     * Get all modified components (modifications of generated components)
     */
    fun getModifiedOverlays(): List<UserComponentOverlay> {
        return getActiveOverlays().filter { !it.isUserCreated && it.hasModifications() }
    }
    
    /**
     * Check if a component has user modifications
     */
    fun hasModifications(componentId: String): Boolean {
        val overlay = getOverlay(componentId)
        return overlay != null && overlay.hasModifications()
    }
    
    /**
     * Update usage statistics for a component
     */
    fun recordUsage(componentId: String) {
        val overlay = getOverlay(componentId) ?: UserComponentOverlay(componentId = componentId)
        val updatedOverlay = overlay.copy(
            lastUsed = LocalDateTime.now(),
            usageCount = overlay.usageCount + 1,
            modifiedAt = LocalDateTime.now(),
            version = overlay.version + 1
        )
        saveOverlay(updatedOverlay)
    }
    
    /**
     * Get frequently used components (high usage count)
     */
    fun getFrequentlyUsedComponents(limit: Int = 10): List<UserComponentOverlay> {
        return getActiveOverlays()
            .filter { it.usageCount > 0 }
            .sortedByDescending { it.usageCount }
            .take(limit)
    }
    
    /**
     * Get recently used components
     */
    fun getRecentlyUsedComponents(limit: Int = 10): List<UserComponentOverlay> {
        return getActiveOverlays()
            .filter { it.lastUsed != null }
            .sortedByDescending { it.lastUsed }
            .take(limit)
    }
    
    /**
     * Get pinned components
     */
    fun getPinnedComponents(): List<UserComponentOverlay> {
        return getActiveOverlays().filter { it.isPinned }
    }
    
    /**
     * Search overlays by name, notes, or metadata
     */
    fun searchOverlays(query: String): List<UserComponentOverlay> {
        val lowerQuery = query.lowercase()
        return getActiveOverlays().filter { overlay ->
            overlay.nameOverride?.lowercase()?.contains(lowerQuery) == true ||
            overlay.userNotes.lowercase().contains(lowerQuery) ||
            overlay.metadataOverrides.values.any { it.lowercase().contains(lowerQuery) } ||
            overlay.metadataAdditions.values.any { it.lowercase().contains(lowerQuery) }
        }
    }
    
    /**
     * Clear all overlays (for testing/reset)
     */
    fun clearAllOverlays() {
        sharedPreferences.edit()
            .remove(OVERLAYS_KEY)
            .remove(MODIFICATIONS_KEY)
            .apply()
        Log.d(TAG, "Cleared all user component overlays")
    }
    
    /**
     * Export overlays for backup
     */
    fun exportOverlays(): String {
        return gson.toJson(getAllOverlays())
    }
    
    /**
     * Import overlays from backup (merges with existing)
     */
    fun importOverlays(json: String): Boolean {
        return try {
            val type = object : TypeToken<List<UserComponentOverlay>>() {}.type
            val importedOverlays: List<UserComponentOverlay> = gson.fromJson(json, type) ?: emptyList()
            
            val existingOverlays = getAllOverlays().toMutableList()
            
            // Merge imported overlays with existing ones
            importedOverlays.forEach { importedOverlay ->
                val existingIndex = existingOverlays.indexOfFirst { 
                    it.componentId == importedOverlay.componentId 
                }
                
                if (existingIndex >= 0) {
                    // Keep the more recent version
                    if (importedOverlay.version > existingOverlays[existingIndex].version) {
                        existingOverlays[existingIndex] = importedOverlay
                    }
                } else {
                    existingOverlays.add(importedOverlay)
                }
            }
            
            saveAllOverlays(existingOverlays)
            Log.d(TAG, "Imported ${importedOverlays.size} overlays")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error importing overlays", e)
            false
        }
    }
    
    /**
     * Save all overlays to storage
     */
    private fun saveAllOverlays(overlays: List<UserComponentOverlay>) {
        val json = gson.toJson(overlays)
        sharedPreferences.edit()
            .putString(OVERLAYS_KEY, json)
            .apply()
    }
    
    /**
     * Get storage statistics
     */
    fun getStorageStats(): Map<String, Any> {
        val overlays = getAllOverlays()
        return mapOf(
            "totalOverlays" to overlays.size,
            "activeOverlays" to overlays.count { !it.isDeleted },
            "userCreated" to overlays.count { it.isUserCreated },
            "modifications" to overlays.count { !it.isUserCreated && it.hasModifications() },
            "deleted" to overlays.count { it.isDeleted },
            "archived" to overlays.count { it.isArchived },
            "storageSize" to (sharedPreferences.getString(OVERLAYS_KEY, "")?.length ?: 0)
        )
    }
}