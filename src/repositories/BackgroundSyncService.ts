import {AppState, AppStateStatus} from 'react-native';
import {GraphRepository} from './GraphRepository';

interface BackgroundTask {
  id: string;
  type: 'SYNC' | 'CLEANUP' | 'MAINTENANCE' | 'EXPORT';
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  createdAt: number;
  scheduledAt?: number;
  maxRetries: number;
  retryCount: number;
  lastAttempt?: number;
  execute: () => Promise<boolean>;
  onComplete?: (success: boolean) => void;
  onError?: (error: Error) => void;
}

interface SyncMetrics {
  totalTasks: number;
  completedTasks: number;
  failedTasks: number;
  averageExecutionTime: number;
  lastSyncTime: number;
}

export class BackgroundSyncService {
  private repository: GraphRepository;
  private tasks: Map<string, BackgroundTask> = new Map();
  private isRunning = false;
  private isPaused = false;
  private appStateSubscription?: any;
  private syncInterval?: NodeJS.Timeout;
  private maintenanceInterval?: NodeJS.Timeout;
  private metrics: SyncMetrics;

  private readonly SYNC_INTERVAL = 30000; // 30 seconds
  private readonly MAINTENANCE_INTERVAL = 300000; // 5 minutes
  private readonly MAX_CONCURRENT_TASKS = 3;
  private readonly TASK_TIMEOUT = 30000; // 30 seconds

  constructor(repository: GraphRepository) {
    this.repository = repository;
    this.metrics = {
      totalTasks: 0,
      completedTasks: 0,
      failedTasks: 0,
      averageExecutionTime: 0,
      lastSyncTime: 0,
    };

    this.setupAppStateListener();
  }

  // Lifecycle management
  start(): void {
    if (this.isRunning) {
      return;
    }

    this.isRunning = true;
    this.isPaused = false;

    // Start sync interval
    this.syncInterval = setInterval(() => {
      this.processTasks();
    }, this.SYNC_INTERVAL);

    // Start maintenance interval
    this.maintenanceInterval = setInterval(() => {
      this.performMaintenance();
    }, this.MAINTENANCE_INTERVAL);

    // Initial task processing
    this.processTasks();
  }

  stop(): void {
    this.isRunning = false;

    if (this.syncInterval) {
      clearInterval(this.syncInterval);
      this.syncInterval = undefined;
    }

    if (this.maintenanceInterval) {
      clearInterval(this.maintenanceInterval);
      this.maintenanceInterval = undefined;
    }
  }

  pause(): void {
    this.isPaused = true;
  }

  resume(): void {
    this.isPaused = false;
    if (this.isRunning) {
      this.processTasks();
    }
  }

