package com.tally.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import com.tally.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import com.tally.app.data.Medicine
import com.tally.app.data.MedicineWithSchedules
import com.tally.app.data.ScheduleResolver
import com.tally.app.data.SettingsStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    items: List<MedicineWithSchedules>,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onToggle: (Medicine) -> Unit,
    onDelete: (Medicine) -> Unit,
    onTakeNow: (Medicine) -> Unit,
    onSettings: () -> Unit,
    onHistory: () -> Unit,
    autoFoundAt: Long? = null,
    onRestorePick: () -> Unit = {},
    onRestoreAuto: () -> Unit = {}
) {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }

    // Re-check permissions whenever we come back to the foreground.
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PillSync", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onHistory) {
                        Icon(Icons.Filled.History, contentDescription = "History")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add medicine") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ReliabilityBanners(context, refresh) }

            if (items.isEmpty()) {
                item { EmptyState() }
                item {
                    Spacer(Modifier.height(12.dp))
                    RestoreCard(
                        autoFoundAt = autoFoundAt,
                        onRestorePick = onRestorePick,
                        onRestoreAuto = onRestoreAuto
                    )
                }
            } else {
                items(items, key = { it.medicine.id }) { mws ->
                    MedicineCard(
                        context = context,
                        settings = settings,
                        item = mws,
                        onEdit = { onEdit(mws.medicine.id) },
                        onToggle = { onToggle(mws.medicine) },
                        onDelete = { onDelete(mws.medicine) },
                        onTakeNow = { onTakeNow(mws.medicine) }
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun EmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.tally_logo),
                contentDescription = null,
                modifier = Modifier.size(84.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text("No medicines yet", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap \"Add medicine\" to create your first reminder.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun RestoreCard(
    autoFoundAt: Long?,
    onRestorePick: () -> Unit,
    onRestoreAuto: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (autoFoundAt != null)
                MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            if (autoFoundAt != null) {
                val date = remember(autoFoundAt) {
                    java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date(autoFoundAt))
                }
                Text(
                    "We found your backup",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "A safety copy from $date is saved on this phone. Restore it to bring back your medicines.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRestoreAuto, modifier = Modifier.fillMaxWidth()) {
                    Text("Restore my data")
                }
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onRestorePick, modifier = Modifier.fillMaxWidth()) {
                    Text("Choose a different backup file")
                }
            } else {
                Text(
                    "Switched phones or reinstalled?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "If you saved or exported a PillSync backup, restore it here.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onRestorePick, modifier = Modifier.fillMaxWidth()) {
                    Text("Restore from a backup")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun MedicineCard(
    context: Context,
    settings: SettingsStore,
    item: MedicineWithSchedules,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onTakeNow: () -> Unit
) {
    val med = item.medicine
    var confirmDelete by remember { mutableStateOf(false) }
    var tookNow by remember { mutableStateOf(false) }

    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            colorFromHex(med.colorHex).copy(alpha = 0.18f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Medication,
                        contentDescription = null,
                        tint = colorFromHex(med.colorHex)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        med.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (med.dosage.isNotBlank()) {
                        Text(
                            med.dosage,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(checked = med.active, onCheckedChange = { onToggle() })
            }

            if (med.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    med.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))
            if (item.schedules.isEmpty()) {
                Text(
                    "No times set — tap to add",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item.schedules.forEach { s ->
                        val minute = ScheduleResolver.minuteOfDay(s, settings)
                        AssistChip(
                            onClick = onEdit,
                            label = { Text(scheduleSummary(context, s, minute), fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = null
                        )
                    }
                }
            }

            if (med.stockEnabled) {
                Spacer(Modifier.height(10.dp))
                val low = med.stockCount <= med.lowStockThreshold
                val dosesPerDay = item.schedules.count { it.active }
                val stockColor = if (low) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Inventory2,
                        contentDescription = null,
                        tint = stockColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    val daysText = if (dosesPerDay > 0) " · ~${med.stockCount / dosesPerDay} days left" else ""
                    Text(
                        "${med.stockCount} doses left$daysText",
                        color = stockColor,
                        fontSize = 13.sp,
                        fontWeight = if (low) FontWeight.Bold else FontWeight.Medium
                    )
                    if (low) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFDC2626).copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Refill soon", color = Color(0xFFDC2626), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onTakeNow(); tookNow = true }) {
                    Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (tookNow) "Logged!" else "Take now")
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }

    if (confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${med.name}?") },
            text = { Text("This removes the medicine and cancels all its reminders.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ReliabilityBanners(context: Context, @Suppress("UNUSED_PARAMETER") refreshKey: Int) {
    val needsNotif = !Permissions.hasNotifications(context)
    val needsExact = !Permissions.canScheduleExact(context)
    val needsBattery = !isIgnoringBatteryOptimizations(context)

    if (needsNotif) {
        Banner(
            "Turn on notifications",
            "PillSync needs notification access to show alarms.",
            "Open settings"
        ) {
            launchFirst(
                context,
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                appDetailsIntent(context)
            )
        }
    }
    if (needsExact) {
        Banner(
            "Allow exact alarms",
            "Required so reminders fire at the precise time.",
            "Allow"
        ) {
            val intents = mutableListOf<Intent>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                intents += Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.parse("package:${context.packageName}"))
            }
            intents += appDetailsIntent(context)
            launchFirst(context, *intents.toTypedArray())
        }
    }
    if (needsBattery) {
        Banner(
            "Ignore battery optimization",
            "Keeps PillSync reliable and running after restarts.",
            "Fix"
        ) {
            val intents = mutableListOf<Intent>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Direct "allow?" dialog (needs REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission)…
                intents += Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:${context.packageName}"))
                // …then the full battery-optimization list…
                intents += Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            }
            // …then this app's details page as a last resort.
            intents += appDetailsIntent(context)
            launchFirst(context, *intents.toTypedArray())
        }
    }
}

private fun appDetailsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.parse("package:${context.packageName}"))

/** Try each intent in order; launch the first one that actually starts, else show a toast. */
private fun launchFirst(context: Context, vararg intents: Intent) {
    for (intent in intents) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: Throwable) { /* not available on this device; try the next */ }
    }
    android.widget.Toast.makeText(
        context,
        "Please open Settings and allow this manually for PillSync.",
        android.widget.Toast.LENGTH_LONG
    ).show()
}

@Composable
private fun Banner(title: String, body: String, action: String, onAction: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = Color(0xFFB45309))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF7C2D12))
                Text(body, fontSize = 13.sp, color = Color(0xFF7C2D12))
            }
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

fun colorFromHex(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF1D63E8)
}
