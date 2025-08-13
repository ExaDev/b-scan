package com.bscan.model

data class NfcTagData(
    val uid: String,
    val bytes: ByteArray,
    val technology: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NfcTagData

        if (uid != other.uid) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (technology != other.technology) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uid.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + technology.hashCode()
        return result
    }
}