package com.zed.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zed.app.core.domain.model.AudioTrack
import com.zed.app.core.domain.repository.CreditRepository
import com.zed.app.core.domain.repository.HabitRepository
import com.zed.app.core.domain.repository.TransactionRepository
import com.zed.app.core.domain.util.MoneyFormatter
import com.zed.app.core.media.LocalAudioScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchItem(
    val title: String,
    val subtitle: String,
    val tabRoute: String
)

data class SearchUiState(
    val query: String = "",
    val results: List<SearchItem> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    habitRepository: HabitRepository,
    transactionRepository: TransactionRepository,
    creditRepository: CreditRepository,
    scanner: LocalAudioScanner
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val tracks = MutableStateFlow<List<AudioTrack>>(emptyList())

    init {
        viewModelScope.launch { tracks.value = scanner.scan() }
    }

    val state: StateFlow<SearchUiState> = combine(
        habitRepository.observeHabits(),
        transactionRepository.observeTransactions(),
        creditRepository.observeCredits(),
        tracks,
        query
    ) { habits, transactions, credits, music, rawQuery ->
        val q = rawQuery.trim().lowercase()
        if (q.isEmpty()) {
            SearchUiState(rawQuery, emptyList())
        } else {
            val results = mutableListOf<SearchItem>()
            habits.filter { it.title.lowercase().contains(q) || it.note.lowercase().contains(q) }
                .forEach { results += SearchItem(it.title, it.note, "habits") }
            transactions.filter {
                it.note.lowercase().contains(q) || MoneyFormatter.format(it.amountMinor).contains(q)
            }.forEach {
                results += SearchItem(
                    title = if (it.note.isBlank()) MoneyFormatter.format(it.amountMinor) else it.note,
                    subtitle = MoneyFormatter.format(it.amountMinor),
                    tabRoute = "finance"
                )
            }
            credits.filter { it.title.lowercase().contains(q) || it.note.lowercase().contains(q) }
                .forEach { results += SearchItem(it.title, it.note, "credits") }
            // Музыка ведёт на полноэкранный плеер
            music.filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) }
                .forEach { results += SearchItem(it.title, it.artist, "player_screen") }
            SearchUiState(rawQuery, results)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }
}
