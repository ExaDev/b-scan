/**
 * ActivityService - Tracks scan activities and history management
 * Manages activity logging, history tracking, and activity analytics
 */

import { BaseService, ServiceResult } from '../core/BaseService';
import { ValidationResult, ValidationUtils } from '../validation';
import { EntityService } from '../entity/EntityService';
import {
  Activity,
  EntityType,
  ScanHistoryEntry,
  FilamentInfo,
} from '../../types/FilamentInfo';

export interface ActivityCreateRequest {
  activityType: 'SCAN' | 'UPDATE' | 'CREATE' | 'DELETE';
  description: string;
  relatedEntityId?: string;
  metadata?: Record<string, any>;
  userId?: string;
}

export interface ActivityQuery {
  activityType?: 'SCAN' | 'UPDATE' | 'CREATE' | 'DELETE';
  relatedEntityId?: string;
  userId?: string;
  startDate?: number;
  endDate?: number;
  limit?: number;
  offset?: number;
}

export interface ActivityStats {
  totalActivities: number;
  activitiesByType: Record<string, number>;
  recentActivityCount: number;
  mostActiveDay: {
    date: string;
    count: number;
  } | null;
  averageActivitiesPerDay: number;
}

export interface ScanHistoryQuery {
  startDate?: number;
  endDate?: number;
  successful?: boolean;
  tagUid?: string;
  limit?: number;
  offset?: number;
}

export class ActivityService extends BaseService {
  private entityService?: EntityService;
  private scanHistory: Map<string, ScanHistoryEntry> = new Map();

  constructor(entityService?: EntityService) {
    super();
    this.entityService = entityService;
  }

  /**
   * Set the entity service dependency
   */
  setEntityService(entityService: EntityService): void {
    this.entityService = entityService;
  }

  /**
   * Record a new activity
   */
  async recordActivity(request: ActivityCreateRequest): Promise<ServiceResult<Activity>> {
    this.ensureInitialized();

    if (!this.entityService) {
      return ValidationResult.singleError(
        'entityService',
        'DEPENDENCY_MISSING',
        'EntityService is required but not provided'
      );
    }

    try {
      // Validate activity request
      const validationResult = this.validateActivityRequest(request);
      if (!validationResult.isValid) {
        return ValidationResult.error(validationResult.issues);
      }

      // Create activity entity
      const activity: Omit<Activity, 'id' | 'createdAt' | 'updatedAt'> = {
        type: EntityType.ACTIVITY,
        activityType: request.activityType,
        description: request.description,
        relatedEntityId: request.relatedEntityId,
      };

      const createResult = await this.entityService.createEntity({
        entityData: activity,
        generateId: true,
      });

      if (!createResult.isValid) {
        return ValidationResult.error(createResult.issues);
      }

      const createdActivity = createResult.data!;

      this.log('info', `Recorded activity: ${createdActivity.activityType} - ${createdActivity.id}`);
      return ValidationResult.success(createdActivity);

    } catch (error) {
      return this.handleError(error, 'recordActivity');
    }
  }

  /**
   * Record scan activity with scan result details
   */
  async recordScanActivity(
    filamentInfo: FilamentInfo,
    success: boolean,
    error?: string,
    metadata?: Record<string, any>
  ): Promise<ServiceResult<{ activity: Activity; historyEntry: ScanHistoryEntry }>> {
    this.ensureInitialized();

    try {
      // Record scan history entry
      const historyEntry: ScanHistoryEntry = {
        id: `scan_${filamentInfo.tagUid}_${Date.now()}`,
        timestamp: Date.now(),
        filamentInfo: success ? filamentInfo : undefined,
        result: success ? 'SUCCESS' : 'READ_ERROR',
        error,
      };

      this.scanHistory.set(historyEntry.id, historyEntry);

      // Record activity
      const description = success
        ? `Successfully scanned ${filamentInfo.manufacturerName} ${filamentInfo.filamentType} (${filamentInfo.colorName})`
        : `Failed to scan tag ${filamentInfo.tagUid}: ${error || 'Unknown error'}`;

      const activityResult = await this.recordActivity({
        activityType: 'SCAN',
        description,
        metadata: {
          ...metadata,
          scanHistoryId: historyEntry.id,
          tagUid: filamentInfo.tagUid,
          success,
        },
      });

      if (!activityResult.isValid) {
        // Remove history entry if activity recording failed
        this.scanHistory.delete(historyEntry.id);
        return ValidationResult.error(activityResult.issues);
      }

      return ValidationResult.success({
        activity: activityResult.data!,
        historyEntry,
      });

    } catch (error) {
      return this.handleError(error, 'recordScanActivity');
    }
  }

