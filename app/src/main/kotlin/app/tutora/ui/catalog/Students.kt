package app.tutora.ui.catalog

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.Avatar
import app.tutora.designsystem.components.AvatarSize
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.Card
import app.tutora.designsystem.components.CardElevation
import app.tutora.designsystem.components.Choice
import app.tutora.designsystem.components.ChoiceField
import app.tutora.designsystem.components.ControlSize
import app.tutora.designsystem.components.FieldError
import app.tutora.designsystem.components.HeaderAction
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconButton
import app.tutora.designsystem.components.IconButtonVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.MoneyField
import app.tutora.designsystem.components.NumberField
import app.tutora.designsystem.components.RowTrailing
import app.tutora.designsystem.components.SectionHeader
import app.tutora.designsystem.components.SegmentedControl
import app.tutora.designsystem.components.Tag
import app.tutora.designsystem.components.TagDot
import app.tutora.designsystem.components.TextField
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.CatalogError
import app.tutora.domain.Group
import app.tutora.domain.MemberShare
import app.tutora.domain.Payer
import app.tutora.domain.PayerKind
import app.tutora.domain.PayerRef
import app.tutora.domain.PlaceKind
import app.tutora.domain.PlaceLink
import app.tutora.domain.Price
import app.tutora.domain.RateType
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.domain.Who
import app.tutora.domain.canSplit
import app.tutora.domain.initials
import app.tutora.domain.memberDues
import app.tutora.domain.randomTag
import app.tutora.domain.resolvePrice
import app.tutora.domain.sum
import app.tutora.domain.unlink
import app.tutora.domain.withDefault
import app.tutora.state.LocalServices
import app.tutora.ui.GroupDetail
import app.tutora.ui.GroupEdit
import app.tutora.ui.PlaceDetail
import app.tutora.ui.RegistryHome
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.StudentDetail
import app.tutora.ui.errorText
import app.tutora.ui.kindName
import app.tutora.ui.payments.CreditsSection
import app.tutora.ui.payments.PackageCard
import app.tutora.ui.payments.PayerBalance
import app.tutora.ui.payments.PayerHistory
import app.tutora.ui.placeName
import app.tutora.ui.placePriceText
import app.tutora.ui.rateMoney
import app.tutora.ui.rateText
import app.tutora.ui.tag
import app.tutora.usecases.Kind
import app.tutora.usecases.Result
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** A student: contact, rate and places, which open the place (AC-004-15). */
@Composable
fun StudentDetailScreen(
    nav: NavHostController,
    id: String,
    /** Back, or closing Cadastro's right side when the phone is sideways (AC-017-11). */
    onBack: () -> Unit = { nav.popBackStack() },
) {
    val openPage = app.tutora.ui.rememberOpenPage(nav)
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val item by remember(id) { services.catalogRepo.student(id) }.collectAsState(initial = null)
    val places by remember { services.catalogRepo.places() }.collectAsState(initial = emptyList())
    val groups by remember { services.catalogRepo.groups() }.collectAsState(initial = emptyList())
    val settings by services.settings.collectAsState(initial = null)
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    val s = item ?: return
    val archived = s.student.archivedAt != null
    val names = places.associate { it.place.id to placeName(it.place) }
    val myGroups = groups.filter { id in it.memberIds }
    val byId = places.associateBy { it.place.id }
    ScreenScaffold(
        stringResource(R.string.students_detail),
        "student-detail",
        variant = HeaderVariant.PUSH,
        onBack = onBack,
        // Arquivar sits beside the pencil (AC-004-23).
        actions =
            listOfNotNull(
                if (!archived) {
                    HeaderAction(IconName.ARCHIVE, stringResource(R.string.catalog_archive)) {
                        scope.launch { services.catalog.archive(Kind.STUDENT, id).also { onBack() } }
                    }
                } else {
                    null
                },
            ),
        trailing = { EditButton { nav.navigate(app.tutora.ui.StudentEdit(id)) } },
    ) {
        if (archived) {
            ArchiveActions(
                s.student.name,
                archived = true,
                onArchive = {},
                onRestore = { scope.launch { services.catalog.restore(Kind.STUDENT, id) } },
                onRemove = {
                    scope.launch {
                        services.catalog.removePermanently(Kind.STUDENT, id)
                        nav.navigate(RegistryHome()) { popUpTo<RegistryHome> { inclusive = true } }
                    }
                },
            )
        }
        // As on the StudentDetail mockup: who, three figures, then the actions (Victor, 29 Oct 2026).
        DetailIdentity(s.student.name, listOfNotNull(s.student.subject, s.student.phone).joinToString(" · ")) {
            Avatar(initials(s.student.name), s.student.color.tag(), size = AvatarSize.LG)
        }
        s.student.guardian?.let {
            Text(stringResource(R.string.stu_guardianLine, it), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
        }
        s.student.notes?.let { Text(it, style = TutoraTheme.type.body, color = TutoraTheme.colors.inkMuted) }
        val defaultLink = s.links.firstOrNull { it.isDefault } ?: s.links.firstOrNull()
        val rate =
            defaultLink?.let { link ->
                byId[link.placeId]?.place?.let { place ->
                    when (val p = resolvePrice(place, s.student, link)) {
                        is Price.Set -> rateText(p.cents, p.rateType)
                        Price.Salary -> stringResource(R.string.groups_coveredBySalary)
                        Price.Unset -> stringResource(R.string.places_noPrice)
                    }
                }
            }
        // What the student owes, Registrar pagamento and Relatório, near the top (spec 008, AC-010-1).
        PayerBalance(
            PayerRef(PayerKind.STUDENT, id),
            stringResource(R.string.pay_record),
            stats =
                listOfNotNull(
                    monthStat { l ->
                        (l.who as? Who.OfStudent)?.student?.id == id ||
                            (l.who as? Who.OfGroup)?.group?.memberIds?.contains(id) == true
                    },
                    rate?.let { DetailStat(stringResource(R.string.pay_amount), it) },
                ),
            onReport = { nav.navigate(app.tutora.ui.Report("STUDENT", id)) },
            onNewLesson = { nav.navigate(app.tutora.ui.LessonNew()) },
        )
        if (s.student.rateType == RateType.PACKAGE) PackageCard(id, s.student.packagePriceCents ?: 0)
        CreditsSection(PayerRef(PayerKind.STUDENT, id))
        val elsewhere by remember(id) { services.paymentRepo.elsewhere(id) }.collectAsState(initial = emptyList())
        val language = currentLanguage()
        val placePays = stringResource(R.string.pay_byPlace)
        val paidText = stringResource(R.string.pay_allReceived)
        val salary = stringResource(R.string.groups_coveredBySalary)
        val unset = stringResource(R.string.places_noPrice)
        val name = s.student.name
        // Locais: the student's places, with their price there (AC-004-33).
        ConnectionSection(
            stringResource(R.string.students_places),
            s.links.mapNotNull { link ->
                val place = byId[link.placeId]?.place ?: return@mapNotNull null
                val price =
                    when (val p = resolvePrice(place, s.student, link)) {
                        is Price.Set -> rateText(p.cents, p.rateType)
                        Price.Salary -> salary
                        Price.Unset -> unset
                    }
                // A place that pays you for this student's classes says so, with what it still owes (AC-008-26).
                val owedThere = elsewhere.filter { it.payer.id == place.id }
                val paysNote =
                    if (owedThere.isEmpty()) {
                        null
                    } else {
                        val left = owedThere.sumOf { it.left }
                        listOf(
                            placePays,
                            if (left > 0) stringResource(R.string.pay_leftShort, formatMoney(left, language)) else paidText,
                        ).joinToString(" · ")
                    }
                Connection(
                    place.id,
                    placeName(place),
                    listOfNotNull(price, paysNote).joinToString(" · "),
                    icon = if (link.isDefault) IconName.STAR else IconName.PIN,
                )
            },
            stringResource(R.string.connections_noPlaces),
            if (archived) null else stringResource(R.string.connections_addPlace),
            { picking = "places" },
            { openPage(PlaceDetail(it.id)) },
            { c -> scope.launch { services.catalog.unlinkStudentFromPlace(id, c.id) } },
        ) { c -> unlinkConfirm(name, c.title, myGroups.filter { it.placeId == c.id }.map { it.name }) }
        // Turmas: joining one at another place links the student to it.
        ConnectionSection(
            stringResource(R.string.places_groups),
            myGroups.map { g ->
                Connection(g.id, g.name, listOfNotNull(g.subject, names[g.placeId]).joinToString(" · "), icon = IconName.USERS)
            },
            stringResource(R.string.connections_noGroups),
            if (archived) null else stringResource(R.string.connections_addGroup),
            { picking = "groups" },
            { openPage(GroupDetail(it.id)) },
            { c -> scope.launch { services.catalog.removeMember(c.id, id) } },
        ) { c -> RemoveConfirm(stringResource(R.string.connections_leaveGroupTitle, name, c.title)) }
        PayerHistory(nav, PayerRef(PayerKind.STUDENT, id))
    }
    when (picking) {
        "places" -> {
            val showDefaults = settings?.showDefaultPlaces ?: true
            PickerSheet(
                stringResource(R.string.connections_addPlace),
                places
                    .filter { p -> s.links.none { it.placeId == p.place.id } && (showDefaults || p.place.builtin == null) }
                    .map { Connection(it.place.id, placeName(it.place), placePriceText(it.place), icon = IconName.PIN) },
                { picking = null },
                { ids ->
                    scope.launch { services.catalog.linkStudents(ids.map { id to it }) }
                    picking = null
                },
            )
        }
        "groups" ->
            PickerSheet(
                stringResource(R.string.connections_addGroup),
                groups.filter { id !in it.memberIds }.map { g ->
                    Connection(g.id, g.name, listOfNotNull(g.subject, names[g.placeId]).joinToString(" · "), icon = IconName.USERS)
                },
                { picking = null },
                { ids ->
                    scope.launch { ids.forEach { services.catalog.addMembers(it, listOf(id)) } }
                    picking = null
                },
            )
    }
}

/** "Tirar Ana de Salinha?", naming the groups there she also leaves (AC-004-36). */
@Composable
fun unlinkConfirm(
    student: String,
    place: String,
    groupsThere: List<String>,
): RemoveConfirm =
    RemoveConfirm(
        stringResource(R.string.connections_unlinkTitle, student, place),
        if (groupsThere.isEmpty()) null else stringResource(R.string.connections_alsoLeaves, groupsThere.joinToString(", ")),
    )

/** A new package starts at 10 classes (AC-008-19). */
private const val PACKAGE_LESSONS = 10

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
                app.tutora.state
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
    val places by remember { services.catalogRepo.places() }.collectAsState(initial = emptyList())
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
        RequiredLegend()
        TextField(
            stringResource(R.string.students_name),
            d.name,
            { d = d.copy(name = it) },
            placeholder = stringResource(R.string.students_namePlaceholder),
            capitalization = KeyboardCapitalization.Words,
            error = error?.takeIf { it == CatalogError.NAME_REQUIRED }?.let { errorText(it) },
            errorKey = tries,
            required = true,
        )
        TextField(stringResource(R.string.students_subject), d.subject.orEmpty(), {
            d = d.copy(subject = it.ifEmpty { null })
        }, placeholder = stringResource(R.string.students_subjectPlaceholder))
        TextField(stringResource(R.string.students_phone), d.phone.orEmpty(), {
            d = d.copy(phone = it.ifEmpty { null })
        }, keyboardType = KeyboardType.Phone, capitalization = KeyboardCapitalization.None)
        // Responsável: who the Fatura is for, when not the student (AC-020-17).
        TextField(
            stringResource(R.string.stu_guardian),
            d.guardian.orEmpty(),
            {
                d = d.copy(guardian = it.ifEmpty { null })
            },
            Modifier.testTag(
                "student-guardian",
            ),
            helper = stringResource(R.string.stu_guardianHint),
            capitalization = KeyboardCapitalization.Words,
        )
        SectionHeader(stringResource(R.string.students_wherePays), aside = stringResource(R.string.students_starHint), required = true)
        SegmentedControl(
            listOf(
                RateType.HOUR to stringResource(R.string.students_perHour),
                RateType.LESSON to stringResource(R.string.students_perLesson),
                RateType.PACKAGE to stringResource(R.string.students_package),
            ),
            d.rateType,
            {
                d =
                    if (it ==
                        RateType.PACKAGE
                    ) {
                        d.copy(rateType = it, packageLessons = d.packageLessons ?: PACKAGE_LESSONS)
                    } else {
                        d.copy(rateType = it)
                    }
            },
            stringResource(R.string.students_charging),
            size = ControlSize.SM,
        )
        // Pacote: how many classes and the price for all of them (AC-008-19).
        if (d.rateType == RateType.PACKAGE) {
            Card(Modifier.testTag("package-terms")) {
                Text(stringResource(R.string.pay_packageHint), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
                NumberField(
                    stringResource(R.string.pay_packageLessons),
                    (d.packageLessons ?: PACKAGE_LESSONS).toLong(),
                    { d = d.copy(packageLessons = it.toInt()) },
                    min = 1,
                    max = 100,
                )
                MoneyField(stringResource(R.string.pay_packagePrice), d.packagePriceCents, { d = d.copy(packagePriceCents = it) })
            }
        }
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
                            link.rateCents,
                            { v -> links = links.map { if (it.placeId == link.placeId) it.copy(rateCents = v ?: 0) else it } },
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

/** A group in full: place, price and members; the pencil edits, Arquivar sits beside it (AC-004-23, 30). */
@Composable
fun GroupDetailScreen(
    nav: NavHostController,
    id: String,
    /** Back, or closing Cadastro's right side when the phone is sideways (AC-017-11). */
    onBack: () -> Unit = { nav.popBackStack() },
) {
    val openPage = app.tutora.ui.rememberOpenPage(nav)
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val language = currentLanguage()
    val group by remember(id) { services.catalogRepo.groupFlow(id) }.collectAsState(initial = null)
    val everyone by remember { services.catalogRepo.students() }.collectAsState(initial = emptyList())
    val places by remember { services.catalogRepo.places() }.collectAsState(initial = emptyList())
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    var moving by rememberSaveable { mutableStateOf<String?>(null) }
    var sharing by rememberSaveable { mutableStateOf<String?>(null) }
    val g = group ?: return
    val place by remember(g.placeId) { services.catalogRepo.place(g.placeId) }.collectAsState(initial = null)
    val archived = g.archivedAt != null
    val members = everyone.map { it.student }.filter { it.id in g.memberIds }
    ScreenScaffold(
        stringResource(R.string.groups_detail),
        "group-detail",
        variant = HeaderVariant.PUSH,
        onBack = onBack,
        actions =
            listOfNotNull(
                if (!archived) {
                    HeaderAction(IconName.ARCHIVE, stringResource(R.string.catalog_archive)) {
                        scope.launch { services.catalog.archive(Kind.GROUP, id).also { onBack() } }
                    }
                } else {
                    null
                },
            ),
        trailing = { EditButton { nav.navigate(GroupEdit(id = id)) } },
    ) {
        if (archived) {
            ArchiveActions(
                g.name,
                archived = true,
                onArchive = {},
                onRestore = { scope.launch { services.catalog.restore(Kind.GROUP, id) } },
                onRemove = { scope.launch { services.catalog.removePermanently(Kind.GROUP, id).also { onBack() } } },
            )
        }
        DetailIdentity(g.name, listOfNotNull(g.subject, place?.let { placeName(it) }).joinToString(" · ")) {
            Avatar(initials(g.name), Tag.TEAL, size = AvatarSize.LG)
        }
        val price =
            DetailStat(
                stringResource(R.string.pay_amount),
                when {
                    place?.kind == PlaceKind.SCHOOL -> stringResource(R.string.groups_coveredBySalary)
                    g.rateCents != null -> stringResource(R.string.places_perLesson, rateMoney(g.rateCents ?: 0, language))
                    else -> stringResource(R.string.places_noPrice)
                },
            )
        val month = monthStat { l -> (l.who as? Who.OfGroup)?.group?.id == id }
        val report = { nav.navigate(app.tutora.ui.Report("GROUP", id)) }
        val newLesson = { nav.navigate(app.tutora.ui.LessonNew()) }
        // A group owes as one, unless its place pays the teacher or a salary covers it (AC-008-1).
        val groupOwes = place?.let { it.payer == Payer.PLACE || it.kind == PlaceKind.SCHOOL } == false
        if (groupOwes && g.splitSince != null) {
            // Split: what the members owe for this group, paid by several at once (AC-020-40).
            app.tutora.ui.payments.GroupSplitBalance(
                id,
                byName(members).map { it.id to it.name },
                stringResource(R.string.pay_record),
                listOf(month, price),
                report,
                newLesson,
            )
        } else if (groupOwes) {
            PayerBalance(
                PayerRef(PayerKind.GROUP, id),
                stringResource(R.string.pay_record),
                stats = listOf(month, price),
                onReport = report,
                onNewLesson = newLesson,
            )
        } else {
            DetailOverview(listOf(month, price), null, report, newLesson)
        }
        if (groupOwes) CreditsSection(PayerRef(PayerKind.GROUP, id))
        // Local: one place, moved with Trocar local; Alunos: the members (AC-004-35).
        ConnectionSection(
            stringResource(R.string.lesson_groupPlace),
            listOfNotNull(place?.let { Connection(it.id, placeName(it), placePriceText(it), icon = IconName.PIN, removable = false) }),
            stringResource(R.string.connections_noPlaces),
            if (archived) null else stringResource(R.string.connections_changePlace),
            { picking = "place" },
            { openPage(PlaceDetail(it.id)) },
            {},
        ) { RemoveConfirm(it.title) }
        ConnectionSection(
            stringResource(R.string.groups_members),
            members.map { st -> Connection(st.id, st.name, st.subject, avatar = initials(st.name) to st.color.tag()) },
            stringResource(R.string.groups_noMembers),
            if (archived) null else stringResource(R.string.groups_addStudent),
            { picking = "students" },
            { openPage(StudentDetail(it.id)) },
            { c -> scope.launch { services.catalog.removeMember(id, c.id) } },
        ) { c -> RemoveConfirm(stringResource(R.string.connections_leaveGroupTitle, c.title, g.name)) }
        // The price split between the members, each with an equal part or their own (AC-020-34, 35).
        if (groupOwes && !archived && place?.let { canSplit(members.size, it.kind, it.payer) } == true) {
            SplitSection(g, members, language) { sharing = it }
        }
        if (groupOwes) PayerHistory(nav, PayerRef(PayerKind.GROUP, id))
    }
    when (picking) {
        "place" ->
            PickerSheet(
                stringResource(R.string.connections_pickPlace),
                places
                    .filter {
                        it.place.id != g.placeId
                    }.map { Connection(it.place.id, placeName(it.place), placePriceText(it.place), icon = IconName.PIN) },
                { picking = null },
                { ids ->
                    moving = ids.firstOrNull()
                    picking = null
                },
                single = true,
            )
        "students" ->
            PickerSheet(
                stringResource(R.string.groups_addStudent),
                everyone.filter { it.student.id !in g.memberIds }.map { e ->
                    Connection(e.student.id, e.student.name, e.student.subject, avatar = initials(e.student.name) to e.student.color.tag())
                },
                { picking = null },
                { ids ->
                    scope.launch { services.catalog.addMembers(id, ids) }
                    picking = null
                },
            )
    }
    sharing?.let { sid ->
        val member = members.firstOrNull { it.id == sid } ?: return@let
        ShareSheet(member.name, g.shares[sid], g.rateCents ?: 0, { sharing = null }) { share ->
            scope.launch { services.catalog.setShare(id, sid, share) }
            sharing = null
        }
    }
    moving?.let { target ->
        val to = places.firstOrNull { it.place.id == target }?.place ?: return@let
        MoveConfirm(placeName(to), listOf(g.name), { moving = null }) {
            scope.launch { services.catalog.moveGroup(id, target) }
            moving = null
        }
    }
}

/** "Mudar para Salinha?": what moving groups to a place does, before it happens (AC-004-34, 35). */
@Composable
fun MoveConfirm(
    place: String,
    groups: List<String>,
    onDismiss: () -> Unit,
    onMove: () -> Unit,
) {
    BottomSheet(
        stringResource(R.string.connections_moveTitle, place),
        onDismiss,
        subtitle = stringResource(R.string.connections_moveBody, groups.joinToString(", ")),
        actions =
            listOf(
                { m -> Button(stringResource(R.string.catalog_cancel), onDismiss, m, variant = ButtonVariant.OUTLINE) },
                { m -> Button(stringResource(R.string.connections_move), onMove, m) },
            ),
    )
}

/** Nova turma / Editar turma: name, subject, price and, when new, its place; no price at a school (AC-004-16, 37). */
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
    val place by remember(d.placeId) { services.catalogRepo.place(d.placeId) }.collectAsState(initial = null)
    val places by remember { services.catalogRepo.places() }.collectAsState(initial = emptyList())
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
        RequiredLegend()
        // A new group picks its place; an existing one moves from its detail (AC-004-35, 37).
        if (id == null) {
            // A list that grows: one field with a search box, not a row of chips (AC-020-38).
            ChoiceField(
                stringResource(R.string.lesson_groupPlace),
                places.map { p -> Choice(p.place.id, placeName(p.place), kindName(p.place.kind)) },
                setOfNotNull(d.placeId.takeIf { it.isNotEmpty() }),
                { picked -> picked.firstOrNull()?.let { d = d.copy(placeId = it) } },
                Modifier.testTag("group-place"),
                allLabel = stringResource(R.string.places_choose),
                searchLabel = stringResource(R.string.places_search),
                icon = IconName.PIN,
                required = true,
                isError = error == CatalogError.PLACE_REQUIRED,
                errorKey = tries,
            )
            if (error == CatalogError.PLACE_REQUIRED) FieldError(errorText(CatalogError.PLACE_REQUIRED), key = tries)
        } else {
            place?.let { Text(placeName(it), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted) }
        }
        TextField(
            stringResource(R.string.groups_name),
            d.name,
            {
                d = d.copy(name = it)
            },
            placeholder =
                stringResource(
                    R.string.groups_namePlaceholder,
                ),
            error = error?.takeIf { it == CatalogError.NAME_REQUIRED }?.let { errorText(it) },
            errorKey = tries,
            required = true,
        )
        TextField(stringResource(R.string.groups_subject), d.subject.orEmpty(), { d = d.copy(subject = it.ifEmpty { null }) })
        if (school) {
            Text(stringResource(R.string.groups_coveredBySalary), style = TutoraTheme.type.body, color = TutoraTheme.colors.inkMuted)
        } else {
            MoneyField(
                stringResource(R.string.groups_price),
                d.rateCents,
                { d = d.copy(rateCents = it) },
                placeholder = stringResource(R.string.places_noPrice),
                helper = stringResource(R.string.places_perLessonOption),
            )
        }
    }
}

