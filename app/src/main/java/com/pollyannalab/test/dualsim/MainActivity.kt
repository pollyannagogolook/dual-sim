package com.pollyannalab.test.dualsim

import android.app.Activity
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pollyannalab.test.dualsim.ui.theme.DualSimTheme


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        val simStateMonitor = SimStateMonitor(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainScreen(simStateMonitor)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
private fun MainScreen(simStateMonitor: SimStateMonitor) {
    val countries by simStateMonitor.subIdCountryMap.collectAsState()
    val ringSimCountry by simStateMonitor.ringSimCountry.collectAsState()
    val context = LocalContext.current as ComponentActivity

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            Log.d("MainActivity", "ALL PERMISSIONS GRANTED")
            simStateMonitor.startMonitoring()
        }
    }

    // Role launcher
    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("ExampleScreen", "CALL SCREENING ROLE GRANTED")
        } else {
            Log.d("ExampleScreen", "CALL SCREENING ROLE DENIED")
        }
    }

    // Check and request permissions/role
    LaunchedEffect(Unit) {
        handlePermissionsAndRole(
            context = context,
            simStateMonitor = simStateMonitor,
            permissionLauncher = permissionLauncher,
            roleLauncher = roleLauncher
        )
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextButton(
            modifier = Modifier.padding(16.dp),
            content = { Text("start sim card monitoring") },
            onClick = { simStateMonitor.startMonitoring() })
        Greeting(countries.map { it.value }.joinToString())
        DualSimTheme {
            Greeting(countries.map { it.value }.joinToString())
            if (ringSimCountry.isNotEmpty()) {
                Text("Incoming call from $ringSimCountry")
            }
        }
    }


}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello, Your sim card countries: $name!",
        modifier = modifier
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun handlePermissionsAndRole(
    context: ComponentActivity,
    simStateMonitor: SimStateMonitor,
    permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    roleLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    val requiredPermissions = arrayOf(
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.RECEIVE_SMS,
        android.Manifest.permission.READ_PHONE_STATE,
        android.Manifest.permission.POST_NOTIFICATIONS,
        android.Manifest.permission.MANAGE_OWN_CALLS,
        android.Manifest.permission.FOREGROUND_SERVICE,
    )

    val allPermissionsGranted = requiredPermissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    if (allPermissionsGranted) {
        Log.d("MainActivity", "ALL PERMISSIONS ALREADY GRANTED")
        simStateMonitor.startMonitoring()
    } else {
        permissionLauncher.launch(requiredPermissions)
        val roleManager = context.getSystemService(RoleManager::class.java)
        roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
    }
}



