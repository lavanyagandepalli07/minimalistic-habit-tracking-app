package com.example.habittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habittracker.ui.theme.HabitTrackerTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = HabitRepository(this)
        enableEdgeToEdge()
        setContent {
            val themeMode by repository.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            HabitTrackerTheme(darkTheme = darkTheme) {
                val habits by repository.habitsFlow.collectAsState(initial = emptyList())
                val scope = rememberCoroutineScope()
                var showMenu by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    repository.checkAndResetMonthlyData()
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Habit Tracker") },
                            actions = {
                                IconButton(onClick = { showMenu = true }) {
                                    val icon = when (themeMode) {
                                        ThemeMode.LIGHT -> Icons.Default.LightMode
                                        ThemeMode.DARK -> Icons.Default.DarkMode
                                        ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                                    }
                                    Icon(icon, contentDescription = "Switch Theme")
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("System Theme") },
                                        leadingIcon = { Icon(Icons.Default.SettingsBrightness, null) },
                                        onClick = {
                                            scope.launch { repository.updateThemeMode(ThemeMode.SYSTEM) }
                                            showMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Light Mode") },
                                        leadingIcon = { Icon(Icons.Default.LightMode, null) },
                                        onClick = {
                                            scope.launch { repository.updateThemeMode(ThemeMode.LIGHT) }
                                            showMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Dark Mode") },
                                        leadingIcon = { Icon(Icons.Default.DarkMode, null) },
                                        onClick = {
                                            scope.launch { repository.updateThemeMode(ThemeMode.DARK) }
                                            showMenu = false
                                        }
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        if (habits.isNotEmpty()) {
                            HabitGrid(
                                habits = habits,
                                onNameChange = { index, name ->
                                    scope.launch { repository.updateHabitName(index, name) }
                                },
                                onToggleCompletion = { habitIndex, day ->
                                    scope.launch { repository.toggleCompletion(habitIndex, day) }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            
                            HorizontalDivider()
                            
                            CompletionCalendar(
                                habits = habits,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HabitGrid(
    habits: List<Habit>,
    onNameChange: (Int, String) -> Unit,
    onToggleCompletion: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        habits.forEachIndexed { habitIndex, habit ->
            HabitColumn(
                habit = habit,
                habitIndex = habitIndex,
                today = today,
                onNameChange = onNameChange,
                onToggleCompletion = onToggleCompletion,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun HabitColumn(
    habit: Habit,
    habitIndex: Int,
    today: Int,
    onNameChange: (Int, String) -> Unit,
    onToggleCompletion: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var localName by remember(habitIndex) { mutableStateOf(habit.name) }
    
    LaunchedEffect(habit.name) {
        if (localName != habit.name) {
            localName = habit.name
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = localName,
            onValueChange = { 
                localName = it
                onNameChange(habitIndex, it)
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, textAlign = TextAlign.Center),
            singleLine = true,
            placeholder = { Text("Habit", fontSize = 10.sp) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Today",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        val isChecked = habit.monthlyCompletions[today - 1]
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .padding(4.dp)
                .background(
                    if (isChecked) MaterialTheme.colorScheme.primary 
                    else Color.Transparent,
                    RoundedCornerShape(8.dp)
                )
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(8.dp)
                )
                .clickable { onToggleCompletion(habitIndex, today) },
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun CompletionCalendar(
    habits: List<Habit>,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
    val currentYear = calendar.get(Calendar.YEAR)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
    val offset = (firstDayOfWeek + 5) % 7 // Adjust so Mon=0

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "$currentMonth $currentYear",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Days of week header
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Calendar Grid
        val totalCells = ((daysInMonth + offset + 6) / 7) * 7
        for (row in 0 until totalCells / 7) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                for (col in 0 until 7) {
                    val dayNum = row * 7 + col - offset + 1
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayNum in 1..daysInMonth) {
                            val allDone = habits.all { it.monthlyCompletions[dayNum - 1] }
                            val anyDone = habits.any { it.monthlyCompletions[dayNum - 1] }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(0.8f)
                                    .background(
                                        color = when {
                                            allDone -> MaterialTheme.colorScheme.primary
                                            anyDone -> MaterialTheme.colorScheme.primaryContainer
                                            else -> Color.Transparent
                                        },
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    fontSize = 10.sp,
                                    color = if (allDone) MaterialTheme.colorScheme.onPrimary 
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            Text(" All 3 habits done", fontSize = 10.sp, modifier = Modifier.padding(end = 16.dp))
            Box(Modifier.size(12.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape))
            Text(" Some habits done", fontSize = 10.sp)
        }
    }
}
