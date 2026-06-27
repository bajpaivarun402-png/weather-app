package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.screens.MainScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WeatherPage
import com.example.ui.viewmodel.WeatherViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: WeatherViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        var showSplash by remember { mutableStateOf(true) }
        val currentPage by viewModel.currentPage.collectAsState()

        AnimatedContent(
          targetState = if (showSplash) WeatherPage.SPLASH else currentPage,
          transitionSpec = {
            fadeIn() togetherWith fadeOut()
          },
          label = "rootNavigation"
        ) { page ->
          when (page) {
            WeatherPage.SPLASH -> {
              SplashScreen(
                onSplashComplete = { showSplash = false }
              )
            }
            WeatherPage.ONBOARDING -> {
              OnboardingScreen(
                onComplete = { isFahrenheit, allowGps ->
                  if (isFahrenheit) {
                    viewModel.toggleTemperatureUnit() // switch to F
                  }
                  viewModel.setGpsPermissionGranted(allowGps)
                  viewModel.completeOnboarding()
                }
              )
            }
            else -> {
              MainScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            }
          }
        }
      }
    }
  }
}

