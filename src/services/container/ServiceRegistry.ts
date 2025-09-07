/**
 * ServiceRegistry - Central registry for all application services
 * Provides pre-configured service registration and dependency wiring
 */

import { ServiceContainer } from './ServiceContainer';
import { BaseService } from '../core/BaseService';
import { ValidationResult } from '../validation';

// Import all services
import { ScanService } from '../scan/ScanService';
import { EntityService } from '../entity/EntityService';
import { ActivityService } from '../activity/ActivityService';
import { ComponentService } from '../component/ComponentService';

export class ServiceRegistry {
  private static instance: ServiceRegistry | null = null;
  private container: ServiceContainer;
  private isConfigured = false;

  private constructor() {
    this.container = ServiceContainer.getInstance({
      enableAutoInitialization: true,
      enableLogging: __DEV__ ?? false,
    });
  }

  /**
   * Get the singleton instance
   */
  static getInstance(): ServiceRegistry {
    if (!ServiceRegistry.instance) {
      ServiceRegistry.instance = new ServiceRegistry();
    }
    return ServiceRegistry.instance;
  }

  /**
   * Reset the singleton instance (for testing)
   */
  static resetInstance(): void {
    ServiceRegistry.instance = null;
    ServiceContainer.resetInstance();
  }

  /**
   * Configure all services with proper dependencies
   */
  async configure(): Promise<ValidationResult<void>> {
    if (this.isConfigured) {
      return ValidationResult.success(undefined);
    }

    try {
      // Register services in dependency order
      this.registerCoreServices();
      this.registerBusinessServices();

      // Validate dependencies
      const validationResult = this.container.validateDependencies();
      if (!validationResult.isValid) {
        return validationResult;
      }

      // Initialize all services
      const initResult = await this.container.initializeAll();
      if (!initResult.isValid) {
        return initResult;
      }

      // Wire up cross-dependencies
      await this.wireServiceDependencies();

      this.isConfigured = true;
      console.info('[ServiceRegistry] All services configured successfully');

      return ValidationResult.success(undefined);

    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      console.error('[ServiceRegistry] Configuration failed:', error);
      
      return ValidationResult.singleError(
        'configuration',
        'CONFIG_FAILED',
        `Service registry configuration failed: ${errorMessage}`
      );
    }
  }

  /**
   * Get a service by name
   */
  async getService<T extends BaseService>(serviceName: string): Promise<T> {
    if (!this.isConfigured) {
      throw new Error('ServiceRegistry must be configured before accessing services. Call configure() first.');
    }

    return this.container.resolve<T>(serviceName);
  }

  /**
   * Get scan service
   */
  async getScanService(): Promise<ScanService> {
    return this.getService<ScanService>('ScanService');
  }

  /**
   * Get entity service
   */
  async getEntityService(): Promise<EntityService> {
    return this.getService<EntityService>('EntityService');
  }

  /**
   * Get activity service
   */
  async getActivityService(): Promise<ActivityService> {
    return this.getService<ActivityService>('ActivityService');
  }

  /**
   * Get component service
   */
  async getComponentService(): Promise<ComponentService> {
    return this.getService<ComponentService>('ComponentService');
  }

  /**
   * Check if services are configured
   */
  isServiceConfigured(): boolean {
    return this.isConfigured;
  }

  /**
   * Get list of all registered services
   */
  getRegisteredServices(): string[] {
    return this.container.getRegisteredServices();
  }

  /**
   * Cleanup all services
   */
  async cleanup(): Promise<void> {
    try {
      await this.container.cleanupAll();
      this.isConfigured = false;
      console.info('[ServiceRegistry] All services cleaned up');
    } catch (error) {
      console.error('[ServiceRegistry] Cleanup failed:', error);
      throw error;
    }
  }

  /**
   * Create a scoped registry for testing or specific contexts
   */
  createScope(serviceNames: string[]): ServiceContainer {
    return this.container.createScope(serviceNames);
  }

  // Private methods

  private registerCoreServices(): void {
    // EntityService has no dependencies - register first
    this.container.register('EntityService', EntityService, {
      singleton: true,
      dependencies: [],
    });

    console.debug('[ServiceRegistry] Registered core services');
  }

  private registerBusinessServices(): void {
    // ActivityService depends on EntityService
    this.container.register('ActivityService', ActivityService, {
      singleton: true,
      dependencies: ['EntityService'],
    });

    // ComponentService depends on EntityService and ActivityService
    this.container.register('ComponentService', ComponentService, {
      singleton: true,
      dependencies: ['EntityService', 'ActivityService'],
    });

    // ScanService depends on all other services
    this.container.register('ScanService', ScanService, {
      singleton: true,
      dependencies: [],
    });

    console.debug('[ServiceRegistry] Registered business services');
  }

  private async wireServiceDependencies(): Promise<void> {
    try {
      // Get all services
      const entityService = await this.container.resolve<EntityService>('EntityService');
      const activityService = await this.container.resolve<ActivityService>('ActivityService');
      const componentService = await this.container.resolve<ComponentService>('ComponentService');
      const scanService = await this.container.resolve<ScanService>('ScanService');

      // Wire up dependencies that couldn't be injected via constructor
      activityService.setEntityService(entityService);
      componentService.setEntityService(entityService);
      componentService.setActivityService(activityService);

      console.debug('[ServiceRegistry] Wired service dependencies');

    } catch (error) {
      console.error('[ServiceRegistry] Failed to wire dependencies:', error);
      throw error;
    }
  }
}

/**
 * Convenience function to get the global service registry
 */
export function getServiceRegistry(): ServiceRegistry {
  return ServiceRegistry.getInstance();
}

/**
 * Convenience function to configure services (idempotent)
 */
export async function configureServices(): Promise<ValidationResult<void>> {
  return ServiceRegistry.getInstance().configure();
}

/**
 * Convenience function to cleanup all services
 */
export async function cleanupServices(): Promise<void> {
  return ServiceRegistry.getInstance().cleanup();
}