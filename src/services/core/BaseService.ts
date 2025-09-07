/**
 * BaseService - Abstract base class for all business logic services
 * Provides common patterns and lifecycle management
 */

import { ValidationResult } from '../validation';

export abstract class BaseService {
  protected isInitialized = false;

  /**
   * Initialize the service
   * Override in subclasses to perform service-specific initialization
   */
  async initialize(): Promise<void> {
    if (this.isInitialized) {
      return;
    }

    await this.doInitialize();
    this.isInitialized = true;
  }

  /**
   * Check if service is initialized
   */
  getInitialized(): boolean {
    return this.isInitialized;
  }

  /**
   * Cleanup resources
   * Override in subclasses to perform service-specific cleanup
   */
  async cleanup(): Promise<void> {
    if (!this.isInitialized) {
      return;
    }

    await this.doCleanup();
    this.isInitialized = false;
  }

  /**
   * Validate service is initialized before operations
   */
  protected ensureInitialized(): void {
    if (!this.isInitialized) {
      throw new Error(`${this.constructor.name} is not initialized. Call initialize() first.`);
    }
  }

  /**
   * Perform service-specific initialization
   * Override in subclasses
   */
  protected async doInitialize(): Promise<void> {
    // Default implementation does nothing
  }

  /**
   * Perform service-specific cleanup
   * Override in subclasses  
   */
  protected async doCleanup(): Promise<void> {
    // Default implementation does nothing
  }

  /**
   * Handle errors consistently across services
   */
  protected handleError<T>(error: unknown, context: string): ValidationResult<T> {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    console.error(`${this.constructor.name} error in ${context}:`, error);
    
    return ValidationResult.singleError(
      context,
      'SERVICE_ERROR',
      `${this.constructor.name}: ${errorMessage}`
    );
  }

  /**
   * Log operations for debugging
   */
  protected log(level: 'debug' | 'info' | 'warn' | 'error', message: string, data?: any): void {
    const logMessage = `[${this.constructor.name}] ${message}`;
    
    switch (level) {
      case 'debug':
        console.debug(logMessage, data);
        break;
      case 'info':
        console.info(logMessage, data);
        break;
      case 'warn':
        console.warn(logMessage, data);
        break;
      case 'error':
        console.error(logMessage, data);
        break;
    }
  }
}

/**
 * Service operation result wrapper
 */
export type ServiceResult<T> = ValidationResult<T>;

/**
 * Service configuration interface
 */
export interface ServiceConfig {
  enableLogging?: boolean;
  logLevel?: 'debug' | 'info' | 'warn' | 'error';
}

/**
 * Service lifecycle state
 */
export enum ServiceState {
  UNINITIALIZED = 'UNINITIALIZED',
  INITIALIZING = 'INITIALIZING',
  READY = 'READY',
  ERROR = 'ERROR',
  CLEANUP = 'CLEANUP',
}