/**
 * NfcManagerService - Singleton service wrapper for NfcManager
 * Provides a single instance for use throughout the application
 */

import { NfcManager, TagReadResult } from './NfcManager';

export class NfcManagerService {
  private static instance: NfcManager | null = null;

  /**
   * Get the singleton instance of NfcManager
   */
  static getInstance(): NfcManager {
    if (!NfcManagerService.instance) {
      NfcManagerService.instance = new NfcManager();
    }
    return NfcManagerService.instance;
  }

  /**
   * Reset the singleton instance (useful for testing)
   */
  static resetInstance(): void {
    NfcManagerService.instance = null;
  }

  /**
   * Check if NFC is enabled on the device
   */
  static async isNfcEnabled(): Promise<boolean> {
    const manager = NfcManagerService.getInstance();
    return manager.isAvailable();
  }

  /**
   * Scan for an NFC tag
   */
  static async scanTag(): Promise<TagReadResult> {
    const manager = NfcManagerService.getInstance();
    return manager.scanTag();
  }

  /**
   * Cancel any ongoing scan
   */
  static async cancelScan(): Promise<void> {
    const manager = NfcManagerService.getInstance();
    return manager.cancelScan();
  }

  /**
   * Clean up NFC resources
   */
  static async cleanup(): Promise<void> {
    if (NfcManagerService.instance) {
      await NfcManagerService.instance.cleanup();
      NfcManagerService.instance = null;
    }
  }
}