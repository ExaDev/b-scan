package com.bscan.utils

import com.google.gson.Gson
import java.io.File

/**
 * Default test data loader for B-Scan unit tests.
 * 
 * Uses real Bambu Lab RFID tag dumps from the git submodule at test-data/rfid-library/
 * as the primary data source for comprehensive testing.
 */
object RfidTestDataLoader {
    
    private val gson = Gson()
    
    /**
     * Get the path to the RFID library submodule
     */
    fun getTestDataPath(): File {
        val testDataPath = System.getProperty("test.data.path")
            ?: throw IllegalStateException("test.data.path system property not set")
        
        val rfidLibraryPath = File(testDataPath)
        if (!rfidLibraryPath.exists()) {
            throw IllegalStateException("RFID library not found at: ${rfidLibraryPath.absolutePath}\nRun: git submodule update --init --recursive")
        }
        
        return rfidLibraryPath
    }
    
    /**
     * Load all available RFID dump files from the submodule (BIN and JSON)
     * @return List of dump files from real Bambu Lab tags (822 BIN + 361 JSON files)
     */
    fun loadAllDumpFiles(): List<File> {
        val rfidLibrary = getTestDataPath()
        return rfidLibrary.walkTopDown()
            .filter { 
                it.name.endsWith("-dump.json") || 
                (it.name.endsWith(".bin") && !it.name.endsWith("-key.bin"))
            }
            .toList()
    }
    
    /**
     * Load only JSON dump files
     */
    fun loadJsonDumpFiles(): List<File> {
        val rfidLibrary = getTestDataPath()
        return rfidLibrary.walkTopDown()
            .filter { it.name.endsWith("-dump.json") }
            .toList()
    }
    
    /**
     * Load only BIN dump files (excluding key files)
     */
    fun loadBinDumpFiles(): List<File> {
        val rfidLibrary = getTestDataPath()
        return rfidLibrary.walkTopDown()
            .filter { it.name.endsWith(".bin") && !it.name.endsWith("-key.bin") }
            .toList()
    }
    
    /**
     * Load dump files for specific material type
     * @param material Material name (e.g. "PLA Basic", "PETG CF", "ABS")
     */
    fun loadByMaterial(material: String): List<File> {
        return loadAllDumpFiles().filter { file ->
            file.path.contains("/$material/", ignoreCase = true)
        }
    }
    
    /**
     * Load dump files for specific material category
     * @param category Material category (e.g. "PLA", "PETG", "ABS")
     */
    fun loadByCategory(category: String): List<File> {
        return loadAllDumpFiles().filter { file ->
            file.path.contains("/$category/", ignoreCase = true) && 
            !file.path.contains("/$category ", ignoreCase = true) // Avoid "PLA " matching "PLA Basic"
        }
    }
    
    /**
     * Load dump files for specific color
     * @param color Color name (e.g. "Black", "White", "Red")
     */
    fun loadByColor(color: String): List<File> {
        return loadAllDumpFiles().filter { file ->
            file.path.contains("/$color/", ignoreCase = true)
        }
    }
    
    /**
     * Get a representative sample of diverse materials for comprehensive testing
     */
    fun loadDiverseMaterialSample(): List<Pair<File, String>> {
        val samples = mutableListOf<Pair<File, String>>()
        
        // PLA variants
        findFirstTagOfType("PLA", "PLA Basic")?.let { samples.add(it to "PLA") }
        findFirstTagOfType("PLA", "PLA Matte")?.let { samples.add(it to "PLA") }
        findFirstTagOfType("PLA", "PLA Silk")?.let { samples.add(it to "PLA") }
        
        // PETG variants
        findFirstTagOfType("PETG", "PETG Basic")?.let { samples.add(it to "PETG") }
        findFirstTagOfType("PETG", "PETG CF")?.let { samples.add(it to "PETG") }
        
        // ABS
        findFirstTagOfType("ABS", "ABS")?.let { samples.add(it to "ABS") }
        
        // ASA
        findFirstTagOfType("ASA", "ASA")?.let { samples.add(it to "ASA") }
        
        // Support materials
        findFirstTagOfType("Support Material", "Support G")?.let { samples.add(it to "Support") }
        findFirstTagOfType("Support Material", "Support W")?.let { samples.add(it to "Support") }
        
        return samples
    }
    
