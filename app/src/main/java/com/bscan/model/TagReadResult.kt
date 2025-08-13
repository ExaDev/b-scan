package com.bscan.model

sealed class TagReadResult {
    data class Success(val filamentInfo: FilamentInfo) : TagReadResult()
    object NoNfc : TagReadResult()
    object NfcDisabled : TagReadResult()
    object InvalidTag : TagReadResult()
    object ReadError : TagReadResult()
    object InsufficientData : TagReadResult()
}