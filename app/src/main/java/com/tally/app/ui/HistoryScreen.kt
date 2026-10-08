package com.tally.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tally.app.data.DoseLogWithName
import com.tally.app.data.DoseStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    logs: List<DoseLogWithName>,
    onBack: () -> Unit,
    onSetStatus: (Long, DoseStatus) -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("No doses recorded yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@Scaffold
        }

        // Group by calendar day (headers).
        val grouped = logs.groupBy { dayKey(it.log.scheduledAt) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            grouped.forEach { (_, dayLogs) ->
                item {
                    Text(
                        dayLabel(dayLogs.first().log.scheduledAt),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                items(dayLogs.size) { idx ->
                    LogRow(
                        entry = dayLogs[idx],
                        timeText = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(dayLogs[idx].log.scheduledAt)),
                        onTaken = { onSetStatus(dayLogs[idx].log.id, DoseStatus.TAKEN) },
                        onSkip = { onSetStatus(dayLogs[idx].log.id, DoseStatus.SKIPPED) }
                    )
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun LogRow(
    entry: DoseLogWithName,
    timeText: String,
    onTaken: () -> Unit,
    onSkip: () -> Unit
) {
    val isPast = entry.log.scheduledAt < System.currentTimeMillis()
    val (label, color) = when (entry.log.status) {
        DoseStatus.TAKEN -> "Taken" to Color(0xFF16A34A)
        DoseStatus.SKIPPED -> "Skipped" to Color(0xFF6B7280)
        DoseStatus.PENDING -> if (isPast) "Missed" to Color(0xFFDC2626) else "Scheduled" to Color(0xFFB45309)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    entry.medicineName ?: "(deleted medicine)",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(timeText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            // Allow correcting pending/missed rows.
            if (entry.log.status == DoseStatus.PENDING) {
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = onTaken, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Filled.Check, contentDescription = "Mark taken", tint = Color(0xFF16A34A))
                }
                IconButton(onClick = onSkip, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Skip", tint = Color(0xFF6B7280))
                }
            }
        }
    }
}

private fun dayKey(millis: Long): Long {
    val c = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return c.timeInMillis
}

private fun dayLabel(millis: Long): String {
    val today = dayKey(System.currentTimeMillis())
    val day = dayKey(millis)
    return when (today - day) {
        0L -> "Today"
        86_400_000L -> "Yesterday"
        else -> SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date(millis))
    }
}
