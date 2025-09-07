/**
 * ServiceUsageExample - Demonstrates how to use the business logic services
 * This file shows integration patterns and typical usage scenarios
 */

import {
  configureServices,
  getServiceRegistry,
  cleanupServices,
  ComponentService,
} from '../index';

import { FilamentInfo, TagFormat, EntityType } from '../../types/FilamentInfo';

/**
 * Example: Initialize and configure all services
 */
export async function initializeServices(): Promise<boolean> {
  try {
    console.log('Configuring services...');
    const result = await configureServices();
    
    if (!result.isValid) {
      console.error('Service configuration failed:', result.firstErrorMessage);
      return false;
    }

    console.log('Services configured successfully');
    return true;
  } catch (error) {
    console.error('Failed to initialize services:', error);
    return false;
  }
}

/**
 * Example: Complete scan workflow from NFC scan to data persistence
 */
export async function performCompleteScanning(): Promise<void> {
  try {
    const registry = getServiceRegistry();
    const scanService = await registry.getScanService();
    const componentService = await registry.getComponentService();

    // Start scan operation
    const scanResult = await scanService.performScan(
      {
        scanId: `scan_${Date.now()}`,
        userId: 'user_123',
        metadata: {
          location: 'workshop',
          device: 'smartphone',
        },
      },
      {
        validateData: true,
        createPhysicalComponent: true,
        trackActivity: true,
        allowDuplicates: false,
      }
    );

    if (!scanResult.isValid) {
      console.error('Scan failed:', scanResult.firstErrorMessage);
      return;
    }

    const scanData = scanResult.data!;
    console.log('Scan completed successfully:', {
      scanId: scanData.scanId,
      success: scanData.success,
      duration: scanData.duration,
      filament: scanData.filamentInfo ? 
        `${scanData.filamentInfo.manufacturerName} ${scanData.filamentInfo.filamentType}` : 
        'Unknown',
    });

    // If we have filament info, create or update component
    if (scanData.filamentInfo) {
      await handleScannedComponent(scanData.filamentInfo, componentService);
    }

  } catch (error) {
    console.error('Scanning workflow failed:', error);
  }
}

/**
 * Example: Handle scanned component creation or update
 */
async function handleScannedComponent(
  filamentInfo: FilamentInfo, 
  componentService: ComponentService
): Promise<void> {
  // Check if component already exists
  const existingResult = await componentService.findComponentByTagUid(filamentInfo.tagUid);
  
  if (existingResult.isValid && existingResult.data) {
    // Update existing component weight (simulate weight loss from usage)
    const currentWeight = existingResult.data.currentWeight || filamentInfo.spoolWeight;
    const newWeight = Math.max(0, currentWeight - 5); // Simulate 5g usage
    
    const updateResult = await componentService.updateComponentWeight({
      componentId: existingResult.data.id,
      newWeight,
      reason: 'Usage detected from scan',
    });

    if (updateResult.isValid) {
      console.log('Updated component weight:', {
        componentId: existingResult.data.id,
        oldWeight: currentWeight,
        newWeight,
        remainingPercentage: updateResult.data!.usageCalculation?.remainingPercentage,
      });
    }

  } else {
    // Create new component
    const createResult = await componentService.createComponent({
      filamentInfo,
      currentWeight: filamentInfo.spoolWeight,
      notes: `Created from scan on ${new Date().toISOString()}`,
      initialInventoryQuantity: 1,
      location: 'Storage',
    });

    if (createResult.isValid) {
      console.log('Created new component:', {
        componentId: createResult.data!.component.id,
        tagUid: filamentInfo.tagUid,
        material: `${filamentInfo.manufacturerName} ${filamentInfo.filamentType}`,
        color: filamentInfo.colorName,
      });
    }
  }
}

/**
 * Example: Query and analyze component inventory
 */
export async function analyzeInventory(): Promise<void> {
  try {
    const registry = getServiceRegistry();
    const componentService = await registry.getComponentService();
    const activityService = await registry.getActivityService();

    // Get all components
    const componentsResult = await componentService.queryComponents({
      hasCurrentWeight: true,
      limit: 100,
    });

    if (!componentsResult.isValid) {
      console.error('Failed to query components:', componentsResult.firstErrorMessage);
      return;
    }

    const components = componentsResult.data!;
    console.log(`Found ${components.length} components with weight data`);

    // Analyze usage patterns
    const usageAnalysis = components.map(component => {
      const usage = componentService.calculateFilamentUsage(component);
      return {
        component,
        usage,
        status: usage.isEmpty ? 'EMPTY' : 
                usage.isLowFilament ? 'LOW' : 'OK',
      };
    });

    // Report low/empty filaments
    const lowFilaments = usageAnalysis.filter(item => item.status !== 'OK');
    if (lowFilaments.length > 0) {
      console.log(`⚠️  ${lowFilaments.length} filaments need attention:`);
      
      lowFilaments.forEach(item => {
        console.log(`  - ${item.component.filamentInfo.manufacturerName} ${item.component.filamentInfo.filamentType} (${item.component.filamentInfo.colorName})`);
        console.log(`    Status: ${item.status}, Remaining: ${item.usage.remainingPercentage.toFixed(1)}%`);
      });
    }

    // Get activity statistics
    const statsResult = await activityService.getActivityStats();
    if (statsResult.isValid) {
      const stats = statsResult.data!;
      console.log('Activity Statistics:', {
        totalActivities: stats.totalActivities,
        recentActivities: stats.recentActivityCount,
        averagePerDay: stats.averageActivitiesPerDay.toFixed(1),
        mostActiveDay: stats.mostActiveDay,
      });
    }

  } catch (error) {
    console.error('Inventory analysis failed:', error);
  }
}

