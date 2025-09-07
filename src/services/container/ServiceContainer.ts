/**
 * ServiceContainer - Dependency injection container for service management
 * Provides singleton pattern, dependency resolution, and lifecycle management
 */

import { BaseService } from '../core/BaseService';
import { ValidationResult } from '../validation';

export interface ServiceConstructor<T extends BaseService> {
  new (...args: any[]): T;
}

export interface ServiceRegistration<T extends BaseService> {
  constructor: ServiceConstructor<T>;
  singleton: boolean;
  dependencies: string[];
  instance?: T;
  initialized: boolean;
}

export interface ServiceConfig {
  enableAutoInitialization?: boolean;
  enableLogging?: boolean;
}

export class ServiceContainer {
  private static instance: ServiceContainer | null = null;
  private services: Map<string, ServiceRegistration<any>> = new Map();
  private config: ServiceConfig;
  private initializationOrder: string[] = [];

  private constructor(config: ServiceConfig = {}) {
    this.config = {
      enableAutoInitialization: true,
      enableLogging: false,
      ...config,
    };
  }

  /**
   * Get the singleton instance of ServiceContainer
   */
  static getInstance(config?: ServiceConfig): ServiceContainer {
    if (!ServiceContainer.instance) {
      ServiceContainer.instance = new ServiceContainer(config);
    }
    return ServiceContainer.instance;
  }

  /**
   * Reset the singleton instance (useful for testing)
   */
  static resetInstance(): void {
    ServiceContainer.instance = null;
  }

  /**
   * Register a service with the container
   */
  register<T extends BaseService>(
    name: string,
    constructor: ServiceConstructor<T>,
    options: {
      singleton?: boolean;
      dependencies?: string[];
    } = {}
  ): void {
    const { singleton = true, dependencies = [] } = options;

    if (this.services.has(name)) {
      throw new Error(`Service '${name}' is already registered`);
    }

    this.services.set(name, {
      constructor,
      singleton,
      dependencies,
      initialized: false,
    });

    this.log(`Registered service: ${name}${singleton ? ' (singleton)' : ''}`);
  }

  /**
   * Register a service instance directly
   */
  registerInstance<T extends BaseService>(
    name: string,
    instance: T,
    dependencies: string[] = []
  ): void {
    if (this.services.has(name)) {
      throw new Error(`Service '${name}' is already registered`);
    }

    this.services.set(name, {
      constructor: instance.constructor as ServiceConstructor<T>,
      singleton: true,
      dependencies,
      instance,
      initialized: instance.getInitialized(),
    });

    this.log(`Registered service instance: ${name}`);
  }

  /**
   * Resolve a service by name
   */
  async resolve<T extends BaseService>(name: string): Promise<T> {
    const registration = this.services.get(name);
    if (!registration) {
      throw new Error(`Service '${name}' is not registered`);
    }

    // Return existing singleton instance if available
    if (registration.singleton && registration.instance) {
      return registration.instance as T;
    }

    // Resolve dependencies first
    const resolvedDependencies = await this.resolveDependencies(registration.dependencies);

    // Create new instance
    const instance = new registration.constructor(...resolvedDependencies) as T;

    // Store singleton instance
    if (registration.singleton) {
      registration.instance = instance;
    }

    // Auto-initialize if enabled
    if (this.config.enableAutoInitialization && !registration.initialized) {
      await instance.initialize();
      registration.initialized = true;
    }

    this.log(`Resolved service: ${name}`);
    return instance;
  }

  /**
   * Check if a service is registered
   */
  isRegistered(name: string): boolean {
    return this.services.has(name);
  }

  /**
   * Get list of registered service names
   */
  getRegisteredServices(): string[] {
    return Array.from(this.services.keys());
  }

  /**
   * Initialize all registered services
   */
  async initializeAll(): Promise<ValidationResult<void>> {
    try {
      // Calculate initialization order based on dependencies
      const initOrder = this.calculateInitializationOrder();
      
      for (const serviceName of initOrder) {
        const registration = this.services.get(serviceName);
        if (registration && !registration.initialized) {
          // Ensure service is resolved and initialized
          const service = await this.resolve(serviceName);
          if (!service.getInitialized()) {
            await service.initialize();
          }
          registration.initialized = true;
          
          this.log(`Initialized service: ${serviceName}`);
        }
      }

      this.log(`Initialized all services in order: ${initOrder.join(' -> ')}`);
      return ValidationResult.success(undefined);

    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      console.error('Service container initialization failed:', error);
      
      return ValidationResult.singleError(
        'initialization',
        'INIT_FAILED',
        `Failed to initialize services: ${errorMessage}`
      );
    }
  }

