package com.zed.app.core.data.repository

import com.zed.app.core.data.local.BUILT_IN_CATEGORIES
import com.zed.app.core.data.local.CategoryDao
import com.zed.app.core.data.local.CategoryEntity
import com.zed.app.core.domain.model.TransactionType
import com.zed.app.core.domain.repository.CategoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepository {

    override fun observeCategories(): Flow<List<CategoryEntity>> = dao.observe()

    // Стартовый набор — один раз, при пустой таблице
    override suspend fun ensureSeeded() {
        if (dao.count() == 0) dao.insertAll(BUILT_IN_CATEGORIES)
    }

    override suspend fun create(name: String, type: TransactionType): Long =
        dao.insert(CategoryEntity(name = name, type = type.name, builtIn = false))

    override suspend fun delete(id: Int) = dao.delete(id)
}
