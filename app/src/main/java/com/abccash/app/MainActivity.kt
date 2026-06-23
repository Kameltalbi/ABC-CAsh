package com.abccash.app

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abccash.app.treasury.TreasuryApp
import com.abccash.app.treasury.backup.GoogleBackupManager
import com.abccash.app.treasury.datastore.UserPreferences
import com.abccash.app.treasury.local.TreasuryDatabase
import com.abccash.app.treasury.repository.TreasuryRepository
import com.abccash.app.treasury.viewmodel.TreasuryViewModel
import com.abccash.app.treasury.viewmodel.TreasuryViewModelFactory
import com.abccash.app.ui.theme.AppColors
import com.abccash.app.ui.theme.AppPalette
import com.abccash.app.ui.theme.appColorScheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val userPreferences = UserPreferences(this)
        val database = TreasuryDatabase.getInstance(this)
        val repository = TreasuryRepository(database.treasuryDao(), database)
        val googleBackupManager = GoogleBackupManager(this)
        val factory = TreasuryViewModelFactory(repository, googleBackupManager, userPreferences)

        setContent {
            MaterialTheme(
                colorScheme = appColorScheme(darkMode = false, palette = AppPalette.ABC_CASH)
            ) {
                val vm: TreasuryViewModel = viewModel(factory = factory)
                TreasuryApp(
                    repository = repository,
                    viewModel = vm,
                    userPreferences = userPreferences,
                    googleBackupManager = googleBackupManager
                )
            }
        }
    }
}
