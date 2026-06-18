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
import com.abccash.app.treasury.local.TreasuryDatabase
import com.abccash.app.treasury.repository.TreasuryRepository
import com.abccash.app.treasury.viewmodel.TreasuryViewModel
import com.abccash.app.treasury.viewmodel.TreasuryViewModelFactory
import com.abccash.app.ui.theme.AppColors

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val database = TreasuryDatabase.getInstance(this)
        val repository = TreasuryRepository(database.treasuryDao(), database)
        val factory = TreasuryViewModelFactory(repository)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = AppColors.Primary,
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFD1FAE8),
                    onPrimaryContainer = Color(0xFF065F46),
                    secondary = AppColors.Secondary,
                    onSecondary = Color.White,
                    background = AppColors.Background,
                    surface = AppColors.Surface,
                    surfaceVariant = AppColors.SurfaceVariant,
                    error = AppColors.Error,
                    onError = Color.White
                )
            ) {
                val vm: TreasuryViewModel = viewModel(factory = factory)
                TreasuryApp(repository = repository, viewModel = vm)
            }
        }
    }
}
