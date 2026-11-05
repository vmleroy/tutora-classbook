package app.tutora.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import app.tutora.AppInfo
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.NavRail
import app.tutora.designsystem.components.TabBar
import app.tutora.designsystem.components.TabItem
import app.tutora.state.LocalServices
import app.tutora.ui.catalog.ArchivedScreen
import app.tutora.ui.catalog.GroupDetailScreen
import app.tutora.ui.catalog.GroupEditScreen
import app.tutora.ui.catalog.PlaceDetailScreen
import app.tutora.ui.catalog.PlaceEditScreen
import app.tutora.ui.catalog.RegistryScreen
import app.tutora.ui.catalog.StudentDetailScreen
import app.tutora.ui.catalog.StudentEditScreen
import app.tutora.ui.earnings.EarningsScreen
import app.tutora.ui.live.LiveScreen
import app.tutora.ui.payments.PayerLessonsScreen
import app.tutora.ui.payments.PaymentDetailScreen
import app.tutora.ui.reports.ReportScreen
import app.tutora.ui.schedule.AgendaScreen
import app.tutora.ui.schedule.LessonDetailScreen
import app.tutora.ui.schedule.LessonEditScreen
import app.tutora.ui.schedule.LessonNewScreen
import app.tutora.ui.schedule.TodayScreen
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
    /** Open Alunos with Devendo on, from Hoje's alert (AC-008-15). */
    val owing: Boolean = false,
)

@Serializable data object EarningsGraph

@Serializable data object EarningsHome

/** The saved-state key that asks Ganhos to open on a period kind, such as DAY from Hoje's card (AC-016-28). */
const val OPEN_PERIOD = "openPeriod"

/**
 * Switches to a tab as the tab bar does: each tab keeps its own stack, so going back to Hoje
 * always finds its home page.
 */
