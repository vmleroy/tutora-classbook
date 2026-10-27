package com.vmleroy.tutora.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import androidx.navigation.toRoute
import com.vmleroy.tutora.app.AppInfo
import com.vmleroy.tutora.app.R
import com.vmleroy.tutora.app.ui.catalog.ArchivedScreen
import com.vmleroy.tutora.app.ui.catalog.GroupEditScreen
import com.vmleroy.tutora.app.ui.catalog.PlaceDetailScreen
import com.vmleroy.tutora.app.ui.catalog.PlaceEditScreen
import com.vmleroy.tutora.app.ui.catalog.RegistryScreen
import com.vmleroy.tutora.app.ui.catalog.StudentDetailScreen
import com.vmleroy.tutora.app.ui.catalog.StudentEditScreen
import com.vmleroy.tutora.app.ui.schedule.AgendaScreen
import com.vmleroy.tutora.app.ui.schedule.LessonEditScreen
import com.vmleroy.tutora.app.ui.schedule.LessonNewScreen
import com.vmleroy.tutora.app.ui.schedule.TodayScreen
import com.vmleroy.tutora.designsystem.TutoraTheme
import com.vmleroy.tutora.designsystem.components.IconName
import com.vmleroy.tutora.designsystem.components.TabBar
import com.vmleroy.tutora.designsystem.components.TabItem
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

// Each tab is its own nested graph, so it keeps its own back stack (AC-001-2).
@Serializable data object TodayGraph

@Serializable data object TodayHome

@Serializable data object ScheduleGraph

@Serializable data object ScheduleHome

@Serializable data object RegistryGraph

@Serializable data class RegistryHome(
    val view: String = "students",
)

@Serializable data object EarningsGraph

@Serializable data object EarningsHome

@Serializable data object SettingsGraph

@Serializable data object SettingsHome

// Details and forms sit above the tabs, so student and place links never cross tab stacks.
@Serializable data class PlaceDetail(
    val id: String,
)

@Serializable data class PlaceEdit(
    val id: String? = null,
)

@Serializable data class StudentDetail(
    val id: String,
)

@Serializable data class StudentEdit(
    val id: String? = null,
)

@Serializable data class GroupEdit(
    val id: String? = null,
    val placeId: String? = null,
)

@Serializable data object Archived

@Serializable data class LessonNew(
    val date: String? = null,
)

@Serializable data class LessonEdit(
    val id: String,
)

/** The five tabs, always in this order (AC-001-1, AC-005-27). */
enum class Tab(
    val key: String,
    val graph: Any,
    val graphClass: KClass<*>,
    val label: Int,
    val icon: IconName,
) {
    TODAY("today", TodayGraph, TodayGraph::class, R.string.tab_today, IconName.HOME),
    SCHEDULE("schedule", ScheduleGraph, ScheduleGraph::class, R.string.tab_schedule, IconName.CALENDAR),
    REGISTRY("registry", RegistryGraph, RegistryGraph::class, R.string.tab_registry, IconName.USERS),
    EARNINGS("earnings", EarningsGraph, EarningsGraph::class, R.string.tab_earnings, IconName.WALLET),
    SETTINGS("settings", SettingsGraph, SettingsGraph::class, R.string.tab_settings, IconName.GEAR),
}

@Composable
fun TutoraNav(
    appInfo: AppInfo,
    schemaVersion: Int,
    modifier: Modifier = Modifier,
    nav: NavHostController = rememberNavController(),
) {
    val entry by nav.currentBackStackEntryAsState()
    val activeTab = Tab.entries.firstOrNull { tab -> entry?.destination?.hierarchy?.any { it.hasRoute(tab.graphClass) } == true }
    Scaffold(
        modifier = modifier,
        containerColor = TutoraTheme.colors.background,
        bottomBar = {
            // The bar shows on the tabs; details and forms take the whole screen.
            if (activeTab != null) {
                TabBar(
                    tabs = Tab.entries.map { TabItem(it.key, stringResource(it.label), it.icon) },
                    active = activeTab.key,
                    onSelect = { key ->
                        val tab = Tab.entries.first { it.key == key }
                        nav.navigate(tab.graph) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = TodayGraph, modifier = Modifier.padding(padding)) {
            navigation<TodayGraph>(startDestination = TodayHome) {
                composable<TodayHome> { TodayScreen(nav) }
            }
            navigation<ScheduleGraph>(startDestination = ScheduleHome) {
                composable<ScheduleHome> { AgendaScreen(nav) }
            }
            navigation<RegistryGraph>(startDestination = RegistryHome()) {
                composable<RegistryHome> { RegistryScreen(nav, it.toRoute<RegistryHome>().view) }
            }
            navigation<EarningsGraph>(startDestination = EarningsHome) {
                composable<EarningsHome> { EarningsScreen() }
            }
            navigation<SettingsGraph>(startDestination = SettingsHome) {
                composable<SettingsHome> { SettingsScreen(appInfo, schemaVersion) }
            }
            composable<PlaceDetail> { PlaceDetailScreen(nav, it.toRoute<PlaceDetail>().id) }
            composable<PlaceEdit> { PlaceEditScreen(nav, it.toRoute<PlaceEdit>().id) }
            composable<StudentDetail> { StudentDetailScreen(nav, it.toRoute<StudentDetail>().id) }
            composable<StudentEdit> { StudentEditScreen(nav, it.toRoute<StudentEdit>().id) }
            composable<GroupEdit> {
                val r = it.toRoute<GroupEdit>()
                GroupEditScreen(nav, r.id, r.placeId)
            }
            composable<Archived> { ArchivedScreen(nav) }
            composable<LessonNew> { LessonNewScreen(nav, it.toRoute<LessonNew>().date) }
            composable<LessonEdit> { LessonEditScreen(nav, it.toRoute<LessonEdit>().id) }
        }
    }
}
