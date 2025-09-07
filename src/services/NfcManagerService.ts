/**
 * Enhanced NfcManagerService - Advanced NFC scanning service for B-Scan
 * Provides multi-format tag support, progress reporting, and sophisticated error handling
 * Based on Kotlin implementation patterns from MrBambuSpoolPal
 */

import { NfcManager, TagReadResult } from './NfcManager';
import {
  ScanProgress,
  ScanStage,
  NfcScanError,
  NfcErrorType,
  TagFormat,
  ScanCache,
  CachedScanResult,
  FilamentInfo,
} from '../types/FilamentInfo';

export interface NfcScanOptions {
  enableCache?: boolean;
  cacheTimeout?: number;
  maxRetries?: number;
  progressCallback?: (progress: ScanProgress) => void;
  errorCallback?: (error: NfcScanError) => void;
  formats?: TagFormat[];
  timeout?: number;
}

export interface NfcManagerServiceConfig {
  cacheSize: number;
  cacheTtl: number;
  defaultTimeout: number;
  enableLogging: boolean;
  retryAttempts: number;
  supportedFormats: TagFormat[];
}

export class NfcManagerService {
  private static instance: NfcManagerService | null = null;
  private nfcManager: NfcManager | null = null;
  private config: NfcManagerServiceConfig;
  private scanCache: ScanCache;
  private isInitialized = false;
  private currentScan: Promise<TagReadResult> | null = null;
  private scanId = 0;

  private constructor(config?: Partial<NfcManagerServiceConfig>) {
    this.config = {
      cacheSize: 100,
      cacheTtl: 300000, // 5 minutes
      defaultTimeout: 15000, // 15 seconds
      enableLogging: true,
      retryAttempts: 3,
      supportedFormats: [
        TagFormat.BAMBU_LAB,
        TagFormat.CREALITY,
        TagFormat.OPENSPOOL,
      ],
      ...config,
    };

    this.scanCache = {
      entries: new Map<string, CachedScanResult>(),
      maxSize: this.config.cacheSize,
      ttlMs: this.config.cacheTtl,
    };
  }

  /**
   * Get the singleton instance of NfcManagerService
   */
  static getInstance(config?: Partial<NfcManagerServiceConfig>): NfcManagerService {
    if (!NfcManagerService.instance) {
      NfcManagerService.instance = new NfcManagerService(config);
    }
    return NfcManagerService.instance;
  }

  /**
   * Reset the singleton instance (useful for testing)
   */
  static resetInstance(): void {
    if (NfcManagerService.instance) {
      NfcManagerService.instance.cleanup();
    }
    NfcManagerService.instance = null;
  }

  /**
   * Initialize the NFC service
   */
  async initialize(): Promise<boolean> {
    if (this.isInitialized) {
      return true;
    }

    try {
      this.nfcManager = new NfcManager();
      const initialized = await this.nfcManager.initialize();
      
      if (initialized) {
        this.isInitialized = true;
        this.log('NfcManagerService initialized successfully');
      } else {
        this.log('NfcManagerService initialization failed');
      }
      
      return initialized;
    } catch (error) {
      this.log('NfcManagerService initialization error:', error);
      return false;
    }
  }

  /**
   * Check if NFC is available and enabled on the device
   */
  async isNfcAvailable(): Promise<boolean> {
    if (!this.isInitialized) {
      const initialized = await this.initialize();
      if (!initialized) return false;
    }

    return this.nfcManager?.isAvailable() ?? false;
  }

  /**
   * Check if NFC is enabled on the device
   */
  async isNfcEnabled(): Promise<boolean> {
    if (!this.isInitialized) {
      const initialized = await this.initialize();
      if (!initialized) return false;
    }

    return this.nfcManager?.isNfcEnabled() ?? false;
  }

  /**
   * Enhanced tag scanning with multi-format support and progress reporting
   */
  async scanTag(options: NfcScanOptions = {}): Promise<TagReadResult> {
    const scanId = ++this.scanId;
    this.log(`Starting scan ${scanId} with options:`, options);

    // Prevent concurrent scans
    if (this.currentScan) {
      throw this.createError(
        NfcErrorType.UNKNOWN,
        'Scan already in progress',
        ScanStage.INITIALIZING,
        'NFC_CONCURRENT_SCAN'
      );
    }

    const {
      enableCache = true,
      cacheTimeout = this.config.cacheTtl,
      maxRetries = this.config.retryAttempts,
      progressCallback,
      errorCallback,
      formats = this.config.supportedFormats,
      timeout = this.config.defaultTimeout,
    } = options;

    try {
      const scanOptions: {
        scanId: number;
        enableCache: boolean;
        cacheTimeout: number;
        maxRetries: number;
        formats: TagFormat[];
        timeout: number;
        progressCallback?: (progress: ScanProgress) => void;
        errorCallback?: (error: NfcScanError) => void;
      } = {
        scanId,
        enableCache,
        cacheTimeout,
        maxRetries,
        formats,
        timeout,
      };

      if (progressCallback) {
        scanOptions.progressCallback = progressCallback;
      }
      if (errorCallback) {
        scanOptions.errorCallback = errorCallback;
      }

      this.currentScan = this.performEnhancedScan(scanOptions);

      return await this.currentScan;
    } finally {
      this.currentScan = null;
    }
  }

