package com.swiftiecx.timeledger.data.model

/**
 * 兼容层：
 * data 层统一从 com.swiftiecx.timeledger.data.model.CategoryType 引用类型，
 * 实际类型定义放在 ui.viewmodel.model.CategoryType（单一来源，避免重复 enum）。
 */
typealias CategoryType = com.swiftiecx.timeledger.ui.viewmodel.model.CategoryType
