package com.swiftiecx.timeledger.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodicTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: PeriodicTransaction)

    @Update
    suspend fun update(transaction: PeriodicTransaction)

    @Delete
    suspend fun delete(transaction: PeriodicTransaction)

    @Query("SELECT * FROM periodic_transactions ORDER BY id DESC")
    fun getAll(): Flow<List<PeriodicTransaction>>

    // 【关键新增】供后台任务和启动检查使用的同步查询方法 (非 Flow)
    @Query("SELECT * FROM periodic_transactions")
    suspend fun getAllSync(): List<PeriodicTransaction>

    @Query("SELECT * FROM periodic_transactions WHERE id = :id")
    suspend fun getById(id: Long): PeriodicTransaction?
}