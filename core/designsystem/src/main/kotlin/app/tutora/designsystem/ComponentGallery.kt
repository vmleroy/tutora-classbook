package app.tutora.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.tutora.designsystem.components.AgendaBlock
import app.tutora.designsystem.components.AlertBanner
import app.tutora.designsystem.components.AlertTone
import app.tutora.designsystem.components.Avatar
import app.tutora.designsystem.components.AvatarSize
import app.tutora.designsystem.components.Badge
import app.tutora.designsystem.components.BadgeTone
import app.tutora.designsystem.components.BlockState
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.Card
import app.tutora.designsystem.components.CardElevation
import app.tutora.designsystem.components.Checkbox
import app.tutora.designsystem.components.Chip
import app.tutora.designsystem.components.ControlSize
import app.tutora.designsystem.components.DateField
import app.tutora.designsystem.components.DurationField
import app.tutora.designsystem.components.EarningsStat
import app.tutora.designsystem.components.EarningsSummary
import app.tutora.designsystem.components.EmptyState
import app.tutora.designsystem.components.Fab
import app.tutora.designsystem.components.FieldError
import app.tutora.designsystem.components.HeaderAction
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.Icon
import app.tutora.designsystem.components.IconButton
import app.tutora.designsystem.components.IconButtonVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.LessonRow
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.Money
import app.tutora.designsystem.components.MoneyField
import app.tutora.designsystem.components.MoneySize
import app.tutora.designsystem.components.MoneyTone
import app.tutora.designsystem.components.ProgressBar
import app.tutora.designsystem.components.RowTrailing
import app.tutora.designsystem.components.ScreenHeader
import app.tutora.designsystem.components.SectionHeader
import app.tutora.designsystem.components.SegmentedControl
import app.tutora.designsystem.components.SelectField
import app.tutora.designsystem.components.Stepper
import app.tutora.designsystem.components.Switch
import app.tutora.designsystem.components.TabBar
import app.tutora.designsystem.components.TabItem
import app.tutora.designsystem.components.Tag
import app.tutora.designsystem.components.TagDot
import app.tutora.designsystem.components.TextField
import app.tutora.designsystem.components.TileTone
import app.tutora.designsystem.components.TimeField
import app.tutora.designsystem.components.TimerRing
import app.tutora.designsystem.components.WeekDay
import app.tutora.designsystem.components.WeekStrip
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.ThemeMode
import app.tutora.domain.theme.themeIds
import kotlinx.datetime.LocalDate

/** The 30 components, in a section each. */
val galleryComponents =
    listOf(
        "Icon",
        "Button",
        "IconButton",
        "Fab",
        "SegmentedControl",
        "Chip",
        "Switch",
        "Checkbox",
        "Stepper",
        "TextField",
        "SelectField",
        "Badge",
        "Avatar",
        "TagDot",
        "Money",
        "ProgressBar",
        "Card",
        "EarningsSummary",
        "SectionHeader",
        "LessonRow",
        "ListGroup",
        "ListRow",
        "AlertBanner",
        "ScreenHeader",
        "TabBar",
        "WeekStrip",
        "AgendaBlock",
        "BottomSheet",
        "EmptyState",
        "TimerRing",
    )

/**
 * The developer screen (AC-002-15): every component in every variant, with theme, mode and
 * text-size switchers. Opened from the debug build only.
 */
@Composable
fun ComponentGallery(initialTheme: String = "quadro") {
    var theme by remember { mutableStateOf(initialTheme) }
    var mode by remember { mutableStateOf(ThemeMode.LIGHT) }
    var large by remember { mutableStateOf(false) }
    TutoraTheme(themeId = theme, mode = mode, largeText = large) {
        Column(
            Modifier
                .fillMaxSize()
                .background(TutoraTheme.colors.background)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.s5)
                .testTag("component-gallery"),
            verticalArrangement = Arrangement.spacedBy(Spacing.s5),
        ) {
            SegmentedControl(themeIds.map { it to it }, theme, { theme = it }, "Theme", size = ControlSize.SM)
            SegmentedControl(
                listOf(ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark"),
                mode,
                { mode = it },
                "Mode",
                size = ControlSize.SM,
            )
            SegmentedControl(listOf(false to "Normal", true to "Large"), large, { large = it }, "Text", size = ControlSize.SM)
            GallerySections()
        }
    }
}

@Composable
private fun Section(
    name: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.testTag("section-$name"), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Text(name, style = TutoraTheme.type.overline, color = TutoraTheme.colors.inkMuted)
        content()
    }
}

