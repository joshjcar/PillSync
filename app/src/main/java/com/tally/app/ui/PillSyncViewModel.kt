package com.tally.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.data.AutoBackup
import com.tally.app.data.BackupManager
import com.tally.app.data.DoseLogWithName
import com.tally.app.data.DoseStatus
import com.tally.app.data.Medicine
import com.tally.app.data.MedicineWithSchedules
import com.tally.app.data.ScheduleItem
import com.tally.app.data.SettingsStore
import com.tally.app.data.PillSyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PillSyncViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PillSyncRepository(app)
    val settings = SettingsStore(app)

    /** A safety backup found in Downloads on first run (for the one-tap restore prompt). */
    val autoFound = kotlinx.coroutines.flow.MutableStateFlow<AutoBackup.Found?>(null)

    init {
        // Look for a safety copy in Downloads; the UI only offers to restore it when empty.
        viewModelScope.launch(Dispatchers.IO) {
            autoFound.value = runCatching { AutoBackup.find(getApplication()) }.getOrNull()
        }
    }

    /** Restore data from any readable backup Uri (SAF-picked or the auto safety copy). */
    fun restoreFromUri(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val json = AutoBackup.readText(getApplication(), uri)
                    BackupManager.importJson(getApplication(), json)
                }.isSuccess
            }
            if (ok) autoFound.value = null
            onResult(ok)
        }
    }

    val medicines: StateFlow<List<MedicineWithSchedules>> =
        combine(repo.observeMedicines(), repo.observeSchedules()) { meds, schedules ->
            meds.map { m ->
                MedicineWithSchedules(m, schedules.filter { it.medicineId == m.id }.sortedBy { it.minuteOfDay })
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(medicine: Medicine, schedules: List<ScheduleItem>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.saveMedicine(medicine, schedules) }
            onDone()
        }
    }

    fun delete(medicine: Medicine) {
        viewModelScope.launch(Dispatchers.IO) { repo.deleteMedicine(medicine) }
    }

    fun toggleActive(medicine: Medicine) {
        viewModelScope.launch(Dispatchers.IO) { repo.setMedicineActive(medicine, !medicine.active) }
    }

    /** Dose history for roughly the last two weeks. */
    val recentLogs: StateFlow<List<DoseLogWithName>> =
        repo.observeRecentLogsWithName(System.currentTimeMillis() - 14L * 86_400_000L)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setDoseStatus(logId: Long, status: DoseStatus) {
        viewModelScope.launch(Dispatchers.IO) { repo.setDoseStatus(logId, status) }
    }

    fun takeNow(medicine: Medicine) {
        viewModelScope.launch(Dispatchers.IO) { repo.logAdhocTaken(medicine) }
    }

    suspend fun schedulesFor(medicineId: Long): List<ScheduleItem> = repo.getSchedulesForMedicine(medicineId)

    suspend fun getMedicine(id: Long): Medicine? = repo.getMedicine(id)
}