/**
 * Example: Entity management operations
 */
export async function manageEntities(): Promise<void> {
  try {
    const registry = getServiceRegistry();
    const entityService = await registry.getEntityService();

    // Query recent activities
    const recentActivities = await entityService.queryEntities({
      entityType: EntityType.ACTIVITY,
      createdAfter: Date.now() - (7 * 24 * 60 * 60 * 1000), // Last 7 days
      limit: 10,
    });

    if (recentActivities.isValid) {
      console.log(`Found ${recentActivities.data!.length} recent activities`);
    }

    // Count entities by type
    const counts = await Promise.all([
      entityService.countEntitiesByType(EntityType.PHYSICAL_COMPONENT),
      entityService.countEntitiesByType(EntityType.ACTIVITY),
      entityService.countEntitiesByType(EntityType.INVENTORY_ITEM),
    ]);

    console.log('Entity counts:', {
      components: counts[0].isValid ? counts[0].data : 0,
      activities: counts[1].isValid ? counts[1].data : 0,
      inventory: counts[2].isValid ? counts[2].data : 0,
    });

  } catch (error) {
    console.error('Entity management failed:', error);
  }
}

/**
 * Example: Progress tracking for scan operations
 */
export async function performScanWithProgress(): Promise<void> {
  try {
    const registry = getServiceRegistry();
    const scanService = await registry.getScanService();

    const scanId = `scan_with_progress_${Date.now()}`;

    // Set up progress callback
    scanService.setScanProgressCallback(scanId, (progress) => {
      console.log(`Scan Progress [${scanId}]: ${progress.stage} - ${progress.percentage}% - ${progress.statusMessage}`);
    });

    // Perform scan
    const result = await scanService.performScan(
      { scanId },
      { validateData: true, trackActivity: true }
    );

    // Clean up callback
    scanService.removeScanProgressCallback(scanId);

    if (result.isValid) {
      console.log('Scan with progress completed:', result.data!.scanId);
    } else {
      console.error('Scan with progress failed:', result.firstErrorMessage);
    }

  } catch (error) {
    console.error('Progressive scan failed:', error);
  }
}

/**
 * Example: Validation handling
 */
export async function demonstrateValidation(): Promise<void> {
  try {
    const registry = getServiceRegistry();
    const componentService = await registry.getComponentService();

    // Create component with invalid data to show validation
    const invalidFilamentInfo: FilamentInfo = {
      tagUid: '', // Invalid - empty
      trayUid: 'TRAY_123',
      tagFormat: TagFormat.BAMBU_LAB,
      manufacturerName: '', // Invalid - empty
      filamentType: 'PLA',
      detailedFilamentType: 'PLA Basic',
      colorHex: 'not-a-color', // Invalid - not hex
      colorName: 'Red',
      spoolWeight: -100, // Invalid - negative
      filamentDiameter: 1.75,
      filamentLength: 300000,
      productionDate: '2024-01-01',
      minTemperature: 190,
      maxTemperature: 220,
      bedTemperature: 60,
      dryingTemperature: 40,
      dryingTime: 8,
      materialVariantId: 'PLA_BASIC_RED',
      materialId: 'PLA_BASIC',
      nozzleDiameter: 0.4,
      spoolWidth: 70,
      bedTemperatureType: 1,
      shortProductionDate: '240101',
      colorCount: 1,
      shortProductionDateHex: '240101',
      unknownBlock17Hex: '00000000000000000000000000000000',
      exactSku: '10101',
      rfidCode: 'GFA00:A00-K0',
    };

    const result = await componentService.createComponent({
      filamentInfo: invalidFilamentInfo,
    });

    if (!result.isValid) {
      console.log('Validation failed as expected:');
      result.issues.forEach(issue => {
        console.log(`  - ${issue.field}: ${issue.message} (${issue.code})`);
      });
    }

  } catch (error) {
    console.error('Validation demonstration failed:', error);
  }
}

/**
 * Example: Cleanup and shutdown
 */
export async function shutdownServices(): Promise<void> {
  try {
    console.log('Shutting down services...');
    await cleanupServices();
    console.log('Services shut down successfully');
  } catch (error) {
    console.error('Failed to shutdown services:', error);
  }
}

/**
 * Run all examples
 */
export async function runAllExamples(): Promise<void> {
  console.log('=== Business Logic Services Examples ===\n');

  try {
    // Initialize
    const initialized = await initializeServices();
    if (!initialized) {
      console.error('Failed to initialize services, stopping examples');
      return;
    }

    // Run examples
    console.log('\n1. Complete Scanning Workflow:');
    await performCompleteScanning();

    console.log('\n2. Inventory Analysis:');
    await analyzeInventory();

    console.log('\n3. Entity Management:');
    await manageEntities();

    console.log('\n4. Progressive Scanning:');
    await performScanWithProgress();

    console.log('\n5. Validation Demonstration:');
    await demonstrateValidation();

  } catch (error) {
    console.error('Examples failed:', error);
  } finally {
    // Always cleanup
    console.log('\n6. Service Shutdown:');
    await shutdownServices();
  }

  console.log('\n=== Examples Complete ===');
}