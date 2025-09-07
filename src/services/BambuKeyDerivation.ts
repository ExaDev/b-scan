/**
 * BambuKeyDerivation - RFID key derivation for Bambu Lab tags
 * Port of the original Kotlin implementation with HKDF expansion
 */

import * as CryptoJS from 'crypto-js';

export class BambuKeyDerivation {
  private static readonly BAMBU_SALT = 'BambuLab';
  private static readonly INFO_STRING = 'RFID-Keys';
  private static readonly KEY_LENGTH = 6; // MIFARE Classic keys are 6 bytes
  private static readonly NUM_KEYS = 16; // 16 sectors on MIFARE Classic 1K

  /**
   * Derive authentication keys from UID using HKDF expansion
   * This implements the algorithm from commit 49939d6 - single 96-byte expansion + splitting
   */
  static deriveKeys(uid: Uint8Array): Uint8Array[] {
    // Validate UID length (must be 4 or 7 bytes for MIFARE Classic)
    if (uid.length < 4 || uid.length > 7) {
      return [];
    }

    try {
      // Convert UID to hex string for HKDF
      const uidHex = Array.from(uid)
        .map(b => b.toString(16).padStart(2, '0'))
        .join('');

      // Perform HKDF expansion to generate 96 bytes (16 keys × 6 bytes each)
      const expandedKeyMaterial = this.hkdfExpand(uidHex, this.NUM_KEYS * this.KEY_LENGTH);

      // Split the 96-byte expansion into 16 individual 6-byte keys
      const keys: Uint8Array[] = [];
      for (let i = 0; i < this.NUM_KEYS; i++) {
        const startByte = i * this.KEY_LENGTH;
        const endByte = startByte + this.KEY_LENGTH;
        const keyBytes = expandedKeyMaterial.slice(startByte, endByte);
        keys.push(new Uint8Array(keyBytes));
      }

      return keys;
    } catch (error) {
      console.error('Key derivation failed:', error);
      return [];
    }
  }

  /**
   * HKDF Expand function using HMAC-SHA256
   * Based on RFC 5869 - HMAC-based Extract-and-Expand Key Derivation Function
   */
  private static hkdfExpand(ikm: string, length: number): number[] {
    // Extract phase: HKDF-Extract(salt, IKM)
    const salt = CryptoJS.enc.Utf8.parse(this.BAMBU_SALT);
    const inputKeyMaterial = CryptoJS.enc.Hex.parse(ikm);
    const prk = CryptoJS.HmacSHA256(inputKeyMaterial, salt);

    // Expand phase: HKDF-Expand(PRK, info, L)
    const info = CryptoJS.enc.Utf8.parse(this.INFO_STRING);
    const hashLength = 32; // SHA256 output length
    const n = Math.ceil(length / hashLength);
    
    let okm = CryptoJS.lib.WordArray.create();
    let previousT = CryptoJS.lib.WordArray.create();

    for (let i = 1; i <= n; i++) {
      // eslint-disable-next-line no-bitwise
      const hmacInput = previousT.concat(info).concat(CryptoJS.lib.WordArray.create([i << 24])); // Convert counter to 4 bytes
      previousT = CryptoJS.HmacSHA256(hmacInput, prk);
      okm = okm.concat(previousT);
    }

    // Truncate to desired length and convert to byte array
    const truncatedOkm = CryptoJS.lib.WordArray.create(okm.words, length);
    const bytes: number[] = [];
    
    // Convert WordArray to byte array
    for (let i = 0; i < length; i++) {
      const wordIndex = Math.floor(i / 4);
      const byteIndex = i % 4;
      const word = truncatedOkm.words[wordIndex] || 0;
      // eslint-disable-next-line no-bitwise
      const byte = (word >>> (24 - (byteIndex * 8))) & 0xFF;
      bytes.push(byte);
    }

    return bytes;
  }

  /**
   * Get a specific key by index (0-15 for MIFARE Classic 1K)
   */
  static getKeyByIndex(uid: Uint8Array, keyIndex: number): Uint8Array {
    if (keyIndex < 0 || keyIndex >= this.NUM_KEYS) {
      throw new Error(`Key index ${keyIndex} out of range [0-${this.NUM_KEYS - 1}]`);
    }

    const keys = this.deriveKeys(uid);
    if (keys.length === 0) {
      throw new Error('Failed to derive keys from UID');
    }

    const key = keys[keyIndex];
    if (!key) {
      throw new Error(`Key at index ${keyIndex} is undefined`);
    }
    
    return key;
  }

  /**
   * Validate UID format for key derivation
   */
  static isValidUid(uid: Uint8Array): boolean {
    return uid.length >= 4 && uid.length <= 7;
  }

  /**
   * Convert hex string to UID bytes
   */
  static hexToUid(hex: string): Uint8Array {
    const cleanHex = hex.replace(/[^0-9A-Fa-f]/g, '');
    if (cleanHex.length % 2 !== 0 || cleanHex.length === 0) {
      throw new Error('Invalid hex string format');
    }

    const bytes = [];
    for (let i = 0; i < cleanHex.length; i += 2) {
      bytes.push(parseInt(cleanHex.substr(i, 2), 16));
    }

    return new Uint8Array(bytes);
  }

  /**
   * Convert UID bytes to hex string
   */
  static uidToHex(uid: Uint8Array): string {
    return Array.from(uid)
      .map(b => b.toString(16).padStart(2, '0'))
      .join('')
      .toUpperCase();
  }

  /**
   * Generate test vectors for validation (used in testing)
   */
  static generateTestVectors(): Array<{ uid: string; expectedKeyCount: number }> {
    return [
      { uid: '04914CCA', expectedKeyCount: 16 },
      { uid: '04914CCA5E6480', expectedKeyCount: 16 },
      { uid: '12345678', expectedKeyCount: 16 },
      { uid: 'AABBCCDD', expectedKeyCount: 16 },
      { uid: '00000000', expectedKeyCount: 16 },
      { uid: 'FFFFFFFF', expectedKeyCount: 16 },
    ];
  }

  /**
   * Verify key entropy and uniqueness (used for quality assurance)
   */
  static verifyKeyQuality(keys: Uint8Array[]): {
    allUnique: boolean;
    hasGoodEntropy: boolean;
    averageEntropy: number;
  } {
    if (keys.length === 0) {
      return { allUnique: false, hasGoodEntropy: false, averageEntropy: 0 };
    }

    // Check uniqueness
    const keyStrings = keys.map(key => Array.from(key).join(','));
    const uniqueKeys = new Set(keyStrings);
    const allUnique = uniqueKeys.size === keys.length;

    // Check entropy
    let totalEntropy = 0;
    for (const key of keys) {
      const uniqueBytes = new Set(Array.from(key));
      const entropy = uniqueBytes.size / key.length;
      totalEntropy += entropy;
    }
    const averageEntropy = totalEntropy / keys.length;
    const hasGoodEntropy = averageEntropy > 0.5; // At least 50% unique bytes per key

    return { allUnique, hasGoodEntropy, averageEntropy };
  }
}