    /**
     * Find first available tag of specific material type (BIN or JSON)
     */
    fun findFirstTagOfType(category: String, material: String): File? {
        val materialPath = File(getTestDataPath(), "$category/$material")
        if (!materialPath.exists()) return null
        
        return materialPath.walkTopDown()
            .firstOrNull { 
                it.name.endsWith("-dump.json") || 
                (it.name.endsWith(".bin") && !it.name.endsWith("-key.bin"))
            }
    }
    
    /**
     * Parse dump file into structured data (supports both BIN and JSON)
     */
    fun parseDumpFile(dumpFile: File): BambuDumpFormat {
        return when {
            dumpFile.name.endsWith("-dump.json") -> {
                val jsonContent = dumpFile.readText()
                gson.fromJson(jsonContent, BambuDumpFormat::class.java)
            }
            dumpFile.name.endsWith(".bin") -> {
                parseBinDumpFile(dumpFile)
            }
            else -> throw IllegalArgumentException("Unsupported file format: ${dumpFile.name}")
        }
    }
    
    /**
     * Parse BIN dump file into structured format
     */
    private fun parseBinDumpFile(binFile: File): BambuDumpFormat {
        val binData = binFile.readBytes()
        val actualSize = binData.size
        
        println("Processing BIN file: ${binFile.name} - size $actualSize bytes")
        
        // Handle different BIN file sizes
        val processedData = when {
            actualSize == 1024 -> binData // Standard Mifare Classic 1K
            actualSize == 1152 -> {
                // Extended format: 1024 bytes standard dump + 128 bytes metadata/keys
                println("Processing 1152-byte BIN file - extracting standard 1024-byte section")
                binData.take(1024).toByteArray()
            }
            actualSize == 768 -> {
                // Missing trailer blocks (48 data blocks * 16 bytes)
                println("Processing 768-byte BIN file - expanding to 1024 bytes")
                expandBinData(binData)
            }
            actualSize < 1024 -> {
                println("WARNING: BIN file smaller than expected ($actualSize bytes) - padding to 1024")
                binData + ByteArray(1024 - actualSize) // Pad with zeros
            }
            else -> {
                println("WARNING: Large BIN file ($actualSize bytes) - using first 1024 bytes")
                binData.take(1024).toByteArray()
            }
        }
        
        // Extract UID from first 4 bytes
        val uid = processedData.take(4).joinToString("") { "%02X".format(it) }
        
        // Convert to block format (64 blocks of 16 bytes each)
        val blocks = mutableMapOf<String, String>()
        for (blockNum in 0 until 64) {
            val blockStart = blockNum * 16
            val blockEnd = blockStart + 16
            if (blockEnd <= processedData.size) {
                val blockData = processedData.sliceArray(blockStart until blockEnd)
                blocks[blockNum.toString()] = blockData.joinToString("") { "%02X".format(it) }
            } else {
                // Fill missing blocks with zeros
                blocks[blockNum.toString()] = "00".repeat(16)
            }
        }
        
        // Extract sector keys if this is a 1152-byte file
        val sectorKeys = if (actualSize == 1152) {
            extractSectorKeysFromExtendedData(binData.drop(1024).toByteArray())
        } else null

        return BambuDumpFormat(
            Created = "bin-file-parser",
            FileType = if (actualSize == 1152) "mfc bin extended" else "mfc bin",
            Card = CardInfo(
                UID = uid,
                ATQA = "0400", // Default for Mifare Classic 1K
                SAK = "08"     // Default for Mifare Classic 1K
            ),
            blocks = blocks,
            SectorKeys = sectorKeys
        )
    }
    
    /**
     * Convert dump data to ByteArray (simulates NFC tag read)
     */
    fun convertDumpToByteArray(dump: BambuDumpFormat): ByteArray {
        val result = ByteArray(1024) // Mifare Classic 1K
        dump.blocks.forEach { (blockNum, hexData) ->
            val blockIndex = blockNum.toInt() * 16
            
            // Skip blocks that would exceed the array bounds (Mifare Classic 1K has only 64 blocks)
            if (blockIndex + 16 > result.size) {
                println("Warning: Skipping block $blockNum (index $blockIndex) - exceeds Mifare Classic 1K bounds")
                return@forEach
            }
            
            val blockData = hexStringToByteArray(hexData)
            blockData.copyInto(result, blockIndex, 0, minOf(16, blockData.size))
        }
        return result
    }
    
