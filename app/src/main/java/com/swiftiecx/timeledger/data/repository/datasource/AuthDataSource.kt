package com.swiftiecx.timeledger.data.repository.datasource

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import com.swiftiecx.timeledger.R
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

/**
 * FirebaseAuth 封装（保持原行为）
 *
 * 说明：
 * - 通过 AuthStateListener 更新 user / emailVerified
 * - login() 如果邮箱未验证：立刻 signOut 并返回失败（与原实现一致）
 * - 不在这里做 UI / Toast / Dialog（上层决定怎么展示）
 */
class AuthDataSource(
    private val firebaseAuth: FirebaseAuth,
    /**
     * 统一字符串获取函数。
     * 注意：不要在这里用 @StringRes 标注函数类型参数，会导致
     * "This annotation is not applicable to target 'type usage'"
     */
    private val getString: (Int, Array<out Any>) -> String
) {
    private val _firebaseUser = MutableStateFlow<FirebaseUser?>(firebaseAuth.currentUser)
    val firebaseUser: StateFlow<FirebaseUser?> = _firebaseUser.asStateFlow()

    private val _emailVerified = MutableStateFlow(firebaseAuth.currentUser?.isEmailVerified == true)
    val emailVerified: StateFlow<Boolean> = _emailVerified.asStateFlow()

    val isLoggedIn: Flow<Boolean> = combine(firebaseUser, emailVerified) { user, verified ->
        user != null && verified
    }

    init {
        firebaseAuth.useAppLanguage()
        firebaseAuth.addAuthStateListener { auth ->
            _firebaseUser.value = auth.currentUser
            _emailVerified.value = auth.currentUser?.isEmailVerified == true
        }
    }

    suspend fun register(email: String, password: String): Result<Boolean> {
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            firebaseAuth.currentUser?.sendEmailVerification()?.await()
            _firebaseUser.value = firebaseAuth.currentUser
            _emailVerified.value = false
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    suspend fun login(email: String, password: String): Result<Boolean> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            // 邮箱未验证：与原逻辑一致 -> 退出并报错
            if (!refreshEmailVerification()) {
                firebaseAuth.signOut()
                _firebaseUser.value = null
                _emailVerified.value = false
                Result.failure(Exception(mapAuthError(FirebaseAuthInvalidCredentialsException("", ""))))
            } else {
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    suspend fun refreshEmailVerification(): Boolean {
        return try {
            firebaseAuth.currentUser?.reload()?.await()
            val verified = firebaseAuth.currentUser?.isEmailVerified == true
            _firebaseUser.value = firebaseAuth.currentUser
            _emailVerified.value = verified
            verified
        } catch (_: Exception) {
            _emailVerified.value
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Boolean> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Boolean> {
        return try {
            val user = firebaseAuth.currentUser ?: return Result.failure(Exception("not logged in"))
            val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    suspend fun deleteUserAccount(): Result<Boolean> {
        return try {
            firebaseAuth.currentUser?.delete()?.await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    private fun mapAuthError(e: Exception): String {
        // 你项目里没有 error_user_not_found，所以不引用它，避免编译错误
        return when (e) {
            is FirebaseAuthInvalidUserException ->
                getString(R.string.error_email_or_password_wrong, emptyArray())

            is FirebaseAuthInvalidCredentialsException ->
                getString(R.string.error_email_or_password_wrong, emptyArray())

            is FirebaseAuthUserCollisionException ->
                getString(R.string.error_email_already_registered, emptyArray())

            is FirebaseAuthRecentLoginRequiredException ->
                getString(R.string.error_recent_login_required, emptyArray())

            is FirebaseNetworkException ->
                getString(R.string.error_network_check, emptyArray())

            else -> e.message ?: "auth error"
        }
    }
}
