/**
 * ValidationUtils - Common validation utilities and validators
 * Provides reusable validation functions for business entities
 */

import { ValidationResult, ValidationIssue, ValidationResultType } from './ValidationResult';
import { EntityType, TagFormat } from '../../types/FilamentInfo';

export class ValidationUtils {
  /**
   * Validate required string field
   */
  static validateRequired(value: string | undefined | null, fieldName: string): ValidationResult<string> {
    if (!value || value.trim().length === 0) {
      return ValidationResult.singleError(fieldName, 'REQUIRED', `${fieldName} is required`);
    }
    return ValidationResult.success(value.trim());
  }

  /**
   * Validate string length constraints
   */
  static validateLength(
    value: string,
    fieldName: string,
    minLength?: number,
    maxLength?: number
  ): ValidationResult<string> {
    const issues: ValidationIssue[] = [];

    if (minLength !== undefined && value.length < minLength) {
      issues.push({
        field: fieldName,
        code: 'MIN_LENGTH',
        message: `${fieldName} must be at least ${minLength} characters`,
        severity: ValidationResultType.ERROR,
      });
    }

    if (maxLength !== undefined && value.length > maxLength) {
      issues.push({
        field: fieldName,
        code: 'MAX_LENGTH',
        message: `${fieldName} must not exceed ${maxLength} characters`,
        severity: ValidationResultType.ERROR,
      });
    }

    return issues.length > 0 ? ValidationResult.error(issues) : ValidationResult.success(value);
  }

  /**
   * Validate numeric range
   */
  static validateNumberRange(
    value: number,
    fieldName: string,
    min?: number,
    max?: number
  ): ValidationResult<number> {
    const issues: ValidationIssue[] = [];

    if (!Number.isFinite(value)) {
      return ValidationResult.singleError(fieldName, 'INVALID_NUMBER', `${fieldName} must be a valid number`);
    }

    if (min !== undefined && value < min) {
      issues.push({
        field: fieldName,
        code: 'MIN_VALUE',
        message: `${fieldName} must be at least ${min}`,
        severity: ValidationResultType.ERROR,
      });
    }

    if (max !== undefined && value > max) {
      issues.push({
        field: fieldName,
        code: 'MAX_VALUE',
        message: `${fieldName} must not exceed ${max}`,
        severity: ValidationResultType.ERROR,
      });
    }

    return issues.length > 0 ? ValidationResult.error(issues) : ValidationResult.success(value);
  }

  /**
   * Validate positive number
   */
  static validatePositiveNumber(value: number, fieldName: string): ValidationResult<number> {
    return ValidationUtils.validateNumberRange(value, fieldName, 0.01);
  }

  /**
   * Validate non-negative number
   */
  static validateNonNegativeNumber(value: number, fieldName: string): ValidationResult<number> {
    return ValidationUtils.validateNumberRange(value, fieldName, 0);
  }

  /**
   * Validate hex color code
   */
  static validateHexColor(value: string, fieldName: string): ValidationResult<string> {
    const hexColorRegex = /^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/;
    
    if (!hexColorRegex.test(value)) {
      return ValidationResult.singleError(
        fieldName,
        'INVALID_HEX_COLOR',
        `${fieldName} must be a valid hex color (e.g., #FF0000 or #F00)`
      );
    }

    return ValidationResult.success(value.toUpperCase());
  }

  /**
   * Validate RFID/NFC tag UID format
   */
  static validateTagUid(uid: string, fieldName: string = 'tagUid'): ValidationResult<string> {
    if (!uid || uid.trim().length === 0) {
      return ValidationResult.singleError(fieldName, 'REQUIRED', `${fieldName} is required`);
    }

    const cleanUid = uid.trim().toUpperCase();
    
    // Validate hex characters only
    if (!/^[0-9A-F]+$/.test(cleanUid)) {
      return ValidationResult.singleError(
        fieldName,
        'INVALID_HEX',
        `${fieldName} must contain only hexadecimal characters (0-9, A-F)`
      );
    }

    // Validate typical UID lengths (4, 7, or 10 bytes = 8, 14, or 20 hex characters)
    const validLengths = [8, 14, 20];
    if (!validLengths.includes(cleanUid.length)) {
      return ValidationResult.warning(cleanUid, [{
        field: fieldName,
        code: 'UNUSUAL_LENGTH',
        message: `${fieldName} has unusual length (${cleanUid.length} characters). Expected 8, 14, or 20`,
        severity: ValidationResultType.WARNING,
      }]);
    }

    return ValidationResult.success(cleanUid);
  }

  /**
   * Validate ISO date string
   */
  static validateISODate(dateString: string, fieldName: string): ValidationResult<string> {
    if (!dateString || dateString.trim().length === 0) {
      return ValidationResult.singleError(fieldName, 'REQUIRED', `${fieldName} is required`);
    }

    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) {
        return ValidationResult.singleError(
          fieldName,
          'INVALID_DATE',
          `${fieldName} must be a valid date`
        );
      }

