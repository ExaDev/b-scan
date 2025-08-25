# Test Data Integration Guide

This document describes B-Scan's default test data approach using real RFID tag dumps.

## Default Test Data: Real RFID Tags

B-Scan uses **real Bambu Lab RFID tag data as the default for all unit tests**. The `test-data/rfid-library/` submodule contains:

- **822 BIN dump files** (raw 1024-byte tag dumps)
- **361 JSON dump files** (structured format with metadata)

All files are from actual Bambu Lab RFID tags, organized by:
```
Material Category/Material Name/Color Name/Tag UID/hf-mf-[UID]-dump.json
```

## Unit Test Integration Approaches

### 1. Comprehensive Decoder Testing

Use real tag data to validate `BambuTagDecoder` against diverse material types:

```kotlin
@RunWith(Parameterized::class)
class BambuTagDecoderRealDataTest(
    private val testFile: File,
    private val expectedMaterial: String
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1} - {0}")
        fun data(): Collection<Array<Any>> {
            return loadRealTagData()
        }
        
        private fun loadRealTagData(): List<Array<Any>> {
            val testData = mutableListOf<Array<Any>>()
            val rfidLibraryPath = File("test-data/rfid-library")
            
            // PLA materials
            addMaterialTests(testData, rfidLibraryPath, "PLA", "PLA Basic")
            addMaterialTests(testData, rfidLibraryPath, "PLA", "PLA Matte")
            addMaterialTests(testData, rfidLibraryPath, "PLA", "PLA Silk")
            
            // PETG materials  
            addMaterialTests(testData, rfidLibraryPath, "PETG", "PETG Basic")
            addMaterialTests(testData, rfidLibraryPath, "PETG", "PETG CF")
            
            // ABS materials
            addMaterialTests(testData, rfidLibraryPath, "ABS", "ABS")
            
            return testData
        }
        
        private fun addMaterialTests(
            testData: MutableList<Array<Any>>, 
            basePath: File, 
            category: String, 
            material: String
        ) {
            val materialPath = File(basePath, "$category/$material")
            if (!materialPath.exists()) return
            
            materialPath.listFiles()?.forEach { colorDir ->
                colorDir.listFiles()?.forEach { tagDir ->
                    tagDir.listFiles { _, name -> 
                        name.endsWith("-dump.json") 
                    }?.forEach { dumpFile ->
                        testData.add(arrayOf(dumpFile, material))
                    }
                }
            }
        }
    }
    
    @Test
    fun testRealTagDecoding() {
        // Load JSON dump file
        val jsonContent = testFile.readText()
        val dumpData = Gson().fromJson(jsonContent, BambuDumpFormat::class.java)
        
        // Extract blocks as ByteArray (simulate NFC read)
        val tagData = convertDumpToByteArray(dumpData)
        
        // Test decoder
        val decoder = BambuTagDecoder()
        val result = decoder.decodeTag(tagData, dumpData.Card.UID)
        
        // Validate result
        assertTrue("Failed to decode ${testFile.name}", result.isSuccess)
        assertNotNull("No filament info decoded", result.filamentInfo)
        
        // Material-specific validation
        when (expectedMaterial) {
            "PLA Basic" -> assertEquals("PLA", result.filamentInfo?.filamentType)
            "PLA Matte" -> {
                assertEquals("PLA", result.filamentInfo?.filamentType)
                assertTrue("Expected matte finish", result.filamentInfo?.colorName?.contains("Matte") == true)
            }
            "PETG CF" -> {
                assertEquals("PETG", result.filamentInfo?.filamentType)
                assertTrue("Expected CF variant", result.filamentInfo?.colorName?.contains("CF") == true)
            }
        }
    }
}
```

### 2. Block Structure Validation

Test `BlockStructure` parsing against different tag formats:

