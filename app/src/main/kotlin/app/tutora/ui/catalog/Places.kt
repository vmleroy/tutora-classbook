package app.tutora.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.Card
import app.tutora.designsystem.components.Chip
import app.tutora.designsystem.components.ControlSize
import app.tutora.designsystem.components.FieldError
import app.tutora.designsystem.components.HeaderAction
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.MoneyField
import app.tutora.designsystem.components.RowTrailing
import app.tutora.designsystem.components.SectionHeader
import app.tutora.designsystem.components.SegmentedControl
import app.tutora.designsystem.components.Stepper
import app.tutora.designsystem.components.TextField
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.CatalogError
import app.tutora.domain.MAX_COMMISSION_BP
import app.tutora.domain.PayCycle
import app.tutora.domain.PayMode
import app.tutora.domain.Payer
import app.tutora.domain.Place
import app.tutora.domain.PlaceKind
import app.tutora.domain.Preview
import app.tutora.domain.RateType
import app.tutora.domain.TravelMode
import app.tutora.domain.builtinPlace
import app.tutora.domain.earningsType
import app.tutora.domain.initials
import app.tutora.domain.payMode
import app.tutora.domain.previewLesson
import app.tutora.state.LocalServices
import app.tutora.ui.GroupEdit
import app.tutora.ui.RegistryHome
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.StudentDetail
import app.tutora.ui.countText
import app.tutora.ui.errorText
import app.tutora.ui.kindName
import app.tutora.ui.minutesText
import app.tutora.ui.placeName
import app.tutora.ui.placePriceText
import app.tutora.ui.rateMoney
import app.tutora.ui.tag
import app.tutora.ui.travelText
import app.tutora.usecases.Kind
import app.tutora.usecases.Result
import kotlinx.coroutines.launch

/** A place: type, address, model, its students with + Ligar aluno, and its groups (AC-004-17). */
@Composable
fun PlaceDetailScreen(
    nav: NavHostController,
    id: String,
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val place by services.catalogRepo.place(id).collectAsState(initial = null)
    val students by services.catalogRepo.studentsAt(id).collectAsState(initial = emptyList())
    val allStudents by services.catalogRepo.students().collectAsState(initial = emptyList())
    val groups by services.catalogRepo.groups().collectAsState(initial = emptyList())
    var picking by rememberSaveable { mutableStateOf(false) }
    val p = place ?: return
    val language = currentLanguage()
    val mode = payMode(p.kind)
    ScreenScaffold(
        stringResource(R.string.places_detail),
        "place-detail",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
        actions =
            listOf(
                HeaderAction(IconName.PENCIL, stringResource(R.string.catalog_edit)) {
                    nav.navigate(
                        app.tutora.ui
                            .PlaceEdit(p.id),
                    )
                },
            ),
    ) {
        Card {
            Text(placeName(p), style = TutoraTheme.type.titleLg, color = TutoraTheme.colors.ink)
            Text(
                listOfNotNull(kindName(p.kind), p.address).joinToString(" · "),
                style = TutoraTheme.type.meta,
                color = TutoraTheme.colors.inkMuted,
            )
        }
        Card(label = stringResource(R.string.places_model)) {
            Text(stringResource(R.string.places_model), style = TutoraTheme.type.caption, color = TutoraTheme.colors.inkMuted)
            val who = stringResource(if (p.payer == Payer.PLACE) R.string.places_whoPlace else R.string.places_whoStudent)
            val cycle =
                stringResource(
                    when (p.payCycle) {
                        PayCycle.LESSON -> R.string.places_cycles_lesson
                        PayCycle.WEEKLY -> R.string.places_cycles_weekly
                        PayCycle.MONTHLY -> R.string.places_cycles_monthly
                    },
                )
            listOfNotNull(
                if (mode == PayMode.CHOOSE) stringResource(R.string.places_whoPaysValue, who) else null,
                placePriceText(p),
                if (p.payer == Payer.STUDENT &&
                    p.commissionBp > 0
                ) {
                    "${stringResource(R.string.places_commission)} ${p.commissionBp / 100}%"
                } else {
                    null
                },
                minutesText(p.defaultDurationMin),
                if (mode == PayMode.CHOOSE && p.payer == Payer.PLACE) cycle else null,
                if (p.kind == PlaceKind.STUDENT_HOME) travelText(p) else null,
            ).forEach { Text(it, style = TutoraTheme.type.body, color = TutoraTheme.colors.ink) }
        }
        SectionHeader("${stringResource(R.string.places_studentsHere)} · ${students.size}")
        if (students.isNotEmpty()) {
            ListGroup(
                students.map { s ->
                    {
                        ListRow(
                            s.name,
                            subtitle = s.subject,
                            avatar = initials(s.name) to s.color.tag(),
                            onClick = { nav.navigate(StudentDetail(s.id)) },
                        )
                    }
                },
            )
        }
        Button(stringResource(R.string.places_linkStudent), { picking = true }, variant = ButtonVariant.OUTLINE)
        SectionHeader(stringResource(R.string.places_groups))
        val here = groups.filter { it.placeId == p.id }
        if (here.isNotEmpty()) {
            ListGroup(
                here.map { g ->
                    val subtitle =
                        listOfNotNull(
                            g.subject,
                            countText(R.string.groups_size_zero, R.plurals.groups_size, g.memberIds.size),
                            g.rateCents?.let { stringResource(R.string.places_perLesson, rateMoney(it, language)) },
                        ).joinToString(" · ")
                    val row: @Composable () -> Unit = {
                        ListRow(
                            g.name,
                            subtitle = subtitle,
                            onClick = { nav.navigate(GroupEdit(id = g.id)) },
                        )
                    }
                    row
                },
            )
        }
        Button(stringResource(R.string.places_newGroup), { nav.navigate(GroupEdit(placeId = p.id)) }, variant = ButtonVariant.OUTLINE)
    }
    if (picking) {
        val linked = students.map { it.id }.toSet()
        val unlinked = allStudents.filter { it.student.id !in linked }
        BottomSheet(stringResource(R.string.places_linkWhich), { picking = false }) {
            if (unlinked.isEmpty()) {
                Text(stringResource(R.string.places_noStudentsToLink), style = TutoraTheme.type.body, color = TutoraTheme.colors.inkMuted)
            } else {
                ListGroup(
                    unlinked.map { s ->
                        {
                            ListRow(
                                s.student.name,
                                avatar = initials(s.student.name) to s.student.color.tag(),
                                trailing = RowTrailing.NONE,
                                onClick = {
                                    scope.launch { services.catalog.linkStudentToPlace(s.student.id, p.id) }
                                    picking = false
                                },
                            )
                        }
                    },
                )
            }
        }
    }
}

