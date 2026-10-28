package app.tutora.data

import app.tutora.domain.BuiltinPlace
import app.tutora.domain.Group
import app.tutora.domain.LessonTimer
import app.tutora.domain.PayCycle
import app.tutora.domain.Payer
import app.tutora.domain.Place
import app.tutora.domain.PlaceKind
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.domain.TravelMode

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
)

fun StudentRow.toStudent() =
    Student(
        id,
        name,
        subject,
        phone,
        notes,
        TagColor.valueOf(color),
        RateType.valueOf(rateType),
        archivedAt,
        packageLessons,
        packagePriceCents,
    )

fun Student.toRow(
    createdAt: String,
    now: String,
) = StudentRow(id, name, subject, phone, notes, color.name, rateType.name, archivedAt, createdAt, now, packageLessons, packagePriceCents)

fun LinkRow.toLink() = PlaceLink(placeId, isDefault, rateCents)

fun GroupRow.toGroup(members: List<String>) = Group(id, placeId, name, subject, rateCents, members, archivedAt)

fun Group.toRow(
    createdAt: String,
    now: String,
) = GroupRow(id, placeId, name, subject, rateCents, archivedAt, createdAt, now)

fun TimerRow.toTimer() = LessonTimer(lessonId, startedAt, pausedMs, pausedAt, note)
