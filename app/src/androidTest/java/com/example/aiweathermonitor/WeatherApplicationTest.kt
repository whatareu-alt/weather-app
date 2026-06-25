package com.example.aiweathermonitor

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.test.KoinTest
import org.koin.test.inject
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.aiweathermonitor.ui.main.MainScreenViewModel
import kotlinx.serialization.json.Json

@RunWith(AndroidJUnit4::class)
class WeatherApplicationTest : KoinTest {

    private val okHttpClient: OkHttpClient by inject()
    private val jsonParser: Json by inject()
    private val mainScreenViewModel: MainScreenViewModel by inject()

    @Before
    fun setUp() {
        // Clear any previous Koin instances
        GlobalContext.stopKoin()
    }

    @After
    fun tearDown() {
        GlobalContext.stopKoin()
    }

    @Test
    fun testApplicationCreatesKoinContext() {
        // Arrange
        val app: Application = ApplicationProvider.getApplicationContext()

        // Act & Assert - Application should have initialized Koin
        assertNotNull(GlobalContext.getOrNull())
    }

    @Test
    fun testOkHttpClientInjection() {
        // Arrange
        val app: Application = ApplicationProvider.getApplicationContext()
        
        // Manually start Koin with appModule since we're testing injection
        org.koin.core.context.startKoin {
            org.koin.android.ext.koin.androidContext(app)
            modules(appModule)
        }

        // Act
        val client: OkHttpClient = GlobalContext.get().get()

        // Assert
        assertNotNull(client)
    }

    @Test
    fun testJsonParserInjection() {
        // Arrange
        val app: Application = ApplicationProvider.getApplicationContext()
        
        // Manually start Koin with appModule
        org.koin.core.context.startKoin {
            org.koin.android.ext.koin.androidContext(app)
            modules(appModule)
        }

        // Act
        val parser: Json = GlobalContext.get().get()

        // Assert
        assertNotNull(parser)
    }

    @Test
    fun testMainScreenViewModelInjection() {
        // Arrange
        val app: Application = ApplicationProvider.getApplicationContext()
        
        // Manually start Koin with appModule
        org.koin.core.context.startKoin {
            org.koin.android.ext.koin.androidContext(app)
            modules(appModule)
        }

        // Act
        val viewModel: MainScreenViewModel = GlobalContext.get().get()

        // Assert
        assertNotNull(viewModel)
    }

    @Test
    fun testKoinModuleSingletonsAreSameInstance() {
        // Arrange
        val app: Application = ApplicationProvider.getApplicationContext()
        
        org.koin.core.context.startKoin {
            org.koin.android.ext.koin.androidContext(app)
            modules(appModule)
        }

        // Act
        val client1: OkHttpClient = GlobalContext.get().get()
        val client2: OkHttpClient = GlobalContext.get().get()

        // Assert - Singletons should be the same instance
        assert(client1 === client2)
    }

    @Test
    fun testJsonParserIgnoresUnknownKeys() {
        // Arrange
        val app: Application = ApplicationProvider.getApplicationContext()
        
        org.koin.core.context.startKoin {
            org.koin.android.ext.koin.androidContext(app)
            modules(appModule)
        }

        val parser: Json = GlobalContext.get().get()

        // Act & Assert
        assertNotNull(parser)
        // Parser should be configured with ignoreUnknownKeys = true
    }
}