@Composable
fun GallerySections() {
    var segment by remember { mutableStateOf("mes") }
    var chip by remember { mutableStateOf(true) }
    var switch by remember { mutableStateOf(true) }
    var check by remember { mutableStateOf(true) }
    var minutes by remember { mutableLongStateOf(60L) }
    var cents by remember { mutableLongStateOf(12_000L) }
    var name by remember { mutableStateOf("") }
    var day by remember { mutableIntStateOf(2) }
    var sheet by remember { mutableStateOf(false) }
    val tags = Tag.entries
    Section("Icon") { FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) { IconName.entries.forEach { Icon(it) } } }
    Section("Button") {
        ButtonVariant.entries.forEach { Button(it.name.lowercase(), {}, variant = it) }
        Button("Salvar", {}, size = ControlSize.LG, icon = IconName.CHECK, fullWidth = true)
        Button("Carregando", {}, loading = true)
        Button("Desabilitado", {}, enabled = false)
    }
    Section("IconButton") { FlowRow { IconButtonVariant.entries.forEach { IconButton(IconName.PLUS, it.name, {}, variant = it) } } }
    Section("Fab") { Fab({}) }
    Section("SegmentedControl") {
        SegmentedControl(listOf("mes" to "Mês", "tri" to "Trimestre", "ano" to "Ano"), segment, { segment = it }, "Período")
        SegmentedControl(listOf("dia" to "Dia", "semana" to "Semana"), "dia", {}, "Visão", size = ControlSize.SM)
    }
    Section("Chip") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Chip("Online", chip, { chip = !chip })
            Chip("Casa do Aluno", false, {})
        }
    }
    Section("Switch") {
        Switch(switch, { switch = it }, "Cronômetro")
        Switch(false, {}, "Desligado", enabled = false)
    }
    Section("Checkbox") { Checkbox("Incluir pagamentos", check, { check = it }) }
    Section("Stepper") {
        Stepper(minutes, { minutes = it }, step = 15, min = 15, max = 300, format = { "$it min" })
        Stepper(500, {}, step = 500, size = ControlSize.SM, format = { "R$ ${it / 100},00" }, onPressValue = {}, valueLabel = "Valor")
    }
    Section("TextField") {
        TextField("Nome", name, { name = it }, placeholder = "Ex.: Ana Souza", helper = "Como aparece nos relatórios")
        TextField("Telefone", "(11) 9", {}, error = "Número incompleto")
        MoneyField("Valor da aula", cents, { cents = it ?: 0 })
        MoneyField("Preço por aula", null, {}, placeholder = "Sem preço")
        DurationField("Duração", 90, {})
        TextField("Nome", "", {}, error = "Digite o nome", required = true)
        FieldError("Escolha um aluno ou turma")
    }
    Section("SelectField") {
        SelectField("Ana Souza", {}, label = "Aluno", sub = "Matemática", avatar = "AS" to Tag.AMBER, actionLabel = "Trocar")
        SelectField("Seg, 26 out", {}, label = "Data", icon = IconName.CALENDAR)
        DateField("Data", "Seg, 26 out", LocalDate(2026, 10, 26), {})
        TimeField("Início", 14, 0, { _, _ -> })
    }
    Section("Badge") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) { BadgeTone.entries.forEach { Badge(it.name.lowercase(), tone = it) } }
    }
    Section("Avatar") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) { AvatarSize.entries.forEach { Avatar("AS", Tag.VIOLET, size = it) } }
    }
    Section("TagDot") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            tags.forEach { TagDot(it) }
            TagDot(Tag.RED, small = true)
        }
    }
    Section("Money") {
        Money(4737, size = MoneySize.DISPLAY, tone = MoneyTone.EARNINGS)
        Money(24_000, size = MoneySize.XL, tone = MoneyTone.EARNINGS)
        Money(18_000, size = MoneySize.LG)
        Money(-56_000, size = MoneySize.MD, tone = MoneyTone.OWED, compact = true)
        Money(12_000, size = MoneySize.SM, tone = MoneyTone.PAID, showSign = true)
    }
    Section("ProgressBar") {
        ProgressBar(0.63f, "Ganho em relação ao previsto")
        ProgressBar(0.3f, "Fino", thin = true)
    }
    Section("Card") {
        CardElevation.entries.forEach { e ->
            Card(elevation = e, label = e.name) { Text(e.name, style = TutoraTheme.type.body) }
        }
    }
    Section("EarningsSummary") {
        EarningsSummary(
            "Ganho hoje",
            24_000,
            expectedCents = 38_000,
            expectedLabel = "de R$ 380,00 previstos",
            stats =
                listOf(
                    EarningsStat("Semana", 118_000),
                    EarningsStat("Outubro", 502_000),
                    EarningsStat("Alunos devem", 56_000, MoneyTone.OWED),
                ),
        )
    }
    Section("SectionHeader") {
        SectionHeader("Aulas de hoje", aside = "4 aulas")
        SectionHeader("Pagamentos", aside = "Ver todos", asidePrimary = true)
    }
    Section("LessonRow") {
        LessonRow("10:00", Tag.GREEN, "Pedro Lima", subtitle = "Física · Presencial", trailing = "+ R$ 120", onClick = {})
        LessonRow("08:00", Tag.BLUE, "9º ano B", subtitle = "Colégio Santa Clara", status = BadgeTone.SUCCESS to "Concluída", onClick = {})
    }
    Section("ListGroup") {
        ListGroup(
            title = "Seus dados",
            rows =
                listOf(
                    { ListRow("Backup e planilha", subtitle = "Automático", icon = IconName.CLOUD, onClick = {}) },
                    { ListRow("Cronômetro ao vivo", subtitle = "Desligado", icon = IconName.CLOCK, trailing = RowTrailing.SWITCH) },
                ),
        )
    }
    Section("ListRow") {
        TileTone.entries.forEach { ListRow(it.name.lowercase(), icon = IconName.PIN, iconTone = it, onClick = {}) }
        ListRow("Ana Souza", subtitle = "Matemática", avatar = "AS" to Tag.VIOLET, badge = BadgeTone.DANGER to "Deve", onClick = {})
        ListRow("Sem seta", trailing = RowTrailing.NONE)
        ListRow("Pedro Lima", subtitle = "Física", avatar = "PL" to Tag.GREEN, trailing = RowTrailing.REMOVE, onClick = {}, onRemove = {})
    }
    Section("AlertBanner") {
        AlertTone.entries.forEach { AlertBanner("3 alunos com pagamento pendente", tone = it, value = "R$ 560", onClick = {}) }
    }
    Section("ScreenHeader") {
        HeaderVariant.entries.forEach { v ->
            ScreenHeader(
                v.name.lowercase(),
                variant = v,
                eyebrow = "Segunda",
                actions =
                    listOf(
                        HeaderAction(IconName.PLUS, "Nova aula", IconButtonVariant.ACCENT) {
                        },
                    ),
                onBack =
                    if (v ==
                        HeaderVariant.PUSH
                    ) {
                        ({})
                    } else {
                        null
                    },
            )
        }
    }
    Section("TabBar") {
        TabBar(
            listOf(
                TabItem("today", "Hoje", IconName.HOME),
                TabItem("schedule", "Agenda", IconName.CALENDAR),
                TabItem("registry", "Cadastro", IconName.USERS),
                TabItem("earnings", "Ganhos", IconName.WALLET),
                TabItem("settings", "Ajustes", IconName.GEAR),
            ),
            active = "today",
            onSelect = {},
        )
    }
    Section("WeekStrip") {
        WeekStrip(
            listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom").mapIndexed { i, w ->
                WeekDay(
                    w,
                    26 + i,
                    if (i % 2 ==
                        0
                    ) {
                        tags[i % tags.size]
                    } else {
                        null
                    },
                )
            },
            selected = day,
            onSelect = { day = it },
            today = 0,
        )
    }
    Section("AgendaBlock") {
        BlockState.entries.forEach {
            AgendaBlock(Tag.GREEN, "Ana Souza", subtitle = "Matemática", state = it, height = Size.agendaHour, onClick = {})
        }
    }
    Section("BottomSheet") {
        Button("Abrir folha", { sheet = true }, variant = ButtonVariant.OUTLINE)
        if (sheet) {
            BottomSheet(
                "Remover aula?",
                { sheet = false },
                subtitle = "Isso não pode ser desfeito.",
                actions =
                    listOf({ m -> Button("Cancelar", { sheet = false }, m, variant = ButtonVariant.OUTLINE) }, { m ->
                        Button("Remover", {
                            sheet =
                                false
                        }, m, variant = ButtonVariant.DANGER)
                    }),
            )
        }
    }
    Section("EmptyState") {
        EmptyState(
            "Nenhuma aula hoje",
            icon = IconName.CALENDAR,
            body = "As aulas do dia aparecem aqui.",
            action = { Button("Nova aula", {}) },
        )
    }
    Section("TimerRing") {
        Column(Modifier.background(TutoraTheme.colors.header).padding(Spacing.s4)) { TimerRing("47:12", 0.78f, caption = "de 60 min") }
    }
}
