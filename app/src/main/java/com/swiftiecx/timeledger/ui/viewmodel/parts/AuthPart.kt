package com.swiftiecx.timeledger.ui.viewmodel.parts

import com.swiftiecx.timeledger.data.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthPart(
    private val repository: ExpenseRepository,
    private val scope: CoroutineScope
) {
    val isLoggedIn: StateFlow<Boolean> =
        repository.isLoggedIn.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)

    val userEmail: StateFlow<String> =
        repository.userEmail.stateIn(scope, SharingStarted.WhileSubscribed(5000), "")

    fun register(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        scope.launch {
            val result = repository.register(email, password)
            if (result.isSuccess) onSuccess() else onError(result.exceptionOrNull()?.message ?: "注册失败")
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        scope.launch {
            val result = repository.login(email, password)
            if (result.isSuccess) onSuccess() else onError(result.exceptionOrNull()?.message ?: "登录失败")
        }
    }

    suspend fun refreshEmailVerification(): Boolean = repository.refreshEmailVerification()

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        scope.launch {
            val result = repository.sendPasswordResetEmail(email)
            if (result.isSuccess) onSuccess() else onError(result.exceptionOrNull()?.message ?: "发送失败")
        }
    }

    fun changePassword(oldPass: String, newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        scope.launch {
            val result = repository.changePassword(oldPass, newPass)
            if (result.isSuccess) onSuccess() else onError(result.exceptionOrNull()?.message ?: "修改失败")
        }
    }

    fun logout() = repository.logout()

    fun deleteUserAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        scope.launch {
            val result = repository.deleteUserAccount()
            if (result.isSuccess) onSuccess() else onError(result.exceptionOrNull()?.message ?: "注销失败")
        }
    }

    // privacy
    fun verifyPin(pin: String): Boolean = repository.verifyPin(pin)
    fun savePin(pin: String) = repository.savePin(pin)
    fun verifyPattern(pattern: List<Int>) = repository.verifyPattern(pattern)
    fun savePattern(pattern: List<Int>) = repository.savePattern(pattern)

    fun getPrivacyType(): String = repository.getPrivacyType()
    fun setPrivacyType(type: String) = repository.savePrivacyType(type)
    fun setBiometricEnabled(enabled: Boolean) = repository.setBiometricEnabled(enabled)
    fun isBiometricEnabled(): Boolean = repository.isBiometricEnabled()
}
