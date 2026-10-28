package app.tutora.data

import app.tutora.domain.BuiltinPlace
import app.tutora.domain.FeeAmount
import app.tutora.domain.FeeChoice
import app.tutora.domain.FeeRule
import app.tutora.domain.Group
import app.tutora.domain.PayCycle
import app.tutora.domain.Payer
import app.tutora.domain.Place
import app.tutora.domain.PlaceKind
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.domain.TravelMode
import org.json.JSONObject

fun PlaceRow.toPlace() =
    Place(
        id = id,
        builtin = builtin?.let(BuiltinPlace::valueOf),
        name = name,
        address = address,
        kind = PlaceKind.fromKey(kind),
        payer = Payer.valueOf(payer),
        rateType = RateType.valueOf(rateType),
        rateCents = rateCents,
        commissionBp = commissionBp,
        payCycle = PayCycle.valueOf(payCycle),
        monthlySalaryCents = monthlySalaryCents,
        defaultDurationMin = defaultDurationMin,
        travelMode = TravelMode.valueOf(travelMode),
        travelFeeCents = travelFeeCents,
        archivedAt = archivedAt,
        feeChoice = decodeChoice(feeRule),
    )

fun Place.toRow(
    createdAt: String,
    now: String,
) = PlaceRow(
    id,
    builtin?.name,
    name,
    address,
    kind.key,
    payer.name,
    rateType.name,
    rateCents,
    commissionBp,
    payCycle.name,
    monthlySalaryCents,
    defaultDurationMin,
    travelMode.name,
    travelFeeCents,
    archivedAt,
    createdAt,
    now,
    encodeChoice(feeChoice),
)

fun StudentRow.toStudent() =
    Student(id, name, subject, phone, notes, TagColor.valueOf(color), RateType.valueOf(rateType), archivedAt, decodeChoice(feeRule))

fun Student.toRow(
    createdAt: String,
    now: String,
) = StudentRow(id, name, subject, phone, notes, color.name, rateType.name, archivedAt, createdAt, now, encodeChoice(feeChoice))

fun LinkRow.toLink() = PlaceLink(placeId, isDefault, rateCents)

fun GroupRow.toGroup(members: List<String>) = Group(id, placeId, name, subject, rateCents, members, archivedAt)

fun Group.toRow(
    createdAt: String,
    now: String,
) = GroupRow(id, placeId, name, subject, rateCents, archivedAt, createdAt, now)

// Fee rules have one shape everywhere (architecture, schema): settings hold the default, a
// student or place column holds null (Padrão), "never", or its own rule.

private fun FeeAmount.toJson(): JSONObject =
    when (this) {
        is FeeAmount.Percent -> JSONObject().put("percent_bp", bp)
        is FeeAmount.Fixed -> JSONObject().put("fixed_cents", cents)
    }

private fun amountOf(o: JSONObject): FeeAmount =
    if (o.has("fixed_cents")) FeeAmount.Fixed(o.getLong("fixed_cents")) else FeeAmount.Percent(o.getInt("percent_bp"))

fun encodeRule(r: FeeRule): String =
    JSONObject()
        .put("late_on", r.lateOn)
        .put("window_h", r.windowH)
        .put("late", r.late.toJson())
        .put("miss_on", r.missOn)
        .put("miss", r.miss.toJson())
        .toString()

fun decodeRule(json: String): FeeRule {
    val o = JSONObject(json)
    return FeeRule(
        o.getBoolean("late_on"),
        o.getInt("window_h"),
        amountOf(o.getJSONObject("late")),
        o.getBoolean("miss_on"),
        amountOf(o.getJSONObject("miss")),
    )
}

fun encodeChoice(c: FeeChoice): String? =
    when (c) {
        FeeChoice.Default -> null
        FeeChoice.Never -> "never"
        is FeeChoice.Own -> encodeRule(c.rule)
    }

fun decodeChoice(text: String?): FeeChoice =
    when (text) {
        null -> FeeChoice.Default
        "never" -> FeeChoice.Never
        else -> FeeChoice.Own(decodeRule(text))
    }