  /**
   * Cancel any ongoing scan
   */
  async cancelScan(): Promise<void> {
    if (!this.nfcManager) {
      return;
    }

    try {
      await this.nfcManager.cancelScan();
      this.currentScan = null;
      this.log('Scan cancelled successfully');
    } catch (error) {
      this.log('Error cancelling scan:', error);
    }
  }

  /**
   * Stop ongoing scan (alias for cancelScan for backward compatibility)
   */
  async stopScan(): Promise<void> {
    return this.cancelScan();
  }

  /**
   * Set scan progress callback
   */
  setScanProgressCallback(callback: (progress: ScanProgress) => void): void {
    if (this.nfcManager) {
      this.nfcManager.setScanProgressCallback(callback);
    }
  }

  /**
   * Get cached scan results
   */
  getCachedResult(tagUid: string): FilamentInfo | null {
    const cached = this.scanCache.entries.get(tagUid);
    
    if (!cached) {
      return null;
    }

    const now = Date.now();
    if (now - cached.timestamp > this.scanCache.ttlMs) {
      this.scanCache.entries.delete(tagUid);
      return null;
    }

    cached.hitCount++;
    cached.lastAccessed = now;
    return cached.filamentInfo;
  }

  /**
   * Clear scan cache
   */
  clearCache(): void {
    this.scanCache.entries.clear();
    this.log('Scan cache cleared');
  }

  /**
   * Get cache statistics
   */
  getCacheStats(): { size: number; hitRate: number; oldestEntry: number } {
    const entries = Array.from(this.scanCache.entries.values());
    const totalHits = entries.reduce((sum, entry) => sum + entry.hitCount, 0);
    const totalRequests = entries.length + totalHits;
    const hitRate = totalRequests > 0 ? totalHits / totalRequests : 0;
    const oldestEntry = entries.length > 0 
      ? Math.min(...entries.map(e => e.timestamp))
      : Date.now();

    return {
      size: this.scanCache.entries.size,
      hitRate,
      oldestEntry,
    };
  }

  /**
   * Update service configuration
   */
  updateConfig(newConfig: Partial<NfcManagerServiceConfig>): void {
    this.config = { ...this.config, ...newConfig };
    this.scanCache.maxSize = this.config.cacheSize;
    this.scanCache.ttlMs = this.config.cacheTtl;
    this.log('Configuration updated:', this.config);
  }

  /**
   * Get current configuration
   */
  getConfig(): NfcManagerServiceConfig {
    return { ...this.config };
  }

  /**
   * Clean up NFC resources
   */
  async cleanup(): Promise<void> {
    try {
      if (this.currentScan) {
        await this.cancelScan();
      }
      
      if (this.nfcManager) {
        await this.nfcManager.cleanup();
        this.nfcManager = null;
      }
      
      this.clearCache();
      this.isInitialized = false;
      this.log('NfcManagerService cleanup completed');
    } catch (error) {
      this.log('Error during cleanup:', error);
    }
  }

  // Private methods

