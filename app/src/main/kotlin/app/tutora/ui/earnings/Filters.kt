package app.tutora.ui.earnings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.tutora.R
import app.tutora.designsystem.components.Choice
import app.tutora.designsystem.components.ChoiceField
import app.tutora.designsystem.components.IconName
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

/** "Tipo": Tudo, Particulares or Escola e emprego, one of them, on Ganhos and Evolução (AC-019-7, 11). */
@Composable
fun TypeFilterField(
    filters: List<Pair<TypeFilter, Int>>,
    value: TypeFilter,
    onChange: (TypeFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChoiceField(
        stringResource(R.string.evo_type),
        filters.map { (f, label) -> Choice(f.name, stringResource(label)) },
        setOf(value.name),
        { chosen -> chosen.firstOrNull()?.let { onChange(TypeFilter.valueOf(it)) } },
        modifier.testTag("type-filter"),
        icon = IconName.WALLET,
    )
}

/** The kinds of place Evolução filters by (AC-014-35). */
val EVOLUTION_KINDS =
    listOf(PlaceKind.ONLINE, PlaceKind.STUDENT_HOME, PlaceKind.SCHOOL, PlaceKind.SALINHA, PlaceKind.COURSE, PlaceKind.PRIVATE)

/** "Local": any number of kinds of place; none is Todos os locais (AC-019-8). */
@Composable
fun PlaceKindField(
    value: Set<PlaceKind>,
    onChange: (Set<PlaceKind>) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChoiceField(
        stringResource(R.string.evo_place),
        EVOLUTION_KINDS.map { Choice(it.name, kindName(it)) },
        value.map { it.name }.toSet(),
        { chosen -> onChange(chosen.mapNotNull { k -> EVOLUTION_KINDS.firstOrNull { it.name == k } }.toSet()) },
        modifier.testTag("place-filter"),
        multiple = true,
        allLabel = stringResource(R.string.evo_allPlaces),
        icon = IconName.PIN,
    )
}
