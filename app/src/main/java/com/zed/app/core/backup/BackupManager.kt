package com.zed.app.core.backup

import android.content.Context
import android.net.Uri
import com.zed.app.core.data.local.CategoryDao
import com.zed.app.core.data.local.CategoryEntity
import com.zed.app.core.data.local.CreditDao
import com.zed.app.core.data.local.CreditEntity
import com.zed.app.core.data.local.CreditPaymentDao
import com.zed.app.core.data.local.CreditPaymentEntity
import com.zed.app.core.data.local.HabitCompletionDao
import com.zed.app.core.data.local.HabitCompletionEntity
import com.zed.app.core.data.local.HabitDao
import com.zed.app.core.data.local.HabitEntity
import com.zed.app.core.data.local.TransactionDao
import com.zed.app.core.data.local.TransactionEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// Полный бэкап всех модулей (включая категории) в JSON через SAF.
// Никаких облаков: файл остаётся там, куда его сохранил пользователь.
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitDao: HabitDao,
    private val completionDao: HabitCompletionDao,
    private val transactionDao: TransactionDao,
    private val creditDao: CreditDao,
    private val paymentDao: CreditPaymentDao,
    private val categoryDao: CategoryDao
) {

    suspend fun exportToUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val json = buildJson()
            context.contentResolver.openOutputStream(uri)
                ?.use { it.write(json.toByteArray(Charsets.UTF_8)) } != null
        }.getOrDefault(false)
    }

    suspend fun importFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: return@runCatching false
            restore(text)
        }.getOrDefault(false)
    }

    // Приватность: полное удаление всех данных приложения
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        paymentDao.clear()
        transactionDao.clear()
        completionDao.clear()
        creditDao.clear()
        habitDao.clear()
        categoryDao.clear() // системные категории пересеются автоматически
    }

    // --- приватное ---

    private suspend fun buildJson(): String {
        val root = JSONObject()
        root.put("version", 2)

        root.put("habits", JSONArray().apply {
            habitDao.getAll().forEach {
                put(JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("note", it.note)
                    .put("createdAt", it.createdAt))
            }
        })
        root.put("completions", JSONArray().apply {
            completionDao.getCompletionsOnce().forEach {
                put(JSONObject()
                    .put("id", it.id)
                    .put("habitId", it.habitId)
                    .put("day", it.day))
            }
        })
        root.put("categories", JSONArray().apply {
            categoryDao.getAll().forEach {
                put(JSONObject()
                    .put("id", it.id)
                    .put("key", it.key)
                    .put("name", it.name)
                    .put("type", it.type)
                    .put("builtIn", it.builtIn))
            }
        })
        root.put("transactions", JSONArray().apply {
            transactionDao.getAll().forEach {
                put(JSONObject()
                    .put("id", it.id)
                    .put("type", it.type)
                    .put("amountMinor", it.amountMinor)
                    .put("categoryId", it.categoryId)
                    .put("note", it.note)
                    .put("dateMillis", it.dateMillis)
                    .put("creditId", it.creditId ?: -1)
                    .put("yearMonth", it.yearMonth ?: ""))
            }
        })
        root.put("credits", JSONArray().apply {
            creditDao.getAll().forEach {
                put(JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("monthlyPaymentMinor", it.monthlyPaymentMinor)
                    .put("paymentDay", it.paymentDay)
                    .put("note", it.note))
            }
        })
        root.put("creditPayments", JSONArray().apply {
            paymentDao.getPaymentsOnce().forEach {
                put(JSONObject()
                    .put("id", it.id)
                    .put("creditId", it.creditId)
                    .put("yearMonth", it.yearMonth))
            }
        })
        return root.toString(2)
    }

    private suspend fun restore(text: String): Boolean {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return false

        // Очистка в порядке «дети → родители» (внешние ключи)
        paymentDao.clear()
        transactionDao.clear()
        completionDao.clear()
        creditDao.clear()
        habitDao.clear()
        categoryDao.clear()

        val habits = root.optJSONArray("habits") ?: JSONArray()
        val habitList = mutableListOf<HabitEntity>()
        for (i in 0 until habits.length()) {
            val o = habits.getJSONObject(i)
            habitList += HabitEntity(
                id = o.getInt("id"),
                title = o.getString("title"),
                note = o.optString("note", ""),
                createdAt = o.optLong("createdAt", System.currentTimeMillis())
            )
        }
        habitDao.insertAll(habitList)

        val completions = root.optJSONArray("completions") ?: JSONArray()
        val completionList = mutableListOf<HabitCompletionEntity>()
        for (i in 0 until completions.length()) {
            val o = completions.getJSONObject(i)
            completionList += HabitCompletionEntity(
                id = o.getInt("id"),
                habitId = o.getInt("habitId"),
                day = o.getLong("day")
            )
        }
        completionDao.insertAll(completionList)

        val categories = root.optJSONArray("categories") ?: JSONArray()
        val categoryList = mutableListOf<CategoryEntity>()
        for (i in 0 until categories.length()) {
            val o = categories.getJSONObject(i)
            categoryList += CategoryEntity(
                id = o.getInt("id"),
                key = o.optString("key", ""),
                name = o.optString("name", ""),
                type = o.getString("type"),
                builtIn = o.optBoolean("builtIn", false)
            )
        }
        categoryDao.insertAll(categoryList)

        val transactions = root.optJSONArray("transactions") ?: JSONArray()
        val txList = mutableListOf<TransactionEntity>()
        for (i in 0 until transactions.length()) {
            val o = transactions.getJSONObject(i)
            txList += TransactionEntity(
                id = o.getInt("id"),
                type = o.getString("type"),
                amountMinor = o.getLong("amountMinor"),
                categoryId = o.getInt("categoryId"),
                note = o.optString("note", ""),
                dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                creditId = o.optInt("creditId", -1).takeIf { it != -1 },
                yearMonth = o.optString("yearMonth", "").takeIf { it.isNotEmpty() }
            )
        }
        transactionDao.insertAll(txList)

        val credits = root.optJSONArray("credits") ?: JSONArray()
        val creditList = mutableListOf<CreditEntity>()
        for (i in 0 until credits.length()) {
            val o = credits.getJSONObject(i)
            creditList += CreditEntity(
                id = o.getInt("id"),
                title = o.getString("title"),
                monthlyPaymentMinor = o.getLong("monthlyPaymentMinor"),
                paymentDay = o.getInt("paymentDay"),
                note = o.optString("note", "")
            )
        }
        creditDao.insertAll(creditList)

        val payments = root.optJSONArray("creditPayments") ?: JSONArray()
        val paymentList = mutableListOf<CreditPaymentEntity>()
        for (i in 0 until payments.length()) {
            val o = payments.getJSONObject(i)
            paymentList += CreditPaymentEntity(
                id = o.getInt("id"),
                creditId = o.getInt("creditId"),
                yearMonth = o.getString("yearMonth")
            )
        }
        paymentDao.insertAll(paymentList)

        return true
    }
}
