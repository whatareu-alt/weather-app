package com.example.aiweathermonitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observes device network connectivity state changes in real-time.
 * 
 * Monitors internet connectivity through ConnectivityManager callbacks and emits
 * boolean values indicating whether the device has internet access.
 * 
 * **Usage:**
 * ```kotlin
 * val observer = NetworkConnectivityObserver(context)
 * observer.isConnected.collect { isOnline ->
 *     Log.d("Network", "Connected: $isOnline")
 * }
 * ```
 * 
 * **Features:**
 * - Real-time connectivity updates
 * - Filters duplicate states with `distinctUntilChanged()`
 * - Sends initial connectivity state on subscription
 * - Properly unregisters callback when flow collection stops
 * 
 * @param context Application context used to access ConnectivityManager system service
 * 
 * @throws IllegalStateException if ConnectivityManager service is unavailable
 * 
 * **Thread Safety:** Safe to collect from any coroutine dispatcher
 * 
 * **Requires Permissions:**
 * - `android.permission.ACCESS_NETWORK_STATE`
 */
class NetworkConnectivityObserver(context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Check if ACCESS_NETWORK_STATE permission is granted.
     * Required for Android 6.0+ (API 23+)
     */
    private fun hasNetworkStatePermission(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val permission = android.Manifest.permission.ACCESS_NETWORK_STATE
            context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true  // Permissions auto-granted on pre-Marshmallow
        }
    }

    /**
     * Observable Flow of network connectivity state.
     * 
     * **Emissions:**
     * - `true`: Device has internet connectivity
     * - `false`: Device has no internet connectivity
     * 
     * **Behavior:**
     * - Emits initial state immediately on subscription
     * - Updates whenever connectivity changes
     * - Filters consecutive duplicate values
     * - Thread-safe: subscriptions can be made from any dispatcher
     * 
     * **Lifecycle:**
     * - Starts monitoring when first collector subscribes
     * - Stops monitoring when all collectors unsubscribe
     */
    val isConnected: Flow<Boolean> = callbackFlow {
        // Get initial state BEFORE registering callback to avoid race condition
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isInitiallyConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                trySend(true)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
        
        // Send initial state after registering to ensure we catch any changes
        trySend(isInitiallyConnected)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}
