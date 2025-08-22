# MIFARE Classic NFC Key Derivation Security Analysis

## Executive Summary

**STATUS: CRYPTOGRAPHIC IMPLEMENTATION VULNERABILITY IDENTIFIED**

The MIFARE Classic key derivation implementation is cryptographically correct per RFC 5869 HKDF-SHA256, but the derived keys **do not match the expected authentication keys**. This indicates a fundamental mismatch between the KDF parameters/process and the actual key derivation used by Bambu Lab RFID tags.

## Key Findings

### 1. HKDF Implementation Validation ✅
- **HKDF Extract and Expand phases**: Correctly implemented per RFC 5869
- **Test Vector Verification**: Passes RFC 5869 test case 1 validation
- **Cryptographic Correctness**: No implementation bugs detected

### 2. Key Derivation Parameter Analysis ❌

**Test Case:**
- UID: `049146CA5E6480` (7 bytes)
- Master Key: `9A759CF2C4F7CAFF222CB9769B41BC96`
- Context: `RFID-A\0` (`524649442D4100`)

**Results:**
- **Derived Key at index 2**: `BC1C85A9B3DC`
- **Expected/Failing Key**: `D2AF8DF6F057`
- **Match**: **FALSE** ❌

This confirms the authentication failure is due to incorrect key derivation parameters or process.

## Potential Root Causes

### 1. UID Processing Issues
- **Current**: Uses full 7-byte UID as input key material
- **Alternative**: May need 4-byte UID only
- **Endianness**: Big-endian vs little-endian interpretation
- **Testing Results**:
  - 4-byte UID (`049146CA`): Key 2 = `4AC520465C42`
  - Reversed UID: Key 2 = `7BB8E8FE42CE`
  - **None match expected key**

### 2. Context String Issues
- **Current**: `RFID-A\0` (with null terminator)
- **Alternative**: `RFID-A` (without null terminator)
- **Testing Results**:
  - Without null: Key 2 = `B9E114E5ED56`
  - **Does not match expected key**

### 3. Master Key Issues ⚠️
- **Current master key may be incorrect**
- **Key may be derived/transformed before use**
- **Different master key per tag type/batch**

### 4. Algorithm/Process Issues
- **May not be standard HKDF-SHA256**
- **Custom KDF implementation by Bambu Lab**
- **Additional processing steps not documented**

## Security Implications

### 1. Authentication Bypass Risk
- **Impact**: Complete authentication failure across all sectors
- **Severity**: HIGH - Cannot read protected tag data
- **Business Impact**: Application cannot function with Bambu Lab tags

### 2. Key Management Vulnerabilities
- **Hardcoded Master Key**: Embedded in source code (security risk)
- **Key Rotation**: No mechanism for key updates
- **Key Distribution**: Vulnerable to reverse engineering

### 3. Implementation Security Issues
- **Insufficient Validation**: No verification against known good keys
- **Error Handling**: Falls back to default keys (potential security bypass)
- **Logging**: Keys logged in plaintext (information disclosure)

## Technical Analysis Details

### HKDF Process Flow
```
1. Extract: PRK = HMAC-SHA256(MASTER_KEY, UID)
2. Expand: Key[i] = HKDF-Expand(PRK, "RFID-A\0" + (i+1), 6)
```

### Key Derivation Results
```
UID: 049146CA5E6480
PRK: 22488DD92D9726D2564833A69B7B236A54C9AA89B5A51D9BFA81479736EF2C9F

Key 0: C9DEECDBFE11
Key 1: 840B0F6984B0
Key 2: BC1C85A9B3DC  ← FAILS AUTHENTICATION
Key 3: ECA265B36556
...
```

### Code Security Issues

#### 1. Hardcoded Cryptographic Material
**File**: `BambuKeyDerivation.kt:11-16`
```kotlin
private val MASTER_KEY = byteArrayOf(
    0x9a.toByte(), 0x75.toByte(), 0x9c.toByte(), 0xf2.toByte(),
    // ... hardcoded in source
)
```
**Risk**: Key extraction via static analysis

#### 2. Plaintext Key Logging
**File**: `BambuKeyDerivation.kt:59`
```kotlin
Log.v(TAG, "Standard - Key $i: ${key.joinToString("") { "%02X".format(it) }}")
```
**Risk**: Information disclosure via log files

#### 3. Insufficient Input Validation
**File**: `BambuKeyDerivation.kt:28-31`
- Only validates minimum UID length
- No maximum length validation
- No format validation

## Recommendations

### Immediate Actions (Priority 1)

1. **Verify Master Key Correctness**
   - Cross-reference with other implementations
   - Test with known working tags
   - Check for key transformation requirements

2. **Validate UID Processing**
   - Test with 4-byte vs 7-byte UIDs
   - Verify endianness requirements
   - Check for UID normalization needs

3. **Context String Investigation**
   - Test alternative context strings
   - Check for encoding variations
   - Verify null terminator requirements

### Security Hardening (Priority 2)

1. **Remove Key Logging**
   ```kotlin
   // Remove or conditionally compile debug logging
   // Log.v(TAG, "Key $i: ${key.joinToString("")...}")
   ```

2. **Implement Key Obfuscation**
   - Runtime key generation
   - Encrypted key storage
   - Anti-tampering measures

3. **Enhanced Input Validation**
   ```kotlin
   if (uid.size < 4 || uid.size > 10) {
       throw IllegalArgumentException("Invalid UID length")
   }
   ```

### Long-term Security (Priority 3)

1. **Implement Secure Key Management**
   - Key derivation from device-specific entropy
   - Secure storage (Android Keystore)
   - Key rotation capabilities

2. **Add Cryptographic Agility**
   - Support multiple KDF algorithms
   - Algorithm negotiation based on tag version
   - Backward compatibility maintenance

## Testing Requirements

### Functional Testing
1. **Known Good Tag Testing**
   - Test with working Bambu Lab tags
   - Verify successful authentication
   - Document working key-UID pairs

2. **Regression Testing**
   - Test all UID length variations
   - Verify fallback key functionality
   - Confirm error handling

### Security Testing
1. **Static Analysis**
   - Code review for hardcoded secrets
   - Dependency vulnerability scanning
   - Cryptographic implementation review

2. **Dynamic Analysis**
   - Runtime key extraction testing
   - Memory dump analysis
   - Side-channel attack testing

## Compliance Considerations

### OWASP Mobile Security
- **M2: Insecure Data Storage** - Hardcoded keys
- **M3: Insecure Communication** - Key logging
- **M10: Extraneous Functionality** - Debug key output

### Industry Standards
- **NIST SP 800-57**: Key management lifecycle
- **FIPS 140-2**: Cryptographic module security
- **Common Criteria**: Security evaluation criteria

## Conclusion

The current implementation demonstrates correct HKDF-SHA256 cryptographic implementation but fails to authenticate with actual Bambu Lab tags due to parameter mismatches. The primary security concern is the authentication failure preventing application functionality, compounded by key management vulnerabilities that expose cryptographic material.

**Next Steps:**
1. Investigate alternative master keys and UID processing methods
2. Implement security hardening measures
3. Establish comprehensive testing with real tags
4. Consider reverse engineering validation against working implementations

**Risk Level: HIGH** - Application cannot function as intended with secure key management vulnerabilities.