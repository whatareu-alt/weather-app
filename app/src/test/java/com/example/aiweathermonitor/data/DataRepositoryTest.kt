package com.example.aiweathermonitor.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DataRepositoryTest {

    @Test
    fun testDefaultDataRepositoryEmitsData() = runTest {
        // Arrange
        val repository = DefaultDataRepository()

        // Act
        val data = repository.data.first()

        // Assert
        assertEquals(listOf("Android"), data)
    }

    @Test
    fun testDefaultDataRepositoryEmitsNonEmptyList() = runTest {
        // Arrange
        val repository = DefaultDataRepository()

        // Act
        val data = repository.data.first()

        // Assert
        assertEquals(1, data.size)
        assertEquals("Android", data[0])
    }

    @Test
    fun testDataRepositoryIsFlow() = runTest {
        // Arrange
        val repository = DefaultDataRepository()

        // Act & Assert - Flow should be collectible multiple times
        val firstCollection = repository.data.first()
        val secondCollection = repository.data.first()

        assertEquals(firstCollection, secondCollection)
    }

    @Test
    fun testDataRepositoryImplementsInterface() {
        // Arrange & Act
        val repository: DataRepository = DefaultDataRepository()

        // Assert
        assertEquals(repository.javaClass.simpleName, "DefaultDataRepository")
    }
}
