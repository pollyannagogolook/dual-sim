package com.pollyannalab.test.dualsim

import android.content.Context
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Before
import org.junit.Rule

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class PhoneCallTest {
    private lateinit var context: Context
    private lateinit var uiDevice: UiDevice
    private lateinit var telecomManager: TelecomManager
    private lateinit var subscriptionManager: SubscriptionManager

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.CALL_PHONE,
        android.Manifest.permission.READ_PHONE_STATE,
        android.Manifest.permission.ANSWER_PHONE_CALLS,
        android.Manifest.permission.READ_PHONE_NUMBERS
    )

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)


    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
    }
    @Test
    fun testSimulateIncomingCallSim1() { val tm = ApplicationProvider.getApplicationContext<Context>()
        .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        val phoneStateListener = mockk<PhoneStateListener>(relaxed = true)
        tm.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)

        // 驗證 IDLE 狀態
        verify { phoneStateListener.onCallStateChanged(TelephonyManager.CALL_STATE_IDLE, null) }

        // 模擬來電響鈴狀態
        val number = "1234567890"
        every { phoneStateListener.onCallStateChanged(TelephonyManager.CALL_STATE_RINGING, number) } just Runs
        tm.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }


}