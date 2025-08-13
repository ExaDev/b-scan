package com.bscan.nfc

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object BambuKeyDerivation {
    
    private val MASTER_KEY = byteArrayOf(
        0x9a.toByte(), 0x75.toByte(), 0x9c.toByte(), 0xf2.toByte(),
        0xc4.toByte(), 0xf7.toByte(), 0xca.toByte(), 0xff.toByte(),
        0x22.toByte(), 0x2c.toByte(), 0xb9.toByte(), 0x76.toByte(),
        0x9b.toByte(), 0x41.toByte(), 0xbc.toByte(), 0x96.toByte()
    )
    
    private val CONTEXT = "RFID-A\u0000".toByteArray()
    
    /**
     * Derive authentication keys from the UID using HKDF-SHA256
     * Based on the official Bambu Lab KDF discovered by the research community
     */
    fun deriveKeys(uid: ByteArray): Array<ByteArray> {
        val keys = mutableListOf<ByteArray>()
        
        // HKDF Extract phase
        val salt = MASTER_KEY
        val prk = hkdfExtract(salt, uid)
        
        // HKDF Expand phase - generate 16 keys of 6 bytes each
        for (i in 0 until 16) {
            val info = CONTEXT + byteArrayOf((i + 1).toByte())
            val key = hkdfExpand(prk, info, 6)
            keys.add(key)
        }
        
        return keys.toTypedArray()
    }
    
    private fun hkdfExtract(salt: ByteArray, ikm: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(salt, "HmacSHA256")
        mac.init(keySpec)
        return mac.doFinal(ikm)
    }
    
    private fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(prk, "HmacSHA256")
        mac.init(keySpec)
        
        val output = ByteArray(length)
        val hashLen = mac.macLength
        val n = (length + hashLen - 1) / hashLen
        
        var t = ByteArray(0)
        var pos = 0
        
        for (i in 1..n) {
            mac.reset()
            mac.update(t)
            mac.update(info)
            mac.update(i.toByte())
            t = mac.doFinal()
            
            val copyLen = minOf(t.size, length - pos)
            System.arraycopy(t, 0, output, pos, copyLen)
            pos += copyLen
        }
        
        return output
    }
}