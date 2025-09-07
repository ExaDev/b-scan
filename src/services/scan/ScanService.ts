/**
 * ScanService - Orchestrates scan operations and data processing workflows
 * Central service for managing the complete scan lifecycle from NFC reading to data persistence
 */

import { BaseService, ServiceResult } from '../core/BaseService';
import { ValidationResult, ValidationUtils } from '../validation';
import { NfcManagerService } from '../NfcManagerService';
import {
  FilamentInfo,
  TagReadResult as FilamentTagReadResult,
  ScanProgress,
  ScanStage,
  Activity,
  EntityType,
} from '../../types/FilamentInfo';
import { TagReadResult as NfcTagReadResult } from '../NfcManager';
import { PhysicalComponent } from '../../models/entities/InventoryEntities';

export interface ScanOperationRequest {
  scanId: string;
  userId?: string;
  metadata?: Record<string, unknown>;
}

export interface ScanOperationResult {
  scanId: string;
  success: boolean;
  filamentInfo?: FilamentInfo;
  physicalComponent?: PhysicalComponent;
  activity?: Activity;
  error?: string;
  timestamp: number;
  duration: number;
}

export interface ScanProcessingOptions {
  validateData?: boolean;
  createPhysicalComponent?: boolean;
  trackActivity?: boolean;
  allowDuplicates?: boolean;
}

export class ScanService extends BaseService {
  private scanCallbacks: Map<string, (progress: ScanProgress) => void> = new Map();
  private activeScanIds: Set<string> = new Set();

  /**
   * Perform complete scan operation workflow
   */
  async performScan(
    request: ScanOperationRequest,
    options: ScanProcessingOptions = {}
  ): Promise<ServiceResult<ScanOperationResult>> {
    this.ensureInitialized();

    const startTime = Date.now();
    const { scanId } = request;

    try {
      // Check if scan is already in progress
      if (this.activeScanIds.has(scanId)) {
        return ValidationResult.singleError(
          'scanId',
          'SCAN_IN_PROGRESS',
          `Scan ${scanId} is already in progress`
        );
      }

      this.activeScanIds.add(scanId);
      this.log('info', `Starting scan operation: ${scanId}`);

      // Update progress
      this.updateScanProgress(scanId, {
        stage: ScanStage.INITIALIZING,
        percentage: 0,
        currentSector: 0,
        statusMessage: 'Initializing scan...',
      });

      // Perform NFC scan
      const nfcResult = await this.performNfcScan(scanId);
      if (!nfcResult.isValid) {
        const errorResult: Partial<ScanOperationResult> = { success: false };
        if (nfcResult.firstErrorMessage) {
          errorResult.error = nfcResult.firstErrorMessage;
        }
        return this.finalizeScan(scanId, startTime, errorResult);
      }

      const tagReadResult = nfcResult.data!;

      // Update progress
      this.updateScanProgress(scanId, {
        stage: ScanStage.PARSING_DATA,
        percentage: 70,
        currentSector: 0,
        statusMessage: 'Parsing filament data...',
      });

      // Process scan data
      const processResult = await this.processScanData(tagReadResult, options);
      if (!processResult.isValid) {
        const errorResult: Partial<ScanOperationResult> = { success: false };
        if (processResult.firstErrorMessage) {
          errorResult.error = processResult.firstErrorMessage;
        }
        return this.finalizeScan(scanId, startTime, errorResult);
      }

      const scanData = processResult.data!;

      // Complete scan
      const result: ScanOperationResult = {
        scanId,
        success: true,
        filamentInfo: scanData.filamentInfo,
        timestamp: startTime,
        duration: Date.now() - startTime,
      };

      // Only add optional properties if they exist
      if (scanData.physicalComponent) {
        result.physicalComponent = scanData.physicalComponent;
      }
      if (scanData.activity) {
        result.activity = scanData.activity;
      }

      // Update progress
      this.updateScanProgress(scanId, {
        stage: ScanStage.COMPLETED,
        percentage: 100,
        currentSector: 0,
        statusMessage: 'Scan completed successfully',
      });

      return this.finalizeScan(scanId, startTime, result);

    } catch (error) {
      this.log('error', `Scan operation failed: ${scanId}`, error);
      return this.finalizeScan(scanId, startTime, { 
        success: false, 
        error: error instanceof Error ? error.message : 'Unknown error' 
      });
    }
  }

