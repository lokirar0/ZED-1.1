package com.zed.app.feature.credits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zed.app.core.domain.model.Credit
import com.zed.app.core.domain.repository.CreditRepository
import com.zed.app.core.domain.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreditEditorState(
    val title: String = "",
    val amountText: String = "",
    val dayText: String = "",
    val note: String = "",
    val titleError: Boolean = false,
    val amountError: Boolean = false,
    val dayError: Boolean = false,
    val isEditing: Boolean = false,
    val finished: Boolean = false
)

@HiltViewModel
class CreditEditorViewModel @Inject constructor(
    private val repository: CreditRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val creditId: Int = savedStateHandle.get<Int>("creditId") ?: -1

    private val _state = MutableStateFlow(CreditEditorState(isEditing = creditId != -1))
    val state: StateFlow<CreditEditorState> = _state.asStateFlow()

    init {
        if (creditId != -1) {
            viewModelScope.launch {
                repository.getCredit(creditId)?.let { credit ->
                    _state.value = _state.value.copy(
                        title = credit.title,
                        amountText = MoneyFormatter.format(credit.monthlyPaymentMinor),
                        dayText = credit.paymentDay.toString(),
                        note = credit.note
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _state.value = _state.value.copy(title = value, titleError = false)
    }

    fun onAmountChange(value: String) {
        _state.value = _state.value.copy(amountText = value, amountError = false)
    }

    fun onDayChange(value: String) {
        _state.value = _state.value.copy(dayText = value, dayError = false)
    }

    fun onNoteChange(value: String) {
        _state.value = _state.value.copy(note = value)
    }

    fun save() {
        val current = _state.value
        val minor = MoneyFormatter.parseToMinor(current.amountText)
        val day = current.dayText.trim().toIntOrNull()

        val titleBad = current.title.isBlank()
        val amountBad = minor == null
        val dayBad = day == null || day !in 1..31
        if (titleBad || amountBad || dayBad) {
            _state.value = current.copy(
                titleError = titleBad,
                amountError = amountBad,
                dayError = dayBad
            )
            return
        }
        viewModelScope.launch {
            repository.upsert(
                Credit(
                    id = if (current.isEditing) creditId else 0,
                    title = current.title.trim(),
                    monthlyPaymentMinor = minor!!,
                    paymentDay = day!!,
                    note = current.note.trim()
                )
            )
            _state.value = _state.value.copy(finished = true)
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.delete(creditId)
            _state.value = _state.value.copy(finished = true)
        }
    }
}
