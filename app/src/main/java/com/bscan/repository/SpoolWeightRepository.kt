package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.SpoolWeight
import com.bscan.model.SpoolWeightStats
import com.bscan.model.WeightMeasurementType
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpoolWeightRepository(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("spool_weights", Context.MODE_PRIVATE)
    
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
                LocalDateTime.now()
            }
        }
    }
    
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, localDateTimeAdapter)
        .create()
    
    // StateFlow for reactive weight updates
    private val _weightUpdatesFlow = MutableStateFlow<SpoolWeight?>(null)
    val weightUpdatesFlow: StateFlow<SpoolWeight?> = _weightUpdatesFlow.asStateFlow()
    
    /**
     * Records a new weight measurement for a spool
     */
    fun recordWeight(spoolWeight: SpoolWeight) {
        val weights = getAllWeights().toMutableList()
        weights.add(spoolWeight)
        
        // Keep only recent measurements (last 1000 per spool to prevent unlimited growth)
        val weightsBySpoolId = weights.groupBy { it.spoolId }
        val prunedWeights = weightsBySpoolId.flatMap { (_, spoolWeights) ->
            spoolWeights.sortedByDescending { it.timestamp }.take(1000)
        }
        
        saveWeights(prunedWeights)
        _weightUpdatesFlow.value = spoolWeight // Notify observers
    }
    
    /**
     * Gets all weight measurements for a specific spool
     */
    fun getWeightsForSpool(spoolId: String): List<SpoolWeight> {
        return getAllWeights()
            .filter { it.spoolId == spoolId }
            .sortedByDescending { it.timestamp }
    }
    
    /**
     * Gets the most recent weight measurement for a spool
     */
    fun getLatestWeight(spoolId: String): SpoolWeight? {
        return getWeightsForSpool(spoolId).firstOrNull()
    }
    
    /**
     * Gets weight statistics for a specific spool
     */
    fun getSpoolStats(spoolId: String): SpoolWeightStats? {
        val weights = getWeightsForSpool(spoolId)
        if (weights.isEmpty()) return null
        
        val sortedWeights = weights.sortedBy { it.timestamp }
        val initialWeight = sortedWeights.first()
        val currentWeight = sortedWeights.last()
        
        val minWeight = weights.minByOrNull { it.weightGrams }?.weightGrams ?: 0f
        val maxWeight = weights.maxByOrNull { it.weightGrams }?.weightGrams ?: 0f
        
        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
            initialWeight.timestamp.toLocalDate(),
            currentWeight.timestamp.toLocalDate()
        ).coerceAtLeast(1)
        
        val totalUsage = initialWeight.weightGrams - currentWeight.weightGrams
        val averageUsagePerDay = if (daysBetween > 0) totalUsage / daysBetween else 0f
        
        return SpoolWeightStats(
            spoolId = spoolId,
            initialWeight = initialWeight.weightGrams,
            currentWeight = currentWeight.weightGrams,
            minimumWeight = minWeight,
            maximumWeight = maxWeight,
            totalUsage = totalUsage,
            measurementCount = weights.size,
            firstMeasurement = initialWeight.timestamp,
            lastMeasurement = currentWeight.timestamp,
            averageUsagePerDay = averageUsagePerDay
        )
    }
    
    /**
     * Gets all spools that have weight measurements
     */
    fun getTrackedSpools(): List<String> {
        return getAllWeights()
            .map { it.spoolId }
            .distinct()
            .sorted()
    }
    
    /**
     * Gets recent weight measurements across all spools (for dashboard/overview)
     */
    fun getRecentWeights(limit: Int = 50): List<SpoolWeight> {
        return getAllWeights()
            .sortedByDescending { it.timestamp }
            .take(limit)
    }
    
    /**
     * Gets weight measurements within a date range
     */
    fun getWeightsBetween(
        spoolId: String, 
        startDate: LocalDateTime, 
        endDate: LocalDateTime
    ): List<SpoolWeight> {
        return getWeightsForSpool(spoolId)
            .filter { weight ->
                weight.timestamp.isAfter(startDate) && weight.timestamp.isBefore(endDate)
            }
    }
    
    /**
     * Removes all weight data for a specific spool
     */
    fun clearSpoolWeights(spoolId: String) {
        val weights = getAllWeights().filter { it.spoolId != spoolId }
        saveWeights(weights)
        refreshFlows()
    }
    
    /**
     * Removes old weight measurements (older than specified days)
     */
    fun cleanupOldWeights(olderThanDays: Int = 365) {
        val cutoffDate = LocalDateTime.now().minusDays(olderThanDays.toLong())
        val recentWeights = getAllWeights().filter { it.timestamp.isAfter(cutoffDate) }
        saveWeights(recentWeights)
        refreshFlows()
    }
    
    /**
     * Clears all weight data
     */
    fun clearAllWeights() {
        sharedPreferences.edit().clear().apply()
        refreshFlows()
    }
    
    /**
     * Helper function to create spool ID from tray UID and tag UID
     */
    fun createSpoolId(trayUid: String, tagUid: String): String {
        return "${trayUid}_${tagUid}"
    }
    
    /**
     * Parses spool ID back into tray UID and tag UID
     */
    fun parseSpoolId(spoolId: String): Pair<String, String>? {
        val parts = spoolId.split("_", limit = 2)
        return if (parts.size == 2) {
            Pair(parts[0], parts[1])
        } else null
    }
    
    private fun getAllWeights(): List<SpoolWeight> {
        val weightsJson = sharedPreferences.getString("weights", null) ?: return emptyList()
        
        return try {
            val type = object : TypeToken<List<SpoolWeight>>() {}.type
            gson.fromJson(weightsJson, type) ?: emptyList()
        } catch (e: JsonSyntaxException) {
            // If data is corrupted, clear and return empty
            clearAllWeights()
            emptyList()
        }
    }
    
    private fun saveWeights(weights: List<SpoolWeight>) {
        val weightsJson = gson.toJson(weights)
        sharedPreferences.edit()
            .putString("weights", weightsJson)
            .apply()
    }
    
    private fun refreshFlows() {
        // Trigger flow update with null to indicate general data change
        _weightUpdatesFlow.value = null
    }
}