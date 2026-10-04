package com.barathiraja.jk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.barathiraja.jk.AppContainer
import com.barathiraja.jk.data.BodyPart
import com.barathiraja.jk.data.DayMode
import com.barathiraja.jk.data.Exercise
import com.barathiraja.jk.data.PlanDay
import com.barathiraja.jk.data.PlanItem
import com.barathiraja.jk.data.SetSpec
import com.barathiraja.jk.data.TrainingPrefs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingViewModel(private val c: AppContainer) : ViewModel() {
    private val repo = c.training
    val engine get() = repo.engine

    val prefs: StateFlow<TrainingPrefs> = c.prefs.training
    val split = repo.split.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val today: Long get() = LocalDate.now().toEpochDay()
    val weekStart: Long get() = LocalDate.now().with(DayOfWeek.MONDAY).toEpochDay()

    /** Day shown on the Today's Workout screen (week strip selection). */
    val selectedDay = MutableStateFlow(today)

    val day: StateFlow<PlanDay?> = selectedDay.flatMapLatest { repo.day(it) }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val items: StateFlow<List<PlanItem>> = selectedDay.flatMapLatest { repo.items(it) }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val todayDay = repo.day(today).stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val todayItems = repo.items(today).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val weekDays = repo.week(weekStart, weekStart + 6).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val weekItems = repo.weekItems(weekStart, weekStart + 6).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val completedDays = repo.completedDays().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch { repo.ensureDay(today) }
    }

    fun select(day: Long) {
        selectedDay.value = day
        viewModelScope.launch { repo.ensureDay(day) }
    }

    fun refreshToday() = viewModelScope.launch {
        repo.ensureDay(today)
        repo.ensureDay(selectedDay.value)
    }

    fun setup(t: TrainingPrefs) = viewModelScope.launch {
        repo.setup(t)
        repo.ensureDay(today)
        repo.ensureDay(selectedDay.value)
    }

    fun setSplitDay(dow: DayOfWeek, parts: List<BodyPart>) = viewModelScope.launch {
        repo.setSplitDay(dow, parts)
        repo.ensureDay(today)
        repo.ensureDay(selectedDay.value)
    }

    fun regenerate(day: Long, mode: DayMode) = viewModelScope.launch { repo.regenerate(day, mode) }
    fun addExercise(day: Long, part: BodyPart, id: String) = viewModelScope.launch { repo.addExercise(day, part, id) }
    fun remove(id: Long) = viewModelScope.launch { repo.removeItem(id) }
    fun replace(id: Long, newId: String) = viewModelScope.launch { repo.replaceItem(id, newId) }
    fun updateSets(id: Long, sets: List<SetSpec>) = viewModelScope.launch { repo.updateSets(id, sets) }
    fun toggleSet(id: Long, index: Int, done: Boolean) = viewModelScope.launch { repo.toggleSet(id, index, done) }
    fun completeAll(day: Long) = viewModelScope.launch { repo.completeAll(day) }

    fun alternatives(part: BodyPart, currentId: String): List<Exercise> = engine.alternatives(part, currentId, prefs.value)
    fun available(part: BodyPart): List<Exercise> = engine.available(part, prefs.value.copy(level = com.barathiraja.jk.data.Level.ADVANCED))
    fun setLogs(exerciseId: String): Flow<List<com.barathiraja.jk.data.SetLog>> = repo.setLogs(exerciseId)

    var planDialogDay: Long
        get() = c.prefs.planDialogDay
        set(v) { c.prefs.planDialogDay = v }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { TrainingViewModel(c) } }
    }
}
