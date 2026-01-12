package com.swiftiecx.timeledger.ui.viewmodel.model

sealed class SyncUiState {
    data object Idle : SyncUiState()
    data class Loading(val msg: String) : SyncUiState()
    data class Success(val msg: String) : SyncUiState()
    data class Error(val err: String) : SyncUiState()
    data class Conflict(val cloudTime: Long) : SyncUiState()
}
