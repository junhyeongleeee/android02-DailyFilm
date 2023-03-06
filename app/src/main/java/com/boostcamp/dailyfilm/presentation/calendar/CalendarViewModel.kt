package com.boostcamp.dailyfilm.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boostcamp.dailyfilm.data.calendar.CalendarRepository
import com.boostcamp.dailyfilm.data.dataStore.UserPreferencesRepository
import com.boostcamp.dailyfilm.presentation.calendar.adpater.CalendarPagerAdapter
import com.boostcamp.dailyfilm.presentation.calendar.model.DateModel
import com.boostcamp.dailyfilm.presentation.calendar.model.DateState
import com.boostcamp.dailyfilm.presentation.playfilm.model.SpeedState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val calendarRepository: CalendarRepository
) :
    ViewModel() {

    private var item: DateModel? = null
    var calendarIndex: Int? = null
    var userSpeed: SpeedState = SpeedState.NORMAL
    private val localeCalendar = Calendar.getInstance(Locale.getDefault()).apply {
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    var calendar: Calendar = localeCalendar
        private set

    val syncSet = HashSet<Int>()

    private val _calendarEventFlow = MutableSharedFlow<CalendarEvent>()
    val calendarEventFlow: SharedFlow<CalendarEvent> = _calendarEventFlow.asSharedFlow()

    private val _calendarFlow =
        MutableStateFlow("${localeCalendar.get(Calendar.YEAR)}년 ${localeCalendar.get(Calendar.MONTH) + 1}월")
    val calendarFlow: StateFlow<String> = _calendarFlow.asStateFlow()

    private val _isTodayFlow = MutableStateFlow(DateState.TODAY)
    val isTodayFlow: StateFlow<DateState> = _isTodayFlow.asStateFlow()

    private val _userFlow = MutableStateFlow(FirebaseAuth.getInstance().currentUser)
    val userFlow: StateFlow<FirebaseUser?> = _userFlow.asStateFlow()

    private val _filmFlow = MutableStateFlow<List<DateModel>>(emptyList())
    val filmFlow: StateFlow<List<DateModel>> = _filmFlow.asStateFlow()

    init {
        getSpeed()
    }

    private fun getSpeed() {
        viewModelScope.launch {
            preferencesRepository.userFastFlow.collect { index ->
                userSpeed = if (index == null) {
                    SpeedState.NORMAL
                }else {
                    SpeedState.values()[index]
                }
            }
        }
    }

    fun emitFilm(filmList: List<DateModel>) {
        _filmFlow.value = filmList
    }

    fun getViewPagerPosition(position: Int) {
        viewModelScope.launch {
            calendar = Calendar.getInstance(Locale.getDefault()).apply {
                timeInMillis = localeCalendar.timeInMillis
                add(Calendar.MONTH, position - CalendarPagerAdapter.START_POSITION)
            }
            _calendarFlow.emit("${calendar.get(Calendar.YEAR)}년 ${calendar.get(Calendar.MONTH) + 1}월")

            val diff = calendar.timeInMillis - localeCalendar.timeInMillis
            _isTodayFlow.emit(
                when {
                    diff < 0L -> DateState.BEFORE
                    diff == 0L -> DateState.TODAY
                    else -> DateState.AFTER
                }
            )
        }
    }

    fun changeSelectedItem(index: Int?, item: DateModel?) {
        this.item = item
        this.calendarIndex = index
    }

    fun uploadClicked() {
        event(CalendarEvent.UploadSuccess(item))
    }

    private fun event(calendarEvent: CalendarEvent) {
        viewModelScope.launch {
            _calendarEventFlow.emit(calendarEvent)
        }
    }

    fun logout() {
        event(CalendarEvent.Logout)
    }

    fun deleteUser() {
        FirebaseAuth.getInstance().currentUser?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                viewModelScope.launch {
                    calendarRepository.deleteAllData().collectLatest { result ->
                        when (result) {
                            is com.boostcamp.dailyfilm.data.model.Result.Success -> {
                                event(CalendarEvent.DeleteUser)
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

sealed class CalendarEvent {
    data class UploadSuccess(val dateModel: DateModel?) : CalendarEvent()
    data class UpdateMonth(val month: String) : CalendarEvent()
    object Logout : CalendarEvent()
    object DeleteUser : CalendarEvent()
}
