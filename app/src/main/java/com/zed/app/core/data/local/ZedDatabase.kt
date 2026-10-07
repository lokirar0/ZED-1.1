package com.zed.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// v2: habits.reminderTimeMinutes (персональные напоминания)
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
        PlaylistTrackEntity::class
    ],
    version = 2,
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

    companion object {
        const val NAME = "zed.db"
    }
}
