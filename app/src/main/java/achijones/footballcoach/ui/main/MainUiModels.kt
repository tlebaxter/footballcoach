package achijones.footballcoach.ui.main

enum class MainTab { HOME, TEAM, LEAGUE, AWARDS, MORE }

enum class HomeSegment { STATS, ROSTER, GAMES }

enum class BrowseSegment { STATS, ROSTER, GAMES }

enum class TeamPanelSegment { DEPTH_CHART, STRATEGY }

enum class AwardsSegment { HONORS, BOWLS }

enum class PlayersLeavingTab { GRADUATES, MOCK_DRAFT }

data class RosterRowUi(
    val name: String,
    val pos: String,
    val ovr: Int,
    val yearLabel: String,
    val potGrade: String,
    val injured: Boolean,
    val injuryLabel: String?,
    val starter: Boolean,
    val playerKey: Int,
)

data class StatRowUi(
    val label: String,
    val value: String,
    val rank: String,
    val rankNum: Int = Int.MAX_VALUE,
    val category: String = "Overview",
    val rankingsMode: Int? = null,
)

data class ScheduleRowUi(
    val weekLabel: String,
    val result: String,
    val opponent: String,
    val isWin: Boolean?,
    val isLoss: Boolean?,
    val gameKey: Int,
    val opponentTeamName: String?,
    val gameName: String,
    val homeAway: String,
    val opponentLabel: String,
    val opponentAbbr: String,
    val opponentRank: Int,
    val scoreLine: String,
    val played: Boolean,
    val isHome: Boolean,
    val isBye: Boolean = false,
)

data class TeamPickerTeamUi(
    val name: String,
    val abbr: String,
    val programPower: Int,
    val tradition: Int,
    val fanbase: Int,
    val donors: Int,
    val footprint: Int,
    val pipeline: Int,
    val momentum: Int,
    val purse: String,
    val offTalent: Int,
    val defTalent: Int,
    val stTalent: Int,
)

data class TeamPickerConfUi(
    val name: String,
    val teams: List<TeamPickerTeamUi>,
)

data class LineupRowUi(
    val name: String,
    val ovr: Int,
    /** OVR at the selected depth-chart position lens. */
    val posOvr: Int,
    val primaryPos: String,
    val yearLabel: String,
    val potGrade: String,
    val injured: Boolean,
    val injuryLabel: String?,
    val playerKey: Int,
    val depthIndex: Int,
    val depthLabel: String,
    val starter: Boolean,
    val locked: Boolean,
)

data class BowlRowUi(
    val name: String,
    val away: String,
    val home: String,
    val score: String,
)

data class AwardRowUi(
    val lines: List<String>,
    val highlightUser: Boolean,
)

data class RankingRowUi(
    val line: String,
    val teamName: String?,
)

data class HistoryRowUi(
    val text: String,
    val highlightAbbr: String?,
)

data class StatChipUi(
    val label: String,
    val value: String,
)

data class SeasonYearUi(
    val year: Int,
    val teamAbbr: String,
    val teamName: String,
    val classLabel: String,
    val recordLine: String,
    val stats: List<StatChipUi>,
    val awards: List<String>,
    val isCurrent: Boolean,
)

enum class TimelineKind { DEAL, TRANSFER, SCHOOL, AWARD }

data class TimelineEventUi(
    val yearLabel: String,
    val title: String,
    val detail: String?,
    val amountLabel: String?,
    val kind: TimelineKind,
)

data class PlayerCareerUi(
    val name: String,
    val position: String,
    val ovr: Int,
    val yearLabel: String,
    val potGrade: String,
    val teamName: String,
    val teamAbbr: String,
    val rosterStatus: String,
    val nilLabel: String?,
    val attrChips: List<StatChipUi> = emptyList(),
    val secondaryPosOvrs: List<String> = emptyList(),
    val seasonRatings: List<StatChipUi>,
    val seasonStats: List<StatChipUi>,
    val careerTotals: List<StatChipUi>,
    val seasonYears: List<SeasonYearUi>,
    val timeline: List<TimelineEventUi>,
)

data class GameDialogUi(
    val title: String,
    val played: Boolean,
    val awayScore: String?,
    val homeScore: String?,
    val awayName: String?,
    val homeName: String?,
    val otLabel: String?,
    val left: String,
    val center: String,
    val right: String,
    val bottom: String,
    val canCoach: Boolean,
    val gameKey: Int,
)

data class RenameDialogUi(
    val name: String,
    val abbr: String,
    val nameError: String,
    val abbrError: String,
    val showPopups: Boolean,
    val showInjuryReport: Boolean,
)

data class InjuryReportPlayerUi(
    val name: String,
    val pos: String,
    val yearLabel: String,
    val ovr: Int,
    val potGrade: String,
    /** Injury description plus duration, e.g. "Head (3 wk)"; null once recovered. */
    val injuryLabel: String?,
)

