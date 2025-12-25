package com.swiftiecx.timeledger.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
// 显式导入 DebtRecord 以确保 IDE 能识别
import com.swiftiecx.timeledger.data.DebtRecord

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

    @Query("SELECT * FROM debt_records ORDER BY borrowTime DESC")
    fun getAllDebtRecords(): Flow<List<DebtRecord>>

    @Query("DELETE FROM debt_records")
    suspend fun deleteAll()
}