package com.vmleroy.tutora.app.ui.catalog

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.NavHostController
import com.vmleroy.tutora.app.R
import com.vmleroy.tutora.app.state.LocalServices
import com.vmleroy.tutora.app.ui.PlaceDetail
import com.vmleroy.tutora.app.ui.RegistryHome
import com.vmleroy.tutora.app.ui.ScreenScaffold
import com.vmleroy.tutora.app.ui.errorText
import com.vmleroy.tutora.app.ui.placeName
import com.vmleroy.tutora.app.ui.placePriceText
import com.vmleroy.tutora.app.ui.rateText
import com.vmleroy.tutora.app.ui.tag
import com.vmleroy.tutora.app.usecases.Kind
import com.vmleroy.tutora.app.usecases.Result
import com.vmleroy.tutora.designsystem.TutoraTheme
import com.vmleroy.tutora.designsystem.components.Avatar
import com.vmleroy.tutora.designsystem.components.AvatarSize
import com.vmleroy.tutora.designsystem.components.BottomSheet
import com.vmleroy.tutora.designsystem.components.Button
import com.vmleroy.tutora.designsystem.components.ButtonVariant
import com.vmleroy.tutora.designsystem.components.Card
import com.vmleroy.tutora.designsystem.components.CardElevation
import com.vmleroy.tutora.designsystem.components.Checkbox
import com.vmleroy.tutora.designsystem.components.ControlSize
import com.vmleroy.tutora.designsystem.components.FieldError
import com.vmleroy.tutora.designsystem.components.HeaderAction
import com.vmleroy.tutora.designsystem.components.HeaderVariant
import com.vmleroy.tutora.designsystem.components.IconButton
import com.vmleroy.tutora.designsystem.components.IconButtonVariant
import com.vmleroy.tutora.designsystem.components.IconName
import com.vmleroy.tutora.designsystem.components.ListGroup
import com.vmleroy.tutora.designsystem.components.ListRow
import com.vmleroy.tutora.designsystem.components.MoneyField
import com.vmleroy.tutora.designsystem.components.RowTrailing
import com.vmleroy.tutora.designsystem.components.SectionHeader
import com.vmleroy.tutora.designsystem.components.SegmentedControl
import com.vmleroy.tutora.designsystem.components.TagDot
import com.vmleroy.tutora.designsystem.components.TextField
import com.vmleroy.tutora.designsystem.components.TileTone
import com.vmleroy.tutora.designsystem.tokens.TutoraTokens.Size
import com.vmleroy.tutora.designsystem.tokens.TutoraTokens.Spacing
import com.vmleroy.tutora.domain.CatalogError
import com.vmleroy.tutora.domain.Group
import com.vmleroy.tutora.domain.Payer
import com.vmleroy.tutora.domain.PlaceKind
import com.vmleroy.tutora.domain.PlaceLink
import com.vmleroy.tutora.domain.Price
import com.vmleroy.tutora.domain.RateType
import com.vmleroy.tutora.domain.Student
import com.vmleroy.tutora.domain.TagColor
import com.vmleroy.tutora.domain.initials
import com.vmleroy.tutora.domain.randomTag
import com.vmleroy.tutora.domain.resolvePrice
import com.vmleroy.tutora.domain.unlink
import com.vmleroy.tutora.domain.withDefault
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** A student: contact, rate and places, which open the place (AC-004-15). */
@Composable
fun StudentDetailScreen(
    nav: NavHostController,
    id: String,
) {
    val services = LocalServices.current
    val item by services.catalogRepo.student(id).collectAsState(initial = null)
    val places by services.catalogRepo.places().collectAsState(initial = emptyList())
    val s = item ?: return
    val byId = places.associateBy { it.place.id }
    ScreenScaffold(
        stringResource(R.string.students_detail),
        "student-detail",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
        actions =
            listOf(
                HeaderAction(IconName.PENCIL, stringResource(R.string.catalog_edit)) {
                    nav.navigate(
                        com.vmleroy.tutora.app.ui
                            .StudentEdit(id),
                    )
                },
            ),
    ) {
        Card {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3), verticalAlignment = Alignment.CenterVertically) {
                Avatar(initials(s.student.name), s.student.color.tag(), size = AvatarSize.LG)
                Column {
                    Text(s.student.name, style = TutoraTheme.type.titleLg, color = TutoraTheme.colors.ink)
                    Text(
                        listOfNotNull(s.student.subject, s.student.phone).joinToString(" · "),
                        style = TutoraTheme.type.meta,
                        color = TutoraTheme.colors.inkMuted,
                    )
                }
            }
        }
        Card(label = stringResource(R.string.students_rate)) {
            Text(stringResource(R.string.students_rate), style = TutoraTheme.type.caption, color = TutoraTheme.colors.inkMuted)
            Text(
                stringResource(if (s.student.rateType == RateType.HOUR) R.string.students_perHour else R.string.students_perLesson),
                style = TutoraTheme.type.bodyStrong,
                color = TutoraTheme.colors.ink,
            )
            s.student.notes?.let { Text(it, style = TutoraTheme.type.body, color = TutoraTheme.colors.inkMuted) }
        }
        val noPlace = stringResource(R.string.students_noPlace)
        val salary = stringResource(R.string.groups_coveredBySalary)
        val unset = stringResource(R.string.places_noPrice)
        ListGroup(
            if (s.links.isEmpty()) {
                listOf { ListRow(noPlace, trailing = RowTrailing.NONE) }
            } else {
                s.links.mapNotNull { link ->
                    val place = byId[link.placeId]?.place ?: return@mapNotNull null
                    val price =
                        when (val p = resolvePrice(place, s.student, link)) {
                            is Price.Set -> rateText(p.cents, p.rateType)
                            Price.Salary -> salary
                            Price.Unset -> unset
                        }
                    val row: @Composable () -> Unit = {
                        ListRow(
                            placeName(place),
                            subtitle = price,
                            icon = if (link.isDefault) IconName.STAR else IconName.PIN,
                            iconTone = if (link.isDefault) TileTone.ACCENT else TileTone.SECONDARY,
                            onClick = { nav.navigate(PlaceDetail(place.id)) },
                        )
                    }
                    row
                }
            },
            title = stringResource(R.string.students_places),
        )
    }
}

