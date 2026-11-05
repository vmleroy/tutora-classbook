package app.tutora.ui.earnings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.tutora.R
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.RowTrailing
import app.tutora.designsystem.components.SelectField
import app.tutora.domain.EarningsType
import app.tutora.domain.PlaceKind
import app.tutora.domain.Teaching
import app.tutora.domain.TypeFilter
import app.tutora.domain.earningsType
import app.tutora.state.LocalServices
import app.tutora.ui.kindName

/**
 * The type filters this teacher sees: Tudo always, Particulares and Escola e emprego as they
 * teach ("Como você dá aula") or have earnings of that kind (AC-014-19, 20).
 */
@Composable
fun rememberTypeFilters(): List<Pair<TypeFilter, Int>> {
    val services = LocalServices.current
    val settings by services.settings.collectAsState(initial = null)
    val inUse by remember { services.kindsInUse() }.collectAsState(initial = emptySet())
    val chosen = settings?.teaching ?: Teaching.entries.toSet()
    return listOfNotNull(
        TypeFilter.ALL to R.string.earn_all,
        (TypeFilter.PRIVATE to R.string.earn_private).takeIf {
            Teaching.PRIVATE in chosen || inUse.any { earningsType(it) == EarningsType.PRIVATE }
        },
        (TypeFilter.JOB to R.string.earn_job).takeIf {
            Teaching.SCHOOL in chosen || inUse.any { earningsType(it) == EarningsType.JOB }
        },
    )
}

/** "Tipo": Tudo, Particulares or Escola e emprego, as a dropdown on Ganhos and Evolução (AC-019-7, 11). */
@Composable
fun TypeFilterField(
    filters: List<Pair<TypeFilter, Int>>,
    value: TypeFilter,
    onChange: (TypeFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChoiceField(
        stringResource(R.string.evo_type),
        IconName.WALLET,
        filters.map { (f, label) -> f to stringResource(label) },
        value,
        onChange,
        modifier.testTag("type-filter"),
    )
}

/** The kinds of place Evolução filters by (AC-014-35). */
val EVOLUTION_KINDS =
    listOf(PlaceKind.ONLINE, PlaceKind.STUDENT_HOME, PlaceKind.SCHOOL, PlaceKind.SALINHA, PlaceKind.COURSE, PlaceKind.PRIVATE)

/** "Local": Todos os locais or one kind of place, as a dropdown (AC-019-8). */
@Composable
fun PlaceKindField(
    value: PlaceKind?,
    onChange: (PlaceKind?) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChoiceField(
        stringResource(R.string.evo_place),
        IconName.PIN,
        listOf<Pair<PlaceKind?, String>>(null to stringResource(R.string.evo_allPlaces)) + EVOLUTION_KINDS.map { it to kindName(it) },
        value,
        onChange,
        modifier.testTag("place-filter"),
    )
}

/** A field showing the chosen option that opens a sheet of all of them. */
@Composable
private fun <T> ChoiceField(
    label: String,
    icon: IconName,
    options: List<Pair<T, String>>,
    value: T,
    onChange: (T) -> Unit,
    modifier: Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    SelectField(options.firstOrNull { it.first == value }?.second.orEmpty(), { open = true }, modifier, label = label, icon = icon)
    if (open) {
        BottomSheet(label, { open = false }) {
            ListGroup(
                options.map { (option, name) ->
                    {
                        ListRow(
                            name,
                            trailing = RowTrailing.NONE,
                            onClick = {
                                onChange(option)
                                open = false
                            },
                        )
                    }
                },
            )
        }
    }
}
