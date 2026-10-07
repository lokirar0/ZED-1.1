package com.zed.app.core.domain.repository

import com.zed.app.core.data.local.CategoryEntity
import com.zed.app.core.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

// Категории отдаём сущностями Room: builtIn резолвятся в UI через categoryKeyRes
interface CategoryRepository {
    fun observeCategories(): Flow<List<CategoryEntity>>
    suspend fun ensureSeeded()
    suspend fun create(name: String, type: TransactionType): Long
    suspend fun delete(id: Int)
}