private val blank = Student("", "", null, null, null, TagColor.AMBER, RateType.HOUR, null)

/** Editar aluno / Novo aluno: places with prices, color, notes (AC-004-10 to 13, 28). */
@Composable
fun StudentEditScreen(
    nav: NavHostController,
    id: String?,
) {
    val services = LocalServices.current
    // A new student starts with a random color; the teacher can still pick another (AC-004-28).
    val fresh = remember { blank.copy(color = randomTag(Math.random())) }
    val item by remember(id) {
        if (id ==
            null
        ) {
            flowOf(
                com.vmleroy.tutora.app.state
                    .StudentItem(fresh, emptyList()),
            )
        } else {
            services.catalogRepo.student(id)
        }
    }.collectAsState(initial = null)
    val s = item ?: return
    StudentForm(nav, id, s.student, s.links)
}

@Composable
private fun StudentForm(
    nav: NavHostController,
    id: String?,
    initial: Student,
    initialLinks: List<PlaceLink>,
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val places by services.catalogRepo.places().collectAsState(initial = emptyList())
    val settings by services.settings.collectAsState(initial = null)
    var d by remember(initial) { mutableStateOf(initial) }
    var links by remember(initialLinks) { mutableStateOf(initialLinks) }
    var error by remember { mutableStateOf<CatalogError?>(null) }
    // Each Save that fails counts, so the same error blinks again.
    var tries by remember { mutableIntStateOf(0) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val byId = places.associateBy { it.place.id }
    ScreenScaffold(
        stringResource(
            if (id !=
                null
            ) {
                R.string.students_edit
            } else {
                R.string.students_new
            },
        ),
        "student-edit",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
        bottomBar = {
            Button(stringResource(R.string.catalog_save), {
                tries++
                scope.launch {
                    when (val r = services.catalog.saveStudent(d, links)) {
                        is Result.Ok -> nav.popBackStack()
                        is Result.Err -> error = r.error
                    }
                }
            }, fullWidth = true)
        },
    ) {
        TextField(
            stringResource(R.string.students_name),
            d.name,
            { d = d.copy(name = it) },
            placeholder = stringResource(R.string.students_namePlaceholder),
            capitalization = KeyboardCapitalization.Words,
            error = error?.takeIf { it == CatalogError.NAME_REQUIRED }?.let { errorText(it) },
            errorKey = tries,
        )
        TextField(stringResource(R.string.students_subject), d.subject.orEmpty(), {
            d = d.copy(subject = it.ifEmpty { null })
        }, placeholder = stringResource(R.string.students_subjectPlaceholder))
        TextField(stringResource(R.string.students_phone), d.phone.orEmpty(), {
            d = d.copy(phone = it.ifEmpty { null })
        }, keyboardType = KeyboardType.Phone, capitalization = KeyboardCapitalization.None)
        SectionHeader(stringResource(R.string.students_wherePays), aside = stringResource(R.string.students_starHint))
        SegmentedControl(
            listOf(
                RateType.HOUR to stringResource(R.string.students_perHour),
                RateType.LESSON to stringResource(R.string.students_perLesson),
            ),
            d.rateType,
            { d = d.copy(rateType = it) },
            stringResource(R.string.students_charging),
            size = ControlSize.SM,
        )
        links.forEach { link ->
            val place = byId[link.placeId]?.place ?: return@forEach
            val name = placeName(place)
            val placePays = place.payer == Payer.PLACE || place.kind == PlaceKind.SCHOOL
            val custom = link.rateCents != null
            Card(elevation = if (link.isDefault) CardElevation.RAISED else CardElevation.OUTLINE) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        IconName.STAR,
                        stringResource(R.string.students_makeDefault, name),
                        { links = withDefault(links.map { it.copy(isDefault = it.placeId == link.placeId) }) },
                        variant = if (link.isDefault) IconButtonVariant.ACCENT else IconButtonVariant.SUNKEN,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(name, style = TutoraTheme.type.bodyStrong, color = TutoraTheme.colors.ink)
                        Text(
                            when {
                                placePays -> stringResource(R.string.students_paidByPlace, placePriceText(place))
                                custom -> stringResource(R.string.students_ownPrice)
                                else -> stringResource(R.string.students_placePrice, placePriceText(place))
                            },
                            style = TutoraTheme.type.meta,
                            color = TutoraTheme.colors.inkMuted,
                        )
                    }
                    IconButton(IconName.CLOSE, stringResource(R.string.students_unlink, name), {
                        links = unlink(links, link.placeId)
                    }, variant = IconButtonVariant.PLAIN)
                }
                if (!placePays) {
                    SegmentedControl(
                        listOf(false to stringResource(R.string.students_fromPlace), true to stringResource(R.string.students_custom)),
                        custom,
                        { on ->
                            links =
                                links.map {
                                    if (it.placeId ==
                                        link.placeId
                                    ) {
                                        it.copy(rateCents = if (on) place.rateCents ?: 0 else null)
                                    } else {
                                        it
                                    }
                                }
                        },
                        name,
                        size = ControlSize.SM,
                    )
                    if (custom) {
                        MoneyField(
                            stringResource(R.string.students_custom),
                            link.rateCents ?: 0,
                            { v -> links = links.map { if (it.placeId == link.placeId) it.copy(rateCents = v) else it } },
                            helper =
                                stringResource(
                                    if (d.rateType ==
                                        RateType.HOUR
                                    ) {
                                        R.string.places_perHourOption
                                    } else {
                                        R.string.places_perLessonOption
                                    },
                                ),
                        )
                    }
                }
            }
        }
        if (error ==
            CatalogError.PLACE_REQUIRED
        ) {
            FieldError(errorText(CatalogError.PLACE_REQUIRED), key = tries)
        }
        Button(stringResource(R.string.students_linkPlace), { picking = true }, variant = ButtonVariant.OUTLINE)
        Field(stringResource(R.string.students_color)) { ColorPicker(d.color) { d = d.copy(color = it) } }
        TextField(stringResource(R.string.students_notes), d.notes.orEmpty(), {
            d = d.copy(notes = it.ifEmpty { null })
        }, imeAction = ImeAction.Done)
        if (id != null) {
            ArchiveActions(
                d.name,
                d.archivedAt != null,
                onArchive = { scope.launch { services.catalog.archive(Kind.STUDENT, id).also { nav.popBackStack() } } },
                onRestore = { scope.launch { services.catalog.restore(Kind.STUDENT, id).also { nav.popBackStack() } } },
                onRemove = {
                    scope.launch {
                        services.catalog.removePermanently(Kind.STUDENT, id)
                        nav.navigate(RegistryHome()) { popUpTo<RegistryHome> { inclusive = true } }
                    }
                },
            )
        }
    }
    if (picking) {
        // Default places stay out of the picker when Ajustes hides them (AC-004-29).
        val showDefaults = settings?.showDefaultPlaces ?: true
        val available = places.filter { p -> links.none { it.placeId == p.place.id } && (showDefaults || p.place.builtin == null) }
        BottomSheet(stringResource(R.string.students_linkWhich), { picking = false }) {
            ListGroup(
                available.map { p ->
                    {
                        ListRow(placeName(p.place), subtitle = placePriceText(p.place), trailing = RowTrailing.NONE, onClick = {
                            links = withDefault(links + PlaceLink(p.place.id, false, null))
                            picking = false
                        })
                    }
                },
            )
        }
    }
}

