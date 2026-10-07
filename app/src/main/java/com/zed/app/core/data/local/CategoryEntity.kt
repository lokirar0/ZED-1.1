package com.zed.app.core.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.zed.app.R
import kotlinx.coroutines.flow.Flow

// Категория операции. builtIn = системная (key = FOOD и т.д.),
// кастомные хранят пользовательское имя в name.
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val key: String = "",     // только у builtIn
    val name: String = "",    // только у кастомных
    val type: String,         // TransactionType.name
    val builtIn: Boolean
)

// Стартовый набор категорий (сейдится при пустой таблице)
val BUILT_IN_CATEGORIES: List<CategoryEntity> = listOf(
    CategoryEntity(key = "FOOD", type = "EXPENSE", builtIn = true),
    CategoryEntity(key = "TRANSPORT", type = "EXPENSE", builtIn = true),
    CategoryEntity(key = "SUBSCRIPTIONS", type = "EXPENSE", builtIn = true),
    CategoryEntity(key = "HOUSING", type = "EXPENSE", builtIn = true),
    CategoryEntity(key = "ENTERTAINMENT", type = "EXPENSE", builtIn = true),
    CategoryEntity(key = "HEALTH", type = "EXPENSE", builtIn = true),
    CategoryEntity(key = "OTHER_EXPENSE", type = "EXPENSE", builtIn = true),
    CategoryEntity(key = "SALARY", type = "INCOME", builtIn = true),
    CategoryEntity(key = "OTHER_INCOME", type = "INCOME", builtIn = true),
    // Служебная: авто-списания платежей по кредитам (не удаляется)
    CategoryEntity(key = "CREDIT", type = "EXPENSE", builtIn = true)
)

// Отображаемое имя: builtIn → локализованный ресурс, кастомная → как ввёл пользователь
fun CategoryEntity.displayName(context: Context): String =
    if (builtIn) context.getString(categoryKeyRes(key)) else name

// key → строковый ресурс (OTHER_* и неизвестные → «Прочее»)
fun categoryKeyRes(key: String): Int = when (key) {
    "FOOD" -> R.string.cat_food
    "TRANSPORT" -> R.string.cat_transport
    "SUBSCRIPTIONS" -> R.string.cat_subscriptions
    "HOUSING" -> R.string.cat_housing
    "ENTERTAINMENT" -> R.string.cat_entertainment
    "HEALTH" -> R.string.cat_health
    "SALARY" -> R.string.cat_salary
    "CREDIT" -> R.string.cat_credit
    else -> R.string.cat_other
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY type ASC, id ASC")
    fun observe(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT * FROM categories WHERE key = :key LIMIT 1")
    suspend fun findByKey(key: String): CategoryEntity?

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Insert
    suspend fun insertAll(list: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Int)

    // Бэкап
    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<CategoryEntity>

    @Query("DELETE FROM categories")
    suspend fun clear()
}