  /**
   * Clean up all services
   */
  async cleanupAll(): Promise<void> {
    const cleanupOrder = [...this.initializationOrder].reverse();
    
    for (const serviceName of cleanupOrder) {
      const registration = this.services.get(serviceName);
      if (registration?.instance) {
        try {
          await registration.instance.cleanup();
          registration.initialized = false;
          this.log(`Cleaned up service: ${serviceName}`);
        } catch (error) {
          console.error(`Failed to cleanup service '${serviceName}':`, error);
        }
      }
    }

    // Clear all instances but keep registrations
    for (const registration of this.services.values()) {
      registration.instance = undefined;
      registration.initialized = false;
    }

    this.log('Cleaned up all services');
  }

  /**
   * Create a scoped container with specific services
   */
  createScope(serviceNames: string[]): ServiceContainer {
    const scopedContainer = new ServiceContainer(this.config);
    
    for (const serviceName of serviceNames) {
      const registration = this.services.get(serviceName);
      if (registration) {
        scopedContainer.services.set(serviceName, {
          ...registration,
          instance: undefined, // New instances in scope
          initialized: false,
        });
      }
    }

    return scopedContainer;
  }

  /**
   * Validate service dependencies
   */
  validateDependencies(): ValidationResult<void> {
    const issues: string[] = [];
    
    for (const [serviceName, registration] of this.services) {
      // Check if all dependencies are registered
      for (const dependency of registration.dependencies) {
        if (!this.services.has(dependency)) {
          issues.push(`Service '${serviceName}' depends on unregistered service '${dependency}'`);
        }
      }
    }

    // Check for circular dependencies
    const circularDeps = this.findCircularDependencies();
    if (circularDeps.length > 0) {
      issues.push(`Circular dependencies detected: ${circularDeps.join(' -> ')}`);
    }

    if (issues.length > 0) {
      return ValidationResult.singleError(
        'dependencies',
        'INVALID_DEPENDENCIES',
        issues.join('; ')
      );
    }

    return ValidationResult.success(undefined);
  }

  // Private methods

  private async resolveDependencies(dependencies: string[]): Promise<BaseService[]> {
    const resolved: BaseService[] = [];
    
    for (const dependency of dependencies) {
      const service = await this.resolve(dependency);
      resolved.push(service);
    }

    return resolved;
  }

  private calculateInitializationOrder(): string[] {
    const visited = new Set<string>();
    const visiting = new Set<string>();
    const order: string[] = [];

    const visit = (serviceName: string) => {
      if (visiting.has(serviceName)) {
        throw new Error(`Circular dependency detected involving service '${serviceName}'`);
      }
      
      if (visited.has(serviceName)) {
        return;
      }

      visiting.add(serviceName);
      
      const registration = this.services.get(serviceName);
      if (registration) {
        for (const dependency of registration.dependencies) {
          visit(dependency);
        }
      }

      visiting.delete(serviceName);
      visited.add(serviceName);
      order.push(serviceName);
    };

    for (const serviceName of this.services.keys()) {
      if (!visited.has(serviceName)) {
        visit(serviceName);
      }
    }

    this.initializationOrder = order;
    return order;
  }

  private findCircularDependencies(): string[] {
    const visited = new Set<string>();
    const recursionStack = new Set<string>();
    let circularPath: string[] = [];

    const hasCycle = (serviceName: string, path: string[]): boolean => {
      if (!visited.has(serviceName)) {
        visited.add(serviceName);
        recursionStack.add(serviceName);

        const registration = this.services.get(serviceName);
        if (registration) {
          for (const dependency of registration.dependencies) {
            if (!visited.has(dependency) && hasCycle(dependency, [...path, dependency])) {
              return true;
            } else if (recursionStack.has(dependency)) {
              circularPath = [...path, dependency];
              return true;
            }
          }
        }
      }

      recursionStack.delete(serviceName);
      return false;
    };

    for (const serviceName of this.services.keys()) {
      if (hasCycle(serviceName, [serviceName])) {
        return circularPath;
      }
    }

    return [];
  }

  private log(message: string): void {
    if (this.config.enableLogging) {
      console.info(`[ServiceContainer] ${message}`);
    }
  }
}