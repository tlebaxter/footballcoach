package achijones.footballcoach.save

import CFBsimPack.Game
import CFBsimPack.GameSession
import CFBsimPack.League
import CFBsimPack.OffseasonSession
import CFBsimPack.Player
import CFBsimPack.Postseason
import CFBsimPack.RosterStatus
import CFBsimPack.Team
import achijones.footballcoach.testing.LeagueFixtures
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CareerSaveMapperTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        LeagueFixtures.clearSessions()
    }

    @Test
    fun midSeasonRoundTripsThroughSaveDocument() {
        val league = LeagueFixtures.createLeagueWithUser()
        val user = league.userTeam

        val game = firstPlayableGame(user)
        assertNotNull(game)
        game!!.startGame()
        game.resolveUntilDecided()
        assertTrue(game.hasPlayed)
        assertTrue(game.isDecided)

        val homeScore = game.homeScore
        val awayScore = game.awayScore
        val userWins = user.wins
        val qb = user.teamQBs[0]
        qb.gamesPlayed = 1.coerceAtLeast(qb.gamesPlayed)
        qb.seasonSnaps = 25.coerceAtLeast(qb.seasonSnaps)
        qb.seasonStats.passYards = 212

        val document = CareerSaveMapper.fromLeague(league)
        assertEquals(CURRENT_SAVE_VERSION, document.saveVersion)
        assertTrue(document.cfbPayload.isEmpty())
        assertTrue(document.teams.isNotEmpty())
        assertNotNull(document.schedule)
        assertTrue(document.teams.any { it.players.isNotEmpty() })

        val json = CareerSaveMapper.encode(document)
        val decoded = CareerSaveMapper.decode(json)
        assertTrue(decoded.cfbPayload.isEmpty())
        assertTrue(decoded.teams.isNotEmpty())
        val loaded = CareerSaveMapper.toLeague(
            decoded,
            LeagueFixtures.FIRST_NAMES,
            LeagueFixtures.LAST_NAMES,
        )

        assertEquals(0, loaded.currentWeek)
        val loadedUser = loaded.findTeamAbbr(user.abbr)
        assertNotNull(loadedUser)
        assertEquals(userWins, loadedUser!!.wins)
        val loadedGame = firstPlayableGame(loadedUser)
        assertNotNull(loadedGame)
        assertTrue(loadedGame!!.hasPlayed)
        assertEquals(homeScore, loadedGame.homeScore)
        assertEquals(awayScore, loadedGame.awayScore)
        val loadedQb = findPlayer(loadedUser, qb.position, qb.name)
        assertNotNull(loadedQb)
        assertEquals(212, loadedQb!!.seasonStats.passYards)
    }

    @Test
    fun postseasonRoundTripsViaTypedDocument() {
        val league = LeagueFixtures.createLeagueWithUser()
        while (league.currentWeek < League.WEEK_CFP_FIRST_ROUND) {
            league.playWeek()
        }
        assertEquals(League.WEEK_CFP_FIRST_ROUND, league.currentWeek)
        assertTrue(league.hasScheduledBowls)
        assertEquals(Postseason.CFP_FIELD_SIZE, league.cfpField.size)
        assertNotNull(league.cfpFirstRound)
        assertEquals(4, league.cfpFirstRound!!.size)
        assertNull(league.cfpQuarters)
        assertTrue(league.bowlGames.isNotEmpty())

        val fieldAbbrs = league.cfpField.map { it.abbr }
        val frMatchups = league.cfpFirstRound!!.map {
            Triple(it.gameName, it.homeTeam.abbr, it.awayTeam.abbr)
        }
        val bowlNames = league.bowlGames.map { it.gameName }
        val champAbbrs = league.teamList.filter { it.confChampion == "CC" }.map { it.abbr }.toSet()
        val ccgConfs = league.conferences
            .filter { it.hasChampionship && it.ccg != null }
            .map { it.confName }
            .toSet()

        val document = CareerSaveMapper.fromLeague(league)
        assertEquals(CURRENT_SAVE_VERSION, document.saveVersion)
        assertNotNull(document.postseason)
        assertEquals(fieldAbbrs, document.postseason!!.cfpField)

        val loaded = CareerSaveMapper.toLeague(
            CareerSaveMapper.decode(CareerSaveMapper.encode(document)),
            LeagueFixtures.FIRST_NAMES,
            LeagueFixtures.LAST_NAMES,
        )

        assertEquals(League.WEEK_CFP_FIRST_ROUND, loaded.currentWeek)
        assertTrue(loaded.hasScheduledBowls)
        assertEquals(fieldAbbrs, loaded.cfpField.map { it.abbr })
        assertEquals(champAbbrs, loaded.teamList.filter { it.confChampion == "CC" }.map { it.abbr }.toSet())
        assertNotNull(loaded.cfpFirstRound)
        assertEquals(frMatchups, loaded.cfpFirstRound!!.map {
            Triple(it.gameName, it.homeTeam.abbr, it.awayTeam.abbr)
        })
        assertNull(loaded.cfpQuarters)
        assertEquals(bowlNames, loaded.bowlGames.map { it.gameName })
        assertEquals(
            ccgConfs,
            loaded.conferences
                .filter { it.hasChampionship && it.ccg != null }
                .map { it.confName }
                .toSet(),
        )
        val userAbbr = league.userTeam.abbr
        val loadedUser = loaded.findTeamAbbr(userAbbr)
        assertTrue(loadedUser.gameSchedule.size > League.REGULAR_SEASON_WEEKS)
    }

    @Test
    fun midCfpAfterFirstRoundRoundTripsAndAdvances() {
        val league = LeagueFixtures.createLeagueWithUser()
        while (league.currentWeek < League.WEEK_CFP_FIRST_ROUND) {
            league.playWeek()
        }
        league.playWeek()
        assertEquals(League.WEEK_CFP_QUARTERS, league.currentWeek)
        assertNotNull(league.cfpFirstRound)
        assertTrue(league.cfpFirstRound!!.all { it.hasPlayed })
        assertNotNull(league.cfpQuarters)
        assertEquals(4, league.cfpQuarters!!.size)
        assertTrue(league.cfpQuarters!!.none { it.hasPlayed })

        val fieldAbbrs = league.cfpField.map { it.abbr }
        val frScores = league.cfpFirstRound!!.map {
            Triple(it.homeTeam.abbr, it.homeScore, it.awayScore)
        }
        val qfMatchups = league.cfpQuarters!!.map {
            Triple(it.gameName, it.homeTeam.abbr, it.awayTeam.abbr)
        }
        val tagged = league.teamList
            .filter { it.semiFinalWL.isNotEmpty() }
            .associate { it.abbr to it.semiFinalWL }

        val loaded = CareerSaveMapper.toLeague(
            CareerSaveMapper.decode(CareerSaveMapper.encode(CareerSaveMapper.fromLeague(league))),
            LeagueFixtures.FIRST_NAMES,
            LeagueFixtures.LAST_NAMES,
        )

        assertEquals(League.WEEK_CFP_QUARTERS, loaded.currentWeek)
        assertEquals(fieldAbbrs, loaded.cfpField.map { it.abbr })
        assertEquals(frScores, loaded.cfpFirstRound!!.map {
            Triple(it.homeTeam.abbr, it.homeScore, it.awayScore)
        })
        assertEquals(qfMatchups, loaded.cfpQuarters!!.map {
            Triple(it.gameName, it.homeTeam.abbr, it.awayTeam.abbr)
        })
        assertEquals(
            tagged,
            loaded.teamList.filter { it.semiFinalWL.isNotEmpty() }.associate { it.abbr to it.semiFinalWL },
        )

        loaded.playWeek()
        assertEquals(League.WEEK_CFP_SEMIS, loaded.currentWeek)
        assertEquals(fieldAbbrs, loaded.cfpField.map { it.abbr })
        assertNotNull(loaded.cfpSemis)
        assertEquals(2, loaded.cfpSemis!!.size)
        assertTrue(loaded.cfpQuarters!!.all { it.hasPlayed })
    }

    @Test
    fun unsupportedVersionIsIncompatible() {
        val league = LeagueFixtures.createLeagueWithUser()
        val doc = CareerSaveMapper.fromLeague(league).copy(saveVersion = 99)
        try {
            CareerSaveMapper.validate(doc)
            fail("expected incompatible")
        } catch (_: IncompatibleSaveException) {
            // ok
        }
    }

    @Test
    fun missingTeamsFailsClosed() {
        val forced = SaveDocument(
            saveVersion = CURRENT_SAVE_VERSION,
            summary = "2026: ALA (0-0) 0 CCs, 0 NCs",
            userTeamAbbr = "ALA",
            teams = emptyList(),
        )
        try {
            CareerSaveMapper.validate(forced)
            fail("expected corrupt")
        } catch (_: CorruptSaveException) {
            // ok
        }
    }

    @Test
    fun storageEncodingIsGzipPacked() {
        val league = LeagueFixtures.createLeagueWithUser()
        val document = CareerSaveMapper.fromLeague(league)
        val packed = CareerSaveMapper.encodeForStorage(document)
        assertTrue(SaveCompression.isPacked(packed))
        val decoded = CareerSaveMapper.decode(packed)
        assertEquals(CURRENT_SAVE_VERSION, decoded.saveVersion)
        assertTrue(decoded.teams.isNotEmpty())
    }

    @Test
    fun legacyStructuredV11MigratesOnDecode() {
        val league = LeagueFixtures.createLeagueWithUser()
        val structured = CareerSaveMapper.fromCfbText(league.buildSaveString(), league)
        assertEquals(11, structured.saveVersion)
        assertTrue(structured.teams.isNotEmpty())

        val migrated = CareerSaveMapper.migrateToCurrent(
            structured,
            LeagueFixtures.FIRST_NAMES,
            LeagueFixtures.LAST_NAMES,
        )
        assertEquals(CURRENT_SAVE_VERSION, migrated.saveVersion)
        assertTrue(migrated.cfbPayload.isEmpty())
        assertTrue(migrated.teams.isNotEmpty())
        assertTrue(migrated.teams.first().players.isNotEmpty() || migrated.teams.first().playerLines.isEmpty())

        val loaded = CareerSaveMapper.toLeague(
            migrated,
            LeagueFixtures.FIRST_NAMES,
            LeagueFixtures.LAST_NAMES,
        )
        assertEquals(league.userTeam.abbr, loaded.userTeam.abbr)
    }

    @Test
    fun v12CfbEnvelopeMigratesToTypedV13() {
        val league = LeagueFixtures.createLeagueWithUser()
        val v12 = SaveDocument(
            saveVersion = 12,
            summary = league.userTeam.abbr,
            currentWeek = league.currentWeek,
            hasScheduledBowls = league.hasScheduledBowls,
            userTeamAbbr = league.userTeam.abbr,
            cfbPayload = CareerSaveMapper.packCfb(league.buildSaveString()),
        )
        val migrated = CareerSaveMapper.migrateToCurrent(
            v12,
            LeagueFixtures.FIRST_NAMES,
            LeagueFixtures.LAST_NAMES,
        )
        assertEquals(CURRENT_SAVE_VERSION, migrated.saveVersion)
        assertTrue(migrated.cfbPayload.isEmpty())
        assertTrue(migrated.teams.isNotEmpty())
    }

    @Test
    fun legacyStructuredCorruptScheduleFailsMigrate() {
        val forced = SaveDocument(
            saveVersion = 11,
            summary = "2026: ALA (0-0) 0 CCs, 0 NCs",
            userTeamAbbr = "ALA",
            teams = listOf(
                TeamDoc(
                    conference = "SEC",
                    name = "Alabama",
                    abbr = "ALA",
                    profile = ProgramProfileDoc(),
                    profileCsv = "90,88,92,85,80,75,0,0,0,0,,0,0,0,0,1,0,ALA,0,0,false,false,0,0,,,,,false,,,,0,0,0,0,0",
                    evenYearHomeOpp = "",
                    playerLines = emptyList(),
                ),
            ),
            schedule = listOf(
                ScheduleTeamDoc(
                    teamAbbr = "ALA",
                    byeWeek = 6,
                    weeks = List(League.REGULAR_SEASON_WEEKS) {
                        ScheduleSlotDoc(
                            kind = "MATCHUP",
                            home = true,
                            opponentAbbr = "AUB",
                            played = true,
                            result = null,
                        )
                    },
                ),
            ),
        )
        try {
            CareerSaveMapper.migrateToCurrent(
                forced,
                LeagueFixtures.FIRST_NAMES,
                LeagueFixtures.LAST_NAMES,
            )
            fail("expected corrupt")
        } catch (_: CorruptSaveException) {
            // ok
        }
    }

    @Test
    fun roomRoundTripAndLoadFailureKeepsOkStatus() = runBlocking {
        val db = SaveDatabase.createInMemory(app)
        val repo = SaveRepository(app, db)
        val league = LeagueFixtures.createLeagueWithUser()

        assertTrue(repo.save(0, league).isSuccess)
        val slots = repo.listSlots()
        assertEquals(SlotStatus.OK, slots[0].status)
        assertTrue(slots[0].summary.isNotBlank())
        assertEquals(CURRENT_SAVE_VERSION, slots[0].saveVersion)
        assertTrue(SaveCompression.isPacked(db.saveSlotDao().getSlot(0)!!.payloadJson!!))

        val loaded = repo.load(0, LeagueFixtures.FIRST_NAMES, LeagueFixtures.LAST_NAMES)
        assertEquals(league.userTeam.abbr, loaded.userTeam.abbr)

        db.saveSlotDao().upsert(
            SaveSlotEntity(
                slotIndex = 1,
                status = SlotStatus.OK.name,
                summary = "bogus",
                saveVersion = 12,
                updatedAtMillis = 1L,
                payloadJson = "{not-json",
            ),
        )
        try {
            repo.load(1, LeagueFixtures.FIRST_NAMES, LeagueFixtures.LAST_NAMES)
            fail("expected load failure")
        } catch (_: Exception) {
            // ok
        }
        val after = repo.listSlots().first { it.index == 1 }
        assertEquals(SlotStatus.OK, after.status)
        assertEquals("{not-json", db.saveSlotDao().getSlot(1)!!.payloadJson)
        assertEquals("{not-json", repo.exportJson(1))

        repo.delete(0)
        assertEquals(SlotStatus.EMPTY, repo.listSlots()[0].status)
    }

    @Test
    fun roomPostseasonRoundTrip() = runBlocking {
        val db = SaveDatabase.createInMemory(app)
        val repo = SaveRepository(app, db)
        val league = LeagueFixtures.createLeagueWithUser()
        while (league.currentWeek < League.WEEK_CFP_FIRST_ROUND) {
            league.playWeek()
        }
        val fieldAbbrs = league.cfpField.map { it.abbr }

        assertTrue(repo.save(0, league).isSuccess)
        GameSession.clearAll()
        OffseasonSession.clear()

        val loaded = repo.load(0, LeagueFixtures.FIRST_NAMES, LeagueFixtures.LAST_NAMES)
        assertEquals(League.WEEK_CFP_FIRST_ROUND, loaded.currentWeek)
        assertEquals(fieldAbbrs, loaded.cfpField.map { it.abbr })
        assertTrue(loaded.hasScheduledBowls)
    }

    @Test
    fun roomOffseasonPortalRoundTrip() = runBlocking {
        val db = SaveDatabase.createInMemory(app)
        val repo = SaveRepository(app, db)
        val (league, off) = LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.PORTAL)
        val budgetBefore = league.userTeam.recruitMoney
        val keep = league.userTeam.getAllPlayers()[0]
        keep.retainedThisOffseason = true
        keep.applyOffer(RosterStatus.SCHOLARSHIP, 0, 2)
        off.buildTransferPortal()
        assertTrue(off.transferPortal.isNotEmpty())
        val portalName = off.transferPortal[0].name

        assertTrue(repo.save(0, league).isSuccess)
        GameSession.clearAll()
        OffseasonSession.clear()

        val loaded = repo.load(0, LeagueFixtures.FIRST_NAMES, LeagueFixtures.LAST_NAMES)
        assertTrue(loaded.loadedInOffseason)
        assertEquals(OffseasonSession.Phase.PORTAL, loaded.loadedOffseasonPhase)
        assertTrue(OffseasonSession.ready())
        assertEquals(OffseasonSession.Phase.PORTAL, OffseasonSession.phase)
        assertEquals(budgetBefore, loaded.userTeam.recruitMoney)
        assertTrue(OffseasonSession.offseason.transferPortal.any { it.name == portalName })
        assertTrue(loaded.userTeam.getAllPlayers().any { it.name == keep.name && it.retainedThisOffseason })
    }

    @Test
    fun v11StructuredDocumentMigratesThroughRoomAndRewritesSlot() = runBlocking {
        val db = SaveDatabase.createInMemory(app)
        val repo = SaveRepository(app, db)
        val league = LeagueFixtures.createLeagueWithUser()
        val structured = CareerSaveMapper.fromCfbText(league.buildSaveString(), league)
        assertEquals(11, structured.saveVersion)

        db.saveSlotDao().upsert(
            SaveSlotEntity(
                slotIndex = 0,
                status = SlotStatus.OK.name,
                summary = structured.summary,
                saveVersion = 11,
                updatedAtMillis = 1L,
                payloadJson = CareerSaveMapper.encode(structured),
            ),
        )

        val loaded = repo.load(0, LeagueFixtures.FIRST_NAMES, LeagueFixtures.LAST_NAMES)
        assertEquals(league.userTeam.abbr, loaded.userTeam.abbr)

        val rewritten = db.saveSlotDao().getSlot(0)!!
        assertEquals(CURRENT_SAVE_VERSION, rewritten.saveVersion)
        assertTrue(SaveCompression.isPacked(rewritten.payloadJson!!))

        val exported = repo.exportJson(0)
        val redecoded = CareerSaveMapper.decode(
            exported,
            LeagueFixtures.FIRST_NAMES,
            LeagueFixtures.LAST_NAMES,
        )
        assertEquals(CURRENT_SAVE_VERSION, redecoded.saveVersion)
        assertTrue(redecoded.cfbPayload.isEmpty())
        assertTrue(redecoded.teams.isNotEmpty())
    }

    @Test
    fun v12SlotRewritesToV13OnLoad() = runBlocking {
        val db = SaveDatabase.createInMemory(app)
        val repo = SaveRepository(app, db)
        val league = LeagueFixtures.createLeagueWithUser()
        val v12 = SaveDocument(
            saveVersion = 12,
            summary = "legacy",
            userTeamAbbr = league.userTeam.abbr,
            currentWeek = league.currentWeek,
            cfbPayload = CareerSaveMapper.packCfb(league.buildSaveString()),
        )
        db.saveSlotDao().upsert(
            SaveSlotEntity(
                slotIndex = 0,
                status = SlotStatus.OK.name,
                summary = v12.summary,
                saveVersion = 12,
                updatedAtMillis = 1L,
                payloadJson = CareerSaveMapper.encode(v12),
            ),
        )

        val loaded = repo.load(0, LeagueFixtures.FIRST_NAMES, LeagueFixtures.LAST_NAMES)
        assertEquals(league.userTeam.abbr, loaded.userTeam.abbr)
        val rewritten = db.saveSlotDao().getSlot(0)!!
        assertEquals(CURRENT_SAVE_VERSION, rewritten.saveVersion)
        assertTrue(SaveCompression.isPacked(rewritten.payloadJson!!))
    }

    private fun firstPlayableGame(team: Team): Game? {
        for (week in 0 until team.gameSchedule.size) {
            if (team.isByeWeek(week)) continue
            val g = team.gameSchedule[week]
            if (g != null) return g
        }
        return null
    }

    private fun findPlayer(team: Team, position: String, name: String): Player? {
        for (p in team.getAllPlayers()) {
            if (position == p.position && name == p.name) return p
        }
        return null
    }
}
