package com.tally.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.tally.app.data.Meal
import com.tally.app.data.MealRelation
import com.tally.app.data.Medicine
import com.tally.app.data.ScheduleItem
import com.tally.app.data.ScheduleType

private val SWATCHES = listOf(
    "#1D63E8", "#2E9BF0", "#16A34A", "#7C3AED",
    "#DB2777", "#DC2626", "#EA580C", "#CA8A04"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMedicineScreen(
    vm: TallyViewModel,
    medicineId: Long,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val isNew = medicineId <= 0L

    var loaded by remember { mutableStateOf(isNew) }
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(SWATCHES.first()) }
    var active by remember { mutableStateOf(true) }
    var createdAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var stockEnabled by remember { mutableStateOf(false) }
    var stockCount by remember { mutableStateOf(0) }
    var lowThreshold by remember { mutableStateOf(5) }
    val schedules = remember { mutableStateListOf<ScheduleItem>() }

    var showGapDialog by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    LaunchedEffect(medicineId) {
        if (!isNew) {
            val med = vm.getMedicine(medicineId)
            if (med != null) {
                name = med.name
                dosage = med.dosage
                description = med.description
                color = med.colorHex
                active = med.active
                createdAt = med.createdAt
                stockEnabled = med.stockEnabled
                stockCount = med.stockCount
                lowThreshold = med.lowStockThreshold
                schedules.clear()
                schedules.addAll(vm.schedulesFor(medicineId))
            }
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New medicine" else "Edit medicine") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (name.isBlank()) {
                            nameError = true
                        } else {
                            val med = Medicine(
                                id = if (isNew) 0 else medicineId,
                                name = name.trim(),
                                description = description.trim(),
                                dosage = dosage.trim(),
                                colorHex = color,
                                active = active,
                                createdAt = createdAt,
                                stockEnabled = stockEnabled,
                                stockCount = stockCount,
                                lowStockThreshold = lowThreshold
                            )
                            vm.save(med, schedules.toList()) { onDone() }
                        }
                    }) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        if (!loaded) return@Scaffold

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; nameError = false },
                            label = { Text("Medicine name *") },
                            singleLine = true,
                            isError = nameError,
                            supportingText = if (nameError) {
                                { Text("Please enter a name") }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = dosage,
                            onValueChange = { dosage = it },
                            label = { Text("Dosage (e.g. 1 tablet, 5 ml)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description / notes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(14.dp))
                        Text("Colour", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SWATCHES.forEach { hex ->
                                val c = colorFromHex(hex)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(c, CircleShape)
                                        .border(
                                            width = if (color == hex) 3.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                        .clickable { color = hex }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Track remaining stock", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Counts down as doses are taken and warns you to refill.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = stockEnabled, onCheckedChange = { stockEnabled = it })
                        }
                        if (stockEnabled) {
                            Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Doses left", modifier = Modifier.weight(1f))
                                IconButton(onClick = { if (stockCount > 0) stockCount-- }) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                                }
                                Text(
                                    "$stockCount",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    modifier = Modifier.width(48.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                IconButton(onClick = { stockCount++ }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase")
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("Quick refill", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(10, 30, 60, 90).forEach { n ->
                                    OutlinedButton(onClick = { stockCount += n }) { Text("+$n") }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = lowThreshold.toString(),
                                onValueChange = { txt ->
                                    lowThreshold = txt.filter { it.isDigit() }.take(4).toIntOrNull() ?: 0
                                },
                                label = { Text("Warn when this many doses left") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Schedule",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    "When should this medicine be taken?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (schedules.isEmpty()) {
                item {
                    Text(
                        "No times added yet. Use the buttons below.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            itemsIndexed(schedules) { index, item ->
                ScheduleCard(
                    context = context,
                    item = item,
                    onChange = { schedules[index] = it },
                    onRemove = { schedules.removeAt(index) }
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            schedules.add(ScheduleItem(medicineId = 0, type = ScheduleType.FIXED, minuteOfDay = 8 * 60))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null); Spacer(Modifier.width(6.dp))
                        Text("Add a fixed time")
                    }
                    OutlinedButton(
                        onClick = {
                            schedules.add(
                                ScheduleItem(
                                    medicineId = 0,
                                    type = ScheduleType.MEAL,
                                    meal = Meal.BREAKFAST,
                                    relation = MealRelation.BEFORE,
                                    offsetMinutes = 30
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null); Spacer(Modifier.width(6.dp))
                        Text("Add pre/post-meal reminder")
                    }
                    OutlinedButton(
                        onClick = { showGapDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Schedule, contentDescription = null); Spacer(Modifier.width(6.dp))
                        Text("Add doses by gap (e.g. every 12h)")
                    }
                }
                Spacer(Modifier.height(60.dp))
            }
        }
    }

    if (showGapDialog) {
        GapDialog(
            context = context,
            onDismiss = { showGapDialog = false },
            onGenerate = { startMinute, gapHours, count ->
                for (i in 0 until count) {
                    val minute = ((startMinute + i * gapHours * 60) % 1440 + 1440) % 1440
                    schedules.add(ScheduleItem(medicineId = 0, type = ScheduleType.FIXED, minuteOfDay = minute))
                }
                showGapDialog = false
            }
        )
    }
}

@Composable
private fun ScheduleCard(
    context: android.content.Context,
    item: ScheduleItem,
    onChange: (ScheduleItem) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Type", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = item.type == ScheduleType.FIXED,
                    onClick = { onChange(item.copy(type = ScheduleType.FIXED)) },
                    label = { Text("Fixed time") }
                )
                FilterChip(
                    selected = item.type == ScheduleType.MEAL,
                    onClick = {
                        onChange(
                            item.copy(
                                type = ScheduleType.MEAL,
                                meal = item.meal ?: Meal.BREAKFAST,
                                relation = item.relation ?: MealRelation.BEFORE
                            )
                        )
                    },
                    label = { Text("Around a meal") }
                )
            }

            Spacer(Modifier.height(12.dp))

            if (item.type == ScheduleType.FIXED) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Time", modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        showTimePicker(context, item.minuteOfDay) { onChange(item.copy(minuteOfDay = it)) }
                    }) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(formatMinute(context, item.minuteOfDay))
                    }
                }
            } else {
                Text("Meal", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Meal.values().forEach { meal ->
                        FilterChip(
                            selected = item.meal == meal,
                            onClick = { onChange(item.copy(meal = meal)) },
                            label = { Text(meal.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Timing", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MealRelation.values().forEach { rel ->
                        FilterChip(
                            selected = item.relation == rel,
                            onClick = { onChange(item.copy(relation = rel)) },
                            label = { Text(rel.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp) }
                        )
                    }
                }
                if (item.relation != MealRelation.WITH) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = item.offsetMinutes.toString(),
                        onValueChange = { txt ->
                            val n = txt.filter { it.isDigit() }.take(3).toIntOrNull() ?: 0
                            onChange(item.copy(offsetMinutes = n))
                        },
                        label = { Text("Minutes before/after meal") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Days", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            DayPicker(mask = item.daysMask, onChange = { onChange(item.copy(daysMask = it)) })
        }
    }
}

@Composable
private fun DayPicker(mask: Int, onChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0..6) {
            val selected = (mask shr i) and 1 == 1
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        CircleShape
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onChange(mask xor (1 shl i)) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    WEEKDAY_LABELS[i],
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun GapDialog(
    context: android.content.Context,
    onDismiss: () -> Unit,
    onGenerate: (startMinute: Int, gapHours: Int, count: Int) -> Unit
) {
    var startMinute by remember { mutableStateOf(8 * 60) }
    var gapHours by remember { mutableStateOf("12") }
    var count by remember { mutableStateOf("2") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add doses by gap") },
        text = {
            Column {
                Text("Generates evenly-spaced daily reminders.", fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("First dose", modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        showTimePicker(context, startMinute) { startMinute = it }
                    }) { Text(formatMinute(context, startMinute)) }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = gapHours,
                    onValueChange = { gapHours = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Gap between doses (hours)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = count,
                    onValueChange = { count = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Number of doses per day") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val g = gapHours.toIntOrNull()?.coerceIn(1, 24) ?: 12
                val c = count.toIntOrNull()?.coerceIn(1, 12) ?: 2
                onGenerate(startMinute, g, c)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