    /**
     * Extract material information from file path
     */
    fun extractMaterialInfo(dumpFile: File): MaterialInfo {
        val pathParts = dumpFile.path.split(File.separator)
        val categoryIndex = pathParts.indexOfLast { it in listOf("PLA", "PETG", "ABS", "ASA", "Support Material", "TPU", "PC", "PA") }
        
        if (categoryIndex == -1 || categoryIndex + 2 >= pathParts.size) {
            throw IllegalArgumentException("Cannot extract material info from path: ${dumpFile.path}")
        }
        
        val category = pathParts[categoryIndex]
        val material = pathParts[categoryIndex + 1] 
        val color = pathParts[categoryIndex + 2]
        
        return MaterialInfo(category, material, color)
    }
    
    /**
     * Get count of available dump files by material
     */
    fun getMaterialCounts(): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        loadAllDumpFiles().forEach { file ->
            try {
                val info = extractMaterialInfo(file)
                val key = "${info.category}/${info.material}"
                counts[key] = counts.getOrDefault(key, 0) + 1
            } catch (e: Exception) {
                // Skip files with unparseable paths
            }
        }
        return counts
    }
    
    private fun hexStringToByteArray(hex: String): ByteArray {
        val cleanHex = hex.replace(" ", "").uppercase()
        return cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
    
    /**
     * Expand 768-byte BIN data (data blocks only) to 1024 bytes (with trailer blocks)
     * Mifare Classic 1K has 16 sectors, each with 4 blocks (3 data + 1 trailer)
     */
    private fun expandBinData(data: ByteArray): ByteArray {
        val expanded = ByteArray(1024)
        var srcPos = 0
        var destPos = 0
        
        // Copy 16 sectors (each 64 bytes total: 48 bytes data + 16 bytes trailer)
        repeat(16) { sector ->
            // Copy 3 data blocks (48 bytes)
            data.copyInto(expanded, destPos, srcPos, srcPos + 48)
            srcPos += 48
            destPos += 48
            
            // Add dummy trailer block (16 bytes of zeros)
            destPos += 16
        }
        
        return expanded
    }
    
    /**
     * Extract sector keys from the extended 128-byte section of 1152-byte BIN files
     * This appears to contain authentication keys or metadata for the RFID sectors
     */
    private fun extractSectorKeysFromExtendedData(extendedData: ByteArray): Map<String, SectorKeyInfo>? {
        if (extendedData.size != 128) {
            println("WARNING: Expected 128 bytes of extended data, got ${extendedData.size}")
            return null
        }
        
        // For now, just store the raw data - this could be enhanced to parse specific key formats
        // The extended data appears to be mostly template/metadata rather than per-tag unique keys
        val sectorKeys = mutableMapOf<String, SectorKeyInfo>()
        
        // Store first 16 bytes as potential key material
        val keyData = extendedData.take(16).toByteArray()
        val keyHex = keyData.joinToString("") { "%02X".format(it) }
        
        sectorKeys["extended_metadata"] = SectorKeyInfo(
            KeyA = keyHex.take(12), // First 6 bytes as Key A
            KeyB = keyHex.drop(12).take(12), // Next 6 bytes as Key B
            AccessConditions = keyHex.drop(24) // Remaining as access conditions
        )
        
        return sectorKeys
    }
}

/**
 * Represents parsed Bambu Lab RFID dump file format
 */
data class BambuDumpFormat(
    val Created: String,
    val FileType: String,
    val Card: CardInfo,
    val blocks: Map<String, String>,
    val SectorKeys: Map<String, SectorKeyInfo>? = null
)

data class CardInfo(
    val UID: String,
    val ATQA: String,
    val SAK: String
)

data class SectorKeyInfo(
    val KeyA: String,
    val KeyB: String,
    val AccessConditions: String
)

data class MaterialInfo(
    val category: String,    // e.g. "PLA", "PETG", "ABS"
    val material: String,    // e.g. "PLA Basic", "PETG CF"
    val color: String        // e.g. "Black", "White", "Red"
)