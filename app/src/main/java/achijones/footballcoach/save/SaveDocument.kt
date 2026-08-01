package achijones.footballcoach.save

import kotlinx.serialization.Serializable

const val CURRENT_SAVE_VERSION = 13
const val SLOT_COUNT = 10

/**
 * Version 13 career document: typed league snapshot (canonical).
 *
 * Legacy fields (`cfbPayload`, CSV blobs) exist only so v10–v12 JSON can decode
 * before [CareerSaveMapper.migrateToCurrent] upgrades to typed v13.
 */
@Serializable
data class SaveDocument(
    val saveVersion: Int = CURRENT_SAVE_VERSION,
    val summary: String,
    val currentWeek: Int = 0,
    val hasScheduledBowls: Boolean = false,
    val userTeamAbbr: String,
    val offseasonPhase: String? = null,
    val leagueHistory: List<List<String>> = emptyList(),
    val heismanHistory: List<String> = emptyList(),
    val teams: List<TeamDoc> = emptyList(),
    val userTeamHistory: List<String> = emptyList(),
    val leagueRecords: List<RecordDoc> = emptyList(),
    val leagueWinStreak: StreakDoc = StreakDoc(),
    val userTeamRecords: List<RecordDoc> = emptyList(),
    val hallOfFame: List<String> = emptyList(),
    val schedule: List<ScheduleTeamDoc>? = null,
    val teamSeason: List<TeamSeasonDoc>? = null,
    val postseason: PostseasonDoc? = null,
    val oocContracts: OocBookDoc? = null,
    val offseason: OffseasonSaveDoc? = null,
    // --- legacy v10–v12 decode only ---
    val cfbPayload: String = "",
    val leagueRecordLines: List<String> = emptyList(),
    val leagueWinStreakCsv: String = "",
    val userTeamRecordLines: List<String> = emptyList(),
)

@Serializable
data class TeamDoc(
    val conference: String,
    val name: String,
    val abbr: String,
    val profile: ProgramProfileDoc,
    val careerWins: Int = 0,
    val careerLosses: Int = 0,
    val totalCCs: Int = 0,
    val totalNCs: Int = 0,
    val rivalries: List<RivalryDoc> = emptyList(),
    val totalNCLosses: Int = 0,
    val totalCCLosses: Int = 0,
    val totalBowls: Int = 0,
    val totalBowlLosses: Int = 0,
    val showPopups: Boolean = true,
    val yearStartWinStreak: StreakDoc = StreakDoc(),
    val teamTVDeal: Boolean = false,
    val confTVDeal: Boolean = false,
    val offPhilosophy: String = "MULTIPLE",
    val defSystem: String = "BASE_4_3",
    val programProfileUpdatedThisOffseason: Boolean = false,
    val qbPressure: QbPressureDoc = QbPressureDoc(),
    val evenYearHomeOpp: String = "",
    val players: List<PlayerDoc> = emptyList(),
    val specialTeams: SpecialTeamsDepthDoc = SpecialTeamsDepthDoc(),
    /** Legacy v10/v11 only. */
    val profileCsv: String = "",
    val playerLines: List<String> = emptyList(),
    val specialTeamsDepth: String? = null,
)

@Serializable
data class ProgramProfileDoc(
    val tradition: Int = 50,
    val fanbase: Int = 50,
    val donors: Int = 50,
    val footprint: Int = 50,
    val pipeline: Int = 50,
    val momentum: Int = 50,
    val finishHistory: List<Int> = emptyList(),
    val draftHistory: List<Int> = emptyList(),
    val diffProgramPower: Int = 0,
    val diffMomentum: Int = 0,
    val diffDonors: Int = 0,
    val diffFanbase: Int = 0,
    val diffTradition: Int = 0,
    val diffFootprint: Int = 0,
    val diffPipeline: Int = 0,
)

@Serializable
data class RivalryDoc(
    val opponentAbbr: String,
    val strength: Int = 0,
)

@Serializable
data class StreakDoc(
    val length: Int = 0,
    val team: String = "XXX",
    val startYear: Int = 0,
    val endYear: Int = 0,
) {
    fun toCsv(): String = "$length,$team,$startYear,$endYear"

    companion object {
        fun fromCsv(csv: String): StreakDoc {
            val p = csv.split(',')
            if (p.size < 4) return StreakDoc()
            return StreakDoc(
                length = p[0].toIntOrNull() ?: 0,
                team = p[1].ifBlank { "XXX" },
                startYear = p[2].toIntOrNull() ?: 0,
                endYear = p[3].toIntOrNull() ?: 0,
            )
        }
    }
}

@Serializable
data class QbPressureDoc(
    val normal: String = "AUTO",
    val convert: String = "TAKE_THE_FIRST_DOWN",
    val protectLead: String = "SLIDE_SECURE",
    val lateTrailing: String = "SCRAMBLE_FOR_IT",
    val backedUp: String = "THROW_IT_AWAY",
)

