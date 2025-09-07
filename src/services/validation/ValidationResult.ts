/**
 * ValidationResult - Result wrapper for validation operations
 * Based on Kotlin implementation patterns for handling success/error states
 */

export enum ValidationResultType {
  SUCCESS = 'SUCCESS',
  ERROR = 'ERROR',
  WARNING = 'WARNING',
}

export interface ValidationIssue {
  field: string;
  code: string;
  message: string;
  severity: ValidationResultType;
}

export class ValidationResult<T> {
  public readonly isValid: boolean;
  public readonly data?: T;
  public readonly issues: ValidationIssue[];

  private constructor(isValid: boolean, data?: T, issues: ValidationIssue[] = []) {
    this.isValid = isValid;
    if (data !== undefined) {
      this.data = data;
    }
    this.issues = issues;
  }

  /**
   * Create a successful validation result
   */
  static success<T>(data: T): ValidationResult<T> {
    return new ValidationResult(true, data, []);
  }

  /**
   * Create a validation result with warnings but still valid
   */
  static warning<T>(data: T, issues: ValidationIssue[]): ValidationResult<T> {
    return new ValidationResult(true, data, issues);
  }

  /**
   * Create a failed validation result
   */
  static error<T = never>(issues: ValidationIssue[]): ValidationResult<T> {
    return new ValidationResult<T>(false, undefined as T, issues);
  }

  /**
   * Create a failed validation result with single issue
   */
  static singleError<T = never>(field: string, code: string, message: string): ValidationResult<T> {
    return ValidationResult.error<T>([{
      field,
      code,
      message,
      severity: ValidationResultType.ERROR,
    }]);
  }

  /**
   * Get only error issues
   */
  get errors(): ValidationIssue[] {
    return this.issues.filter(issue => issue.severity === ValidationResultType.ERROR);
  }

  /**
   * Get only warning issues
   */
  get warnings(): ValidationIssue[] {
    return this.issues.filter(issue => issue.severity === ValidationResultType.WARNING);
  }

  /**
   * Check if result has any errors
   */
  get hasErrors(): boolean {
    return this.errors.length > 0;
  }

  /**
   * Check if result has any warnings
   */
  get hasWarnings(): boolean {
    return this.warnings.length > 0;
  }

  /**
   * Get the first error message, or undefined if no errors
   */
  get firstErrorMessage(): string | undefined {
    const firstError = this.errors[0];
    return firstError?.message;
  }

  /**
   * Combine this validation result with another
   */
  combine<U>(other: ValidationResult<U>): ValidationResult<T> {
    const combinedIssues = [...this.issues, ...other.issues];
    const isValid = this.isValid && other.isValid;
    
    return new ValidationResult(isValid, this.data, combinedIssues);
  }

  /**
   * Transform the data if validation is successful
   */
  map<U>(transform: (data: T) => U): ValidationResult<U> {
    if (!this.isValid || !this.data) {
      return ValidationResult.error<U>(this.issues);
    }

    try {
      const transformedData = transform(this.data);
      return new ValidationResult(true, transformedData, this.issues);
    } catch (error) {
      const transformError: ValidationIssue = {
        field: 'transform',
        code: 'TRANSFORM_ERROR',
        message: error instanceof Error ? error.message : 'Transform failed',
        severity: ValidationResultType.ERROR,
      };
      return ValidationResult.error<U>([...this.issues, transformError]);
    }
  }

  /**
   * Chain validation operations
   */
  flatMap<U>(transform: (data: T) => ValidationResult<U>): ValidationResult<U> {
    if (!this.isValid || !this.data) {
      return ValidationResult.error<U>(this.issues);
    }

    try {
      const result = transform(this.data);
      return new ValidationResult(
        result.isValid,
        result.data,
        [...this.issues, ...result.issues]
      );
    } catch (error) {
      const transformError: ValidationIssue = {
        field: 'flatMap',
        code: 'TRANSFORM_ERROR',
        message: error instanceof Error ? error.message : 'FlatMap transform failed',
        severity: ValidationResultType.ERROR,
      };
      return ValidationResult.error<U>([...this.issues, transformError]);
    }
  }

  /**
   * Convert to JSON representation
   */
  toJSON(): {
    isValid: boolean;
    data?: T;
    issues: ValidationIssue[];
  } {
    const result: {
      isValid: boolean;
      data?: T;
      issues: ValidationIssue[];
    } = {
      isValid: this.isValid,
      issues: this.issues,
    };

    if (this.data !== undefined) {
      result.data = this.data;
    }

    return result;
  }

  /**
   * Create ValidationResult from JSON
   */
  static fromJSON<T>(json: {
    isValid: boolean;
    data?: T;
    issues: ValidationIssue[];
  }): ValidationResult<T> {
    return new ValidationResult(json.isValid, json.data, json.issues);
  }
}