  /**
   * Get activities with filtering
   */
  async getActivities(query: ActivityQuery = {}): Promise<ServiceResult<Activity[]>> {
    this.ensureInitialized();

    if (!this.entityService) {
      return ValidationResult.singleError(
        'entityService',
        'DEPENDENCY_MISSING',
        'EntityService is required but not provided'
      );
    }

    try {
      const entityQuery = {
        entityType: EntityType.ACTIVITY,
        createdAfter: query.startDate,
        createdBefore: query.endDate,
        limit: query.limit,
        offset: query.offset,
      };

      const entitiesResult = await this.entityService.queryEntities<Activity>(entityQuery);
      if (!entitiesResult.isValid) {
        return ValidationResult.error(entitiesResult.issues);
      }

      let activities = entitiesResult.data!;

      // Apply additional filters
      if (query.activityType) {
        activities = activities.filter(activity => activity.activityType === query.activityType);
      }

      if (query.relatedEntityId) {
        activities = activities.filter(activity => activity.relatedEntityId === query.relatedEntityId);
      }

      return ValidationResult.success(activities);

    } catch (error) {
      return this.handleError(error, 'getActivities');
    }
  }

  /**
   * Get scan history with filtering
   */
  async getScanHistory(query: ScanHistoryQuery = {}): Promise<ServiceResult<ScanHistoryEntry[]>> {
    this.ensureInitialized();

    try {
      let historyEntries = Array.from(this.scanHistory.values());

      // Apply filters
      if (query.startDate !== undefined) {
        historyEntries = historyEntries.filter(entry => entry.timestamp >= query.startDate!);
      }

      if (query.endDate !== undefined) {
        historyEntries = historyEntries.filter(entry => entry.timestamp <= query.endDate!);
      }

      if (query.successful !== undefined) {
        historyEntries = historyEntries.filter(entry => 
          query.successful ? entry.result === 'SUCCESS' : entry.result !== 'SUCCESS'
        );
      }

      if (query.tagUid) {
        historyEntries = historyEntries.filter(entry => 
          entry.filamentInfo?.tagUid === query.tagUid
        );
      }

      // Sort by timestamp (newest first)
      historyEntries.sort((a, b) => b.timestamp - a.timestamp);

      // Apply pagination
      const offset = query.offset || 0;
      const limit = query.limit || 100;
      
      const paginatedEntries = historyEntries.slice(offset, offset + limit);

      return ValidationResult.success(paginatedEntries);

    } catch (error) {
      return this.handleError(error, 'getScanHistory');
    }
  }

  /**
   * Get recent scan activities (last 24 hours)
   */
  async getRecentScanActivities(limit: number = 10): Promise<ServiceResult<Activity[]>> {
    const oneDayAgo = Date.now() - (24 * 60 * 60 * 1000);
    
    return this.getActivities({
      activityType: 'SCAN',
      startDate: oneDayAgo,
      limit,
    });
  }

  /**
   * Get activity statistics
   */
  async getActivityStats(
    startDate?: number,
    endDate?: number
  ): Promise<ServiceResult<ActivityStats>> {
    this.ensureInitialized();

    try {
      const activitiesResult = await this.getActivities({
        startDate,
        endDate,
        limit: 10000, // Get all activities in range
      });

      if (!activitiesResult.isValid) {
        return ValidationResult.error(activitiesResult.issues);
      }

      const activities = activitiesResult.data!;

      // Calculate statistics
      const stats: ActivityStats = {
        totalActivities: activities.length,
        activitiesByType: {},
        recentActivityCount: 0,
        mostActiveDay: null,
        averageActivitiesPerDay: 0,
      };

      // Count by type
      activities.forEach(activity => {
        stats.activitiesByType[activity.activityType] = 
          (stats.activitiesByType[activity.activityType] || 0) + 1;
      });

      // Recent activities (last 24 hours)
      const oneDayAgo = Date.now() - (24 * 60 * 60 * 1000);
      stats.recentActivityCount = activities.filter(
        activity => activity.createdAt > oneDayAgo
      ).length;

      // Most active day
      if (activities.length > 0) {
        const dayGroups = new Map<string, number>();
        
        activities.forEach(activity => {
          const date = new Date(activity.createdAt).toISOString().split('T')[0];
          dayGroups.set(date, (dayGroups.get(date) || 0) + 1);
        });

        let maxCount = 0;
        let mostActiveDate = '';

        for (const [date, count] of dayGroups) {
          if (count > maxCount) {
            maxCount = count;
            mostActiveDate = date;
          }
        }

        if (maxCount > 0) {
          stats.mostActiveDay = {
            date: mostActiveDate,
            count: maxCount,
          };
        }

        // Average activities per day
        const dayCount = dayGroups.size;
        stats.averageActivitiesPerDay = dayCount > 0 ? activities.length / dayCount : 0;
      }

      return ValidationResult.success(stats);

    } catch (error) {
      return this.handleError(error, 'getActivityStats');
    }
  }

