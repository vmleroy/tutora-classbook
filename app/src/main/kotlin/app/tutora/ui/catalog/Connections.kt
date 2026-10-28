package app.tutora.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.Checkbox
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.RowTrailing
import app.tutora.designsystem.components.SectionHeader
import app.tutora.designsystem.components.Tag
import app.tutora.designsystem.components.TextField
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import java.text.Normalizer

// Students, places and groups manage each other the same way on every detail (AC-004-32): a
// section of connections with its count, rows that open the item, × to take one out after a
// confirmation, and "+ Adicionar" opening a searchable list to add several at once.

/** One connected item: a student, a place or a group. */
data class Connection(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val avatar: Pair<String, Tag>? = null,
    val icon: IconName? = null,
    /** False when it can't be taken out here (a group's place). */
    val removable: Boolean = true,
)

/** What the × confirmation says: its title and, when taking it out does more, a line on what. */
data class RemoveConfirm(
    val title: String,
    val body: String? = null,
)

@Composable
fun ConnectionSection(
    title: String,
    items: List<Connection>,
    empty: String,
    addLabel: String?,
    onAdd: () -> Unit,
    onOpen: (Connection) -> Unit,
    onRemove: (Connection) -> Unit,
    confirm: @Composable (Connection) -> RemoveConfirm,
) {
    var removing by remember { mutableStateOf<Connection?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        SectionHeader("$title · ${items.size}")
        if (items.isEmpty()) {
            Text(empty, style = TutoraTheme.type.body, color = TutoraTheme.colors.inkMuted)
        } else {
            ListGroup(
                items.map { c ->
                    {
                        ListRow(
                            c.title,
                            subtitle = c.subtitle,
                            avatar = c.avatar,
                            icon = c.icon,
                            trailing = if (c.removable) RowTrailing.REMOVE else RowTrailing.CHEVRON,
                            onClick = { onOpen(c) },
                            onRemove = { removing = c },
                        )
                    }
                },
            )
        }
        if (addLabel != null) Button(addLabel, onAdd, variant = ButtonVariant.OUTLINE)
    }
    removing?.let { c ->
        val text = confirm(c)
        BottomSheet(
            text.title,
            { removing = null },
            subtitle = text.body,
            actions =
                listOf(
                    { m -> Button(stringResource(R.string.catalog_cancel), { removing = null }, m, variant = ButtonVariant.OUTLINE) },
                    { m ->
                        Button(stringResource(R.string.connections_remove), {
                            removing = null
                            onRemove(c)
                        }, m, variant = ButtonVariant.DANGER)
                    },
                ),
        )
    }
}

/** Accent- and case-insensitive match, so "ana" finds "Ána". */
private fun fold(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()

/**
 * The "+ Adicionar" list: search, then tick several and add them at once; with [single], a tap
 * picks one (a group's place).
 */
@Composable
fun PickerSheet(
    title: String,
    options: List<Connection>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    single: Boolean = false,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var picked by remember { mutableStateOf(emptySet<String>()) }
    val q = fold(query.trim())
    val shown = options.filter { fold(it.title).contains(q) }
    BottomSheet(
        title,
        onDismiss,
        actions =
            if (single || options.isEmpty()) {
                emptyList()
            } else {
                listOf { m ->
                    Button(
                        if (picked.isEmpty()) {
                            stringResource(R.string.connections_pickSome)
                        } else {
                            stringResource(R.string.connections_add, picked.size)
                        },
                        { if (picked.isNotEmpty()) onConfirm(picked.toList()) },
                        m,
                    )
                }
            },
    ) {
        if (options.isEmpty()) {
            Text(stringResource(R.string.connections_nothingToAdd), style = TutoraTheme.type.body, color = TutoraTheme.colors.inkMuted)
            return@BottomSheet
        }
        if (options.size > SEARCH_FROM) TextField(stringResource(R.string.connections_search), query, { query = it })
        if (single) {
            ListGroup(
                shown.map { c ->
                    {
                        ListRow(
                            c.title,
                            subtitle = c.subtitle,
                            avatar = c.avatar,
                            icon = c.icon,
                            trailing = RowTrailing.NONE,
                            onClick = { onConfirm(listOf(c.id)) },
                        )
                    }
                },
            )
        } else {
            shown.forEach { c ->
                val label = listOfNotNull(c.title, c.subtitle).joinToString(" · ")
                Checkbox(label, c.id in picked, { on -> picked = if (on) picked + c.id else picked - c.id })
            }
        }
    }
}

// Short lists are read at a glance; the search field shows once there is something to search.
private const val SEARCH_FROM = 6