/** The seven color tags as a radio row. */
@Composable
private fun ColorPicker(
    selected: TagColor,
    onPick: (TagColor) -> Unit,
) {
    val names =
        mapOf(
            TagColor.AMBER to R.string.students_colors_amber,
            TagColor.GREEN to R.string.students_colors_green,
            TagColor.BLUE to R.string.students_colors_blue,
            TagColor.VIOLET to R.string.students_colors_violet,
            TagColor.ORANGE to R.string.students_colors_orange,
            TagColor.RED to R.string.students_colors_red,
            TagColor.TEAL to R.string.students_colors_teal,
        )
    FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        TagColor.entries.forEach { color ->
            val label = stringResource(names.getValue(color))
            Box(
                Modifier
                    .size(Size.touch)
                    .clip(CircleShape)
                    .border(Size.borderStrong, if (color == selected) TutoraTheme.colors.primary else Color.Transparent, CircleShape)
                    .selectable(selected = color == selected, role = Role.RadioButton, onClick = { onPick(color) })
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) { TagDot(color.tag()) }
        }
    }
}

/** Nova turma / Editar turma: optional members from the place's students; no price at a school (AC-004-16). */
@Composable
fun GroupEditScreen(
    nav: NavHostController,
    id: String?,
    placeId: String?,
) {
    val services = LocalServices.current
    var loaded by remember { mutableStateOf<Group?>(null) }
    androidx.compose.runtime.LaunchedEffect(id) {
        loaded = if (id != null) services.catalogRepo.group(id) else Group("", placeId.orEmpty(), "", null, null, emptyList(), null)
    }
    val initial = loaded ?: return
    GroupForm(nav, id, initial)
}