  /**
   * Get scan history statistics
   */
  async getScanHistoryStats(): Promise<ServiceResult<{
    totalScans: number;
    successfulScans: number;
    failedScans: number;
    successRate: number;
    uniqueTagsScanned: number;
    mostScannedTag?: { tagUid: string; count: number };
  }>> {
    this.ensureInitialized();

    try {
      const historyEntries = Array.from(this.scanHistory.values());
      
      const stats = {
        totalScans: historyEntries.length,
        successfulScans: historyEntries.filter(entry => entry.result === 'SUCCESS').length,
        failedScans: historyEntries.filter(entry => entry.result !== 'SUCCESS').length,
        successRate: 0,
        uniqueTagsScanned: 0,
        mostScannedTag: undefined as { tagUid: string; count: number } | undefined,
      };

      if (stats.totalScans > 0) {
        stats.successRate = (stats.successfulScans / stats.totalScans) * 100;
      }

      // Count unique tags and most scanned tag
      const tagCounts = new Map<string, number>();
      
      historyEntries.forEach(entry => {
        if (entry.filamentInfo?.tagUid) {
          const tagUid = entry.filamentInfo.tagUid;
          tagCounts.set(tagUid, (tagCounts.get(tagUid) || 0) + 1);
        }
      });

      stats.uniqueTagsScanned = tagCounts.size;

      // Find most scanned tag
      let maxCount = 0;
      let mostScannedTagUid = '';

      for (const [tagUid, count] of tagCounts) {
        if (count > maxCount) {
          maxCount = count;
          mostScannedTagUid = tagUid;
        }
      }

      if (maxCount > 0) {
        stats.mostScannedTag = {
          tagUid: mostScannedTagUid,
          count: maxCount,
        };
      }

      return ValidationResult.success(stats);

    } catch (error) {
      return this.handleError(error, 'getScanHistoryStats');
    }
  }

  /**
   * Clear old activities and scan history
   */
  async cleanupOldData(olderThanDays: number = 90): Promise<ServiceResult<{
    activitiesDeleted: number;
    historyEntriesDeleted: number;
  }>> {
    this.ensureInitialized();

    try {
      const cutoffDate = Date.now() - (olderThanDays * 24 * 60 * 60 * 1000);
      
      // Clean up scan history
      let historyEntriesDeleted = 0;
      for (const [id, entry] of this.scanHistory) {
        if (entry.timestamp < cutoffDate) {
          this.scanHistory.delete(id);
          historyEntriesDeleted++;
        }
      }

      // Get old activities (would need to implement deletion in EntityService)
      const oldActivitiesResult = await this.getActivities({
        endDate: cutoffDate,
        limit: 10000,
      });

      let activitiesDeleted = 0;
      if (oldActivitiesResult.isValid && this.entityService) {
        const oldActivities = oldActivitiesResult.data!;
        
        // Delete each old activity
        for (const activity of oldActivities) {
          const deleteResult = await this.entityService.deleteEntity(activity.id);
          if (deleteResult.isValid) {
            activitiesDeleted++;
          }
        }
      }

      const result = {
        activitiesDeleted,
        historyEntriesDeleted,
      };

      this.log('info', `Cleaned up old data`, result);
      return ValidationResult.success(result);

    } catch (error) {
      return this.handleError(error, 'cleanupOldData');
    }
  }

  // Private methods

  private validateActivityRequest(request: ActivityCreateRequest): ValidationResult<void> {
    const validations = [
      ValidationUtils.validateRequired(request.activityType, 'activityType'),
      ValidationUtils.validateRequired(request.description, 'description'),
      ValidationUtils.validateLength(request.description, 'description', 1, 500),
    ];

    return ValidationUtils.combineResults(...validations).map(() => undefined);
  }

  protected async doCleanup(): Promise<void> {
    this.scanHistory.clear();
  }
}