data class InjuryReportUi(
    val injured: List<InjuryReportPlayerUi>,
    val recovered: List<InjuryReportPlayerUi>,
    val showReportsEnabled: Boolean,
)

data class PlayersLeavingDialogUi(
    val title: String,
    val tab: PlayersLeavingTab,
    val gradLines: List<String>,
    val mockDraftLines: List<String>,
)

data class MainUiState(
    val ready: Boolean = false,
    val navigateHome: Boolean = false,
    val navigateToTalentHub: Boolean = false,
    val navigateToSchedule: Boolean = false,
    /** True during Retention / Portal / HS so More can re-open Talent Hub. */
    val showReturnToTalentHub: Boolean = false,
    val selectedTab: MainTab = MainTab.HOME,
    val homeSegment: HomeSegment = HomeSegment.ROSTER,
    val browseSegment: BrowseSegment = BrowseSegment.ROSTER,
    val teamSegment: TeamPanelSegment = TeamPanelSegment.DEPTH_CHART,
    val awardsSegment: AwardsSegment = AwardsSegment.HONORS,
    val playWeekLabel: String = "Play Week",
    val playWeekEnabled: Boolean = true,
    val playingWeek: Boolean = false,
    val browseRosterPosFilter: String = "ALL",
    val homeStats: List<StatRowUi> = emptyList(),
    val homeRoster: List<RosterRowUi> = emptyList(),
    val homeSchedule: List<ScheduleRowUi> = emptyList(),
    val browseStats: List<StatRowUi> = emptyList(),
    val browseRoster: List<RosterRowUi> = emptyList(),
    val browseSchedule: List<ScheduleRowUi> = emptyList(),
    val confNames: List<String> = emptyList(),
    val browseTeamLabels: List<String> = emptyList(),
    val selectedConfIndex: Int = 0,
    val selectedBrowseTeamIndex: Int = 0,
    val lineupPositionIndex: Int = 0,
    val lineupPositionLabel: String = "",
    val lineupRequired: Int = 1,
    val lineupRows: List<LineupRowUi> = emptyList(),
    val lineupStarterCount: Int = 0,
    val lineupBenchCount: Int = 0,
    val offPhilosophyNames: List<String> = emptyList(),
    val defSystemNames: List<String> = emptyList(),
    val offPhilosophyIndex: Int = 0,
    val defSystemIndex: Int = 0,
    val pressureResponseNames: List<String> = emptyList(),
    val pressureSlotLabels: List<String> = emptyList(),
    val pressureResponseIndices: List<Int> = emptyList(),
    val navigateToCoach: Boolean = false,
    val awardsBowlsUnlocked: Boolean = false,
    val awardCategories: List<String> = emptyList(),
    val selectedAwardCategory: Int = 0,
    val potyHeader: String? = null,
    val potySubhead: String? = null,
    val potyStats: String? = null,
    val awardsSectionLabel: String = "",
    val awardRows: List<AwardRowUi> = emptyList(),
    val bowlSpinnerOptions: List<String> = emptyList(),
    val selectedBowlOption: Int = 0,
    val bowlRows: List<BowlRowUi> = emptyList(),
    val snackbarMessage: String? = null,
    val scrollScheduleToIndex: Int? = null,
    val showTeamPicker: Boolean = false,
    val teamPickerConferences: List<TeamPickerConfUi> = emptyList(),
    val showExitConfirm: Boolean = false,
    val showSaveDialog: Boolean = false,
    val saveSlotInfos: List<String> = emptyList(),
    val confirmOverwriteSlot: Int? = null,
    val showSeasonSummary: Boolean = false,
    val seasonSummaryTitle: String = "",
    val seasonSummaryMessage: String = "",
    val showInjuryDialog: Boolean = false,
    val injuryReport: InjuryReportUi? = null,
    val showRecruitingClassDialog: Boolean = false,
    val recruitingClassRows: List<RankingRowUi> = emptyList(),
    val showPlayersLeavingDialog: Boolean = false,
    val playersLeavingDialog: PlayersLeavingDialogUi? = null,
    val showRankingsDialog: Boolean = false,
    val rankingsModeIndex: Int = 0,
    val rankingsRows: List<RankingRowUi> = emptyList(),
    val showLeagueHistoryDialog: Boolean = false,
    val leagueHistoryModeIndex: Int = 0,
    val leagueHistoryRows: List<HistoryRowUi> = emptyList(),
    val showTeamHistoryDialog: Boolean = false,
    val teamHistoryModeIndex: Int = 0,
    val teamHistoryRows: List<HistoryRowUi> = emptyList(),
    val showRenameDialog: Boolean = false,
    val renameDialog: RenameDialogUi? = null,
    val showPlayerCareer: Boolean = false,
    val playerCareer: PlayerCareerUi? = null,
    val showGameDialog: Boolean = false,
    val gameDialog: GameDialogUi? = null,
)
