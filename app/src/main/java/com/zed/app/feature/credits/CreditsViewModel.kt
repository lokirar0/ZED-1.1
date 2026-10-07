package com.zed.app.feature.credits

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zed.app.core.domain.repository.CreditRepository
import com.zed.app.core.domain.repository.TransactionRepository
import com.zed.app.core.domain.util.MoneyFormatter
import com.zed.app.core.domain.util.PaymentCalculator
import com.zed.app.core.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Карточка кредита на экране
data class CreditUiItem(
    val id: Int,
    val title: String,
    val monthlyText: String,
    val payDay: Int,
    val days: Long,            // до ближайшего неоплаченного платежа (<0 = просрочка)
    val paidThisMonth: Boolean,
    val countdownDanger: Boolean
)

data class CreditsUiState(
    val credits: List<CreditUiItem> = emptyList(),
    val burdenText: String = "0",
    val activeCount: Int = 0,
    val loaded: Boolean = false
)

@HiltViewModel
class CreditsViewModel @Inject constructor(
    private val repository: CreditRepository,
    private val transactionRepository: TransactionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val state: StateFlow<CreditsUiState> =
        combine(repository.observeCredits(), repository.observePayments()) { credits, payments ->
            val today = LocalDate.now()
            val paidByCredit = payments.groupBy({ it.creditId }, { it.yearMonth })

            val items = credits.map { credit ->
                val paidMonths = paidByCredit[credit.id]?.toSet() ?: emptySet()
                val (date, paidThisMonth) =
                    PaymentCalculator.nextUnpaid(credit.paymentDay, today, paidMonths)
                val days = PaymentCalculator.daysUntil(date, today)

                CreditUiItem(
                    id = credit.id,
                    title = credit.title,
                    monthlyText = MoneyFormatter.format(credit.monthlyPaymentMinor),
                    payDay = credit.paymentDay,
                    days = days,
                    paidThisMonth = paidThisMonth,
                    countdownDanger = !paidThisMonth && days <= 3
                )
            }

            CreditsUiState(
                credits = items,
                burdenText = MoneyFormatter.format(credits.sumOf { it.monthlyPaymentMinor }),
                activeCount = credits.size,
                loaded = true
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CreditsUiState())

    init {
        // Гарантируем, что воркер кредитов запланирован (идемпотентно)
        viewModelScope.launch { ReminderScheduler.scheduleCredits(context) }
    }

    // Отметка «оплачено» + авто-списание в модуле «Финансы»
    fun togglePaid(creditId: Int, paid: Boolean) {
        viewModelScope.launch {
            val yearMonth = YearMonth.now().toString()
            repository.setPaid(creditId, yearMonth, paid)

            val credit = repository.getCredit(creditId) ?: return@launch
            val date = PaymentCalculator.paymentDate(credit.paymentDay, YearMonth.now())
            val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            transactionRepository.syncCreditPayment(
                creditId = creditId,
                creditTitle = credit.title,
                amountMinor = credit.monthlyPaymentMinor,
                dateMillis = millis,
                yearMonth = yearMonth,
                paid = paid
            )
        }
    }

    fun delete(creditId: Int) {
        viewModelScope.launch { repository.delete(creditId) }
    }
}
