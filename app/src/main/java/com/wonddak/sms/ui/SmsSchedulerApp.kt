package com.wonddak.sms.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wonddak.sms.data.AppStore
import com.wonddak.sms.data.AppSettings
import com.wonddak.sms.scheduling.MessageAlarmScheduler
import com.wonddak.sms.scheduling.NotificationHelper
import com.wonddak.sms.scheduling.syncReminderSchedules
import kotlinx.coroutines.launch

@Composable
fun SmsSchedulerApp(
    store: AppStore,
    scheduler: MessageAlarmScheduler,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    openHistoryRequest: Int = 0,
    openHistoryMessageId: Long = -1L,
) {
    val appState = remember { AppState(store) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(if (openHistoryRequest > 0) 1 else 0) }
    var isDetailScreenVisible by remember { mutableStateOf(false) }
    var isSettingsVisible by remember { mutableStateOf(false) }
    val tabs = listOf("예약", "예약내역", "템플릿", "연락처")
    val tabIcons = listOf(
        Icons.Outlined.DateRange,
        Icons.AutoMirrored.Outlined.List,
        Icons.Outlined.Create,
        Icons.Outlined.Person,
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) appState.reload()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(openHistoryRequest) {
        if (openHistoryRequest > 0) {
            selectedTab = 1
            isDetailScreenVisible = false
            isSettingsVisible = false
        }
    }

    val updateSettings: (AppSettings) -> Unit = { updated ->
        onSettingsChange(updated)
        syncReminderSchedules(
            previous = settings,
            updated = updated,
            messages = appState.messages,
            schedule = { scheduler.scheduleReminder(it) },
            cancel = {
                scheduler.cancelReminder(it.id)
                NotificationHelper.cancelReminder(context, it.id)
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = !isDetailScreenVisible && !isSettingsVisible,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                    ) {
                        tabs.forEachIndexed { index, title ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = {
                                    selectedTab = index
                                    isDetailScreenVisible = false
                                },
                                icon = {
                                    Icon(tabIcons[index], contentDescription = title)
                                },
                                label = { Text(title, maxLines = 1) },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.surface,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding).consumeWindowInsets(padding)
        val notify: (String) -> Unit = { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
        if (isSettingsVisible) {
            SettingsScreen(
                settings = settings,
                onSettingsChange = updateSettings,
                onBack = { isSettingsVisible = false },
                notify = notify,
                modifier = contentModifier,
            )
        } else when (selectedTab) {
            0 -> ScheduleScreen(
                appState = appState,
                scheduler = scheduler,
                notify = notify,
                modifier = contentModifier,
                onOpenSettings = { isSettingsVisible = true },
            )
            1 -> HistoryScreen(
                appState = appState,
                scheduler = scheduler,
                notify = notify,
                modifier = contentModifier,
                targetMessageId = openHistoryMessageId,
                navigationRequest = openHistoryRequest,
            )
            2 -> TemplateScreen(
                appState = appState,
                modifier = contentModifier,
                onDetailVisibilityChange = { isDetailScreenVisible = it },
            )
            else -> ContactScreen(
                appState = appState,
                notify = notify,
                modifier = contentModifier,
                onDetailVisibilityChange = { isDetailScreenVisible = it },
            )
        }
    }
}
