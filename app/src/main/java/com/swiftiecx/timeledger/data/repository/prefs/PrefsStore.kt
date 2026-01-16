package com.swiftiecx.timeledger.data.repository

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * SharedPreferences 统一封装：
 * - key 集中（RepoKeys）
 * - 读写集中（维护时不需要到处 grep）
 *
 * ⚠️ 不改变任何 key/value 行为，完全按原实现。
 */
class PrefsStore(
    private val prefs: SharedPreferences,
    private val gson: Gson
) {

    // -------------------------
    // 首次启动 / 默认货币
    // -------------------------
    fun isFirstLaunch(): Boolean = prefs.getBoolean(RepoKeys.KEY_FIRST_LAUNCH, true)

    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean(RepoKeys.KEY_FIRST_LAUNCH, false).apply()
    }

    fun getSavedCurrency(): String? = prefs.getString(RepoKeys.KEY_DEFAULT_CURRENCY, null)

    fun saveDefaultCurrency(currencyCode: String) {
        prefs.edit().putString(RepoKeys.KEY_DEFAULT_CURRENCY, currencyCode).apply()
    }

    // -------------------------
    // 默认账户 / 账户排序
    // -------------------------
    fun getDefaultAccountId(): Long = prefs.getLong(RepoKeys.KEY_DEFAULT_ACCOUNT_ID, -1L)

    fun saveDefaultAccountId(id: Long) {
        prefs.edit().putLong(RepoKeys.KEY_DEFAULT_ACCOUNT_ID, id).apply()
    }

    fun loadAccountOrder(): List<Long> {
        val json = prefs.getString(RepoKeys.KEY_ACCOUNT_ORDER, null) ?: return emptyList()
        val type = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun saveAccountOrder(order: List<Long>) {
        prefs.edit().putString(RepoKeys.KEY_ACCOUNT_ORDER, gson.toJson(order)).apply()
    }

    // -------------------------
    // 分类 JSON
    // -------------------------
    fun saveJson(key: String, json: String) {
        prefs.edit().putString(key, json).apply()
    }

    fun getJson(key: String): String? = prefs.getString(key, null)

    // -------------------------
    // 隐私锁
    // -------------------------
    fun getPrivacyType(): String = prefs.getString(RepoKeys.KEY_PRIVACY_TYPE, "NONE") ?: "NONE"

    fun savePrivacyType(type: String) {
        prefs.edit().putString(RepoKeys.KEY_PRIVACY_TYPE, type).apply()
    }

    fun savePin(pin: String) {
        prefs.edit().putString(RepoKeys.KEY_PRIVACY_PIN, pin).apply()
    }

    fun verifyPin(inputPin: String): Boolean {
        return prefs.getString(RepoKeys.KEY_PRIVACY_PIN, "") == inputPin
    }

    fun savePattern(pattern: List<Int>) {
        prefs.edit().putString(RepoKeys.KEY_PRIVACY_PATTERN, pattern.joinToString(",")).apply()
    }

    fun verifyPattern(inputPattern: List<Int>): Boolean {
        return prefs.getString(RepoKeys.KEY_PRIVACY_PATTERN, "") == inputPattern.joinToString(",")
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(RepoKeys.KEY_PRIVACY_BIOMETRIC, false)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(RepoKeys.KEY_PRIVACY_BIOMETRIC, enabled).apply()
    }
}
