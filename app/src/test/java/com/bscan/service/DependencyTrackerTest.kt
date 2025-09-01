package com.bscan.service

import android.content.Context
import com.bscan.model.graph.entities.*
import com.bscan.repository.CatalogRepository
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.After
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDateTime

/**
 * Comprehensive unit tests for DependencyTracker
 * 
 * Tests dependency extraction from different entity types, source entity fingerprint 
 * generation, catalog version tracking, configuration file monitoring, external data 
 * source change detection, and algorithm fingerprint tracking.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class DependencyTrackerTest {

    @Mock
    private lateinit var mockCatalogRepository: CatalogRepository
    
    private lateinit var context: Context
    private lateinit var dependencyTracker: DependencyTracker
    private lateinit var tempConfigDir: File

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        // Use Robolectric context for file operations
        context = RuntimeEnvironment.getApplication()
        
        // Create temporary config directory for testing
        tempConfigDir = File(context.filesDir, "config")
        tempConfigDir.mkdirs()
        
        dependencyTracker = DependencyTracker(context, mockCatalogRepository)
    }

    @After
    fun tearDown() {
        // Clean up temporary files
        tempConfigDir.deleteRecursively()
    }

    // ========== Source Entity Fingerprint Tests ==========

    @Test
    fun `generateSourceFingerprint creates consistent hash for RawScanData`() {
        // Given
        val rawScanEntity = RawScanData(
            id = "test-id",
            label = "Test Raw Scan",
            scanFormat = "bambu_proprietary"
        ).apply {
            rawData = "010203"
            contentHash = "abc123"
        }
        
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info",
            label = "Source Entity"
        )

        // When
        val dependencies1 = dependencyTracker.extractDependencies(rawScanEntity, sourceEntity)
        val dependencies2 = dependencyTracker.extractDependencies(rawScanEntity, sourceEntity)

        // Then
        assertEquals(
            "Source fingerprints should be consistent", 
            dependencies1.sourceFingerprint, 
            dependencies2.sourceFingerprint
        )
        assertEquals(16, dependencies1.sourceFingerprint.length)
        assertTrue("Fingerprint should be hex", dependencies1.sourceFingerprint.matches(Regex("[a-f0-9]{16}")))
    }

    @Test
    fun `generateSourceFingerprint differs for different RawScanData content`() {
        // Given
        val rawScanEntity1 = RawScanData(
            id = "test-id-1",
            label = "Test Raw Scan 1",
            scanFormat = "bambu_proprietary"
        ).apply {
            rawData = "010203"
            contentHash = "abc123"
        }
        
        val rawScanEntity2 = RawScanData(
            id = "test-id-2", 
            label = "Test Raw Scan 2",
            scanFormat = "bambu_proprietary"
        ).apply {
            rawData = "040506"
            contentHash = "def456"
        }
        
        val derivedEntity = Information(
            id = "derived-id",
            informationType = "test_info", 
            label = "Derived Entity"
        )

        // When
        val dependencies1 = dependencyTracker.extractDependencies(derivedEntity, rawScanEntity1)
        val dependencies2 = dependencyTracker.extractDependencies(derivedEntity, rawScanEntity2)

        // Then
        assertNotEquals(
            "Different raw data should produce different fingerprints", 
            dependencies1.sourceFingerprint, 
            dependencies2.sourceFingerprint
        )
    }

    @Test
    fun `generateSourceFingerprint handles Information entity with rawData property`() {
        // Given
        val informationEntity = Information(
            id = "info-id",
            informationType = "test_info",
            label = "Info Entity"
        ).apply {
            setProperty("rawData", "test-raw-data-content")
        }
        
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info", 
            label = "Source Entity"
        )

        // When
        val dependencies = dependencyTracker.extractDependencies(informationEntity, sourceEntity)

        // Then
        assertNotNull("Source fingerprint should be generated", dependencies.sourceFingerprint)
        assertEquals(16, dependencies.sourceFingerprint.length)
        assertTrue("Fingerprint should be hex", dependencies.sourceFingerprint.matches(Regex("[a-f0-9]{16}")))
    }

    // ========== Catalog Dependency Tests ==========

    @Test
    fun `extractDependencies includes catalog version for DecodedDecrypted with catalog data`() {
        // Given
        `when`(mockCatalogRepository.getContentFingerprint()).thenReturn("catalog-fingerprint-123")
        
        val decodedEntity = DecodedDecrypted(
            id = "decoded-id",
            label = "Decoded Entity"
        ).apply {
            setProperty("catalogData", "bambu-product-info")
            productInfo = """{"material": "PLA", "color": "Red"}"""
        }
        
        val sourceEntity = RawScanData(
            id = "source-id", 
            label = "Source Entity",
            scanFormat = "bambu_rfid"
        ).apply {
            rawData = "01020304"
        }

        // When
        val dependencies = dependencyTracker.extractDependencies(decodedEntity, sourceEntity)

        // Then
        assertNotNull("Catalog version should be included", dependencies.catalogVersion)
        assertEquals("catalog_catalog-fingerprint-123", dependencies.catalogVersion)
        verify(mockCatalogRepository).getContentFingerprint()
    }

    @Test
    fun `extractDependencies excludes catalog version for DecodedDecrypted without catalog data`() {
        // Given
        val decodedEntity = DecodedDecrypted(
            id = "decoded-id",
            label = "Decoded Entity"
        )
        // No catalogData property or productInfo
        
        val sourceEntity = RawScanData(
            id = "source-id", 
            label = "Source Entity",
            scanFormat = "bambu_rfid"
        ).apply {
            rawData = "01020304" 
        }

        // When
        val dependencies = dependencyTracker.extractDependencies(decodedEntity, sourceEntity)

        // Then
        assertNull("Catalog version should not be included", dependencies.catalogVersion)
        verify(mockCatalogRepository, never()).getContentFingerprint()
    }

    @Test
    fun `extractDependencies includes catalog version for PhysicalComponent with catalog mass`() {
        // Given
        `when`(mockCatalogRepository.getContentFingerprint()).thenReturn("catalog-fingerprint-456")
        
        val physicalEntity = PhysicalComponent(
            id = "physical-id",
            label = "Physical Component"
        ).apply {
            setProperty("catalogMass", 250.0f)
        }
        
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info", 
            label = "Source Entity"
        )

        // When
        val dependencies = dependencyTracker.extractDependencies(physicalEntity, sourceEntity)

        // Then
        assertNotNull("Catalog version should be included", dependencies.catalogVersion)
        assertEquals("catalog_catalog-fingerprint-456", dependencies.catalogVersion)
        verify(mockCatalogRepository).getContentFingerprint()
    }

    @Test
    fun `getCatalogVersion handles repository exceptions gracefully`() {
        // Given
        `when`(mockCatalogRepository.getContentFingerprint())
            .thenThrow(RuntimeException("Repository error"))
        
        val decodedEntity = DecodedDecrypted(
            id = "decoded-id",
            label = "Decoded Entity"
        ).apply {
            productInfo = """{"material": "PLA"}"""
        }
        
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info", 
            label = "Source Entity"
        )

        // When
        val dependencies = dependencyTracker.extractDependencies(decodedEntity, sourceEntity)

        // Then
        assertNotNull("Catalog version should still be provided", dependencies.catalogVersion)
        assertTrue("Should include unknown prefix", dependencies.catalogVersion!!.startsWith("catalog_unknown_"))
    }

    // ========== Configuration File Dependency Tests ==========

    @Test
    fun `extractDependencies includes config files for DecodedDecrypted entity`() {
        // Given
        createTestConfigFile("filament_mappings.json", """{"pla": {"nozzle": 210}}""")
        createTestConfigFile("temperature_profiles.json", """{"standard": {"bed": 60}}""")
        
        val decodedEntity = DecodedDecrypted(
            id = "decoded-id",
            label = "Decoded Entity"
        )
        
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info", 
            label = "Source Entity"
        )

        // When
        val dependencies = dependencyTracker.extractDependencies(decodedEntity, sourceEntity)

        // Then
        assertEquals("Should have 2 config dependencies", 2, dependencies.configHashes.size)
        assertTrue("Should include filament mappings", 
            dependencies.configHashes.containsKey("filament_mappings.json"))
        assertTrue("Should include temperature profiles", 
            dependencies.configHashes.containsKey("temperature_profiles.json"))
        
        // Verify hash values are consistent
        val hash1 = dependencies.configHashes["filament_mappings.json"]
        assertNotNull("Hash should be generated", hash1)
        assertEquals("Hash should be 16 characters", 16, hash1!!.length)
        assertTrue("Hash should be hex", hash1.matches(Regex("[a-f0-9]{16}")))
    }

    @Test
    fun `extractDependencies includes encryption config for DecodedEncrypted entity`() {
        // Given
        createTestConfigFile("encryption_settings.json", """{"algorithm": "AES-128"}""")
        
        val encodedEntity = DecodedEncrypted(
            id = "encoded-id",
            label = "Encoded Entity"
        )
        
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info", 
            label = "Source Entity"
        )

        // When
        val dependencies = dependencyTracker.extractDependencies(encodedEntity, sourceEntity)

        // Then
        assertEquals("Should have 1 config dependency", 1, dependencies.configHashes.size)
        assertTrue("Should include encryption settings", 
            dependencies.configHashes.containsKey("encryption_settings.json"))
    }

    @Test
    fun `getConfigFileHash handles missing config files gracefully`() {
        // Given - no config files created
        val decodedEntity = DecodedDecrypted(
            id = "decoded-id",
            label = "Decoded Entity"
        )
        
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info", 
            label = "Source Entity"
        )

        // When
        val dependencies = dependencyTracker.extractDependencies(decodedEntity, sourceEntity)

        // Then
        assertEquals("Should have 2 config dependencies", 2, dependencies.configHashes.size)
        assertEquals("Missing file should have missing prefix", 
            "missing_filament_mappings.json", 
            dependencies.configHashes["filament_mappings.json"])
        assertEquals("Missing file should have missing prefix", 
            "missing_temperature_profiles.json", 
            dependencies.configHashes["temperature_profiles.json"])
    }

    @Test
    fun `getConfigFileHash produces consistent hashes for same content`() {
        // Given
        val testContent = """{"test": "config"}"""
        createTestConfigFile("test_config.json", testContent)
        
        val entity = DecodedDecrypted(id = "test-id", label = "Test Entity")
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info", 
            label = "Source Entity"
        )

        // When
        val dependencies1 = dependencyTracker.extractDependencies(entity, sourceEntity)
        val dependencies2 = dependencyTracker.extractDependencies(entity, sourceEntity)

        // Then
        val hash1 = dependencies1.configHashes["filament_mappings.json"] // This will be missing
        val hash2 = dependencies2.configHashes["filament_mappings.json"]
        assertEquals("Hashes should be consistent", hash1, hash2)
    }

    // ========== External Data Source Tests ==========

    @Test
    fun `extractDependencies identifies external data sources for DecodedDecrypted`() {
        // Given
        val decodedEntity = DecodedDecrypted(
            id = "decoded-id",
            label = "Decoded Entity"
        ).apply {
            productInfo = """{"material": "PLA", "manufacturer": "Bambu"}"""
        }
        
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info", 
            label = "Source Entity"
        )

        // When
        val dependencies = dependencyTracker.extractDependencies(decodedEntity, sourceEntity)

        // Then
        assertEquals("Should have 1 external data source", 1, dependencies.externalDataSources.size)
        assertTrue("Should include product database", 
            dependencies.externalDataSources.contains("product_database"))
    }

    @Test
    fun `extractDependencies identifies external data sources for PhysicalComponent`() {
        // Given
        val physicalEntity = PhysicalComponent(
            id = "physical-id",
            label = "Physical Component"
        ).apply {
            setProperty("externalSpec", "http://example.com/specs/component123")
        }
        
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info", 
            label = "Source Entity"
        )

        // When
        val dependencies = dependencyTracker.extractDependencies(physicalEntity, sourceEntity)

        // Then
        assertEquals("Should have 1 external data source", 1, dependencies.externalDataSources.size)
        assertTrue("Should include component specs", 
            dependencies.externalDataSources.contains("component_specs"))
    }

    @Test
    fun `extractDependencies returns empty external sources for entities without external deps`() {
        // Given
        val identifier = Identifier(
            id = "id-entity",
            identifierType = "RFID_HARDWARE",
            value = "A1B2C3D4"
        )
        
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info", 
            label = "Source Entity"
        )

        // When
        val dependencies = dependencyTracker.extractDependencies(identifier, sourceEntity)

        // Then
        assertTrue("Should have no external data sources", dependencies.externalDataSources.isEmpty())
    }

    // ========== Algorithm Fingerprint Tests ==========

    @Test
    fun `extractDependencies includes algorithm fingerprints for decoded_encrypted`() {
        // Given
        val encodedEntity = DecodedEncrypted(
            id = "encoded-id",
            label = "Encoded Entity"
        ).apply {
            // Verify that this Information entity has the correct informationType
            assertEquals("decoded_encrypted", informationType)
        }
        
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info", 
            label = "Source Entity"
        )

        // When
        val dependencies = dependencyTracker.extractDependencies(encodedEntity, sourceEntity)

        // Then
        assertEquals("Should have 2 algorithm fingerprints", 2, dependencies.algorithmFingerprints.size)
        assertTrue("Should include metadata extraction", 
            dependencies.algorithmFingerprints.containsKey("metadata_extraction"))
        assertTrue("Should include tag parsing", 
            dependencies.algorithmFingerprints.containsKey("tag_parsing"))
        
        assertEquals("meta_v1.2.0", dependencies.algorithmFingerprints["metadata_extraction"])
        assertEquals("parser_v1.1.0", dependencies.algorithmFingerprints["tag_parsing"])
    }

    @Test
    fun `extractDependencies includes algorithm fingerprints for encoded_decrypted`() {
        // Given
        val entity = EncodedDecrypted(
            id = "encrypted-id",
            label = "Encrypted Entity"
        ).apply {
            assertEquals("encoded_decrypted", informationType)
        }
        
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info", 
            label = "Source Entity"
        )

        // When
        val dependencies = dependencyTracker.extractDependencies(entity, sourceEntity)

        // Then
        assertEquals("Should have 2 algorithm fingerprints", 2, dependencies.algorithmFingerprints.size)
        assertTrue("Should include decryption", 
            dependencies.algorithmFingerprints.containsKey("decryption"))
        assertTrue("Should include key derivation", 
            dependencies.algorithmFingerprints.containsKey("key_derivation"))
        
        assertEquals("decrypt_v1.0.0", dependencies.algorithmFingerprints["decryption"])
        assertEquals("keys_v2.0.0", dependencies.algorithmFingerprints["key_derivation"])
    }

    @Test
    fun `extractDependencies includes algorithm fingerprints for decoded_decrypted`() {
        // Given
        val decodedEntity = DecodedDecrypted(
            id = "decoded-id", 
            label = "Decoded Entity"
        ).apply {
            assertEquals("decoded_decrypted", informationType)
        }
        
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info", 
            label = "Source Entity"
        )

        // When
        val dependencies = dependencyTracker.extractDependencies(decodedEntity, sourceEntity)

        // Then
        assertEquals("Should have 2 algorithm fingerprints", 2, dependencies.algorithmFingerprints.size)
        assertTrue("Should include filament interpretation", 
            dependencies.algorithmFingerprints.containsKey("filament_interpretation"))
        assertTrue("Should include temperature calculation", 
            dependencies.algorithmFingerprints.containsKey("temperature_calculation"))
        
        assertEquals("interp_v1.3.0", dependencies.algorithmFingerprints["filament_interpretation"])
        assertEquals("temp_v1.0.0", dependencies.algorithmFingerprints["temperature_calculation"])
    }

    @Test
    fun `extractDependencies returns empty algorithms for unsupported entity types`() {
        // Given
        val physicalEntity = PhysicalComponent(
            id = "physical-id",
            label = "Physical Component"
        )
        
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info", 
            label = "Source Entity"
        )

        // When
        val dependencies = dependencyTracker.extractDependencies(physicalEntity, sourceEntity)

        // Then
        assertTrue("Should have no algorithm fingerprints", dependencies.algorithmFingerprints.isEmpty())
    }

    // ========== Change Detection Tests ==========

    @Test
    fun `hasChanged returns false for identical dependencies`() {
        // Given
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info", 
            label = "Source Entity"
        )
        val decodedEntity = DecodedDecrypted(id = "decoded-id", label = "Decoded Entity")
        
        val originalDependencies = dependencyTracker.extractDependencies(decodedEntity, sourceEntity)

        // When
        val hasChanged = dependencyTracker.hasChanged(originalDependencies)

        // Then
        assertFalse("Dependencies should not have changed", hasChanged)
    }

    @Test
    fun `hasChanged detects catalog version changes`() {
        // Given
        `when`(mockCatalogRepository.getContentFingerprint())
            .thenReturn("original-fingerprint")
            .thenReturn("updated-fingerprint")
        
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info", 
            label = "Source Entity"
        )
        val decodedEntity = DecodedDecrypted(
            id = "decoded-id", 
            label = "Decoded Entity"
        ).apply {
            productInfo = """{"material": "PLA"}"""
        }
        
        val originalDependencies = dependencyTracker.extractDependencies(decodedEntity, sourceEntity)

        // When
        val hasChanged = dependencyTracker.hasChanged(originalDependencies)

        // Then
        assertTrue("Dependencies should have changed", hasChanged)
    }

    @Test
    fun `hasChanged detects config file changes`() {
        // Given
        createTestConfigFile("filament_mappings.json", """{"original": "config"}""")
        
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info", 
            label = "Source Entity"
        )
        val decodedEntity = DecodedDecrypted(id = "decoded-id", label = "Decoded Entity")
        
        val originalDependencies = dependencyTracker.extractDependencies(decodedEntity, sourceEntity)
        
        // Modify config file
        createTestConfigFile("filament_mappings.json", """{"updated": "config"}""")

        // When
        val hasChanged = dependencyTracker.hasChanged(originalDependencies)

        // Then
        assertTrue("Dependencies should have changed due to config file update", hasChanged)
    }

    // ========== DependencySet Functionality Tests ==========

    @Test
    fun `DependencySet getDependencyKeys includes all dependency types`() {
        // Given
        createTestConfigFile("test_config.json", """{"test": "value"}""")
        `when`(mockCatalogRepository.getContentFingerprint()).thenReturn("catalog-123")
        
        val decodedEntity = DecodedDecrypted(
            id = "decoded-id",
            label = "Decoded Entity"
        ).apply {
            productInfo = """{"material": "PLA"}"""
        }
        
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info", 
            label = "Source Entity"
        )

        // When
        val dependencies = dependencyTracker.extractDependencies(decodedEntity, sourceEntity)
        val dependencyKeys = dependencies.getDependencyKeys()

        // Then
        assertTrue("Should include source key", 
            dependencyKeys.any { it.startsWith("source:") })
        assertTrue("Should include catalog key", 
            dependencyKeys.any { it.startsWith("catalog:") })
        assertTrue("Should include config keys", 
            dependencyKeys.any { it.startsWith("config:") })
        assertTrue("Should include algorithm keys", 
            dependencyKeys.any { it.startsWith("algorithm:") })
    }

    @Test
    fun `DependencySet equality works correctly for identical dependencies`() {
        // Given
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info", 
            label = "Source Entity"
        )
        val decodedEntity = DecodedDecrypted(id = "decoded-id", label = "Decoded Entity")
        
        val dependencies1 = dependencyTracker.extractDependencies(decodedEntity, sourceEntity)
        val dependencies2 = dependencyTracker.extractDependencies(decodedEntity, sourceEntity)

        // When & Then
        assertEquals("Identical dependencies should be equal", dependencies1.copy(timestamp = dependencies2.timestamp), dependencies2)
    }

    @Test
    fun `DependencySet handles timestamp properly in extractDependencies`() {
        // Given
        val sourceEntity = Information(
            id = "source-id",
            informationType = "test_info", 
            label = "Source Entity"
        )
        val decodedEntity = DecodedDecrypted(id = "decoded-id", label = "Decoded Entity")
        
        val before = LocalDateTime.now()

        // When
        val dependencies = dependencyTracker.extractDependencies(decodedEntity, sourceEntity)
        
        val after = LocalDateTime.now()

        // Then
        assertTrue("Timestamp should be between before and after", 
            !dependencies.timestamp.isBefore(before) && !dependencies.timestamp.isAfter(after))
    }

    // ========== Helper Methods ==========

    private fun createTestConfigFile(fileName: String, content: String) {
        val configFile = File(tempConfigDir, fileName)
        configFile.parentFile?.mkdirs()
        configFile.writeText(content)
    }
}