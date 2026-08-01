package achijones.footballcoach.ui.talenthub

import CFBsimPack.NilMoney
import CFBsimPack.Player
import CFBsimPack.RosterStatus
import CFBsimPack.Team

data class DepthPlayerUi(
    val name: String,
    val ovr: Int,
    val yearLabel: String,
    val statusLabel: String,
)

data class PositionDepthUi(
    val position: String,
    val have: Int,
    val sug: Int,
    val scholly: Int,
    val pwo: Int,
    val nilSpend: Int,
    val topOvr: Int,
    val avgOvr: Int,
    val deficit: Int,
    val players: List<DepthPlayerUi>,
) {
    val underDepth: Boolean get() = have < sug

    fun contextLine(): String {
        val nil = NilMoney.format(nilSpend)
        val top = if (have == 0) "—" else topOvr.toString()
        val avg = if (have == 0) "—" else avgOvr.toString()
        return "$position · $have/$sug · Sch $scholly · PWO $pwo · NIL $nil · Top $top · Avg $avg"
    }
}

data class DepthBoardUi(
    val positions: List<PositionDepthUi>,
    val schollyUsed: Int,
    val schollyCap: Int,
    val pwoCount: Int,
    val rosterUsed: Int,
    val rosterCap: Int,
    val cash: Int,
    val cashLabel: String,
    val pwoLabel: String,
    val worstNeedLabels: List<String>,
)

data class ImpactLine(
    val label: String,
    val before: String,
    val after: String,
    val changed: Boolean,
)

data class OfferImpactUi(
    val lines: List<ImpactLine>,
    val blockedReason: String?,
)

enum class OfferImpactKind {
    PORTAL_OR_HS,
    RETENTION,
    DRAFT_STAY,
}

fun buildDepthBoard(team: Team): DepthBoardUi {
    val positions = NilMoney.POSITIONS.map { pos -> buildPositionDepth(team, pos) }
    val all = team.allPlayers
    var pwoCount = 0
    for (p in all) {
        if (p.rosterStatus == RosterStatus.PWO) pwoCount++
    }
    val schollyUsed = team.scholarshipCount
    val rosterUsed = team.rosterCount
    val cash = team.recruitMoney
    val worstNeedLabels = positions
        .filter { it.deficit > 0 }
        .sortedByDescending { it.deficit }
        .take(2)
        .map { "${it.position} −${it.deficit}" }
    return DepthBoardUi(
        positions = positions,
        schollyUsed = schollyUsed,
        schollyCap = NilMoney.SCHOLARSHIP_CAP,
        pwoCount = pwoCount,
        rosterUsed = rosterUsed,
        rosterCap = NilMoney.ROSTER_CAP,
        cash = cash,
        cashLabel = team.budgetCashLabel(),
        pwoLabel = "PWO $pwoCount",
        worstNeedLabels = worstNeedLabels,
    )
}

fun buildPositionDepth(team: Team, position: String): PositionDepthUi {
    val list = team.playersForPosition(position) ?: emptyList()
    val sorted = list.sortedByDescending { it.ratOvr }
    var scholly = 0
    var pwo = 0
    var nilSpend = 0
    var ovrSum = 0
    val players = ArrayList<DepthPlayerUi>(sorted.size)
    for (p in sorted) {
        val status = p.rosterStatus ?: RosterStatus.SCHOLARSHIP
        if (status.usesScholarship()) scholly++
        if (status == RosterStatus.PWO) pwo++
        nilSpend += p.nilDealAmount.coerceAtLeast(0)
        ovrSum += p.ratOvr
        players.add(
            DepthPlayerUi(
                name = p.name ?: "?",
                ovr = p.ratOvr,
                yearLabel = p.yrStr,
                statusLabel = status.displayName(),
            ),
        )
    }
    val have = sorted.size
    val sug = NilMoney.sugFor(position)
    val topOvr = sorted.firstOrNull()?.ratOvr ?: 0
    val avgOvr = if (have == 0) 0 else ovrSum / have
    return PositionDepthUi(
        position = position,
        have = have,
        sug = sug,
        scholly = scholly,
        pwo = pwo,
        nilSpend = nilSpend,
        topOvr = topOvr,
        avgOvr = avgOvr,
        deficit = (sug - have).coerceAtLeast(0),
        players = players,
    )
}

