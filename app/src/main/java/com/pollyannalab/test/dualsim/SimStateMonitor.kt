package com.pollyannalab.test.dualsim

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyCallback.CallStateListener
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update


class SimStateMonitor(private val context: Context) {

    val subscriptionManager: SubscriptionManager =
        context.getSystemService(SubscriptionManager::class.java) ?: throw IllegalStateException(
            "SubscriptionManager not available"
        )


    private var subscriptions: List<SubscriptionInfo> = emptyList()
    private val _subIdCountryMap: MutableStateFlow<MutableMap<Int, String>> =
        MutableStateFlow(mutableMapOf())
    val subIdCountryMap: StateFlow<MutableMap<Int, String>> = _subIdCountryMap

    private val _numCountryMap = mutableMapOf<String, String>()
    val numCountryMap: Map<String, String> = _numCountryMap

    var _ringSimCountry: MutableStateFlow<String> = MutableStateFlow("")
    val ringSimCountry: StateFlow<String> = _ringSimCountry

    private val broadcastReceiver = SimStateReceiver()
    private val intentFilter = IntentFilter("com.example.CALL_STATE_CHANGED")

    init {
        ContextCompat.registerReceiver(
            context,
            broadcastReceiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private val callbacks = mutableListOf<TelephonyCallback>()
    private val listeners = mutableListOf<PhoneStateListener>()

    private val subscriptionChangedListener = object :
        SubscriptionManager.OnSubscriptionsChangedListener() {
        override fun onSubscriptionsChanged() {
            if (!hasPhoneStatePermission()) return

            val newSubscriptions = getActiveSubscriptions()
            if (newSubscriptions != subscriptions) {
                cleanup()
                subscriptions = newSubscriptions
                registerSubscription()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private inner class MyTelephonyCallback(private val countryCode: String) : TelephonyCallback(),
        CallStateListener {

        override fun onCallStateChanged(state: Int) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    _ringSimCountry.value = countryCode
                    val intent = Intent()
                    intent.`package` = context.packageName
                    intent.action = "com.example.CALL_STATE_CHANGED"
                    intent.putExtra("SIM_COUNTRY", countryCode)
                    context.sendBroadcast(intent)
                }
            }
        }
    }


    fun startMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            subscriptionManager.addOnSubscriptionsChangedListener(
                context.mainExecutor,
                subscriptionChangedListener
            )
        } else {
            subscriptionManager.addOnSubscriptionsChangedListener(subscriptionChangedListener)
        }
        registerSubscription()
    }


    fun registerSubscription() {
        if (!hasPhoneStatePermission()) return

        subscriptions = getActiveSubscriptions()
        _subIdCountryMap.value.clear()

        subscriptions.takeIf { it.isNotEmpty() }?.forEach { sub ->
            val subId = sub.subscriptionId
            val telephonyManager = context.getSystemService(
                TelephonyManager::class.java
            )
                .createForSubscriptionId(subId)


            registerCallStateCallback(telephonyManager, subId, sub.countryIso)
            updateCountryInfo(sub, telephonyManager)
        }

    }

    fun stopMonitoring() {
        subscriptionManager.removeOnSubscriptionsChangedListener(subscriptionChangedListener)
        cleanup()
    }

    private fun registerCallStateCallback(
        telephonyManager: TelephonyManager,
        subId: Int,
        subCountry: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = MyTelephonyCallback(subCountry)
            telephonyManager.registerTelephonyCallback(
                context.mainExecutor,
                callback
            )
            callbacks.add(callback)
        } else {
            val listener = SimStateListener(context, subId)
            telephonyManager.listen(
                listener,
                PhoneStateListener.LISTEN_CALL_STATE
            )
            listeners.add(listener)
        }
    }

    private fun updateCountryInfo(sub: SubscriptionInfo, telephonyManager: TelephonyManager) {
        val country = telephonyManager.simCountryIso
        _subIdCountryMap.update { currentMap ->
            currentMap.toMutableMap().apply {
                put(sub.subscriptionId, country)
            }
        }

        _numCountryMap.putAll(
            sub.number?.let { number ->
                mapOf(number to country)
            } ?: emptyMap()
        )


    }


    fun cleanup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            callbacks.forEach { callback ->
                context.getSystemService(TelephonyManager::class.java)
                    .unregisterTelephonyCallback(callback)
            }
            callbacks.clear()
        } else {
            listeners.forEach { listener ->
                context.getSystemService(TelephonyManager::class.java)
                    .listen(listener, PhoneStateListener.LISTEN_NONE)
            }
            listeners.clear()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getActiveSubscriptions(): List<SubscriptionInfo> {
        return subscriptionManager.activeSubscriptionInfoList ?: emptyList()
    }

    private fun hasPhoneStatePermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        @Volatile
        private var INSTANCE: SimStateMonitor? = null

        fun getInstance(context: Context): SimStateMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SimStateMonitor(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

}

