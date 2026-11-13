package app.tutora.domain

// A group whose class value is split between its students, each owing their share (spec 020).

/** A member's own share of each class: a percentage or a set amount; none means an equal part. */
sealed interface MemberShare {
    data class Percent(
        val bp: Int,
    ) : MemberShare

    data class Amount(
        val cents: Cents,
    ) : MemberShare
}

/**
 * What each member owes for a class worth [value] (AC-020-34, 35). Members with their own share
 * take it first (a percentage rounded once, a set amount as it is, neither past what is left);
 * what remains is split equally among the others, leftover cents to the first by name. With
 * everyone on their own share, what they don't cover isn't charged. [members] come sorted by name.
 */
fun memberDues(
    value: Cents,
    members: List<String>,
    custom: Map<String, MemberShare>,
): Map<String, Cents> {
    if (members.isEmpty() || value <= 0) return members.associateWith { 0L }
    var left = value
    val fixed = mutableMapOf<String, Cents>()
    for (m in members) {
        val c = custom[m] ?: continue
        val want =
            when (c) {
                is MemberShare.Percent -> roundHalfAwayFromZero(Math.multiplyExact(value, c.bp.toLong()), 10_000)
                is MemberShare.Amount -> c.cents
            }
        val take = want.coerceIn(0, left)
        fixed[m] = take
        left -= take
    }
    val equal = members.filter { it !in custom }
    val parts = if (equal.isEmpty()) emptyList() else splitShares(left, equal.size)
    return members.associateWith { m -> fixed[m] ?: parts[equal.indexOf(m)] }
}