  // Task management
  scheduleTask(taskConfig: {
    type: BackgroundTask['type'];
    priority: BackgroundTask['priority'];
    scheduledAt?: number;
    maxRetries?: number;
    execute: () => Promise<boolean>;
    onComplete?: (success: boolean) => void;
    onError?: (error: Error) => void;
  }): string {
    const taskId = `${taskConfig.type}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    
    const task: BackgroundTask = {
      id: taskId,
      type: taskConfig.type,
      priority: taskConfig.priority,
      createdAt: Date.now(),
      scheduledAt: taskConfig.scheduledAt,
      maxRetries: taskConfig.maxRetries || 3,
      retryCount: 0,
      execute: taskConfig.execute,
      onComplete: taskConfig.onComplete,
      onError: taskConfig.onError,
    };

    this.tasks.set(taskId, task);
    this.metrics.totalTasks++;

    // If high priority and service is running, try to execute immediately
    if (taskConfig.priority === 'HIGH' && this.isRunning && !this.isPaused) {
      setTimeout(() => this.processTasks(), 100);
    }

    return taskId;
  }

  cancelTask(taskId: string): boolean {
    return this.tasks.delete(taskId);
  }

  getTask(taskId: string): BackgroundTask | undefined {
    return this.tasks.get(taskId);
  }

  // Task processing
  private async processTasks(): Promise<void> {
    if (this.isPaused || !this.isRunning) {
      return;
    }

    const readyTasks = this.getReadyTasks();
    const highPriorityTasks = readyTasks.filter(task => task.priority === 'HIGH');
    const mediumPriorityTasks = readyTasks.filter(task => task.priority === 'MEDIUM');
    const lowPriorityTasks = readyTasks.filter(task => task.priority === 'LOW');

    // Process tasks by priority
    const tasksToProcess = [
      ...highPriorityTasks,
      ...mediumPriorityTasks,
      ...lowPriorityTasks,
    ].slice(0, this.MAX_CONCURRENT_TASKS);

    const processingPromises = tasksToProcess.map(task => this.executeTask(task));
    
    if (processingPromises.length > 0) {
      await Promise.allSettled(processingPromises);
    }
  }

  private getReadyTasks(): BackgroundTask[] {
    const now = Date.now();
    const readyTasks: BackgroundTask[] = [];

    this.tasks.forEach(task => {
      // Check if task is scheduled for future execution
      if (task.scheduledAt && task.scheduledAt > now) {
        return;
      }

      // Check if task has exceeded retry limit
      if (task.retryCount >= task.maxRetries) {
        return;
      }

      // Check if task was attempted recently (backoff)
      if (task.lastAttempt) {
        const backoffTime = this.calculateBackoffTime(task.retryCount);
        if (now - task.lastAttempt < backoffTime) {
          return;
        }
      }

      readyTasks.push(task);
    });

    return readyTasks;
  }

  private async executeTask(task: BackgroundTask): Promise<void> {
    const startTime = Date.now();
    task.lastAttempt = startTime;
    task.retryCount++;

    try {
      // Execute with timeout
      const success = await this.executeWithTimeout(task.execute, this.TASK_TIMEOUT);
      
      const executionTime = Date.now() - startTime;
      this.updateMetrics(executionTime, success);

      if (success) {
        this.tasks.delete(task.id);
        this.metrics.completedTasks++;
        task.onComplete?.(true);
      } else {
        if (task.retryCount >= task.maxRetries) {
          this.tasks.delete(task.id);
          this.metrics.failedTasks++;
          task.onComplete?.(false);
        }
      }
    } catch (error) {
      const executionTime = Date.now() - startTime;
      this.updateMetrics(executionTime, false);

      if (task.retryCount >= task.maxRetries) {
        this.tasks.delete(task.id);
        this.metrics.failedTasks++;
        task.onError?.(error as Error);
      }

      console.error(`Background task ${task.id} failed:`, error);
    }
  }

  private async executeWithTimeout<T>(
    fn: () => Promise<T>,
    timeout: number
  ): Promise<T> {
    return new Promise((resolve, reject) => {
      const timeoutId = setTimeout(() => {
        reject(new Error('Task execution timeout'));
      }, timeout);

      fn()
        .then(result => {
          clearTimeout(timeoutId);
          resolve(result);
        })
        .catch(error => {
          clearTimeout(timeoutId);
          reject(error);
        });
    });
  }

  private calculateBackoffTime(retryCount: number): number {
    // Exponential backoff: 1s, 2s, 4s, 8s, etc.
    return Math.min(1000 * Math.pow(2, retryCount - 1), 30000);
  }

  // App state management
  private setupAppStateListener(): void {
    this.appStateSubscription = AppState.addEventListener('change', this.handleAppStateChange.bind(this));
  }

  private handleAppStateChange(nextAppState: AppStateStatus): void {
    switch (nextAppState) {
      case 'active':
        this.resume();
        // Trigger immediate sync when app becomes active
        setTimeout(() => this.processTasks(), 1000);
        break;
      case 'background':
      case 'inactive':
        // Continue running in background but reduce frequency
        break;
    }
  }

  // Maintenance tasks
  private async performMaintenance(): Promise<void> {
    if (this.isPaused || !this.isRunning) {
      return;
    }

    // Clean up old completed tasks from memory
    this.cleanupOldTasks();

    // Schedule cache cleanup
    this.scheduleTask({
      type: 'CLEANUP',
      priority: 'LOW',
      execute: async () => {
        this.repository.cleanupCache();
        return true;
      },
    });

    // Schedule repository maintenance
    this.scheduleTask({
      type: 'MAINTENANCE',
      priority: 'LOW',
      execute: async () => {
        await this.repository.saveToStorage();
        return true;
      },
    });
  }

  private cleanupOldTasks(): void {
    const now = Date.now();
    const maxAge = 24 * 60 * 60 * 1000; // 24 hours

    this.tasks.forEach((task, id) => {
      if (now - task.createdAt > maxAge && task.retryCount >= task.maxRetries) {
        this.tasks.delete(id);
      }
    });
  }

  // Built-in task types
  scheduleDataSync(): string {
    return this.scheduleTask({
      type: 'SYNC',
      priority: 'MEDIUM',
      execute: async () => {
        return await this.repository.saveToStorage();
      },
      onComplete: (success) => {
        if (success) {
          this.metrics.lastSyncTime = Date.now();
        }
      },
    });
  }

  scheduleCacheCleanup(): string {
    return this.scheduleTask({
      type: 'CLEANUP',
      priority: 'LOW',
      execute: async () => {
        this.repository.cleanupCache();
        return true;
      },
    });
  }

  scheduleDataExport(): string {
    return this.scheduleTask({
      type: 'EXPORT',
      priority: 'LOW',
      execute: async () => {
        try {
          const data = await this.repository.exportData();
          // Here you could save to a backup location or send to a server
          console.log('Data export completed', data);
          return true;
        } catch (error) {
          console.error('Data export failed:', error);
          return false;
        }
      },
    });
  }

  // Statistics and monitoring
  getMetrics(): SyncMetrics {
    return {...this.metrics};
  }

  getTaskCount(): number {
    return this.tasks.size;
  }

  getTasksByType(): Record<BackgroundTask['type'], number> {
    const counts: Record<BackgroundTask['type'], number> = {
      SYNC: 0,
      CLEANUP: 0,
      MAINTENANCE: 0,
      EXPORT: 0,
    };

    this.tasks.forEach(task => {
      counts[task.type]++;
    });

    return counts;
  }

  private updateMetrics(executionTime: number, success: boolean): void {
    if (success) {
      // Update average execution time
      const totalExecutions = this.metrics.completedTasks + 1;
      this.metrics.averageExecutionTime = 
        (this.metrics.averageExecutionTime * this.metrics.completedTasks + executionTime) / totalExecutions;
    }
  }

  // Cleanup
  dispose(): void {
    this.stop();
    
    if (this.appStateSubscription) {
      this.appStateSubscription.remove();
      this.appStateSubscription = undefined;
    }

    this.tasks.clear();
  }
}