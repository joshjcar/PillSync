package com.tally.app.alarm

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tally.app.data.Meal
import com.tally.app.data.MealRelation
import com.tally.app.data.ScheduleType
import com.tally.app.data.TallyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : ComponentActivity() {

    private var scheduleId = -1L
    private var scheduledAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        scheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1L)
        scheduledAt = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, System.currentTimeMillis())

        setContent {
            AlarmScreen(
                scheduleId = scheduleId,
                scheduledAt = scheduledAt,
                onTaken = {
                    AlarmService.stop(this, AlarmService.ACTION_STOP, scheduleId, scheduledAt)
                    finish()
                },
                onSnooze = {
                    AlarmService.stop(this, AlarmService.ACTION_SNOOZE, scheduleId, scheduledAt)
                    finish()
                },
                onSkip = {
                    AlarmService.stop(this, AlarmService.ACTION_SKIP, scheduleId, scheduledAt)
                    finish()
                }
            )
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        scheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, scheduleId)
        scheduledAt = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, scheduledAt)
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
                .requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

private data class AlarmInfo(
    val name: String,
    val dosage: String,
    val description: String,
    val instruction: String
)

@Composable
private fun AlarmScreen(
    scheduleId: Long,
    scheduledAt: Long,
    onTaken: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var info by remember { mutableStateOf(AlarmInfo("Medicine", "", "", "")) }

    LaunchedEffect(scheduleId) {
        val loaded = withContext(Dispatchers.IO) {
            val dao = TallyDatabase.get(context).dao()
            val sch = dao.getSchedule(scheduleId)
            val med = sch?.let { dao.getMedicine(it.medicineId) }
            if (med != null && sch != null) {
                AlarmInfo(
                    name = med.name,
                    dosage = med.dosage,
                    description = med.description,
                    instruction = scheduleInstruction(sch.type, sch.meal, sch.relation, sch.offsetMinutes)
                )
            } else null
        }
        if (loaded != null) info = loaded
    }

    val timeText = remember(scheduledAt) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(scheduledAt))
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF2563EB), Color(0xFF0A1E63)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .background(Color.White.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "MEDICATION REMINDER",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                info.name,
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (info.dosage.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(info.dosage, color = Color.White, fontSize = 20.sp)
            }
            if (info.instruction.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(info.instruction, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
            if (info.description.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    info.description,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Scheduled for $timeText", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = onTaken,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1D4ED8))
            ) {
                Icon(Icons.Filled.Medication, contentDescription = null)
                Spacer(Modifier.size(10.dp))
                Text("I've taken it", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Snooze", fontSize = 18.sp)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
            ) {
                Text("Skip this dose", fontSize = 15.sp)
            }
        }
    }
}

private fun scheduleInstruction(
    type: ScheduleType,
    meal: Meal?,
    relation: MealRelation?,
    offset: Int
): String {
    if (type != ScheduleType.MEAL || meal == null) return ""
    val mealName = meal.name.lowercase().replaceFirstChar { it.uppercase() }
    return when (relation) {
        MealRelation.BEFORE -> "Take $offset min before $mealName"
        MealRelation.AFTER -> "Take $offset min after $mealName"
        MealRelation.WITH, null -> "Take with $mealName"
    }
}