@Composable
private fun GroupForm(
    nav: NavHostController,
    id: String?,
    initial: Group,
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    var d by remember(initial) { mutableStateOf(initial) }
    var error by remember { mutableStateOf<CatalogError?>(null) }
    // Each Save that fails counts, so the same error blinks again.
    var tries by remember { mutableIntStateOf(0) }
    val place by services.catalogRepo.place(d.placeId).collectAsState(initial = null)
    val students by services.catalogRepo.studentsAt(d.placeId).collectAsState(initial = emptyList())
    val school = place?.kind == PlaceKind.SCHOOL
    ScreenScaffold(
        stringResource(
            if (id !=
                null
            ) {
                R.string.groups_edit
            } else {
                R.string.groups_new
            },
        ),
        "group-edit",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
        bottomBar = {
            Button(stringResource(R.string.catalog_save), {
                tries++
                scope.launch {
                    when (val r = services.catalog.saveGroup(d)) {
                        is Result.Ok -> nav.popBackStack()
                        is Result.Err -> error = r.error
                    }
                }
            }, fullWidth = true)
        },
    ) {
        place?.let { Text(placeName(it), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted) }
        TextField(stringResource(R.string.groups_name), d.name, {
            d = d.copy(name = it)
        }, placeholder = stringResource(R.string.groups_namePlaceholder), error = error?.let { errorText(it) }, errorKey = tries)
        TextField(stringResource(R.string.groups_subject), d.subject.orEmpty(), { d = d.copy(subject = it.ifEmpty { null }) })
        if (school) {
            Text(stringResource(R.string.groups_coveredBySalary), style = TutoraTheme.type.body, color = TutoraTheme.colors.inkMuted)
        } else {
            MoneyField(
                stringResource(R.string.groups_price),
                d.rateCents ?: 0,
                { d = d.copy(rateCents = it) },
                helper = stringResource(if (d.rateCents == null) R.string.places_noPrice else R.string.places_perLessonOption),
            )
        }
        Field(stringResource(R.string.groups_members)) {
            Text(stringResource(R.string.groups_membersHint), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
            students.forEach { s ->
                Checkbox(s.name, s.id in d.memberIds, { on -> d = d.copy(memberIds = if (on) d.memberIds + s.id else d.memberIds - s.id) })
            }
        }
        if (id != null) {
            ArchiveActions(
                d.name,
                d.archivedAt != null,
                onArchive = { scope.launch { services.catalog.archive(Kind.GROUP, id).also { nav.popBackStack() } } },
                onRestore = { scope.launch { services.catalog.restore(Kind.GROUP, id).also { nav.popBackStack() } } },
                onRemove = { scope.launch { services.catalog.removePermanently(Kind.GROUP, id).also { nav.popBackStack() } } },
            )
        }
    }
}
