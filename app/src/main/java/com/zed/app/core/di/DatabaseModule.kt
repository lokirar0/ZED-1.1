package com.zed.app.core.di

import android.content.Context
import androidx.room.Room
import com.zed.app.core.data.local.CategoryDao
import com.zed.app.core.data.local.CreditDao
import com.zed.app.core.data.local.CreditPaymentDao
import com.zed.app.core.data.local.FavoriteDao
import com.zed.app.core.data.local.HabitCompletionDao
import com.zed.app.core.data.local.HabitDao
import com.zed.app.core.data.local.PlaylistDao
import com.zed.app.core.data.local.TransactionDao
import com.zed.app.core.data.local.ZedDatabase
import com.zed.app.core.data.repository.CategoryRepositoryImpl
import com.zed.app.core.data.repository.CreditRepositoryImpl
import com.zed.app.core.data.repository.HabitRepositoryImpl
import com.zed.app.core.data.repository.PlayerRepositoryImpl
import com.zed.app.core.data.repository.TransactionRepositoryImpl
import com.zed.app.core.domain.repository.CategoryRepository
import com.zed.app.core.domain.repository.CreditRepository
import com.zed.app.core.domain.repository.HabitRepository
import com.zed.app.core.domain.repository.PlayerRepository
import com.zed.app.core.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ZedDatabase =
        Room.databaseBuilder(context, ZedDatabase::class.java, ZedDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideHabitDao(db: ZedDatabase): HabitDao = db.habitDao()

    @Provides
    fun provideCompletionDao(db: ZedDatabase): HabitCompletionDao = db.habitCompletionDao()

    @Provides
    fun provideTransactionDao(db: ZedDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCreditDao(db: ZedDatabase): CreditDao = db.creditDao()

    @Provides
    fun provideCreditPaymentDao(db: ZedDatabase): CreditPaymentDao = db.creditPaymentDao()

    @Provides
    fun provideCategoryDao(db: ZedDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideFavoriteDao(db: ZedDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun providePlaylistDao(db: ZedDatabase): PlaylistDao = db.playlistDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCreditRepository(impl: CreditRepositoryImpl): CreditRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindPlayerRepository(impl: PlayerRepositoryImpl): PlayerRepository
}