fun NavHostController.openTab(graph: Any) {
    navigate(graph) {
        popUpTo(this@openTab.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Serializable data object SettingsGraph

@Serializable data object SettingsHome

/** Notificações, behind the bell (AC-014-39). */
@Serializable data object NoticesHome

/** Ganhos › Evolução (AC-014-31). */
@Serializable data object Evolution

/** Ajustes' pages (AC-014-28). */
@Serializable data object SettingsGeneral

@Serializable data object SettingsLessons

@Serializable data object SettingsPayments

@Serializable data object SettingsAbout

/** Ajustes › Seus dados (spec 010). */
@Serializable data object PersonalData

/** Ajustes › Geral › Aparência, and Suas cores on one part (spec 013). */
@Serializable data object AppearanceHome

@Serializable data class CustomColors(
    val part: String,
)

/** Ajustes › Geral › Calendário (spec 012). */
@Serializable data object CalendarHome

/** Aulas para confirmar (spec 014), from Hoje's alert and the daily notification. */
@Serializable data object ConfirmClasses

/** Ajustes › Lembretes (spec 014). */
@Serializable data object RemindersHome

/** Ajustes › Geral › Como você dá aula (AC-014-20). */
@Serializable data object TeachingHome

/** Configuração inicial on the very first run (AC-014-17). */
@Serializable data object SetupHome

/** Ajustes › Backup e planilha (spec 011); Hoje's backup alert opens it too. */
@Serializable data object BackupHome

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

@Serializable data class LiveLesson(
    val id: String,
    /** Open on the end sheet (Encerrar from the notification). */
    val end: Boolean = false,
)

/** A tap on the live notification: open the live screen, or its end sheet. */
data class LiveRequest(
    val lessonId: String,
    val end: Boolean,
)

@Serializable data class GroupDetail(
    val id: String,
)

@Serializable data object Archived

@Serializable data class LessonNew(
    val date: String? = null,
)

@Serializable data class LessonDetail(
    val id: String,
)

@Serializable data class PaymentDetail(
    val id: String,
)

@Serializable data class PayerLessons(
    val kind: String,
    val id: String,
)

/** Relatório for a student, group or place (spec 010); [kind] is a SubjectKind name. */
@Serializable data class Report(
    val kind: String,
    val id: String,
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
    liveRequest: LiveRequest? = null,
    onLiveRequestHandled: () -> Unit = {},
    /** A route a reminder's notification asked for: a class, the confirm list or who owes (spec 014). */
    openRequest: Any? = null,
    onOpenRequestHandled: () -> Unit = {},
) {
    LaunchedEffect(openRequest) {
        val r = openRequest ?: return@LaunchedEffect
        when (r) {
            is RegistryHome -> {
                nav.openTab(RegistryGraph)
                nav.navigate(r) { launchSingleTop = true }
            }
            TodayGraph -> nav.openTab(TodayGraph)
            else -> nav.navigate(r) { launchSingleTop = true }
        }
        onOpenRequestHandled()
    }
    // The notification opens the live screen, or Encerrar's sheet (AC-007-15).
    LaunchedEffect(liveRequest) {
        val r = liveRequest ?: return@LaunchedEffect
        nav.navigate(LiveLesson(r.lessonId, r.end)) { launchSingleTop = true }
        onLiveRequestHandled()
    }
    val entry by nav.currentBackStackEntryAsState()
    val activeTab = Tab.entries.firstOrNull { tab -> entry?.destination?.hierarchy?.any { it.hasRoute(tab.graphClass) } == true }
    // Durações em HH:MM reaches every screen from here.
    val clockDurations by LocalServices.current.settings.collectAsState(initial = null)
    // The Agenda tab counts the classes to confirm (AC-014-4).
    val services = LocalServices.current
    val toConfirm by androidx.compose.runtime
        .remember { services.toConfirm() }
        .collectAsState(initial = emptyList())
    // The very first run, with no data, opens on the setup (AC-014-17).
    LaunchedEffect(Unit) {
        // Not again when the screen is made anew (a language change): the setup already open keeps its steps.
        val open = nav.currentBackStackEntry?.destination?.hasRoute(SetupHome::class) == true
        if (!open && services.setup.needed()) nav.navigate(SetupHome) { launchSingleTop = true }
    }
    val landscape = TutoraTheme.landscape
    val tabItems =
        Tab.entries.map {
            TabItem(it.key, stringResource(it.label), it.icon, count = if (it == Tab.SCHEDULE) toConfirm.size else 0)
        }
    val onTab: (String) -> Unit = { key -> nav.openTab(Tab.entries.first { it.key == key }.graph) }
    CompositionLocalProvider(LocalClockDurations provides (clockDurations?.clockDurations == true)) {
        Scaffold(
            modifier = modifier,
            containerColor = TutoraTheme.colors.background,
            // Screens pad for the system bars themselves, so Hoje's band reaches the top edge; the
            // tab bar's height (with the navigation bar) is consumed here so it is not padded twice.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                // The bar shows on the tabs; details and forms take the whole screen. Sideways
                // the tabs move to the rail on the left (AC-017-2).
                if (activeTab != null && !landscape) TabBar(tabs = tabItems, active = activeTab.key, onSelect = onTab)
            },
        ) { padding ->
            Row(Modifier.padding(padding).consumeWindowInsets(padding)) {
                val railTab = activeTab?.takeIf { landscape }
                val rail = railTab != null
                if (railTab != null) NavRail(tabs = tabItems, active = railTab.key, onSelect = onTab)
                NavHost(
                    nav,
                    startDestination = TodayGraph,
                    // The rail already keeps clear of the cut-out on the left (AC-017-5).
                    modifier =
                        Modifier
                            .weight(1f)
                            .then(
                                if (rail) {
                                    Modifier.consumeWindowInsets(
                                        WindowInsets.safeDrawing.only(WindowInsetsSides.Start),
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                ) {
                    navigation<TodayGraph>(startDestination = TodayHome) {
                        composable<TodayHome> { TodayScreen(nav) }
                    }
                    navigation<ScheduleGraph>(startDestination = ScheduleHome) {
                        composable<ScheduleHome> { AgendaScreen(nav) }
                    }
                    navigation<RegistryGraph>(startDestination = RegistryHome()) {
                        composable<RegistryHome> {
                            val r = it.toRoute<RegistryHome>()
                            RegistryScreen(nav, r.view, r.owing)
                        }
                    }
                    navigation<EarningsGraph>(startDestination = EarningsHome) {
                        composable<EarningsHome> { entry ->
                            val asked by entry.savedStateHandle.getStateFlow<String?>(OPEN_PERIOD, null).collectAsState()
                            EarningsScreen(nav, asked) { entry.savedStateHandle[OPEN_PERIOD] = null }
                        }
                    }
                    navigation<SettingsGraph>(startDestination = SettingsHome) {
                        composable<SettingsHome> { SettingsScreen(appInfo, schemaVersion) { route -> nav.navigate(route) } }
                        composable<SettingsGeneral> { SettingsGeneralScreen(nav) }
                        composable<SettingsLessons> { SettingsLessonsScreen(nav) }
                        composable<SettingsPayments> { SettingsPaymentsScreen(nav) }
                        composable<SettingsAbout> { SettingsAboutScreen(nav, appInfo, schemaVersion) }
                        composable<PersonalData> { PersonalDataScreen { nav.popBackStack() } }
                    }
                    composable<BackupHome> {
                        app.tutora.ui.backup
                            .BackupScreen(nav)
                    }
                    composable<AppearanceHome> {
                        app.tutora.ui.appearance
                            .AppearanceScreen(nav)
                    }
                    composable<CustomColors> {
                        app.tutora.ui.appearance
                            .CustomColorsScreen(nav, it.toRoute<CustomColors>().part)
                    }
                    composable<ConfirmClasses> {
                        app.tutora.ui.confirm
                            .ConfirmClassesScreen(nav)
                    }
                    composable<NoticesHome> {
                        app.tutora.ui.reminders
                            .NoticesScreen(nav)
                    }
                    composable<Evolution> {
                        app.tutora.ui.earnings
                            .EvolutionScreen(nav)
                    }
                    composable<RemindersHome> {
                        app.tutora.ui.reminders
                            .RemindersScreen(nav)
                    }
                    composable<TeachingHome> {
                        app.tutora.ui.setup
                            .TeachingScreen(nav)
                    }
                    composable<SetupHome> {
                        app.tutora.ui.setup
                            .SetupScreen(nav)
                    }
                    composable<CalendarHome> {
                        app.tutora.ui.calendar
                            .CalendarScreen(nav)
                    }
                    composable<PlaceDetail> { PlaceDetailScreen(nav, it.toRoute<PlaceDetail>().id) }
                    composable<PlaceEdit> { PlaceEditScreen(nav, it.toRoute<PlaceEdit>().id) }
                    composable<StudentDetail> { StudentDetailScreen(nav, it.toRoute<StudentDetail>().id) }
                    composable<StudentEdit> { StudentEditScreen(nav, it.toRoute<StudentEdit>().id) }
                    composable<GroupEdit> {
                        val r = it.toRoute<GroupEdit>()
                        GroupEditScreen(nav, r.id, r.placeId)
                    }
                    composable<LiveLesson> {
                        val r = it.toRoute<LiveLesson>()
                        LiveScreen(nav, r.id, r.end)
                    }
                    composable<GroupDetail> { GroupDetailScreen(nav, it.toRoute<GroupDetail>().id) }
                    composable<Archived> { ArchivedScreen(nav) }
                    composable<LessonNew> { LessonNewScreen(nav, it.toRoute<LessonNew>().date) }
                    composable<LessonDetail> { LessonDetailScreen(nav, it.toRoute<LessonDetail>().id) }
                    composable<LessonEdit> { LessonEditScreen(nav, it.toRoute<LessonEdit>().id) }
                    composable<Report> {
                        val r = it.toRoute<Report>()
                        ReportScreen(nav, r.kind, r.id)
                    }
                    composable<PaymentDetail> { PaymentDetailScreen(nav, it.toRoute<PaymentDetail>().id) }
                    composable<PayerLessons> {
                        val r = it.toRoute<PayerLessons>()
                        PayerLessonsScreen(nav, r.kind, r.id)
                    }
                }
            }
        }
    }
}
