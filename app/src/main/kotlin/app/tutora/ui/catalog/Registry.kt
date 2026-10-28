package app.tutora.ui.catalog

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
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.Chip
import app.tutora.designsystem.components.EmptyState
import app.tutora.designsystem.components.HeaderAction
import app.tutora.designsystem.components.IconButtonVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.SegmentedControl
import app.tutora.designsystem.components.TextField
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.Group
import app.tutora.domain.PayerKind
import app.tutora.domain.PayerRef
import app.tutora.domain.PlaceKind
import app.tutora.domain.RateType
import app.tutora.domain.initials
import app.tutora.state.LocalServices
import app.tutora.state.PlaceItem
import app.tutora.state.StudentItem
import app.tutora.ui.Archived
import app.tutora.ui.GroupDetail
import app.tutora.ui.GroupEdit
import app.tutora.ui.PlaceDetail
import app.tutora.ui.PlaceEdit
import app.tutora.ui.RegistryHome
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.StudentDetail
import app.tutora.ui.StudentEdit
import app.tutora.ui.countText
import app.tutora.ui.kindName
import app.tutora.ui.minutesText
import app.tutora.ui.payments.balanceBadge
import app.tutora.ui.payments.packageUseText
import app.tutora.ui.payments.paysTeacher
import app.tutora.ui.placeName
import app.tutora.ui.placePriceText
import app.tutora.ui.rateMoney
import app.tutora.ui.tag
import app.tutora.ui.travelText
import java.text.Normalizer

/** Accent- and case-insensitive name match, so "ana" finds "Ána". */
private fun fold(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()

/**
 * Cadastro: students, groups and places share one tab, since each links to the others; Alunos,
 * Turmas and Locais are its sub-pages (AC-004-31, AC-005-27). The header has Arquivados and +.
 */
@Composable
fun RegistryScreen(
    nav: NavHostController,
    view: String,
    owing: Boolean = false,
) {
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
                    stringResource(
                        when (view) {
                            "places" -> R.string.places_new
                            "groups" -> R.string.groups_new
                            else -> R.string.students_new
                        },
                    ),
                    IconButtonVariant.ACCENT,
                ) {
                    nav.navigate(
                        when (view) {
                            "places" -> PlaceEdit()
                            "groups" -> GroupEdit()
                            else -> StudentEdit()
                        },
                    )
                },
            ),
    ) {
        SegmentedControl(
            listOf(
                "students" to stringResource(R.string.tab_students),
                "groups" to stringResource(R.string.tab_groups),
                "places" to stringResource(R.string.tab_places),
            ),
            view,
            { nav.navigate(RegistryHome(it)) { popUpTo<RegistryHome> { inclusive = true } } },
            stringResource(R.string.tab_registry),
        )
        when (view) {
            "places" -> PlacesPane(nav)
            "groups" -> GroupsPane(nav)
            else -> StudentsPane(nav, owing)
        }
    }
}

@Composable
private fun StudentsPane(
    nav: NavHostController,
    owingFirst: Boolean,
) {
    val services = LocalServices.current
    val students by services.catalogRepo.students().collectAsState(initial = emptyList())
    val balances by services.paymentRepo.balances().collectAsState(initial = emptyMap())
    var owingOnly by rememberSaveable { mutableStateOf(owingFirst) }

    fun owes(id: String) = (balances[PayerRef(PayerKind.STUDENT, id)] ?: 0) > 0
    val places by services.catalogRepo.places().collectAsState(initial = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var byPlace by rememberSaveable { mutableStateOf(false) }
    val names = places.associate { it.place.id to placeName(it.place) }
    Column(Modifier.testTag("students-screen"), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
        if (students.isEmpty()) {
            EmptyState(
                stringResource(R.string.empty_registry_title),
                icon = IconName.USERS,
                body = stringResource(R.string.empty_registry_body),
            )
            return@Column
        }
        TextField(stringResource(R.string.students_search), query, { query = it })
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Chip(stringResource(R.string.students_all, students.size.toString()), !byPlace && !owingOnly, {
                byPlace = false
                owingOnly = false
            })
            // Devendo: only students who owe, with how many (AC-008-13).
            Chip(stringResource(R.string.pay_owingFilter, students.count { owes(it.student.id) }), owingOnly, {
                owingOnly = !owingOnly
                if (owingOnly) byPlace = false
            })
            Chip(stringResource(R.string.students_byPlace), byPlace, {
                byPlace = true
                owingOnly = false
            })
        }
        val q = fold(query.trim())
        val shownStudents = students.filter { fold(it.student.name).contains(q) && (!owingOnly || owes(it.student.id)) }
        val noPlace = stringResource(R.string.students_noPlace)

        @Composable
        fun studentRow(s: StudentItem): @Composable () -> Unit {
            val subtitle =
                listOfNotNull(
                    s.student.subject,
                    if (s.links.isEmpty()) noPlace else s.links.mapNotNull { names[it.placeId] }.joinToString(", "),
                ).joinToString(" · ")
            return {
                // A package student shows its use; everyone shows their balance (AC-008-13, 21).
                val pkg =
                    if (s.student.rateType == RateType.PACKAGE) {
                        services.paymentRepo
                            .lastPackage(s.student.id)
                            .collectAsState(initial = null)
                            .value
                    } else {
                        null
                    }
                ListRow(
                    s.student.name,
                    subtitle = listOfNotNull(subtitle, pkg?.let { packageUseText(it) }).joinToString(" · "),
                    avatar = initials(s.student.name) to s.student.color.tag(),
                    badge = balanceBadge(balances[PayerRef(PayerKind.STUDENT, s.student.id)] ?: 0),
                    onClick = { nav.navigate(StudentDetail(s.student.id)) },
                )
            }
        }

        when {
            shownStudents.isEmpty() ->
                Text(stringResource(R.string.students_noSearchResults), style = TutoraTheme.type.body, color = TutoraTheme.colors.inkMuted)
            byPlace -> {
                places.forEach { p ->
                    val here = shownStudents.filter { s -> s.links.any { it.placeId == p.place.id } }
                    if (here.isNotEmpty()) ListGroup(here.map { studentRow(it) }, title = placeName(p.place))
                }
                val without = shownStudents.filter { it.links.isEmpty() }
                if (without.isNotEmpty()) ListGroup(without.map { studentRow(it) }, title = noPlace)
            }
            else -> ListGroup(shownStudents.map { studentRow(it) })
        }
    }
}

