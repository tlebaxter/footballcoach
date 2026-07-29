package achijones.footballcoach.testing

import CFBsimPack.GameSession
import CFBsimPack.League
import CFBsimPack.LeagueOffseason
import CFBsimPack.OffseasonSession
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

object LeagueFixtures {
    const val FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J"
    const val LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T"

    fun createLeagueWithUser(): League {
        val csvPath = listOf(
            Paths.get("src/main/assets/fbs_2026.csv"),
            Paths.get("app/src/main/assets/fbs_2026.csv"),
        ).first { Files.exists(it) }
        val csv = String(Files.readAllBytes(csvPath), StandardCharsets.UTF_8)
        val league = League(FIRST_NAMES, LAST_NAMES, csv)
        val user = league.userTeam ?: league.teamList[0]
        league.userTeam = user
        user.userControlled = true
        return league
    }

    fun beginOffseason(
        league: League = createLeagueWithUser(),
        phase: OffseasonSession.Phase = OffseasonSession.Phase.RETENTION,
        grantBudgets: Boolean = true,
    ): Pair<League, LeagueOffseason> {
        val off = LeagueOffseason(league)
        league.offseason = off
        if (grantBudgets) {
            off.grantAllBudgets()
        }
        GameSession.beginOffseason(league, off, phase)
        return league to off
    }

    fun clearSessions() {
        GameSession.clearAll()
        OffseasonSession.clear()
    }
}
