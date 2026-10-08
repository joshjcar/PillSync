package com.tally.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.tally.app.ui.EditMedicineScreen
import com.tally.app.ui.HistoryScreen
import com.tally.app.ui.HomeScreen
import com.tally.app.ui.PrivacyScreen
import com.tally.app.ui.SettingsScreen
import com.tally.app.ui.TallyViewModel
import com.tally.app.ui.theme.TallyTheme
import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TallyTheme {
                val vm: TallyViewModel = viewModel()

                var privacyAccepted by remember { mutableStateOf(vm.settings.privacyAccepted) }
                if (!privacyAccepted) {
                    // One-time consent gate shown only on the very first launch.
                    PrivacyScreen(onAccept = {
                        vm.settings.privacyAccepted = true
                        privacyAccepted = true
                    })
                    return@TallyTheme
                }

                val notifLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* result handled by system; UI re-checks on resume */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val context = androidx.compose.ui.platform.LocalContext.current
                val restoreLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) vm.restoreFromUri(uri) { ok ->
                        android.widget.Toast.makeText(
                            context,
                            if (ok) "Your data was restored" else "Couldn't read that backup file",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }

                val nav = rememberNavController()
                val medicines by vm.medicines.collectAsState()
                val autoFound by vm.autoFound.collectAsState()

                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            items = medicines,
                            onAdd = { nav.navigate("edit/-1") },
                            onEdit = { id -> nav.navigate("edit/$id") },
                            onToggle = { vm.toggleActive(it) },
                            onDelete = { vm.delete(it) },
                            onTakeNow = { vm.takeNow(it) },
                            onSettings = { nav.navigate("settings") },
                            onHistory = { nav.navigate("history") },
                            autoFoundAt = autoFound?.savedAt,
                            onRestorePick = { restoreLauncher.launch(arrayOf("application/json", "*/*")) },
                            onRestoreAuto = {
                                autoFound?.let { f ->
                                    vm.restoreFromUri(f.uri) { ok ->
                                        android.widget.Toast.makeText(
                                            context,
                                            if (ok) "Your data was restored" else "Couldn't restore the backup",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        )
                    }
                    composable("history") {
                        val logs by vm.recentLogs.collectAsState()
                        HistoryScreen(
                            logs = logs,
                            onBack = { nav.popBackStack() },
                            onSetStatus = { id, status -> vm.setDoseStatus(id, status) }
                        )
                    }
                    composable(
                        "edit/{medicineId}",
                        arguments = listOf(navArgument("medicineId") { type = NavType.LongType })
                    ) { backStack ->
                        val id = backStack.arguments?.getLong("medicineId") ?: -1L
                        EditMedicineScreen(
                            vm = vm,
                            medicineId = id,
                            onDone = { nav.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            settings = vm.settings,
                            onBack = { nav.popBackStack() },
                            onPrivacy = { nav.navigate("privacy") }
                        )
                    }
                    composable("privacy") {
                        PrivacyScreen(onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
