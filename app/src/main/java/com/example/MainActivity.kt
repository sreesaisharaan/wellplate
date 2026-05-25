package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.WellplateApp
import com.example.ui.WellplateViewModel
import com.example.ui.theme.WellplateTheme

class MainActivity : ComponentActivity() {
  private val viewModel: WellplateViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      WellplateTheme {
        WellplateApp(viewModel = viewModel)
      }
    }
  }
}
