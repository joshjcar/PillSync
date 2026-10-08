package com.tally.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tally.app.alarm.AlarmScheduler
import com.tally.app.data.AutoBackup
import com.tally.app.data.BackupManager
import com.tally.app.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: SettingsStore, onBack: () -> Unit, onPrivacy: () -> Unit) {
    val context = LocalContext.current

    var breakfast by remember { mutableIntStateOf(settings.breakfastMinute) }
    var lunch by remember { mutableIntStateOf(settings.lunchMinute) }
    var dinner by remember { mutableIntStateOf(settings.dinnerMinute) }
    var snack by remember { mutableIntStateOf(settings.snackMinute) }
    var snooze by remember { mutableFloatStateOf(settings.snoozeMinutes.toFloat()) }
    var autoSilence by remember { mutableFloatStateOf(settings.autoSilenceMinutes.toFloat()) }
    var escalate by remember { mutableStateOf(settings.escalate) }

    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching {
                val json = BackupManager.exportJson(context)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }
            }.isSuccess
            Toast.makeText(context, if (ok) "Backup saved" else "Export failed", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            val result = runCatching {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("empty file")
                }
                BackupManager.importJson(context, json)
            }
            Toast.makeText(
                context,
                result.fold({ "Restored ${it.medicines} medicine(s)" }, { "Import failed — invalid file" }),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Persist + re-arm meal-relative alarms when leaving the screen.
    DisposableEffect(Unit) {
        onDispose {
            settings.breakfastMinute = breakfast
            settings.lunchMinute = lunch
            settings.dinnerMinute = dinner
            settings.snackMinute = snack
            settings.snoozeMinutes = snooze.toInt()
            settings.autoSilenceMinutes = autoSilence.toInt()
            settings.escalate = escalate
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { AlarmScheduler.rescheduleAll(context) }
                runCatching { AutoBackup.maybeWrite(context) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionCard("Meal times", "Used for pre-meal and post-meal reminders.") {
                    MealRow(context, "Breakfast", breakfast) { breakfast = it }
                    MealRow(context, "Lunch", lunch) { lunch = it }
                    MealRow(context, "Dinner", dinner) { dinner = it }
                    MealRow(context, "Snack", snack) { snack = it }
                }
            }
            item {
                SectionCard("Alarm behaviour", null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Escalating volume", fontWeight = FontWeight.Medium)
                            Text(
                                "Start quiet and grow louder until answered.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = escalate, onCheckedChange = { escalate = it })
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Snooze length: ${snooze.toInt()} min", fontWeight = FontWeight.Medium)
                    Slider(value = snooze, onValueChange = { snooze = it }, valueRange = 1f..60f)
                    Spacer(Modifier.height(6.dp))
                    Text("Auto-silence after: ${autoSilence.toInt()} min", fontWeight = FontWeight.Medium)
                    Text(
                        "Safety cap so the alarm doesn't ring forever if you're away.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(value = autoSilence, onValueChange = { autoSilence = it }, valueRange = 1f..30f)
                }
            }
            item {
                SectionCard("Backup & restore", "Save all medicines and settings to a file on your phone.") {
                    Text(
                        "A safety copy is kept automatically in your Downloads folder so your data " +
                            "survives reinstalling or switching phones. It stays on this device and is " +
                            "never uploaded.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { exportLauncher.launch("pillsync-backup.json") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.width(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export")
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.width(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Import")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Importing replaces everything currently in the app.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                SectionCard("Privacy & legal", null) {
                    Text(
                        "PillSync works entirely on this phone. It has no internet permission and never " +
                            "sends your data anywhere.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onPrivacy)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Read the full Privacy Policy", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, subtitle: String?, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun MealRow(
    context: android.content.Context,
    label: String,
    minute: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { showTimePicker(context, minute) { onChange(it) } }) {
            Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.width(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(formatMinute(context, minute))
        }
    }
}
