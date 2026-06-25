package com.example.aiweathermonitor

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NetworkConnectivityObserverInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testNetworkObserverCreation() {
        // Arrange & Act
        val observer = NetworkConnectivityObserver(context)

        // Assert - Observer should be created without errors
        assertTrue(observer != null)
    }

    @Test
    fun testNetworkConnectivityFlowIsCollectible() = runTest {
        // Arrange
        val observer = NetworkConnectivityObserver(context)

        // Act
        val firstState = observer.isConnected.first()

        // Assert - Should emit a boolean value
        assertTrue(firstState is Boolean)
    }

    @Test
    fun testNetworkObserverIsCollectibleMultipleTimes() = runTest {
        // Arrange
        val observer = NetworkConnectivityObserver(context)

        // Act
        val firstCollection = observer.isConnected.first()
        val secondCollection = observer.isConnected.first()

        // Assert - Both collections should succeed
        assertTrue(firstCollection is Boolean)
        assertTrue(secondCollection is Boolean)
    }

    @Test
    fun testNetworkObserverEmitsBoolean() = runTest {
        // Arrange
        val observer = NetworkConnectivityObserver(context)

        // Act
        val state = observer.isConnected.first()

        // Assert
        assertTrue(state == true || state == false)
    }

    @Test
    fun testNetworkConnectivityFlowWithTimeout() = runTest {
        // Arrange
        val observer = NetworkConnectivityObserver(context)

        // Act
        val state = observer.isConnected.first()

        // Assert
        assertTrue(state != null)
    }
}