```kotlin
class BlockStructureRealDataTest {
    
    @Test
    fun testBlockStructureAcrossMaterials() {
        val testCases = loadDiverseTagSample()
        
        testCases.forEach { (dumpFile, expectedType) ->
            val jsonContent = dumpFile.readText()
            val dumpData = Gson().fromJson(jsonContent, BambuDumpFormat::class.java)
            
            // Test block 2 (filament type)
            val block2 = dumpData.blocks["2"]?.let { 
                hexStringToByteArray(it) 
            }
            assertNotNull("Block 2 missing in ${dumpFile.name}", block2)
            
            // Test block 9 (tray UID)
            val block9 = dumpData.blocks["9"]?.let { 
                hexStringToByteArray(it) 
            }
            assertNotNull("Block 9 missing in ${dumpFile.name}", block9)
            
            // Validate extracted data matches expected type
            val extractedType = String(block2!!.takeWhile { it != 0.toByte() }.toByteArray())
            assertEquals("Material type mismatch in ${dumpFile.name}", expectedType, extractedType)
        }
    }
    
    private fun loadDiverseTagSample(): List<Pair<File, String>> {
        // Load representative samples from each material category
        return listOf(
            findFirstTagOfType("PLA", "PLA Basic") to "PLA",
            findFirstTagOfType("PETG", "PETG Basic") to "PETG", 
            findFirstTagOfType("ABS", "ABS") to "ABS",
            findFirstTagOfType("Support Material", "Support G") to "Support"
        ).filterNotNull()
    }
}
```

### 3. FilamentInterpreter Validation

Test interpretation engine against real-world variations:

```kotlin
class FilamentInterpreterRealDataTest {
    
    @Test
    fun testInterpreterAcrossColorVariants() {
        val interpreter = FilamentInterpreter()
        val colorVariants = loadColorVariantSample()
        
        colorVariants.forEach { (dumpFile, expectedColor) ->
            val decryptedData = loadAndDecryptDump(dumpFile)
            val filamentInfo = interpreter.interpret(decryptedData)
            
            assertNotNull("Failed to interpret ${dumpFile.name}", filamentInfo)
            assertEquals("Color mismatch in ${dumpFile.name}", expectedColor, filamentInfo?.colorName)
            assertNotNull("Missing hex code in ${dumpFile.name}", filamentInfo?.colorHex)
            assertTrue("Invalid hex format", filamentInfo?.colorHex?.matches(Regex("#[0-9A-Fa-f]{6}")) == true)
        }
    }
}
```

### 4. Test Utilities

Create helper functions for common test operations:

```kotlin
object RfidTestDataLoader {
    
    fun loadAllDumpFiles(): List<File> {
        val rfidLibrary = File("test-data/rfid-library")
        return rfidLibrary.walkTopDown()
            .filter { it.name.endsWith("-dump.json") }
            .toList()
    }
    
    fun loadByMaterial(material: String): List<File> {
        return loadAllDumpFiles().filter { file ->
            file.path.contains("/$material/", ignoreCase = true)
        }
    }
    
    fun loadByColor(color: String): List<File> {
        return loadAllDumpFiles().filter { file ->
            file.path.contains("/$color/", ignoreCase = true)
        }
    }
    
    fun convertDumpToByteArray(dump: BambuDumpFormat): ByteArray {
        val result = ByteArray(1024) // Mifare Classic 1K
        dump.blocks.forEach { (blockNum, hexData) ->
            val blockIndex = blockNum.toInt() * 16
            val blockData = hexStringToByteArray(hexData)
            blockData.copyInto(result, blockIndex, 0, minOf(16, blockData.size))
        }
        return result
    }
}

data class BambuDumpFormat(
    val Created: String,
    val FileType: String,
    val Card: CardInfo,
    val blocks: Map<String, String>
)

data class CardInfo(
    val UID: String,
    val ATQA: String, 
    val SAK: String
)
```

## Test Configuration

### Add to `build.gradle.kts`:

```kotlin
android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testImplementation("com.google.code.gson:gson:2.8.9")
}
```

### Directory Structure:

```
app/src/test/kotlin/com/bscan/
├── decoder/
│   ├── BambuTagDecoderRealDataTest.kt
│   └── BlockStructureRealDataTest.kt  
├── interpreter/
│   └── FilamentInterpreterRealDataTest.kt
└── utils/
    └── RfidTestDataLoader.kt
```

## Benefits

1. **Comprehensive Coverage**: Test against 361 real tag dumps spanning all material types
2. **Edge Case Detection**: Discover parsing issues with unusual color names or special variants  
3. **Regression Prevention**: Catch decoder changes that break real-world compatibility
4. **Format Validation**: Ensure BlockStructure handles all tag format variations correctly
5. **Color Accuracy**: Validate color name and hex code extraction across diverse palettes

## Usage in CI/CD

```bash
# Initialize submodule in CI
git submodule update --init --recursive

# Run real data tests  
./gradlew testDebugUnitTest --tests "*RealDataTest"
```

This approach provides comprehensive validation using the community's most complete collection of real Bambu Lab RFID tag data.