  private async performEnhancedScan(params: {
    scanId: number;
    enableCache: boolean;
    cacheTimeout: number;
    maxRetries: number;
    progressCallback?: (progress: ScanProgress) => void;
    errorCallback?: (error: NfcScanError) => void;
    formats: TagFormat[];
    timeout: number;
  }): Promise<TagReadResult> {
    const { scanId, enableCache, progressCallback, errorCallback, maxRetries } = params;

    this.updateProgress(progressCallback, ScanStage.INITIALIZING, 0, 'Initializing NFC scan...');

    if (!this.isInitialized) {
      const initialized = await this.initialize();
      if (!initialized) {
        const error = this.createError(
          NfcErrorType.NFC_UNAVAILABLE,
          'NFC not available on this device',
          ScanStage.INITIALIZING,
          'NFC_UNAVAILABLE'
        );
        errorCallback?.(error);
        return { type: 'NO_NFC' };
      }
    }

    this.updateProgress(progressCallback, ScanStage.NFC_DETECTION, 10, 'Detecting NFC tag...');

    let lastError: NfcScanError | null = null;
    
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        this.log(`Scan attempt ${attempt}/${maxRetries} for scan ${scanId}`);
        
        const result = await this.attemptScan({
          ...params,
          attempt,
          lastError,
        });
        
        if (result.type === 'SUCCESS' && enableCache) {
          this.cacheResult(result.filamentInfo);
        }
        
        return result;
      } catch (error) {
        lastError = error instanceof Error 
          ? this.createError(
              NfcErrorType.UNKNOWN,
              error.message,
              ScanStage.ERROR,
              'SCAN_ATTEMPT_FAILED'
            )
          : error as NfcScanError;
        
        if (attempt === maxRetries) {
          errorCallback?.(lastError);
          return this.createTagReadResultFromError(lastError);
        }
        
        this.log(`Scan attempt ${attempt} failed, retrying...`, lastError);
        await this.delay(1000 * attempt); // Progressive delay
      }
    }

    const finalError = lastError || this.createError(
      NfcErrorType.UNKNOWN,
      'All scan attempts failed',
      ScanStage.ERROR,
      'ALL_ATTEMPTS_FAILED'
    );
    
    errorCallback?.(finalError);
    return this.createTagReadResultFromError(finalError);
  }

  private async attemptScan(params: {
    scanId: number;
    enableCache: boolean;
    progressCallback?: (progress: ScanProgress) => void;
    formats: TagFormat[];
    timeout: number;
    attempt: number;
    lastError: NfcScanError | null;
  }): Promise<TagReadResult> {
    const { progressCallback, enableCache } = params;
    
    if (!this.nfcManager) {
      throw this.createError(
        NfcErrorType.NFC_UNAVAILABLE,
        'NFC manager not initialized',
        ScanStage.INITIALIZING,
        'MANAGER_NOT_INITIALIZED'
      );
    }

    // Check cache first if enabled
    this.updateProgress(progressCallback, ScanStage.INITIALIZING, 15, 'Checking cache...');
    
    const result = await this.nfcManager.scanTag();
    
    if (result.type === 'SUCCESS' && enableCache) {
      const cached = this.getCachedResult(result.filamentInfo.tagUid);
      if (cached) {
        this.updateProgress(progressCallback, ScanStage.COMPLETED, 100, 'Scan completed from cache');
        return {
          type: 'SUCCESS',
          filamentInfo: cached,
        };
      }
    }

    this.updateProgress(progressCallback, ScanStage.COMPLETED, 100, 'Scan completed successfully');
    return result;
  }

  private cacheResult(filamentInfo: FilamentInfo): void {
    const now = Date.now();
    const cacheEntry: CachedScanResult = {
      tagUid: filamentInfo.tagUid,
      filamentInfo,
      timestamp: now,
      hitCount: 0,
      lastAccessed: now,
    };

    // Remove oldest entries if cache is full
    while (this.scanCache.entries.size >= this.scanCache.maxSize) {
      const oldestKey = this.findOldestCacheEntry();
      if (oldestKey) {
        this.scanCache.entries.delete(oldestKey);
      }
    }

    this.scanCache.entries.set(filamentInfo.tagUid, cacheEntry);
    this.log(`Cached scan result for UID: ${filamentInfo.tagUid}`);
  }

  private findOldestCacheEntry(): string | null {
    let oldestKey: string | null = null;
    let oldestTime = Date.now();

    this.scanCache.entries.forEach((entry, key) => {
      if (entry.lastAccessed < oldestTime) {
        oldestTime = entry.lastAccessed;
        oldestKey = key;
      }
    });

    return oldestKey;
  }

  private updateProgress(
    callback: ((progress: ScanProgress) => void) | undefined,
    stage: ScanStage,
    percentage: number,
    statusMessage: string,
    currentSector: number = 0
  ): void {
    if (callback) {
      callback({
        stage,
        percentage,
        currentSector,
        statusMessage,
      });
    }
  }

  private createError(
    type: NfcErrorType,
    message: string,
    stage: ScanStage,
    code: string
  ): NfcScanError {
    return {
      type,
      message,
      code,
      stage,
      timestamp: Date.now(),
    };
  }

  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  private log(message: string, ...args: unknown[]): void {
    if (this.config.enableLogging) {
      console.log(`[NfcManagerService] ${message}`, ...args);
    }
  }

  private createTagReadResultFromError(error: NfcScanError): TagReadResult {
    switch (error.type) {
      case NfcErrorType.NFC_DISABLED:
      case NfcErrorType.NFC_UNAVAILABLE:
        return { type: 'NO_NFC' };
      case NfcErrorType.INVALID_TAG_FORMAT:
      case NfcErrorType.TAG_LOST:
        return { type: 'INVALID_TAG' };
      case NfcErrorType.AUTHENTICATION_FAILED:
        return { type: 'AUTHENTICATION_FAILED' };
      case NfcErrorType.PARSING_ERROR:
      case NfcErrorType.CHECKSUM_MISMATCH:
        return { type: 'PARSING_ERROR', error: error.message };
      default:
        return { type: 'READ_ERROR', error: error.message };
    }
  }

  // Static convenience methods for backward compatibility

  static async isNfcEnabled(): Promise<boolean> {
    const service = NfcManagerService.getInstance();
    return service.isNfcEnabled();
  }

  static async scanTag(options?: NfcScanOptions): Promise<TagReadResult> {
    const service = NfcManagerService.getInstance();
    return service.scanTag(options);
  }

  static async cancelScan(): Promise<void> {
    const service = NfcManagerService.getInstance();
    return service.cancelScan();
  }

  static async cleanup(): Promise<void> {
    if (NfcManagerService.instance) {
      await NfcManagerService.instance.cleanup();
      NfcManagerService.instance = null;
    }
  }
}