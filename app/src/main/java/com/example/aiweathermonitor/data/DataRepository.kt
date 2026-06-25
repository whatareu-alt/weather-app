package com.example.aiweathermonitor.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository interface for managing application data access.
 * Provides a reactive interface for consuming data through Flows.
 */
interface DataRepository {
    /**
     * Reactive stream of data updates.
     * Emits a list of strings representing application data.
     * Collectors will receive updates whenever data changes.
     */
    val data: Flow<List<String>>
}

/**
 * Default implementation of [DataRepository].
 * Currently provides a simple static list of data.
 * 
 * @see DataRepository
 */
class DefaultDataRepository : DataRepository {
    /**
     * Static data flow that emits a single list containing "Android".
     * In a production app, this would fetch data from network/database.
     */
    override val data: Flow<List<String>> = flow { 
        emit(listOf("Android")) 
    }
}
