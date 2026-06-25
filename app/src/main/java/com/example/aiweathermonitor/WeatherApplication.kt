package com.example.aiweathermonitor

import android.app.Application
import com.example.aiweathermonitor.ui.main.MainScreenViewModel
import com.example.aiweathermonitor.util.HttpClientFactory
import com.example.aiweathermonitor.data.WeatherRepository
import com.example.aiweathermonitor.data.DefaultWeatherRepository
import com.example.aiweathermonitor.data.NewsRepository
import com.example.aiweathermonitor.data.DefaultNewsRepository
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

val appModule = module {
    // Shared OkHttpClient (timeouts/retry centralised in HttpClientFactory)
    single { HttpClientFactory.create() }

    // Json parser as a singleton
    single { Json { ignoreUnknownKeys = true } }

    // Weather data layer (network + parsing + mapping)
    single<WeatherRepository> { DefaultWeatherRepository(get(), get()) }

    // News data layer (RSS fetch + XML parsing)
    single<NewsRepository> { DefaultNewsRepository(get()) }

    // MainScreenViewModel with injected dependencies
    viewModel { MainScreenViewModel(get(), get(), get()) }
}

class WeatherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@WeatherApplication)
            modules(appModule)
        }
        NotificationHelper.createChannels(this)
        WeatherRefreshWorker.schedule(this)
    }
}
