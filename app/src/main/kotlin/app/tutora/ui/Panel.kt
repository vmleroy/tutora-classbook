package app.tutora.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.SidePanel
import app.tutora.ui.catalog.GroupDetailScreen
import app.tutora.ui.catalog.PlaceDetailScreen
import app.tutora.ui.catalog.StudentDetailScreen
import app.tutora.ui.schedule.AgendaDayScreen
import app.tutora.ui.schedule.LessonDetailScreen
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/*
 * The side panel (spec 017, option B, Victor 5 Nov 2026). With the phone sideways, every screen
 * keeps its upright structure and what is tapped opens in a panel over the right side. Screens
 * never check the orientation: they open a page with rememberOpenPage, which navigates upright
 * and fills the panel sideways. The shell hosts the one panel, its Back and its content.
 */

/** A day of the Agenda's Mês, which only exists in the panel (AC-017-10). */
@Serializable
data class AgendaDay(
    val date: String,
)

/** The pages that open in the panel sideways; anything else always navigates. */
private fun opensInPanel(route: Any) =
    route is StudentDetail || route is GroupDetail || route is PlaceDetail || route is LessonDetail || route is AgendaDay

/** What the panel shows, and over which screen (a back stack entry) it was opened. */
class DetailPanel {
    var route by mutableStateOf<Any?>(null)
        private set
    private var origin by mutableStateOf<String?>(null)

    fun open(
        route: Any,
        over: String?,
    ) {
        // A page opened from inside the panel replaces it, over the same screen.
        if (this.route == null) origin = over
        this.route = route
    }

    fun close() {
        route = null
        origin = null
    }

    fun showsOver(entry: String?) = route != null && entry == origin
}

val LocalDetailPanel = staticCompositionLocalOf<DetailPanel?> { null }

/** Opens [route]: in the panel when the phone is sideways and the page has one, else full screen. */
@Composable
fun rememberOpenPage(nav: NavHostController): (Any) -> Unit {
    val panel = LocalDetailPanel.current
    val sideways = TutoraTheme.landscape
    return remember(nav, panel, sideways) {
        { route ->
            if (sideways && panel != null && opensInPanel(route)) {
                panel.open(route, nav.currentBackStackEntry?.id)
            } else {
                nav.navigate(route)
            }
        }
    }
}

/**
 * The shell's panel over [content] (the tabs' screens, never the rail). It shows only over the
 * screen it was opened from, so a form opened from it full screen is never covered. Back closes
 * it first (AC-017-11); turning the phone upright opens its page full screen.
 */
@Composable
fun DetailPanelHost(
    nav: NavHostController,
    panel: DetailPanel,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val sideways = TutoraTheme.landscape
    val entry by nav.currentBackStackEntryAsState()
    val visible = sideways && panel.showsOver(entry?.id)
    LaunchedEffect(sideways) {
        val route = panel.route
        if (!sideways && route != null) {
            panel.close()
            if (route !is AgendaDay) nav.navigate(route)
        }
    }
    Box(modifier) {
        content()
        // Keeps the last page while the panel slides out.
        val last = remember { arrayOfNulls<Any>(1) }
        panel.route?.let { last[0] = it }
        SidePanel(visible, onDismiss = panel::close) {
            last[0]?.let { route -> key(route) { PanelPage(nav, route, panel::close) } }
        }
    }
    BackHandler(enabled = visible) { panel.close() }
}

@Composable
private fun PanelPage(
    nav: NavHostController,
    route: Any,
    close: () -> Unit,
) {
    when (route) {
        is StudentDetail -> StudentDetailScreen(nav, route.id, onBack = close)
        is GroupDetail -> GroupDetailScreen(nav, route.id, onBack = close)
        is PlaceDetail -> PlaceDetailScreen(nav, route.id, onBack = close)
        is LessonDetail -> LessonDetailScreen(nav, route.id, onBack = close)
        is AgendaDay -> AgendaDayScreen(nav, LocalDate.parse(route.date), onBack = close)
    }
}
