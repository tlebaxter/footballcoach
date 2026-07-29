package achijones.footballcoach.save

import CFBsimPack.GameSession
import CFBsimPack.OffseasonSession

/**
 * Applies offseason phase transitions on the live [OffseasonSession].
 * Callers must [CareerPersistence.saveActive] after a successful transition.
 */
object OffseasonFlow {
    enum class Next {
        STAY_TALENT_HUB,
        SCHEDULE,
        TALENT_HUB,
        MAIN,
    }

    /** Retention offers already applied; build portal and enter PORTAL. */
    fun finishRetention(): Next {
        check(OffseasonSession.ready()) { "Offseason not ready" }
        val league = OffseasonSession.league
        GameSession.setLeague(league)
        OffseasonSession.offseason.buildTransferPortal()
        OffseasonSession.phase = OffseasonSession.Phase.PORTAL
        return Next.STAY_TALENT_HUB
    }

    fun finishPortal(): Next {
        check(OffseasonSession.ready()) { "Offseason not ready" }
        val league = OffseasonSession.league
        val user = league.userTeam
        GameSession.setLeague(league)
        val off = OffseasonSession.offseason
        off.aiClaimRemainingPortal()
        league.updateTeamHistories()
        league.updateLeagueHistory()
        user.resetStats()
        league.advanceSeasonForScheduling()
        OffseasonSession.phase = OffseasonSession.Phase.SCHEDULE
        return Next.SCHEDULE
    }

    fun finishSchedule(): Next {
        check(OffseasonSession.ready()) { "Offseason not ready" }
        val league = OffseasonSession.league
        GameSession.setLeague(league)
        val off = OffseasonSession.offseason
        league.completeOocSchedule()
        val yearOneScheduling = GameSession.needsOocScheduling()
        GameSession.setNeedsOocScheduling(false)
        return if (yearOneScheduling) {
            GameSession.clearOffseason()
            league.updateTeamTalentRatings()
            Next.MAIN
        } else {
            off.generateHsClass()
            OffseasonSession.phase = OffseasonSession.Phase.HS
            Next.TALENT_HUB
        }
    }

    fun finishRecruiting(remainingBudget: Int): Next {
        check(OffseasonSession.ready()) { "Offseason not ready" }
        val league = OffseasonSession.league
        val user = league.userTeam
        GameSession.setLeague(league)
        if (remainingBudget >= 0) {
            user.recruitMoney = remainingBudget
        }
        OffseasonSession.offseason.aiSignHsClass()
        GameSession.clearOffseason()
        league.updateTeamTalentRatings()
        return Next.MAIN
    }
}
