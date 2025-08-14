package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.ScanHistory
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ScanHistoryRepository(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("scan_history", Context.MODE_PRIVATE)
    
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
    
    private val maxHistorySize = 100 // Keep last 100 scans
    
    fun saveScan(scanHistory: ScanHistory) {
        val scans = getAllScans().toMutableList()
        
        // Add new scan with current timestamp if not set
        val scanWithTimestamp = if (scanHistory.timestamp == LocalDateTime.MIN) {
            scanHistory.copy(timestamp = LocalDateTime.now())
        } else {
            scanHistory
        }
        
        scans.add(0, scanWithTimestamp) // Add to beginning (most recent first)
        
        // Keep only the most recent scans
        if (scans.size > maxHistorySize) {
            scans.subList(maxHistorySize, scans.size).clear()
        }
        
        // Save to SharedPreferences
        val scansJson = gson.toJson(scans)
        sharedPreferences.edit()
            .putString("scans", scansJson)
            .apply()
    }
    
    fun getAllScans(): List<ScanHistory> {
        val scansJson = sharedPreferences.getString("scans", null) ?: return emptyList()
        
        return try {
            val type = object : TypeToken<List<ScanHistory>>() {}.type
            gson.fromJson(scansJson, type) ?: emptyList()
        } catch (e: JsonSyntaxException) {
            // If data is corrupted, return empty list and clear storage
            clearHistory()
            emptyList()
        }
    }
    
    fun getScansByResult(result: com.bscan.model.ScanResult): List<ScanHistory> {
        return getAllScans().filter { it.scanResult == result }
    }
    
    fun getSuccessfulScans(): List<ScanHistory> {
        return getScansByResult(com.bscan.model.ScanResult.SUCCESS)
    }
    
    fun getFailedScans(): List<ScanHistory> {
        return getAllScans().filter { it.scanResult != com.bscan.model.ScanResult.SUCCESS }
    }
    
    fun getScanByUid(uid: String): List<ScanHistory> {
        return getAllScans().filter { it.uid == uid }
    }
    
    fun clearHistory() {
        sharedPreferences.edit()
            .remove("scans")
            .apply()
    }
    
    fun getHistoryCount(): Int {
        return getAllScans().size
    }
    
    fun getSuccessRate(): Float {
        val allScans = getAllScans()
        if (allScans.isEmpty()) return 0f
        
        val successfulScans = allScans.count { it.scanResult == com.bscan.model.ScanResult.SUCCESS }
        return successfulScans.toFloat() / allScans.size
    }
}