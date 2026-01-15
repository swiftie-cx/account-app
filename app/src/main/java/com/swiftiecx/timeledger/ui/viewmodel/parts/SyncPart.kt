package com.swiftiecx.timeledger.ui.viewmodel.parts

import com.swiftiecx.timeledger.data.ExpenseRepository
import com.swiftiecx.timeledger.data.repository.SyncStrategy
import com.swiftiecx.timeledger.ui.viewmodel.model.SyncUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SyncPart(
    private val repository: ExpenseRepository,
    private val scope: CoroutineScope,
    private val afterLocalOverwritten: () -> Unit
) {
    private val _syncState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    fun startSync() {
        scope.launch {
            _syncState.value = SyncUiState.Loading("正在检查云端数据...")
            val result = repository.checkCloudStatus()
            if (result.isFailure) {
                _syncState.value = SyncUiState.Error(result.exceptionOrNull()?.message ?: "连接失败")
                return@launch
            }

            val status = result.getOrNull()!!
            if (status.hasCloudData && status.hasLocalData) {
                _syncState.value = SyncUiState.Conflict(status.cloudTimestamp)
            } else if (status.hasCloudData && !status.hasLocalData) {
                performSync(SyncStrategy.OVERWRITE_LOCAL)
            } else {
                performSync(SyncStrategy.OVERWRITE_CLOUD)
            }
        }
    }

    fun performSync(strategy: SyncStrategy) {
        scope.launch {
            _syncState.value = SyncUiState.Loading(
                if (strategy == SyncStrategy.MERGE) "正在智能合并..." else "正在同步..."
            )

            val result = repository.executeSync(strategy)
            if (result.isSuccess) {
                _syncState.value = SyncUiState.Success(result.getOrNull() ?: "同步成功")
                if (strategy == SyncStrategy.OVERWRITE_LOCAL || strategy == SyncStrategy.MERGE) {
                    afterLocalOverwritten()
                }
            } else {
                _syncState.value = SyncUiState.Error(result.exceptionOrNull()?.message ?: "同步失败")
            }

            delay(3000)
            _syncState.value = SyncUiState.Idle
        }
    }

    fun resetSyncState() {
        _syncState.value = SyncUiState.Idle
    }
}