/** Members sorted as people read names, as the split orders them (AC-020-34). */
private fun byName(members: List<Student>) =
    members.sortedBy {
        java.text.Normalizer
            .normalize(it.name, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .lowercase()
    }

/**
 * "Dividir o valor entre os alunos": on, every class given from now on is owed by the members,
 * each their share; each member's row says what they owe per class and opens their share (AC-020-34, 35).
 */
@Composable
private fun SplitSection(
    g: Group,
    members: List<Student>,
    language: app.tutora.domain.Language,
    onShare: (String) -> Unit,
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val on = g.splitSince != null
    val price = g.rateCents ?: 0
    val sorted = byName(members)
    val dues = memberDues(price, sorted.map { it.id }, g.shares)
    SectionHeader(stringResource(R.string.grp_splitTitle))
    val shareLabels =
        sorted.associate { st ->
            st.id to
                when (val share = g.shares[st.id]) {
                    is MemberShare.Percent -> "${share.bp / 100}%"
                    is MemberShare.Amount -> stringResource(R.string.grp_shareAmount)
                    null -> stringResource(R.string.grp_shareEqual)
                }
        }
    val perLesson = stringResource(R.string.places_perLesson, "%s")
    val rows: List<@Composable () -> Unit> =
        listOf<@Composable () -> Unit> {
            ListRow(
                stringResource(R.string.grp_split),
                Modifier.testTag("group-split"),
                subtitle = stringResource(if (on) R.string.grp_splitHint else R.string.grp_splitOffHint),
                trailing = RowTrailing.SWITCH,
                switchValue = on,
                onSwitchChange = { v -> scope.launch { services.catalog.setSplit(g.id, v) } },
            )
        } +
            if (on) {
                sorted.map { st ->
                    @Composable {
                        ListRow(
                            st.name,
                            Modifier.testTag("share-${st.name}"),
                            subtitle = "${shareLabels[st.id]} · ${perLesson.replace("%s", formatMoney(dues[st.id] ?: 0, language))}",
                            avatar = initials(st.name) to st.color.tag(),
                            onClick = { onShare(st.id) },
                        )
                    }
                }
            } else {
                emptyList()
            }
    ListGroup(rows)
    if (on) {
        val custom = sorted.filter { it.id in g.shares }
        val covered = dues.values.sum()
        when {
            custom.isNotEmpty() && custom.size == sorted.size && covered < price ->
                Text(
                    stringResource(R.string.grp_uncovered, rateMoney(price - covered, language)),
                    style = TutoraTheme.type.meta,
                    color = TutoraTheme.colors.dangerInk,
                )
            custom.isNotEmpty() && sum(custom.map { dues[it.id] ?: 0 }) >= price && custom.size < sorted.size ->
                Text(stringResource(R.string.grp_over), style = TutoraTheme.type.meta, color = TutoraTheme.colors.dangerInk)
        }
    }
}

/** A member's share: an equal part, a percentage of each class or a set amount (AC-020-35). */
@Composable
private fun ShareSheet(
    name: String,
    current: MemberShare?,
    price: Long,
    onDismiss: () -> Unit,
    onSave: (MemberShare?) -> Unit,
) {
    var kind by rememberSaveable {
        mutableStateOf(
            when (current) {
                is MemberShare.Percent -> "percent"
                is MemberShare.Amount -> "amount"
                null -> "equal"
            },
        )
    }
    var percent by rememberSaveable { mutableStateOf(((current as? MemberShare.Percent)?.bp ?: 5_000) / 100L) }
    var amount by rememberSaveable { mutableStateOf((current as? MemberShare.Amount)?.cents ?: price / 2) }
    BottomSheet(
        stringResource(R.string.grp_shareTitle, name),
        onDismiss,
        actions =
            listOf { m ->
                Button(stringResource(R.string.catalog_save), {
                    onSave(
                        when (kind) {
                            "percent" -> MemberShare.Percent((percent * 100).toInt())
                            "amount" -> MemberShare.Amount(amount)
                            else -> null
                        },
                    )
                }, m.testTag("share-save"))
            },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            SegmentedControl(
                listOf(
                    "equal" to stringResource(R.string.grp_shareEqual),
                    "percent" to stringResource(R.string.grp_sharePercent),
                    "amount" to stringResource(R.string.grp_shareAmount),
                ),
                kind,
                { kind = it },
                stringResource(R.string.grp_shareTitle, name),
                size = ControlSize.SM,
            )
            when (kind) {
                "percent" ->
                    NumberField(
                        stringResource(R.string.grp_percentLabel),
                        percent,
                        { percent = it },
                        min = 1,
                        max = 100,
                        suffix = "%",
                    )
                "amount" -> MoneyField(stringResource(R.string.grp_amountLabel), amount, { amount = it ?: 0 })
                else -> Text(stringResource(R.string.grp_equalHint), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
            }
        }
    }
}
