package com.kruju.habits

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.kruju.habits.ui.HabitApp
import com.kruju.habits.ui.HabitTheme

class MainActivity : ComponentActivity() {
    private val viewModel: HabitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitTheme {
                HabitApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshToday()
    }
}