fun buildOfferImpact(
    team: Team,
    player: Player,
    kind: OfferImpactKind,
    proposedStatus: RosterStatus,
    proposedNil: Int,
    years: Int,
    stayBonus: Int = 0,
): OfferImpactUi {
    val pos = player.position ?: "QB"
    val beforePos = buildPositionDepth(team, pos)
    val board = buildDepthBoard(team)
    val nil = proposedNil.coerceAtLeast(0)
    val yearOneNilPurse = when (kind) {
        OfferImpactKind.DRAFT_STAY -> stayBonus.coerceAtLeast(0)
        else -> team.nilPurseCost(proposedStatus, nil)
    }

    val addsToRoster = kind == OfferImpactKind.PORTAL_OR_HS
    val oldStatus = player.rosterStatus ?: RosterStatus.SCHOLARSHIP
    val oldNil = player.nilDealAmount.coerceAtLeast(0)
    val oldUsesScholly = oldStatus.usesScholarship()
    val newUsesScholly = proposedStatus.usesScholarship()
    val oldIsPwo = oldStatus == RosterStatus.PWO
    val newIsPwo = proposedStatus == RosterStatus.PWO

    val afterHave = if (addsToRoster) beforePos.have + 1 else beforePos.have
    val afterSug = beforePos.sug

    var afterScholly = board.schollyUsed
    var afterPwo = board.pwoCount
    var afterPosScholly = beforePos.scholly
    var afterPosPwo = beforePos.pwo
    var afterPosNil = beforePos.nilSpend

    when (kind) {
        OfferImpactKind.DRAFT_STAY -> {
            // depth / status unchanged
        }
        OfferImpactKind.PORTAL_OR_HS -> {
            if (newUsesScholly) {
                afterScholly++
                afterPosScholly++
            }
            if (newIsPwo) {
                afterPwo++
                afterPosPwo++
            }
            afterPosNil += if (proposedStatus == RosterStatus.SCHOLARSHIP_PLUS_NIL) nil else 0
        }
        OfferImpactKind.RETENTION -> {
            if (oldUsesScholly && !newUsesScholly) {
                afterScholly--
                afterPosScholly--
            } else if (!oldUsesScholly && newUsesScholly) {
                afterScholly++
                afterPosScholly++
            }
            if (oldIsPwo && !newIsPwo) {
                afterPwo--
                afterPosPwo--
            } else if (!oldIsPwo && newIsPwo) {
                afterPwo++
                afterPosPwo++
            }
            afterPosNil = afterPosNil - oldNil +
                if (proposedStatus == RosterStatus.SCHOLARSHIP_PLUS_NIL) nil else 0
        }
    }

    val afterRoster = if (addsToRoster) board.rosterUsed + 1 else board.rosterUsed
    val afterCash = (board.cash - yearOneNilPurse).coerceAtLeast(0)

    val (afterTop, afterAvg) = projectedQuality(
        before = beforePos,
        playerOvr = player.ratOvr,
        addsPlayer = addsToRoster,
    )

    val lines = listOf(
        impactLine(
            label = "$pos depth",
            before = "${beforePos.have}/${beforePos.sug}",
            after = "$afterHave/$afterSug",
        ),
        impactLine(
            label = "Scholarships",
            before = "${board.schollyUsed}/${board.schollyCap}",
            after = "$afterScholly/${board.schollyCap}",
        ),
        impactLine(
            label = "PWOs",
            before = board.pwoCount.toString(),
            after = afterPwo.toString(),
        ),
        impactLine(
            label = "Roster",
            before = "${board.rosterUsed}/${board.rosterCap}",
            after = "$afterRoster/${board.rosterCap}",
        ),
        impactLine(
            label = "Purse",
            before = NilMoney.format(board.cash),
            after = NilMoney.format(afterCash),
        ),
        impactLine(
            label = "$pos NIL",
            before = NilMoney.format(beforePos.nilSpend),
            after = NilMoney.format(afterPosNil.coerceAtLeast(0)),
        ),
        impactLine(
            label = "$pos quality",
            before = qualityLabel(beforePos.have, beforePos.topOvr, beforePos.avgOvr),
            after = qualityLabel(afterHave, afterTop, afterAvg),
        ),
    )

    val blockedReason = when (kind) {
        OfferImpactKind.DRAFT_STAY -> {
            when {
                stayBonus > team.recruitMoney -> "Cannot afford stay bonus."
                else -> null
            }
        }
        OfferImpactKind.PORTAL_OR_HS -> {
            when {
                !team.canAddToRoster() -> "Roster is full."
                proposedStatus.usesScholarship() && !team.canAwardScholarship() ->
                    "Would exceed scholarship cap."
                !team.canAffordContract(proposedStatus, nil, years) ->
                    "Cannot afford NIL (purse)."
                else -> null
            }
        }
        OfferImpactKind.RETENTION -> {
            val alreadyScholly = oldUsesScholly
            when {
                proposedStatus.usesScholarship() && !alreadyScholly && !team.canAwardScholarship() ->
                    "Would exceed scholarship cap."
                !team.canAffordContract(proposedStatus, nil, years) ->
                    "Cannot afford NIL (purse)."
                else -> null
            }
        }
    }

    return OfferImpactUi(lines = lines, blockedReason = blockedReason)
}

private fun impactLine(label: String, before: String, after: String): ImpactLine =
    ImpactLine(label = label, before = before, after = after, changed = before != after)

private fun qualityLabel(have: Int, top: Int, avg: Int): String {
    if (have <= 0) return "— / —"
    return "Top $top · Avg $avg"
}

private fun projectedQuality(
    before: PositionDepthUi,
    playerOvr: Int,
    addsPlayer: Boolean,
): Pair<Int, Int> {
    if (!addsPlayer) {
        return before.topOvr to before.avgOvr
    }
    val ovrs = before.players.map { it.ovr } + playerOvr
    val top = ovrs.maxOrNull() ?: 0
    val avg = if (ovrs.isEmpty()) 0 else ovrs.sum() / ovrs.size
    return top to avg
}
