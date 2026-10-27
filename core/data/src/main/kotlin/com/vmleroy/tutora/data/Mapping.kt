package com.vmleroy.tutora.data

import com.vmleroy.tutora.domain.BuiltinPlace
import com.vmleroy.tutora.domain.Group
import com.vmleroy.tutora.domain.PayCycle
import com.vmleroy.tutora.domain.Payer
import com.vmleroy.tutora.domain.Place
import com.vmleroy.tutora.domain.PlaceKind
import com.vmleroy.tutora.domain.PlaceLink
import com.vmleroy.tutora.domain.RateType
import com.vmleroy.tutora.domain.Student
import com.vmleroy.tutora.domain.TagColor
import com.vmleroy.tutora.domain.TravelMode

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

fun StudentRow.toStudent() = Student(id, name, subject, phone, notes, TagColor.valueOf(color), RateType.valueOf(rateType), archivedAt)

fun Student.toRow(
    createdAt: String,
    now: String,
) = StudentRow(id, name, subject, phone, notes, color.name, rateType.name, archivedAt, createdAt, now)

fun LinkRow.toLink() = PlaceLink(placeId, isDefault, rateCents)

fun GroupRow.toGroup(members: List<String>) = Group(id, placeId, name, subject, rateCents, members, archivedAt)

fun Group.toRow(
    createdAt: String,
    now: String,
) = GroupRow(id, placeId, name, subject, rateCents, archivedAt, createdAt, now)