/** Turmas: active groups with search, each with its subject, place, size and price (AC-004-31). */
@Composable
private fun GroupsPane(nav: NavHostController) {
    val services = LocalServices.current
    val groups by services.catalogRepo.groups().collectAsState(initial = emptyList())
    val places by services.catalogRepo.places().collectAsState(initial = emptyList())
    val balances by services.paymentRepo.balances().collectAsState(initial = emptyMap())
    var query by rememberSaveable { mutableStateOf("") }
    val names = places.associate { it.place.id to placeName(it.place) }
    val language = currentLanguage()
    Column(Modifier.testTag("groups-screen"), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
        if (groups.isEmpty()) {
            EmptyState(stringResource(R.string.groups_empty), icon = IconName.USERS, body = stringResource(R.string.groups_emptyBody))
            return@Column
        }
        TextField(stringResource(R.string.groups_search), query, { query = it })
        val q = fold(query.trim())
        val shown = groups.filter { fold(it.name).contains(q) }
        if (shown.isEmpty()) {
            Text(stringResource(R.string.students_noSearchResults), style = TutoraTheme.type.body, color = TutoraTheme.colors.inkMuted)
            return@Column
        }
        ListGroup(
            shown.map { g: Group ->
                val subtitle =
                    listOfNotNull(
                        g.subject,
                        names[g.placeId],
                        countText(R.string.groups_size_zero, R.plurals.groups_size, g.memberIds.size),
                        g.rateCents?.let { stringResource(R.string.places_perLesson, rateMoney(it, language)) },
                    ).joinToString(" · ")
                val row: @Composable () -> Unit = {
                    ListRow(
                        g.name,
                        subtitle = subtitle,
                        icon = IconName.USERS,
                        badge = balanceBadge(balances[PayerRef(PayerKind.GROUP, g.id)] ?: 0),
                        onClick = { nav.navigate(GroupDetail(g.id)) },
                    )
                }
                row
            },
        )
    }
}

@Composable
private fun PlacesPane(nav: NavHostController) {
    val services = LocalServices.current
    val places by services.catalogRepo.places().collectAsState(initial = emptyList())
    val balances by services.paymentRepo.balances().collectAsState(initial = emptyMap())
    val settings by services.settings.collectAsState(initial = null)
    val showDefaults = settings?.showDefaultPlaces ?: true
    val builtins = if (showDefaults) places.filter { it.place.builtin != null } else emptyList()
    val own = places.filter { it.place.builtin == null }

    @Composable
    fun row(p: PlaceItem): @Composable () -> Unit {
        // Like Alunos and Turmas: what it is in the subtitle, its balance on the right (AC-008-14).
        val summary =
            listOfNotNull(
                if (p.place.builtin == null) kindName(p.place.kind) else null,
                placePriceText(p.place),
                minutesText(p.place.defaultDurationMin),
                countText(R.string.places_students_zero, R.plurals.places_students, p.studentCount),
                if (p.place.kind == PlaceKind.STUDENT_HOME) travelText(p.place) else null,
            ).joinToString(" · ")
        val badge = if (paysTeacher(p.place)) balanceBadge(balances[PayerRef(PayerKind.PLACE, p.place.id)] ?: 0) else null
        return {
            ListRow(
                placeName(p.place),
                subtitle = summary,
                icon = IconName.PIN,
                badge = badge,
                onClick = { nav.navigate(PlaceDetail(p.place.id)) },
            )
        }
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
                            onClick = { nav.navigate(StudentDetail(s.student.id)) },
                        )
                    }
                },
                title = stringResource(R.string.tab_students),
            )
        }
        if (groups.isNotEmpty()) {
            ListGroup(
                groups.map { g ->
                    { ListRow(g.name, onClick = { nav.navigate(GroupDetail(g.id)) }) }
                },
                title = stringResource(R.string.places_groups),
            )
        }
        if (places.isNotEmpty()) {
            ListGroup(
                places.map { p ->
                    { ListRow(placeName(p.place), onClick = { nav.navigate(PlaceDetail(p.place.id)) }) }
                },
                title = stringResource(R.string.tab_places),
            )
        }
    }
}
