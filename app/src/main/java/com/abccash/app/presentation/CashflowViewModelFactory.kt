package com.abccash.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.abccash.app.data.local.dao.BalanceDao
import com.abccash.app.data.local.dao.CategoryDao
import com.abccash.app.data.local.dao.TransactionDao
import com.abccash.app.domain.usecase.Build12MonthCumulativeForecastUseCase
import com.abccash.app.domain.usecase.CreateTransactionWithRecurrenceUseCase

class CashflowViewModelFactory(
    private val transactionDao: TransactionDao,
    private val balanceDao: BalanceDao,
    private val categoryDao: CategoryDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CashflowViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CashflowViewModel(
                transactionDao = transactionDao,
                balanceDao = balanceDao,
                categoryDao = categoryDao,
                createTransactionUseCase = CreateTransactionWithRecurrenceUseCase(transactionDao),
                buildForecastUseCase = Build12MonthCumulativeForecastUseCase(balanceDao, transactionDao)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
