package app.tutora.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.tutora.R
import app.tutora.designsystem.components.Tag
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.Cents
import app.tutora.domain.Language
import app.tutora.domain.Payer
import app.tutora.domain.Place
import app.tutora.domain.PlaceKind
import app.tutora.domain.RateType
import app.tutora.domain.TagColor
import app.tutora.domain.TravelMode
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// Text the screens build from domain values, in the app language.

fun TagColor.tag(): Tag = Tag.valueOf(name)

/** Built-in names follow the app language; names the teacher typed never change (AC-004-26). */
@Composable
@ReadOnlyComposable
fun placeName(place: Place): String =
    when (place.builtin) {
        BuiltinPlace.STUDENT_HOME -> stringResource(R.string.places_builtin_student_home)
        BuiltinPlace.ONLINE -> stringResource(R.string.places_builtin_online)
        null -> place.name.orEmpty()
    }

@Composable
@ReadOnlyComposable
fun kindName(kind: PlaceKind): String =
    stringResource(
        when (kind) {
            PlaceKind.SCHOOL -> R.string.places_kind_school
            PlaceKind.SALINHA -> R.string.places_kind_salinha
            PlaceKind.COURSE -> R.string.places_kind_course
            PlaceKind.PRIVATE -> R.string.places_kind_private
            PlaceKind.STUDENT_HOME -> R.string.places_kind_student_home
            PlaceKind.ONLINE -> R.string.places_kind_online
        },
    )

/** Whole reais read "R$ 120"; anything with centavos keeps them. */
fun rateMoney(
    cents: Cents,
    language: Language,
): String = formatMoney(cents, language, compact = cents % 100 == 0L)

@Composable
@ReadOnlyComposable
fun rateText(
    cents: Cents,
    rateType: RateType,
): String {
    val price = rateMoney(cents, currentLanguage())
    return if (rateType ==
        RateType.HOUR
    ) {
        stringResource(R.string.places_perHour, price)
    } else {
        stringResource(R.string.places_perLesson, price)
    }
}

/** "R$ 60/h pagos pelo local", "Salário R$ 2.400/mês", "Sem preço"… */
@Composable
@ReadOnlyComposable
fun placePriceText(place: Place): String {
    if (place.kind == PlaceKind.SCHOOL) {
        return place.monthlySalaryCents?.let { stringResource(R.string.places_salary, rateMoney(it, currentLanguage())) }
            ?: stringResource(R.string.places_noPrice)
    }
    val rate = place.rateCents ?: return stringResource(R.string.places_noPrice)
    val text = rateText(rate, place.rateType)
    return if (place.payer == Payer.PLACE) stringResource(R.string.places_paidByPlace, text) else text
}

@Composable
@ReadOnlyComposable
fun travelText(place: Place): String =
    when (place.travelMode) {
        TravelMode.VARIES -> stringResource(R.string.places_travel_varies)
        TravelMode.FIXED -> stringResource(R.string.places_travel_fixed, rateMoney(place.travelFeeCents, currentLanguage()))
        TravelMode.NONE -> stringResource(R.string.places_travel_none)
    }

/** Ajustes › Durações em HH:MM, shared with every screen from the top of the app. */
val LocalClockDurations = staticCompositionLocalOf { false }

/** "01:30" for 90 minutes. */
fun clockDuration(minutes: Int): String = "${(minutes / 60).toString().padStart(2, '0')}:${(minutes % 60).toString().padStart(2, '0')}"

/** "90 min", or "01:30" with Durações em HH:MM on. */
@Composable
@ReadOnlyComposable
fun minutesText(minutes: Int): String =
    if (LocalClockDurations.current) clockDuration(minutes) else stringResource(R.string.places_minutes, minutes.toString())

/** "45 min", or "90 min · 1h30" from an hour on, as in the mockups; "01:30" with Durações em HH:MM on. */
@Composable
@ReadOnlyComposable
fun durationText(minutes: Int): String {
    val label = minutesText(minutes)
    if (minutes < 60 || LocalClockDurations.current) return label
    val rest = minutes % 60
    return "$label · ${minutes / 60}h${if (rest > 0) rest.toString().padStart(2, '0') else ""}"
}

