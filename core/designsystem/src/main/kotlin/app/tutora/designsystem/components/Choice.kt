package app.tutora.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.tutora.designsystem.R
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import java.text.Normalizer

/** One option of a [ChoiceField]: a stable [key], the [label] shown and searched, and a [sub] line. */
data class Choice(
    val key: String,
    val label: String,
    val sub: String? = null,
)

/** How many chosen names the field spells out before "+N". */
private const val SHOWN_NAMES = 2

/**
 * A field showing what is chosen that opens a sheet of [choices], like a combo box. With
 * [multiple], any number can be checked and none checked reads [allLabel]; otherwise one is picked
 * and the sheet closes. With [searchLabel], a search box on top filters the list as it is typed,
 * ignoring accents and capitals.
 */
@Composable
fun ChoiceField(
    label: String,
    choices: List<Choice>,
    selected: Set<String>,
    onChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    multiple: Boolean = false,
    allLabel: String? = null,
    searchLabel: String? = null,
    icon: IconName? = null,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val names = choices.filter { it.key in selected }.map { it.label }
    val shown =
        when {
            names.isEmpty() -> allLabel.orEmpty()
            names.size <= SHOWN_NAMES -> names.joinToString(", ")
            else -> names.take(SHOWN_NAMES).joinToString(", ") + " +${names.size - SHOWN_NAMES}"
        }
    SelectField(shown, { open = true }, modifier, label = label, icon = icon)
    if (!open) return
    var query by rememberSaveable { mutableStateOf("") }
    val close = {
        open = false
        query = ""
    }
    BottomSheet(
        label,
        close,
        actions =
            if (multiple) {
                listOf { m -> Button(stringResource(R.string.ds_finish), close, m) }
            } else {
                emptyList()
            },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            if (searchLabel != null) TextField(searchLabel, query, { query = it }, Modifier.testTag("choice-search"))
            val wanted = folded(query)
            val visible = choices.filter { wanted.isEmpty() || folded(it.label).contains(wanted) }
            if (multiple) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                    // Todos clears the choice; it is checked while nothing else is.
                    if (allLabel != null && wanted.isEmpty()) Checkbox(allLabel, selected.isEmpty(), { onChange(emptySet()) })
                    visible.forEach { c ->
                        Checkbox(c.label, c.key in selected, { on -> onChange(if (on) selected + c.key else selected - c.key) })
                    }
                }
            } else {
                ListGroup(
                    visible.map { c ->
                        {
                            ListRow(
                                c.label,
                                subtitle = c.sub,
                                trailing = RowTrailing.NONE,
                                onClick = {
                                    onChange(setOf(c.key))
                                    close()
                                },
                            )
                        }
                    },
                )
            }
        }
    }
}

/** Lower case without accents, so "joao" finds "João". */
private fun folded(text: String): String =
    Normalizer
        .normalize(text.trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