  /**
   * Cancel an ongoing scan operation
   */
  async cancelScan(scanId: string): Promise<ServiceResult<void>> {
    this.ensureInitialized();

    try {
      if (!this.activeScanIds.has(scanId)) {
        return ValidationResult.singleError(
          'scanId',
          'SCAN_NOT_FOUND',
          `No active scan found with ID: ${scanId}`
        );
      }

      // Cancel NFC scan
      await NfcManagerService.cancelScan();

      // Update progress to error state
      this.updateScanProgress(scanId, {
        stage: ScanStage.ERROR,
        percentage: 0,
        currentSector: 0,
        statusMessage: 'Scan cancelled by user',
      });

      this.activeScanIds.delete(scanId);
      this.scanCallbacks.delete(scanId);

      this.log('info', `Scan cancelled: ${scanId}`);
      return ValidationResult.success(undefined);

    } catch (error) {
      return this.handleError(error, 'cancelScan');
    }
  }

  /**
   * Set progress callback for scan updates
   */
  setScanProgressCallback(scanId: string, callback: (progress: ScanProgress) => void): void {
    this.scanCallbacks.set(scanId, callback);
  }

  /**
   * Remove progress callback
   */
  removeScanProgressCallback(scanId: string): void {
    this.scanCallbacks.delete(scanId);
  }

  /**
   * Get list of active scan operations
   */
  getActiveScanIds(): string[] {
    return Array.from(this.activeScanIds);
  }

  /**
   * Validate scan request
   */
  validateScanRequest(request: ScanOperationRequest): ValidationResult<ScanOperationRequest> {
    const scanIdResult = ValidationUtils.validateRequired(request.scanId, 'scanId');
    if (!scanIdResult.isValid) {
      return ValidationResult.error(scanIdResult.issues);
    }

    const lengthResult = ValidationUtils.validateLength(request.scanId, 'scanId', 1, 50);
    if (!lengthResult.isValid) {
      return ValidationResult.error(lengthResult.issues);
    }

    return ValidationResult.success(request);
  }

  // Private methods

  private async performNfcScan(scanId: string): Promise<ServiceResult<NfcTagReadResult>> {
    try {
      this.updateScanProgress(scanId, {
        stage: ScanStage.AUTHENTICATING,
        percentage: 30,
        currentSector: 0,
        statusMessage: 'Scanning NFC tag...',
      });

      const tagResult = await NfcManagerService.scanTag();
      
      if (tagResult.type === 'SUCCESS') {
        // Convert NfcTagReadResult to FilamentTagReadResult
        const filamentResult: FilamentTagReadResult = {
          type: 'SUCCESS',
          filamentInfo: tagResult.filamentInfo
        };
        return ValidationResult.success(filamentResult);
      } else {
        const errorMessage = this.getTagResultErrorMessage(tagResult);
        return ValidationResult.singleError(
          'nfcScan',
          'SCAN_FAILED',
          errorMessage
        );
      }
    } catch (error) {
      return this.handleError(error, 'performNfcScan');
    }
  }

  private async processScanData(
    tagResult: NfcTagReadResult,
    options: ScanProcessingOptions
  ): Promise<ServiceResult<{
    filamentInfo: FilamentInfo;
    physicalComponent?: PhysicalComponent;
    activity?: Activity;
  }>> {
    try {
      if (tagResult.type !== 'SUCCESS' || !tagResult.filamentInfo) {
        return ValidationResult.singleError(
          'tagResult',
          'INVALID_TAG_DATA',
          'No valid filament information in tag result'
        );
      }

      const filamentInfo = tagResult.filamentInfo;

      // Validate filament information if requested
      if (options.validateData) {
        const validationResult = this.validateFilamentInfo(filamentInfo);
        if (!validationResult.isValid) {
          return ValidationResult.error(validationResult.issues);
        }
      }

      const result: {
        filamentInfo: FilamentInfo;
        physicalComponent?: PhysicalComponent;
        activity?: Activity;
      } = { filamentInfo };

      // Create physical component if requested
      if (options.createPhysicalComponent) {
        const componentResult = await this.createPhysicalComponent(filamentInfo);
        if (componentResult.isValid && componentResult.data) {
          result.physicalComponent = componentResult.data;
        }
      }

      // Track activity if requested
      if (options.trackActivity) {
        const activityResult = await this.createScanActivity(filamentInfo);
        if (activityResult.isValid && activityResult.data) {
          result.activity = activityResult.data;
        }
      }

      return ValidationResult.success(result);

    } catch (error) {
      return this.handleError(error, 'processScanData');
    }
  }

