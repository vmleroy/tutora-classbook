package com.vmleroy.tutora.app.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.vmleroy.tutora.app.R
import com.vmleroy.tutora.app.state.LocalServices
import com.vmleroy.tutora.app.state.PlaceItem
import com.vmleroy.tutora.app.state.StudentItem
import com.vmleroy.tutora.app.ui.Archived
import com.vmleroy.tutora.app.ui.GroupEdit
import com.vmleroy.tutora.app.ui.PlaceDetail
import com.vmleroy.tutora.app.ui.PlaceEdit
import com.vmleroy.tutora.app.ui.RegistryHome
import com.vmleroy.tutora.app.ui.ScreenScaffold
import com.vmleroy.tutora.app.ui.StudentDetail
import com.vmleroy.tutora.app.ui.StudentEdit
import com.vmleroy.tutora.app.ui.countText
import com.vmleroy.tutora.app.ui.kindName
import com.vmleroy.tutora.app.ui.minutesText
import com.vmleroy.tutora.app.ui.placeName
import com.vmleroy.tutora.app.ui.placePriceText
import com.vmleroy.tutora.app.ui.rateMoney
import com.vmleroy.tutora.app.ui.tag
import com.vmleroy.tutora.app.ui.travelText
import com.vmleroy.tutora.designsystem.TutoraTheme
import com.vmleroy.tutora.designsystem.components.BadgeTone
import com.vmleroy.tutora.designsystem.components.Chip
import com.vmleroy.tutora.designsystem.components.EmptyState
import com.vmleroy.tutora.designsystem.components.HeaderAction
import com.vmleroy.tutora.designsystem.components.IconButtonVariant
import com.vmleroy.tutora.designsystem.components.IconName
import com.vmleroy.tutora.designsystem.components.ListGroup
import com.vmleroy.tutora.designsystem.components.ListRow
import com.vmleroy.tutora.designsystem.components.SegmentedControl
import com.vmleroy.tutora.designsystem.components.TextField
import com.vmleroy.tutora.designsystem.components.currentLanguage
import com.vmleroy.tutora.designsystem.tokens.TutoraTokens.Spacing
import com.vmleroy.tutora.domain.Group
import com.vmleroy.tutora.domain.PlaceKind
import com.vmleroy.tutora.domain.initials
import java.text.Normalizer

