package com.zed.app.core.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// ZED 2.0: счета (наличные, карта, вклад…). balanceMinor = стартовый остаток в копейках.
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val balanceMinor: Long = 0,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC, createdAt ASC")
    fun observe(): Flow<List<AccountEntity>>

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Query("UPDATE accounts SET name = :name, balanceMinor = :balanceMinor WHERE id = :id")
    suspend fun update(id: Int, name: String, balanceMinor: Long)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    // Бэкап
    @Query("SELECT * FROM accounts")
    suspend fun getAll(): List<AccountEntity>

    @Query("DELETE FROM accounts")
    suspend fun clear()

    @Insert
    suspend fun insertAll(list: List<AccountEntity>)
}