@Composable
@ReadOnlyComposable
fun countText(
    zero: Int,
    plural: Int,
    count: Int,
): String = if (count == 0) stringResource(zero) else pluralStringResource(plural, count, count.toString())

fun localeOf(language: Language): Locale = if (language == Language.PT_BR) Locale.forLanguageTag("pt-BR") else Locale.US

private fun String.capitalized() = replaceFirstChar { it.uppercase() }

/** "Segunda, 26 de outubro" / "Monday, October 26". */
fun longDate(
    date: LocalDate,
    language: Language,
): String {
    val locale = localeOf(language)
    val weekday = DateTimeFormatter.ofPattern("EEEE", locale).format(date.toJavaLocalDate()).removeSuffix("-feira")
    val pattern = if (language == Language.PT_BR) "d 'de' MMMM" else "MMMM d"
    return "${weekday.capitalized()}, ${DateTimeFormatter.ofPattern(pattern, locale).format(date.toJavaLocalDate())}"
}

/** "Outubro 2026" / "October 2026". */
fun monthYear(
    date: LocalDate,
    language: Language,
): String = DateTimeFormatter.ofPattern("LLLL yyyy", localeOf(language)).format(date.toJavaLocalDate()).capitalized()

/** "Seg" / "Mon". */
fun weekdayShort(
    date: LocalDate,
    language: Language,
): String =
    DateTimeFormatter
        .ofPattern("EEE", localeOf(language))
        .format(date.toJavaLocalDate())
        .removeSuffix(".")
        .capitalized()

/** "segunda-feira" / "Monday". */
fun weekdayLong(
    date: LocalDate,
    language: Language,
): String = DateTimeFormatter.ofPattern("EEEE", localeOf(language)).format(date.toJavaLocalDate())

/** "Seg, 26 out" / "Mon, Oct 26". */
fun shortDate(
    date: LocalDate,
    language: Language,
): String {
    val pattern = if (language == Language.PT_BR) "EEE, d MMM" else "EEE, MMM d"
    return DateTimeFormatter
        .ofPattern(pattern, localeOf(language))
        .format(date.toJavaLocalDate())
        .replace(".", "")
        .capitalized()
}

/** "14:00" / "2:00 PM". */
fun clockTime(
    time: LocalTime,
    language: Language,
): String = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(localeOf(language)).format(time.toJavaLocalTime())

/** An hour mark on the Agenda grid: "08:00" / "8 AM". */
fun hourMark(
    hour: Int,
    language: Language,
): String =
    if (language ==
        Language.PT_BR
    ) {
        "${hour.toString().padStart(2, '0')}:00"
    } else {
        DateTimeFormatter.ofPattern("h a", Locale.US).format(java.time.LocalTime.of(hour, 0))
    }

@Composable
@ReadOnlyComposable
fun errorText(error: app.tutora.domain.CatalogError): String =
    stringResource(
        when (error) {
            app.tutora.domain.CatalogError.NAME_REQUIRED -> R.string.catalog_errors_nameRequired
            app.tutora.domain.CatalogError.PLACE_REQUIRED -> R.string.catalog_errors_placeRequired
            app.tutora.domain.CatalogError.BUILTIN_FIXED -> R.string.catalog_errors_builtinFixed
            app.tutora.domain.CatalogError.COMMISSION_RANGE -> R.string.catalog_errors_commissionRange
            app.tutora.domain.CatalogError.NOT_ARCHIVED -> R.string.catalog_errors_notArchived
            app.tutora.domain.CatalogError.NOT_FOUND -> R.string.catalog_errors_notFound
        },
    )

/** hh:mm:ss for the live clock, the same in both languages. */
fun clockText(ms: Long): String {
    val s = ms.coerceAtLeast(0) / 1000
    return "%02d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60)
}
