package com.pollyannalab.test.dualsim

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log


class SimStateListener(private val context: Context, private val subId: Int) :
    PhoneStateListener() {

    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
        super.onCallStateChanged(state, phoneNumber)

        if (state == TelephonyManager.CALL_STATE_RINGING) {
            try {
                context.getSystemService(SubscriptionManager::class.java)?.let { subscriptionManager ->
                    subscriptionManager.activeSubscriptionInfoList?.find {
                        it.subscriptionId == subId
                    }?.let { subscriptionInfo ->
                        val countryIso = subscriptionInfo.countryIso
                        val carrierName = subscriptionInfo.carrierName

                        Log.d(
                            "CallState",
                            "Incoming call from $phoneNumber on SIM: $carrierName (Country: $countryIso)"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("CallState", "Error handling call state: ${e.message}")
            }
        }
    }
}