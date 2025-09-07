/**
 * TextDecoder polyfill for React Native
 * React Native doesn't provide TextDecoder in its JavaScript engine
 */

if (typeof global.TextDecoder === 'undefined') {
  global.TextDecoder = class TextDecoder {
    constructor(encoding = 'utf-8') {
      this.encoding = encoding.toLowerCase();
    }

    decode(input) {
      if (this.encoding === 'utf-8' || this.encoding === 'utf8') {
        // Simple UTF-8 decoder
        let result = '';
        let i = 0;
        while (i < input.length) {
          const byte1 = input[i++];
          
          if (byte1 < 0x80) {
            // ASCII character
            result += String.fromCharCode(byte1);
          } else if ((byte1 & 0xE0) === 0xC0) {
            // 2-byte UTF-8 character
            const byte2 = input[i++];
            result += String.fromCharCode(((byte1 & 0x1F) << 6) | (byte2 & 0x3F));
          } else if ((byte1 & 0xF0) === 0xE0) {
            // 3-byte UTF-8 character
            const byte2 = input[i++];
            const byte3 = input[i++];
            result += String.fromCharCode(((byte1 & 0x0F) << 12) | ((byte2 & 0x3F) << 6) | (byte3 & 0x3F));
          } else {
            // Invalid or unsupported sequence, replace with replacement character
            result += '\uFFFD';
          }
        }
        return result;
      } else {
        // For non-UTF-8 encodings, fallback to simple byte-to-string conversion
        return String.fromCharCode(...Array.from(input));
      }
    }
  };
}