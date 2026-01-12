package com.example.habittracker

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.GlanceTheme
import androidx.glance.layout.Alignment
import java.util.Calendar

class HabitWidget : GlanceAppWidget() {
    companion object {
        val habitIndexKey = ActionParameters.Key<Int>("habitIndex")
        val dayKey = ActionParameters.Key<Int>("day")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = HabitRepository(context)
        
        provideContent {
            val habits by repository.habitsFlow.collectAsState(initial = emptyList())
            val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .padding(8.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Habit Tracker",
                        style = TextStyle(color = GlanceTheme.colors.onSurface)
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Row(
                        modifier = GlanceModifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        habits.forEachIndexed { habitIndex, habit ->
                            Column(
                                modifier = GlanceModifier.defaultWeight().padding(horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = habit.name,
                                    style = TextStyle(color = GlanceTheme.colors.onSurface),
                                    maxLines = 1
                                )
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                
                                val isChecked = if (today <= habit.monthlyCompletions.size) {
                                    habit.monthlyCompletions[today - 1]
                                } else false

                                Box(
                                    modifier = GlanceModifier
                                        .size(32.dp)
                                        .padding(4.dp)
                                        .background(
                                            if (isChecked) GlanceTheme.colors.primary 
                                            else GlanceTheme.colors.outline
                                        )
                                        .clickable(
                                            actionRunCallback<ToggleCompletionCallback>(
                                                parameters = actionParametersOf(
                                                    habitIndexKey to habitIndex,
                                                    dayKey to today
                                                )
                                            )
                                        )
                                ) {}
                                Text(
                                    text = "Day $today",
                                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class ToggleCompletionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val habitIndex = parameters[HabitWidget.habitIndexKey] ?: return
        val day = parameters[HabitWidget.dayKey] ?: return
        
        val repository = HabitRepository(context)
        repository.toggleCompletion(habitIndex, day)
        
        HabitWidget().update(context, glanceId)
    }
}

class HabitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitWidget()
}
