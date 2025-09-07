/**
 * TypeScript declarations for jest-extended matchers
 * Extends Jest's built-in matchers with additional functionality
 */

import 'jest-extended';

declare global {
  namespace jest {
    interface Matchers<R> {
      toBeDisabled(): R;
      toBeEnabled(): R;
      toBeEmpty(): R;
      toBeOneOf(members: readonly unknown[]): R;
      toBeNil(): R;
      toSatisfy<E = unknown>(predicate: (value: E) => boolean): R;
    }
  }
}