@Serializable
data class SpecialTeamsDepthDoc(
    val puntReturner: PlayerRefDoc? = null,
    val kickReturner: PlayerRefDoc? = null,
    val gunner1: PlayerRefDoc? = null,
    val gunner2: PlayerRefDoc? = null,
    val longSnapper: PlayerRefDoc? = null,
)

@Serializable
data class PlayerRefDoc(
    val position: String,
    val name: String,
    val year: Int,
)

@Serializable
data class PlayerDoc(
    val position: String,
    val name: String,
    val year: Int,
    val isRedshirt: Boolean = false,
    val ratings: RatingsDoc = RatingsDoc(),
    val ovr: Int = 50,
    val improvement: Int = 0,
    val careerGamesPlayed: Int = 0,
    val careerSnaps: Int = 0,
    val careerStats: SkillStatsDoc = SkillStatsDoc(),
    val careerHeismans: Int = 0,
    val careerAllAmerican: Int = 0,
    val careerAllConference: Int = 0,
    val careerWins: Int = 0,
    val rosterStatus: String = "SCHOLARSHIP",
    val nilDealAmount: Int = 0,
    val contractYearsRemaining: Int = 0,
    val contractLength: Int = 1,
    val retainedThisOffseason: Boolean = false,
    val depthLocked: Boolean = false,
    val season: PlayerSeasonDoc? = null,
    val careerSeasons: List<PlayerSeasonRecordDoc> = emptyList(),
    val careerPrAtt: Int = 0,
    val careerPrYards: Int = 0,
    val careerPrTd: Int = 0,
    val careerKrAtt: Int = 0,
    val careerKrYards: Int = 0,
    val careerKrTd: Int = 0,
    val careerFairCatches: Int = 0,
)

@Serializable
data class RatingsDoc(
    val pot: Int = 50,
    val footIq: Int = 50,
    val dur: Int = 50,
    val hgt: Int = 50,
    val stre: Int = 50,
    val spd: Int = 50,
    val endu: Int = 50,
    val thv: Int = 50,
    val thp: Int = 50,
    val tha: Int = 50,
    val bsc: Int = 50,
    val elu: Int = 50,
    val rtr: Int = 50,
    val hnd: Int = 50,
    val pbk: Int = 50,
    val rbk: Int = 50,
    val pcv: Int = 50,
    val tck: Int = 50,
    val prs: Int = 50,
    val rns: Int = 50,
    val kpw: Int = 50,
    val kac: Int = 50,
    val ppw: Int = 50,
    val pac: Int = 50,
)

@Serializable
data class SkillStatsDoc(
    val passAtt: Int = 0,
    val passComp: Int = 0,
    val passYards: Int = 0,
    val passTd: Int = 0,
    val passInt: Int = 0,
    val sacked: Int = 0,
    val rushAtt: Int = 0,
    val rushYards: Int = 0,
    val rushTd: Int = 0,
    val fumbles: Int = 0,
    val targets: Int = 0,
    val receptions: Int = 0,
    val recYards: Int = 0,
    val recTd: Int = 0,
    val drops: Int = 0,
    val recFumbles: Int = 0,
    val xpAtt: Int = 0,
    val xpMade: Int = 0,
    val fgAtt: Int = 0,
    val fgMade: Int = 0,
    val puntAtt: Int = 0,
    val puntYards: Int = 0,
    val tackles: Int = 0,
    val tfl: Int = 0,
    val sacksDef: Int = 0,
    val defInt: Int = 0,
    val passDef: Int = 0,
    val forcedFumbles: Int = 0,
    val fumbleRec: Int = 0,
)

@Serializable
data class PlayerSeasonDoc(
    val gamesPlayed: Int = 0,
    val statsWins: Int = 0,
    val seasonSnaps: Int = 0,
    val stats: SkillStatsDoc = SkillStatsDoc(),
    val injuryDescription: String? = null,
    val injuryDuration: Int = 0,
    val isEjected: Boolean = false,
    val prAtt: Int = 0,
    val prYards: Int = 0,
    val prTd: Int = 0,
    val krAtt: Int = 0,
    val krYards: Int = 0,
    val krTd: Int = 0,
    val fairCatches: Int = 0,
)

@Serializable
data class PlayerSeasonRecordDoc(
    val seasonYear: Int,
    val teamAbbr: String,
    val teamName: String,
    val classYear: Int,
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val wonHeisman: Boolean = false,
    val wonAllAmerican: Boolean = false,
    val wonAllConference: Boolean = false,
    val position: String,
    val stats: SkillStatsDoc = SkillStatsDoc(),
    val rushFumbles: Int = 0,
    val prAtt: Int = 0,
    val prYards: Int = 0,
    val prTd: Int = 0,
    val krAtt: Int = 0,
    val krYards: Int = 0,
    val krTd: Int = 0,
    val fairCatches: Int = 0,
)

