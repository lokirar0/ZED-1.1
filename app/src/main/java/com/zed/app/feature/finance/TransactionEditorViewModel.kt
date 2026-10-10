package com.zed.app.feature.finance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zed.app.core.data.local.CategoryEntity
import com.zed.app.core.domain.model.Transaction
import com.zed.app.core.domain.model.TransactionType
import com.zed.app.core.domain.repository.CategoryRepository
import com.zed.app.core.domain.repository.TransactionRepository
import com.zed.app.core.domain.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TxEditorState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val note: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val recurring: Boolean = false,
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: Int? = null,
    val amountError: Boolean = false,
    val finished: Boolean = false
)

@HiltViewModel
class TransactionEditorViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val argDate: Long = savedStateHandle.get<Long>("date") ?: -1L
    private val argType: Int = savedStateHandle.get<Int>("type") ?: 0

    private val _state = MutableStateFlow(
        TxEditorState(
            type = if (argType == 1) TransactionType.INCOME else TransactionType.EXPENSE,
            dateMillis = if (argDate > 0) argDate else System.currentTimeMillis()
        )
    )
    val state: StateFlow<TxEditorState> = _state.asStateFlow()

    init {
        viewModelScope.launch { categoryRepository.ensureSeeded() }
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { list ->
                _state.update { st ->
                    val ofType = list.filter { it.type == st.type.name }
                    val sel = st.selectedCategoryId
                        ?.takeIf { id -> ofType.any { it.id == id } }
                        ?: ofType.firstOrNull()?.id
                    st.copy(categories = list, selectedCategoryId = sel)
                }
            }
        }
    }

    fun onTypeChange(type: TransactionType) {
        _state.update { st ->
            val ofType = st.categories.filter { it.type == type.name }
            st.copy(type = type, selectedCategoryId = ofType.firstOrNull()?.id)
        }
    }

    fun onAmountChange(value: String) {
        _state.value = _state.value.copy(amountText = value, amountError = false)
    }

    fun onNoteChange(value: String) {
        _state.value = _state.value.copy(note = value)
    }

    fun setDate(millis: Long) {
        _state.value = _state.value.copy(dateMillis = millis)
    }

    fun setRecurring(value: Boolean) {
        _state.value = _state.value.copy(recurring = value)
    }

    fun select(categoryId: Int) {
        _state.value = _state.value.copy(selectedCategoryId = categoryId)
    }

    fun selectOtherBuiltIn() {
        _state.update { st ->
            val other = st.categories.firstOrNull {
                it.type == st.type.name && it.key.startsWith("OTHER")
            }
            st.copy(selectedCategoryId = other?.id ?: st.selectedCategoryId)
        }
    }

    fun createAndSelect(name: String) {
        viewModelScope.launch {
            val id = categoryRepository.create(name.trim(), _state.value.type)
            _state.update { it.copy(selectedCategoryId = id.toInt()) }
        }
    }

    fun deleteCategory(id: Int) {
        viewModelScope.launch { categoryRepository.delete(id) }
    }

    fun save() {
        val current = _state.value
        val minor = MoneyFormatter.parseToMinor(current.amountText)
        val categoryId = current.selectedCategoryId
        if (minor == null) {
            _state.value = current.copy(amountError = true)
            return
        }
        if (categoryId == null) return
        viewModelScope.launch {
            repository.insert(
                Transaction(
                    type = current.type,
                    amountMinor = minor,
                    categoryId = categoryId,
                    note = current.note.trim(),
                    dateMillis = current.dateMillis,
                    recurring = current.recurring
                )
            )
            // Сразу материализуем копии, если шаблон создан задним числом
            repository.materializeRecurring()
            _state.value = _state.value.copy(finished = true)
        }
    }
}