  private validateFilamentInfo(filamentInfo: FilamentInfo): ValidationResult<FilamentInfo> {
    const validations = [
      ValidationUtils.validateTagUid(filamentInfo.tagUid),
      ValidationUtils.validateRequired(filamentInfo.manufacturerName, 'manufacturerName'),
      ValidationUtils.validateRequired(filamentInfo.filamentType, 'filamentType'),
      ValidationUtils.validateHexColor(filamentInfo.colorHex, 'colorHex'),
      ValidationUtils.validatePositiveNumber(filamentInfo.spoolWeight, 'spoolWeight'),
      ValidationUtils.validateFilamentDiameter(filamentInfo.filamentDiameter),
      ValidationUtils.validatePositiveNumber(filamentInfo.filamentLength, 'filamentLength'),
      ValidationUtils.validateISODate(filamentInfo.productionDate, 'productionDate'),
      ValidationUtils.validateTemperature(filamentInfo.minTemperature, 'minTemperature', 'nozzle'),
      ValidationUtils.validateTemperature(filamentInfo.maxTemperature, 'maxTemperature', 'nozzle'),
      ValidationUtils.validateTemperature(filamentInfo.bedTemperature, 'bedTemperature', 'bed'),
      ValidationUtils.validateTemperature(filamentInfo.dryingTemperature, 'dryingTemperature', 'drying'),
      ValidationUtils.validatePositiveNumber(filamentInfo.dryingTime, 'dryingTime'),
    ];

    return ValidationUtils.combineResults(...validations)
      .map(() => filamentInfo);
  }

  private async createPhysicalComponent(filamentInfo: FilamentInfo): Promise<ServiceResult<PhysicalComponent>> {
    try {
      const component = new PhysicalComponent(
        `component_${filamentInfo.tagUid}_${Date.now()}`,
        `${filamentInfo.manufacturerName} ${filamentInfo.filamentType} - ${filamentInfo.colorName}`
      );
      
      // Set filament-specific properties
      component.manufacturer = filamentInfo.manufacturerName;
      component.category = 'filament';
      component.setProperty('filamentInfo', filamentInfo);
      component.setProperty('tagUid', filamentInfo.tagUid);
      component.setProperty('trayUid', filamentInfo.trayUid);

      return ValidationResult.success(component);
    } catch (error) {
      return this.handleError(error, 'createPhysicalComponent');
    }
  }

  private async createScanActivity(filamentInfo: FilamentInfo): Promise<ServiceResult<Activity>> {
    try {
      const activity: Activity = {
        id: `activity_scan_${filamentInfo.tagUid}_${Date.now()}`,
        type: EntityType.ACTIVITY,
        activityType: 'SCAN',
        description: `Scanned ${filamentInfo.manufacturerName} ${filamentInfo.filamentType} (${filamentInfo.colorName})`,
        createdAt: Date.now(),
        updatedAt: Date.now(),
      };

      return ValidationResult.success(activity);
    } catch (error) {
      return this.handleError(error, 'createScanActivity');
    }
  }

  private updateScanProgress(scanId: string, progress: ScanProgress): void {
    const callback = this.scanCallbacks.get(scanId);
    if (callback) {
      callback(progress);
    }
  }

  private getTagResultErrorMessage(tagResult: NfcTagReadResult): string {
    switch (tagResult.type) {
      case 'NO_NFC':
        return 'NFC is not available on this device';
      case 'INVALID_TAG':
        return 'Invalid or unsupported NFC tag';
      case 'AUTHENTICATION_FAILED':
        return 'Authentication with NFC tag failed';
      case 'READ_ERROR':
        return tagResult.error;
      case 'PARSING_ERROR':
        return tagResult.error;
      default:
        return 'Unknown scan error';
    }
  }

  private finalizeScan(
    scanId: string, 
    startTime: number, 
    result: Partial<ScanOperationResult>
  ): ServiceResult<ScanOperationResult> {
    this.activeScanIds.delete(scanId);
    this.scanCallbacks.delete(scanId);

    const finalResult: ScanOperationResult = {
      scanId,
      success: false,
      timestamp: startTime,
      duration: Date.now() - startTime,
      ...result,
    };

    this.log('info', `Scan finalized: ${scanId}`, {
      success: finalResult.success,
      duration: finalResult.duration,
      error: finalResult.error,
    });

    return ValidationResult.success(finalResult);
  }

  protected override async doCleanup(): Promise<void> {
    // Cancel all active scans
    for (const scanId of this.activeScanIds) {
      await this.cancelScan(scanId);
    }

    this.scanCallbacks.clear();
    this.activeScanIds.clear();
  }
}