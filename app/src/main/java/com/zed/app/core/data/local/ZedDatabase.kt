package com.zed.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// v5 (ZED 2.0): таблица accounts (счета) + новые колонки у habits/transactions
@Database(
    entities = [
        HabitEntity::class,
        HabitCompletionEntity::class,
        TransactionEntity::class,
        CreditEntity::class,
        CreditPaymentEntity::class,
        CategoryEntity::class,
        FavoriteTrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        SessionEntity::class,
        HabitGoalEntity::class,
        AccountEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class ZedDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun transactionDao(): TransactionDao
    abstract fun creditDao(): CreditDao
    abstract fun creditPaymentDao(): CreditPaymentDao
    abstract fun categoryDao(): CategoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun sessionDao(): SessionDao
    abstract fun habitGoalDao(): HabitGoalDao
    abstract fun accountDao(): AccountDao

    companion object {
        const val NAME = "zed.db"
    }
}
