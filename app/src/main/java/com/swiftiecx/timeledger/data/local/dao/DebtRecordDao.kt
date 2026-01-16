package com.swiftiecx.timeledger.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.swiftiecx.timeledger.data.local.entity.DebtRecord

@Dao
interface DebtRecordDao {

    // 查询指定账户的所有借贷记录，按发生时间倒序排列
    @Query("SELECT * FROM debt_records WHERE accountId = :accountId ORDER BY borrowTime DESC")
    fun observeByAccount(accountId: Long): Flow<List<DebtRecord>>

    @Insert
    suspend fun insert(record: DebtRecord): Long

    @Update
    suspend fun update(record: DebtRecord)

    @Delete
    suspend fun delete(record: DebtRecord)

    // ✅ [新增] 根据ID删除单条记录
    // 配合 ExpenseRepository 中的逻辑，当删除关联的流水时，通过此方法删除对应的债务
    @Query("DELETE FROM debt_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM debt_records ORDER BY borrowTime DESC")
    fun getAllDebtRecords(): Flow<List<DebtRecord>>

    @Query("DELETE FROM debt_records")
    suspend fun deleteAll()
}