      // Check if date is in reasonable range (not too far in past or future)
      const now = new Date();
      const minDate = new Date('2000-01-01');
      const maxDate = new Date(now.getFullYear() + 10, 11, 31);

      if (date < minDate || date > maxDate) {
        return ValidationResult.warning(dateString, [{
          field: fieldName,
          code: 'DATE_OUT_OF_RANGE',
          message: `${fieldName} is outside reasonable range (2000-${maxDate.getFullYear()})`,
          severity: ValidationResultType.WARNING,
        }]);
      }

      return ValidationResult.success(dateString);
    } catch (error) {
      return ValidationResult.singleError(
        fieldName,
        'INVALID_DATE',
        `${fieldName} must be a valid date: ${error instanceof Error ? error.message : 'Unknown error'}`
      );
    }
  }

  /**
   * Validate temperature ranges for 3D printing
   */
  static validateTemperature(
    temperature: number,
    fieldName: string,
    temperatureType: 'nozzle' | 'bed' | 'drying' = 'nozzle'
  ): ValidationResult<number> {
    const ranges = {
      nozzle: { min: 150, max: 300 },
      bed: { min: 0, max: 120 },
      drying: { min: 30, max: 80 },
    };

    const range = ranges[temperatureType];
    const result = ValidationUtils.validateNumberRange(temperature, fieldName, range.min, range.max);

    if (!result.isValid) {
      return result;
    }

    // Add warnings for extreme values
    const warnings: ValidationIssue[] = [];
    if (temperatureType === 'nozzle') {
      if (temperature < 180) {
        warnings.push({
          field: fieldName,
          code: 'LOW_TEMPERATURE',
          message: `${fieldName} (${temperature}°C) is quite low for most filaments`,
          severity: ValidationResultType.WARNING,
        });
      } else if (temperature > 250) {
        warnings.push({
          field: fieldName,
          code: 'HIGH_TEMPERATURE',
          message: `${fieldName} (${temperature}°C) is quite high, ensure proper ventilation`,
          severity: ValidationResultType.WARNING,
        });
      }
    }

    return warnings.length > 0 
      ? ValidationResult.warning(temperature, warnings)
      : ValidationResult.success(temperature);
  }

  /**
   * Validate filament diameter
   */
  static validateFilamentDiameter(diameter: number): ValidationResult<number> {
    const standardDiameters = [1.75, 2.85, 3.0];
    const tolerance = 0.1;

    const result = ValidationUtils.validatePositiveNumber(diameter, 'filamentDiameter');
    if (!result.isValid) {
      return result;
    }

    // Check if diameter is close to a standard size
    const isStandardSize = standardDiameters.some(
      standard => Math.abs(diameter - standard) <= tolerance
    );

    if (!isStandardSize) {
      return ValidationResult.warning(diameter, [{
        field: 'filamentDiameter',
        code: 'NON_STANDARD_DIAMETER',
        message: `Diameter ${diameter}mm is not a standard size (1.75mm, 2.85mm, 3.0mm)`,
        severity: ValidationResultType.WARNING,
      }]);
    }

    return ValidationResult.success(diameter);
  }

  /**
   * Combine multiple validation results
   */
  static combineResults<T>(...results: ValidationResult<unknown>[]): ValidationResult<T[]> {
    const allIssues = results.flatMap(result => result.issues);
    const hasErrors = allIssues.some(issue => issue.severity === ValidationResultType.ERROR);
    
    if (hasErrors) {
      return ValidationResult.error(allIssues);
    }

    const data = results.map(result => result.data).filter(item => item !== undefined);
    return allIssues.length > 0
      ? ValidationResult.warning(data as T[], allIssues)
      : ValidationResult.success(data as T[]);
  }

  /**
   * Validate entity type
   */
  static validateEntityType(type: string): ValidationResult<EntityType> {
    const validTypes = Object.values(EntityType);
    if (!validTypes.includes(type as EntityType)) {
      return ValidationResult.singleError(
        'entityType',
        'INVALID_ENTITY_TYPE',
        `Entity type must be one of: ${validTypes.join(', ')}`
      );
    }
    return ValidationResult.success(type as EntityType);
  }

  /**
   * Validate tag format
   */
  static validateTagFormat(format: string): ValidationResult<TagFormat> {
    const validFormats = Object.values(TagFormat);
    if (!validFormats.includes(format as TagFormat)) {
      return ValidationResult.singleError(
        'tagFormat',
        'INVALID_TAG_FORMAT',
        `Tag format must be one of: ${validFormats.join(', ')}`
      );
    }
    return ValidationResult.success(format as TagFormat);
  }
}