@Serializable
data class RecordDoc(
    val key: String,
    val number: Int = -1,
    val holder: String = "-1",
    val year: Int = -1,
) {
    fun toCsvLine(): String = "$key,$number,$holder,$year"

    companion object {
        fun fromCsvLine(line: String): RecordDoc {
            val p = line.split(',')
            if (p.size < 4) {
                return RecordDoc(key = line)
            }
            return RecordDoc(
                key = p[0],
                number = p[1].toIntOrNull() ?: -1,
                holder = p[2],
                year = p[3].toIntOrNull() ?: -1,
            )
        }
    }
}

@Serializable
data class ScheduleTeamDoc(
    val teamAbbr: String,
    val byeWeek: Int,
    val weeks: List<ScheduleSlotDoc>,
)

@Serializable
data class ScheduleSlotDoc(
    val kind: String, // BYE | EMPTY | MATCHUP
    val home: Boolean = false,
    val opponentAbbr: String = "",
    val played: Boolean = false,
    val result: GameResultDoc? = null,
)

@Serializable
data class GameResultDoc(
    val homeScore: Int,
    val awayScore: Int,
    val homeYards: Int,
    val awayYards: Int,
    val homeTOs: Int,
    val awayTOs: Int,
    val numOT: Int,
    val homeQScore: List<Int> = listOf(0, 0, 0, 0),
    val awayQScore: List<Int> = listOf(0, 0, 0, 0),
)

@Serializable
data class TeamSeasonDoc(
    val abbr: String,
    val wins: Int,
    val losses: Int,
    val teamPoints: Int,
    val teamOppPoints: Int,
    val teamYards: Int,
    val teamOppYards: Int,
    val teamPassYards: Int,
    val teamRushYards: Int,
    val teamOppPassYards: Int,
    val teamOppRushYards: Int,
    val teamTODiff: Int,
    val winStreak: StreakDoc = StreakDoc(),
    val gameWLSchedule: List<String> = emptyList(),
    val gameWinsAgainstAbbrs: List<String> = emptyList(),
    val rivalryResults: Map<String, Boolean> = emptyMap(),
    val confChampion: String = "",
    val semiFinalWL: String = "",
    val natChampWL: String = "",
    /** Legacy v10/v11. */
    val winStreakCsv: String = "",
)

@Serializable
data class PostseasonGameDoc(
    val gameName: String,
    val homeAbbr: String,
    val awayAbbr: String,
    val played: Boolean = false,
    val result: GameResultDoc? = null,
)

@Serializable
data class ConferenceCcgDoc(
    val conference: String,
    val game: PostseasonGameDoc,
)

@Serializable
data class PostseasonDoc(
    val cfpField: List<String> = emptyList(),
    val cfpAutoBids: List<String> = emptyList(),
    val cfpFirstRound: List<PostseasonGameDoc>? = null,
    val cfpQuarters: List<PostseasonGameDoc>? = null,
    val cfpSemis: List<PostseasonGameDoc>? = null,
    val ncg: PostseasonGameDoc? = null,
    val bowlGames: List<PostseasonGameDoc> = emptyList(),
    val conferenceCcgs: List<ConferenceCcgDoc> = emptyList(),
)

@Serializable
data class OocBookDoc(
    val nextId: Int,
    val contracts: List<OocContractDoc> = emptyList(),
    /** Legacy v10/v11. */
    val contractLines: List<String> = emptyList(),
)

@Serializable
data class OocContractDoc(
    val id: String,
    val teamA: String,
    val teamB: String,
    val startYear: Int,
    val lengthYears: Int,
    val type: String,
    val mustFulfillByYear: Int,
    val buyout: Int,
    val games: List<OocGameDoc> = emptyList(),
)

@Serializable
data class OocGameDoc(
    val year: Int,
    val homeAbbr: String,
    val awayAbbr: String,
    val guarantee: Int = 0,
    val winBonus: Int = 0,
    val settled: Boolean = false,
    val preferredWeek: Int = -1,
)

@Serializable
data class OffseasonSaveDoc(
    val phase: String,
    val budgets: List<TeamBudgetDoc> = emptyList(),
    val retained: List<RetainedKeyDoc> = emptyList(),
    val portal: List<PortalPlayerDoc> = emptyList(),
    val hsClass: List<PlayerDoc> = emptyList(),
    /** Legacy v10/v11 HS lines. */
    val hsClassLines: List<String> = emptyList(),
)

@Serializable
data class TeamBudgetDoc(
    val abbr: String,
    val recruitMoney: Int,
)

@Serializable
data class RetainedKeyDoc(
    val teamAbbr: String,
    val position: String,
    val name: String,
    val year: Int,
)

@Serializable
data class PortalPlayerDoc(
    val priorTeamAbbr: String,
    val player: PlayerDoc? = null,
    /** Legacy v10/v11. */
    val playerLine: String = "",
)

enum class SlotStatus {
    EMPTY,
    OK,
    CORRUPT,
    INCOMPATIBLE,
}

data class SlotInfo(
    val index: Int,
    val status: SlotStatus,
    val summary: String,
    val saveVersion: Int = 0,
)
