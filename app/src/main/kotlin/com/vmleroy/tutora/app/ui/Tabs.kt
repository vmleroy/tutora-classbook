package com.vmleroy.tutora.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.vmleroy.tutora.app.AppInfo
import com.vmleroy.tutora.app.R
import com.vmleroy.tutora.designsystem.TutoraTheme
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import com.vmleroy.tutora.designsystem.R as DsR

// Each tab is its own nested graph, so it keeps its own back stack (AC-001-2).
@Serializable data object TodayGraph

@Serializable data object TodayHome

@Serializable data object ScheduleGraph

@Serializable data object ScheduleHome

@Serializable data object RegistryGraph

@Serializable data object RegistryHome

@Serializable data object EarningsGraph

@Serializable data object EarningsHome

@Serializable data object SettingsGraph

@Serializable data object SettingsHome

/** The five tabs, always in this order (AC-001-1). */
enum class Tab(
    val key: String,
    val graph: Any,
    val graphClass: KClass<*>,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
) {
    TODAY("today", TodayGraph, TodayGraph::class, R.string.tab_today, DsR.drawable.ic_home),
    SCHEDULE("schedule", ScheduleGraph, ScheduleGraph::class, R.string.tab_schedule, DsR.drawable.ic_calendar),
    REGISTRY("registry", RegistryGraph, RegistryGraph::class, R.string.tab_registry, DsR.drawable.ic_users),
    EARNINGS("earnings", EarningsGraph, EarningsGraph::class, R.string.tab_earnings, DsR.drawable.ic_wallet),
    SETTINGS("settings", SettingsGraph, SettingsGraph::class, R.string.tab_settings, DsR.drawable.ic_gear),
}

@Composable
fun TutoraTabs(
    appInfo: AppInfo,
    schemaVersion: Int,
    modifier: Modifier = Modifier,
    nav: NavHostController = rememberNavController(),
) {
    Scaffold(
        modifier = modifier,
        containerColor = TutoraTheme.colors.background,
        bottomBar = { TabBar(nav) },
    ) { padding ->
        NavHost(nav, startDestination = TodayGraph, modifier = Modifier.padding(padding)) {
            navigation<TodayGraph>(startDestination = TodayHome) {
                composable<TodayHome> {
                    EmptyTab(R.string.tab_today, R.string.empty_today_title, R.string.empty_today_body, "today-screen")
                }
            }
            navigation<ScheduleGraph>(startDestination = ScheduleHome) {
                composable<ScheduleHome> {
                    EmptyTab(R.string.tab_schedule, R.string.empty_schedule_title, R.string.empty_schedule_body, "schedule-screen")
                }
            }
            navigation<RegistryGraph>(startDestination = RegistryHome) {
                composable<RegistryHome> {
                    EmptyTab(R.string.tab_registry, R.string.empty_registry_title, R.string.empty_registry_body, "registry-screen")
                }
            }
            navigation<EarningsGraph>(startDestination = EarningsHome) {
                composable<EarningsHome> {
                    EmptyTab(R.string.tab_earnings, R.string.empty_earnings_title, R.string.empty_earnings_body, "earnings-screen")
                }
            }
            navigation<SettingsGraph>(startDestination = SettingsHome) {
                composable<SettingsHome> { SettingsScreen(appInfo, schemaVersion) }
            }
        }
    }
}

@Composable
private fun TabBar(nav: NavHostController) {
    val entry by nav.currentBackStackEntryAsState()
    NavigationBar(containerColor = TutoraTheme.colors.surface) {
        Tab.entries.forEach { tab ->
            val selected = entry?.destination?.hierarchy?.any { it.hasRoute(tab.graphClass) } == true
            NavigationBarItem(
                modifier = Modifier.testTag("tab-${tab.key}"),
                selected = selected,
                onClick = {
                    nav.navigate(tab.graph) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                label = { Text(stringResource(tab.label), style = TutoraTheme.type.tab) },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = TutoraTheme.colors.primary,
                        selectedTextColor = TutoraTheme.colors.primary,
                        indicatorColor = TutoraTheme.colors.primarySoft,
                        unselectedIconColor = TutoraTheme.colors.inkMuted,
                        unselectedTextColor = TutoraTheme.colors.inkMuted,
                    ),
            )
        }
    }
}