private val newPlace =
    builtinPlace(
        BuiltinPlace.ONLINE,
    ).copy(
        id = "",
        builtin = null,
        name = "",
        kind = PlaceKind.SALINHA,
        payer = Payer.PLACE,
        payCycle = PayCycle.MONTHLY,
        travelMode = TravelMode.NONE,
    )

/** Editar local / Novo local, with a live preview of a 60-minute lesson (AC-004-2 to 9). */
@Composable
fun PlaceEditScreen(
    nav: NavHostController,
    id: String?,
) {
    val services = LocalServices.current
    val saved by remember(id) {
        if (id ==
            null
        ) {
            kotlinx.coroutines.flow.flowOf(newPlace)
        } else {
            services.catalogRepo.place(id)
        }
    }.collectAsState(initial = null)
    val initial = saved ?: return
    PlaceForm(nav, id, initial)
}

@Composable
private fun PlaceForm(
    nav: NavHostController,
    id: String?,
    initial: Place,
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    var d by remember(initial) { mutableStateOf(initial) }
    var error by remember { mutableStateOf<CatalogError?>(null) }
    // Each Save that fails counts, so the same error blinks again.
    var tries by remember { mutableIntStateOf(0) }
    val language = currentLanguage()
    val builtin = d.builtin != null
    val mode = payMode(d.kind)
    ScreenScaffold(
        stringResource(
            if (id !=
                null
            ) {
                R.string.places_edit
            } else {
                R.string.places_new
            },
        ),
        "place-edit",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
        bottomBar = {
            Button(stringResource(R.string.places_save), {
                tries++
                scope.launch {
                    when (val r = services.catalog.savePlace(d)) {
                        is Result.Ok -> nav.popBackStack()
                        is Result.Err -> error = r.error
                    }
                }
            }, fullWidth = true)
        },
    ) {
        if (builtin) {
            Text(placeName(d), style = TutoraTheme.type.titleLg, color = TutoraTheme.colors.ink)
        } else {
            RequiredLegend()
            TextField(
                stringResource(R.string.places_name),
                d.name.orEmpty(),
                {
                    d = d.copy(name = it)
                },
                placeholder =
                    stringResource(
                        R.string.places_namePlaceholder,
                    ),
                error = error?.let { errorText(it) },
                errorKey = tries,
                required = true,
            )
            TextField(stringResource(R.string.places_address), d.address.orEmpty(), { d = d.copy(address = it.ifEmpty { null }) })
            Field(stringResource(R.string.places_type)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                    PlaceKind.own.forEach { k ->
                        Chip(kindName(k), d.kind == k, {
                            d =
                                d.copy(kind = k, payer = if (payMode(k) == PayMode.CHOOSE) d.payer else Payer.STUDENT)
                        })
                    }
                }
                Text(
                    stringResource(
                        if (earningsType(d.kind) ==
                            app.tutora.domain.EarningsType.JOB
                        ) {
                            R.string.places_earnings_job
                        } else {
                            R.string.places_earnings_private
                        },
                    ),
                    style = TutoraTheme.type.meta,
                    color = TutoraTheme.colors.inkMuted,
                )
            }
        }
        if (mode == PayMode.SALARY) {
            MoneyField(stringResource(R.string.places_salaryLabel), d.monthlySalaryCents ?: 0, { d = d.copy(monthlySalaryCents = it) })
        } else {
            if (mode == PayMode.CHOOSE) {
                Field(stringResource(R.string.places_whoPays)) {
                    SegmentedControl(
                        listOf(
                            Payer.PLACE to stringResource(R.string.places_payerPlace),
                            Payer.STUDENT to stringResource(R.string.places_payerStudent),
                        ),
                        d.payer,
                        { d = d.copy(payer = it) },
                        stringResource(R.string.places_whoPays),
                    )
                }
            }
            Field(stringResource(R.string.places_charging)) {
                SegmentedControl(
                    listOf(
                        RateType.HOUR to stringResource(R.string.places_perHourOption),
                        RateType.LESSON to stringResource(R.string.places_perLessonOption),
                    ),
                    d.rateType,
                    { d = d.copy(rateType = it) },
                    stringResource(R.string.places_charging),
                    size = ControlSize.SM,
                )
            }
            val rateLabel = stringResource(if (d.payer == Payer.PLACE) R.string.places_placePays else R.string.places_studentPays)
            MoneyField(
                rateLabel,
                d.rateCents ?: 0,
                { d = d.copy(rateCents = it) },
                helper =
                    stringResource(
                        when {
                            d.rateCents == null -> R.string.places_noPrice
                            d.rateType == RateType.HOUR -> R.string.places_perHourOption
                            else -> R.string.places_perLessonOption
                        },
                    ),
            )
            if (d.payer == Payer.STUDENT && mode == PayMode.CHOOSE) {
                Field(stringResource(R.string.places_commission)) {
                    Stepper(d.commissionBp.toLong(), {
                        d = d.copy(commissionBp = it.toInt())
                    }, step = 500, min = 0, max = MAX_COMMISSION_BP.toLong(), format = { "${it / 100}%" })
                }
            }
            if (d.payer == Payer.PLACE) {
                Field(stringResource(R.string.places_cycle)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                        listOf(
                            PayCycle.LESSON to R.string.places_cycles_lesson,
                            PayCycle.WEEKLY to R.string.places_cycles_weekly,
                            PayCycle.MONTHLY to R.string.places_cycles_monthly,
                        ).forEach { (c, label) -> Chip(stringResource(label), d.payCycle == c, { d = d.copy(payCycle = c) }) }
                    }
                }
            }
        }
        Field(stringResource(R.string.places_duration)) {
            DurationStepper(stringResource(R.string.places_duration), d.defaultDurationMin, {
                d =
                    d.copy(defaultDurationMin = it)
            })
        }
        if (d.kind == PlaceKind.STUDENT_HOME) {
            Field(stringResource(R.string.places_travelLabel)) {
                SegmentedControl(
                    listOf(
                        TravelMode.VARIES to stringResource(R.string.places_travelOptions_varies),
                        TravelMode.FIXED to stringResource(R.string.places_travelOptions_fixed),
                        TravelMode.NONE to stringResource(R.string.places_travelOptions_none),
                    ),
                    d.travelMode,
                    { d = d.copy(travelMode = it) },
                    stringResource(R.string.places_travelLabel),
                    size = ControlSize.SM,
                )
                if (d.travelMode ==
                    TravelMode.FIXED
                ) {
                    MoneyField(stringResource(R.string.places_travelFee), d.travelFeeCents, { d = d.copy(travelFeeCents = it) })
                }
            }
        }
        Card(label = stringResource(R.string.places_previewTitle)) {
            Text(stringResource(R.string.places_previewTitle), style = TutoraTheme.type.caption, color = TutoraTheme.colors.inkMuted)
            val cycleWord =
                stringResource(
                    when (d.payCycle) {
                        PayCycle.LESSON -> R.string.places_cycleWords_lesson
                        PayCycle.WEEKLY -> R.string.places_cycleWords_weekly
                        PayCycle.MONTHLY -> R.string.places_cycleWords_monthly
                    },
                )
            val text =
                when (val p = previewLesson(d, 60)) {
                    is Preview.PlacePays -> stringResource(R.string.places_preview_placePays, formatMoney(p.earned, language), cycleWord)
                    is Preview.StudentPays ->
                        stringResource(
                            R.string.places_preview_studentPays,
                            formatMoney(p.paid, language),
                            formatMoney(p.kept, language),
                        )
                    Preview.Salary -> stringResource(R.string.places_preview_salary)
                    Preview.Unset -> stringResource(R.string.places_preview_unset)
                }
            Text(text, style = TutoraTheme.type.bodyStrong, color = TutoraTheme.colors.ink, modifier = Modifier.testTag("place-preview"))
        }
        if (error != null && builtin) FieldError(errorText(error!!), key = tries)
        if (id != null && !builtin) {
            ArchiveActions(
                placeName(d),
                d.archivedAt != null,
                onArchive = { scope.launch { services.catalog.archive(Kind.PLACE, id).also { nav.popBackStack() } } },
                onRestore = { scope.launch { services.catalog.restore(Kind.PLACE, id).also { nav.popBackStack() } } },
                onRemove = {
                    scope.launch {
                        services.catalog.removePermanently(Kind.PLACE, id)
                        nav.navigate(RegistryHome("places")) { popUpTo<RegistryHome> { inclusive = true } }
                    }
                },
                removeBody = stringResource(R.string.catalog_removePlaceBody),
            )
        }
    }
}
