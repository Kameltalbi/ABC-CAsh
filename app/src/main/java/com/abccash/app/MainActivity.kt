package com.abccash.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abccash.app.data.local.AppDatabase
import com.abccash.app.presentation.CashflowViewModel
import com.abccash.app.presentation.CashflowViewModelFactory
import com.abccash.app.ui.CashflowApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(this)
        val factory = CashflowViewModelFactory(
            transactionDao = db.transactionDao(),
            balanceDao = db.balanceDao(),
            categoryDao = db.categoryDao()
        )

        setContent {
            val vm: CashflowViewModel = viewModel(factory = factory)
            CashflowApp(viewModel = vm)
        }
    }
}
