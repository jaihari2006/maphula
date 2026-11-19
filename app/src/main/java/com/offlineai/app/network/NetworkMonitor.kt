package com.offlineai.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(private val context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    // Auto-switch setting (enabled by default to automatically follow hardware internet)
    private val _isAutoSwitchEnabled = MutableStateFlow(true)
    val isAutoSwitchEnabledFlow: StateFlow<Boolean> = _isAutoSwitchEnabled.asStateFlow()
    var isAutoSwitchEnabled: Boolean
        get() = _isAutoSwitchEnabled.value
        set(value) {
            _isAutoSwitchEnabled.value = value
            updateEffectiveOnlineStatus()
        }

    // Manual Slider position (false = Strict Offline, true = Online Allowed)
    private val _manualOnlineState = MutableStateFlow(false)
    val manualOnlineStateFlow: StateFlow<Boolean> = _manualOnlineState.asStateFlow()
    var manualOnlineState: Boolean
        get() = _manualOnlineState.value
        set(value) {
            _manualOnlineState.value = value
            updateEffectiveOnlineStatus()
        }

    private var hardwareOnline: Boolean = false

    private val _isOnline = MutableStateFlow(false)
    val isOnlineFlow: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        hardwareOnline = checkHardwareOnlineStatus()
        updateEffectiveOnlineStatus()
        registerCallback()
    }

    private fun updateEffectiveOnlineStatus() {
        val effective = if (_isAutoSwitchEnabled.value) {
            hardwareOnline
        } else {
            _manualOnlineState.value
        }
        _isOnline.value = effective
    }

    /**
     * Synchronous check for active effective connectivity status.
     */
    fun isOnline(): Boolean {
        hardwareOnline = checkHardwareOnlineStatus()
        updateEffectiveOnlineStatus()
        return _isOnline.value
    }

    fun isHardwareOnline(): Boolean {
        hardwareOnline = checkHardwareOnlineStatus()
        return hardwareOnline
    }

    fun setManualMode(online: Boolean) {
        _isAutoSwitchEnabled.value = false
        _manualOnlineState.value = online
        updateEffectiveOnlineStatus()
    }

    fun setAutoSwitch(enabled: Boolean) {
        _isAutoSwitchEnabled.value = enabled
        hardwareOnline = checkHardwareOnlineStatus()
        updateEffectiveOnlineStatus()
    }

    private fun checkHardwareOnlineStatus(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun registerCallback() {
        val cm = connectivityManager ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                hardwareOnline = true
                updateEffectiveOnlineStatus()
            }

            override fun onLost(network: Network) {
                hardwareOnline = checkHardwareOnlineStatus()
                updateEffectiveOnlineStatus()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                hardwareOnline = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                updateEffectiveOnlineStatus()
            }
        }

        try {
            cm.registerNetworkCallback(request, networkCallback!!)
        } catch (_: Exception) {
            // Graceful fallback in sandboxed/mock environments
        }
    }

    fun unregister() {
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
    }
}