/** Accent- and case-insensitive name match, so "ana" finds "Ána". */
private fun fold(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()

/**
 * Cadastro: students and places share one tab, since each links to the other; Alunos and
 * Locais are its two sub-pages (AC-005-27). The header has Arquivados and + (AC-004-23).
 */
@Composable
fun RegistryScreen(
    nav: NavHostController,
    view: String,
) {
    val places = view == "places"
    ScreenScaffold(
        stringResource(R.string.tab_registry),
        "registry-screen",
        actions =
            listOf(
                HeaderAction(
                    IconName.ARCHIVE,
                    stringResource(R.string.catalog_archived),
                    IconButtonVariant.PLAIN,
                ) { nav.navigate(Archived) },
                HeaderAction(
                    IconName.PLUS,
                    stringResource(if (places) R.string.places_new else R.string.students_new),
                    IconButtonVariant.ACCENT,
                ) {
                    nav.navigate(if (places) PlaceEdit() else StudentEdit())
                },
            ),
    ) {
        SegmentedControl(
            listOf("students" to stringResource(R.string.tab_students), "places" to stringResource(R.string.tab_places)),
            view,
            { nav.navigate(RegistryHome(it)) { popUpTo<RegistryHome> { inclusive = true } } },
            stringResource(R.string.tab_registry),
        )
        if (places) PlacesPane(nav) else StudentsPane(nav)
    }
}

@Composable
private fun StudentsPane(nav: NavHostController) {
    val services = LocalServices.current
    val students by services.catalogRepo.students().collectAsState(initial = emptyList())
    val groups by services.catalogRepo.groups().collectAsState(initial = emptyList())
    val places by services.catalogRepo.places().collectAsState(initial = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var byPlace by rememberSaveable { mutableStateOf(false) }
    val names = places.associate { it.place.id to placeName(it.place) }
    val language = currentLanguage()
    Column(Modifier.testTag("students-screen"), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
        if (students.isEmpty() && groups.isEmpty()) {
            EmptyState(
                stringResource(R.string.empty_registry_title),
                icon = IconName.USERS,
                body = stringResource(R.string.empty_registry_body),
            )
            return@Column
        }
        TextField(stringResource(R.string.students_search), query, { query = it })
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Chip(stringResource(R.string.students_all, (students.size + groups.size).toString()), !byPlace, { byPlace = false })
            Chip(stringResource(R.string.students_byPlace), byPlace, { byPlace = true })
        }
        val q = fold(query.trim())
        val shownStudents = students.filter { fold(it.student.name).contains(q) }
        val shownGroups = groups.filter { fold(it.name).contains(q) }
        val noPlace = stringResource(R.string.students_noPlace)

        @Composable
        fun studentRow(s: StudentItem): @Composable () -> Unit {
            val subtitle =
                listOfNotNull(
                    s.student.subject,
                    if (s.links.isEmpty()) noPlace else s.links.mapNotNull { names[it.placeId] }.joinToString(", "),
                ).joinToString(" · ")
            return {
                ListRow(
                    s.student.name,
                    subtitle = subtitle,
                    avatar = initials(s.student.name) to s.student.color.tag(),
                    onClick = { nav.navigate(StudentDetail(s.student.id)) },
                )
            }
        }

        @Composable
        fun groupRow(g: Group): @Composable () -> Unit {
            val subtitle =
                listOfNotNull(
                    g.subject,
                    names[g.placeId],
                    countText(R.string.groups_size_zero, R.plurals.groups_size, g.memberIds.size),
                    g.rateCents?.let { stringResource(R.string.places_perLesson, rateMoney(it, language)) },
                ).joinToString(" · ")
            return { ListRow(g.name, subtitle = subtitle, icon = IconName.USERS, onClick = { nav.navigate(GroupEdit(id = g.id)) }) }
        }
        when {
            shownStudents.isEmpty() && shownGroups.isEmpty() ->
                Text(stringResource(R.string.students_noSearchResults), style = TutoraTheme.type.body, color = TutoraTheme.colors.inkMuted)
            byPlace -> {
                places.forEach { p ->
                    val here = shownStudents.filter { s -> s.links.any { it.placeId == p.place.id } }
                    val groupsHere = shownGroups.filter { it.placeId == p.place.id }
                    if (here.isNotEmpty() || groupsHere.isNotEmpty()) {
                        ListGroup(here.map { studentRow(it) } + groupsHere.map { groupRow(it) }, title = placeName(p.place))
                    }
                }
                val without = shownStudents.filter { it.links.isEmpty() }
                if (without.isNotEmpty()) ListGroup(without.map { studentRow(it) }, title = noPlace)
            }
            else -> ListGroup(shownStudents.map { studentRow(it) } + shownGroups.map { groupRow(it) })
        }
    }
}

@Composable
private fun PlacesPane(nav: NavHostController) {
    val services = LocalServices.current
    val places by services.catalogRepo.places().collectAsState(initial = emptyList())
    val settings by services.settings.collectAsState(initial = null)
    val showDefaults = settings?.showDefaultPlaces ?: true
    val builtins = if (showDefaults) places.filter { it.place.builtin != null } else emptyList()
    val own = places.filter { it.place.builtin == null }

    @Composable
    fun row(p: PlaceItem): @Composable () -> Unit {
        val summary =
            listOfNotNull(
                placePriceText(p.place),
                minutesText(p.place.defaultDurationMin),
                countText(R.string.places_students_zero, R.plurals.places_students, p.studentCount),
                if (p.place.kind == PlaceKind.STUDENT_HOME) travelText(p.place) else null,
            ).joinToString(" · ")
        val badge = if (p.place.builtin == null) BadgeTone.SECONDARY to kindName(p.place.kind) else null
        return { ListRow(placeName(p.place), subtitle = summary, badge = badge, onClick = { nav.navigate(PlaceDetail(p.place.id)) }) }
    }
    Column(Modifier.testTag("places-screen"), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
        Text(stringResource(R.string.places_subtitle), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
        if (builtins.isNotEmpty()) {
            ListGroup(
                builtins.map {
                    row(it)
                },
                title = "${stringResource(R.string.places_builtinGroup)} · ${stringResource(R.string.places_builtinHint)}",
            )
        }
        if (own.isNotEmpty()) ListGroup(own.map { row(it) }, title = stringResource(R.string.places_own))
        // Only when nothing at all is listed: the default places are places too (AC-004-1).
        if (builtins.isEmpty() && own.isEmpty()) {
            EmptyState(stringResource(R.string.places_empty), icon = IconName.PIN, body = stringResource(R.string.places_emptyBody))
        }
    }
}

/** Arquivados: everything hidden, each opening its form to restore or remove it (AC-004-23). */
@Composable
fun ArchivedScreen(nav: NavHostController) {
    val services = LocalServices.current
    val places by services.catalogRepo.places(archived = true).collectAsState(initial = emptyList())
    val students by services.catalogRepo.students(archived = true).collectAsState(initial = emptyList())
    val groups by services.catalogRepo.groups(archived = true).collectAsState(initial = emptyList())
    ScreenScaffold(stringResource(R.string.catalog_archived), "archived", onBack = { nav.popBackStack() }) {
        if (places.isEmpty() && students.isEmpty() && groups.isEmpty()) {
            Text(stringResource(R.string.catalog_noArchived), style = TutoraTheme.type.body, color = TutoraTheme.colors.inkMuted)
        }
        if (students.isNotEmpty()) {
            ListGroup(
                students.map { s ->
                    {
                        ListRow(
                            s.student.name,
                            avatar = initials(s.student.name) to s.student.color.tag(),
                            onClick = { nav.navigate(StudentEdit(s.student.id)) },
                        )
                    }
                },
                title = stringResource(R.string.tab_students),
            )
        }
        if (groups.isNotEmpty()) {
            ListGroup(
                groups.map { g ->
                    { ListRow(g.name, onClick = { nav.navigate(GroupEdit(id = g.id)) }) }
                },
                title = stringResource(R.string.places_groups),
            )
        }
        if (places.isNotEmpty()) {
            ListGroup(
                places.map { p ->
                    { ListRow(placeName(p.place), onClick = { nav.navigate(PlaceEdit(p.place.id)) }) }
                },
                title = stringResource(R.string.tab_places),
            )
        }
    }
}
