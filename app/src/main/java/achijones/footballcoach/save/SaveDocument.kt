package achijones.footballcoach.save

import kotlinx.serialization.Serializable

const val CURRENT_SAVE_VERSION = 12
const val SLOT_COUNT = 10

/**
 * Version 12 career document: metadata + [cfbPayload] (League SAVE_VERSION 9 text).
 * Structured team/schedule/postseason/offseason fields remain for decoding v10/v11 exports only.
 */
@Serializable
data class SaveDocument(
    val saveVersion: Int = CURRENT_SAVE_VERSION,
    val summary: String,
    val currentWeek: Int = 0,
    val hasScheduledBowls: Boolean = false,
    val userTeamAbbr: String,
    val offseasonPhase: String? = null,
    /** Canonical league bytes for v12; empty on legacy structured docs until migrate. */
    val cfbPayload: String = "",
    // --- deprecated structured fields (v10/v11 decode / migrate only) ---
    val leagueHistory: List<List<String>> = emptyList(),
    val heismanHistory: List<String> = emptyList(),
    val teams: List<TeamSaveDoc> = emptyList(),
    val userTeamHistory: List<String> = emptyList(),
    val leagueRecordLines: List<String> = emptyList(),
    val leagueWinStreakCsv: String = "0,XXX,0,0",
    val userTeamRecordLines: List<String> = emptyList(),
    val hallOfFame: List<String> = emptyList(),
    val schedule: List<ScheduleTeamDoc>? = null,
    val teamSeason: List<TeamSeasonDoc>? = null,
    val postseason: PostseasonDoc? = null,
    val oocContracts: OocBookDoc? = null,
    val offseason: OffseasonSaveDoc? = null,
)

@Serializable
data class TeamSaveDoc(
    val conference: String,
    val name: String,
    val abbr: String,
    /** Remaining CSV fields after conference,name,abbr through qbPressure (before %). */
    val profileCsv: String,
    val evenYearHomeOpp: String,
    val playerLines: List<String>,
    val specialTeamsDepth: String? = null,
)

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
    val winStreakCsv: String = "0,XXX,0,0",
    val gameWLSchedule: List<String> = emptyList(),
    val gameWinsAgainstAbbrs: List<String> = emptyList(),
    val rivalryResults: Map<String, Boolean> = emptyMap(),
    val confChampion: String = "",
    val semiFinalWL: String = "",
    val natChampWL: String = "",
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
    val contractLines: List<String>,
)

@Serializable
data class OffseasonSaveDoc(
    val phase: String,
    val budgets: List<TeamBudgetDoc> = emptyList(),
    val retained: List<RetainedKeyDoc> = emptyList(),
    val portal: List<PortalPlayerDoc> = emptyList(),
    val hsClass: List<String> = emptyList(),
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
    val playerLine: String,
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
