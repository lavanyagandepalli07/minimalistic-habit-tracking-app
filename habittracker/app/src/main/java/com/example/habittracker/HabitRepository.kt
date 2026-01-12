package com.example.habittracker

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "habits")

data class Habit(
    val name: String,
    val monthlyCompletions: List<Boolean> // Up to 31 days
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class HabitRepository(private val context: Context) {
    companion object {
        private val HABIT_1_NAME = stringPreferencesKey("habit_1_name")
        private val HABIT_2_NAME = stringPreferencesKey("habit_2_name")
        private val HABIT_3_NAME = stringPreferencesKey("habit_3_name")
        private val HABIT_1_STREAK = stringPreferencesKey("habit_1_streak")
        private val HABIT_2_STREAK = stringPreferencesKey("habit_2_streak")
        private val HABIT_3_STREAK = stringPreferencesKey("habit_3_streak")
        private val THEME_MODE = intPreferencesKey("theme_mode")
        private val LAST_RESET_MONTH = intPreferencesKey("last_reset_month")
        private val LAST_RESET_YEAR = intPreferencesKey("last_reset_year")
    }

    val habitsFlow: Flow<List<Habit>> = context.dataStore.data.map { preferences ->
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        
        val lastMonth = preferences[LAST_RESET_MONTH] ?: -1
        val lastYear = preferences[LAST_RESET_YEAR] ?: -1

        if (currentMonth != lastMonth || currentYear != lastYear) {
            // This is a flow, we shouldn't trigger side effects like edit here usually, 
            // but for simplicity in this small app we will handle the reset check.
            // In a real app, this would be done in a worker or on app start.
        }

        listOf(
            Habit(
                name = preferences[HABIT_1_NAME] ?: "Habit 1",
                monthlyCompletions = decodeCompletions(preferences[HABIT_1_STREAK] ?: "0".repeat(31))
            ),
            Habit(
                name = preferences[HABIT_2_NAME] ?: "Habit 2",
                monthlyCompletions = decodeCompletions(preferences[HABIT_2_STREAK] ?: "0".repeat(31))
            ),
            Habit(
                name = preferences[HABIT_3_NAME] ?: "Habit 3",
                monthlyCompletions = decodeCompletions(preferences[HABIT_3_STREAK] ?: "0".repeat(31))
            )
        )
    }

    suspend fun checkAndResetMonthlyData() {
        context.dataStore.edit { preferences ->
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val lastMonth = preferences[LAST_RESET_MONTH] ?: -1
            val lastYear = preferences[LAST_RESET_YEAR] ?: -1

            if (currentMonth != lastMonth || currentYear != lastYear) {
                preferences[HABIT_1_STREAK] = "0".repeat(31)
                preferences[HABIT_2_STREAK] = "0".repeat(31)
                preferences[HABIT_3_STREAK] = "0".repeat(31)
                preferences[LAST_RESET_MONTH] = currentMonth
                preferences[LAST_RESET_YEAR] = currentYear
            }
        }
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        ThemeMode.values()[preferences[THEME_MODE] ?: ThemeMode.SYSTEM.ordinal]
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.ordinal
        }
    }

    suspend fun updateHabitName(index: Int, name: String) {
        context.dataStore.edit { preferences ->
            val key = when (index) {
                0 -> HABIT_1_NAME
                1 -> HABIT_2_NAME
                else -> HABIT_3_NAME
            }
            preferences[key] = name
        }
    }

    suspend fun toggleCompletion(habitIndex: Int, dayOfMonth: Int) {
        context.dataStore.edit { preferences ->
            val key = when (habitIndex) {
                0 -> HABIT_1_STREAK
                1 -> HABIT_2_STREAK
                else -> HABIT_3_STREAK
            }
            val current = preferences[key] ?: "0".repeat(31)
            val updated = current.toCharArray().apply {
                if (dayOfMonth - 1 in indices) {
                    this[dayOfMonth - 1] = if (this[dayOfMonth - 1] == '1') '0' else '1'
                }
            }.joinToString("")
            preferences[key] = updated
        }
    }

    private fun decodeCompletions(str: String): List<Boolean> {
        return str.map { it == '1' }
    }
}
