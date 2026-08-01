package achijones.footballcoach.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import achijones.footballcoach.ui.icons.CalendarMonth
import achijones.footballcoach.ui.icons.EmojiEvents
import achijones.footballcoach.ui.icons.Groups
import achijones.footballcoach.ui.icons.History
import achijones.footballcoach.ui.icons.Leaderboard
import achijones.footballcoach.ui.icons.Save
import achijones.footballcoach.ui.icons.School
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import achijones.footballcoach.ui.components.MainTabContentHost
import achijones.footballcoach.ui.components.TabContentTransition
import achijones.footballcoach.ui.components.TeamLogo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    onNavigateHome: () -> Unit,
    onNavigateTalentHub: () -> Unit,
    onNavigateCoach: () -> Unit = {},
    onNavigateSchedule: () -> Unit = {},
    viewModel: MainViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    // Hold a blank screen after team pick / offseason handoff so the roster
    // never flashes for a frame before NavHost reaches Talent Hub / Schedule.
    var suppressMainContent by remember { mutableStateOf(false) }

    BackHandler {
        if (state.selectedTab == MainTab.MENU && state.menuDestination == MenuDestination.AWARDS) {
            viewModel.closeMenuAwards()
        } else {
            viewModel.requestExit()
        }
    }

    // Resume (including return from coach) refreshes schedule / W-L snapshots.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onScreenEntered()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Returning from Talent Hub / Schedule reuses this composition; clear the
    // outbound blanking flag so Main is visible again.
    LaunchedEffect(state.ready, state.navigateToTalentHub, state.navigateToSchedule) {
        if (state.ready && !state.navigateToTalentHub && !state.navigateToSchedule) {
            suppressMainContent = false
        }
    }

    LaunchedEffect(state.navigateHome) {
        if (state.navigateHome) {
            viewModel.consumeNavigateHome()
            onNavigateHome()
        }
    }
    LaunchedEffect(state.navigateToTalentHub) {
        if (state.navigateToTalentHub) {
            suppressMainContent = true
            viewModel.consumeNavigateToTalentHub()
            onNavigateTalentHub()
        }
    }
    LaunchedEffect(state.navigateToSchedule) {
        if (state.navigateToSchedule) {
            suppressMainContent = true
            viewModel.consumeNavigateToSchedule()
            onNavigateSchedule()
        }
    }
    LaunchedEffect(state.navigateToCoach) {
        if (state.navigateToCoach) {
            viewModel.consumeNavigateToCoach()
            onNavigateCoach()
        }
    }
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeSnackbar()
        }
    }

    if (!state.ready && !state.navigateHome && !state.navigateToTalentHub && !state.navigateToSchedule) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.showTeamPicker) {
        BackHandler { /* Must pick a team to continue */ }
        TeamPickerScreen(
            conferences = state.teamPickerConferences,
            onPick = viewModel::pickTeam,
        )
        return
    }

    if (suppressMainContent || state.navigateToTalentHub || state.navigateToSchedule) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A)),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column {
                AnimatedVisibility(
                    visible = state.selectedTab == MainTab.HOME,
                    enter = expandVertically(
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                    ) + fadeIn(
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                    ),
                    exit = shrinkVertically(
                        animationSpec = tween(200, easing = FastOutSlowInEasing),
                    ) + fadeOut(
                        animationSpec = tween(200, easing = FastOutSlowInEasing),
                    ),
                ) {
                    Button(
                        onClick = viewModel::playWeek,
                        enabled = state.playWeekEnabled && !state.playingWeek,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        if (state.playingWeek) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .height(20.dp)
                                    .width(20.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(state.playWeekLabel)
                    }
                }
                val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                val active = MaterialTheme.colorScheme.onSurface
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedTextColor = active,
                    selectedIconColor = active,
                    unselectedTextColor = muted,
                    unselectedIconColor = muted,
                    indicatorColor = Color.Transparent,
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    NavigationBarItem(
                        selected = state.selectedTab == MainTab.HOME,
                        onClick = { viewModel.selectTab(MainTab.HOME) },
                        icon = {
                            Icon(
                                if (state.selectedTab == MainTab.HOME) {
                                    Icons.Filled.Home
                                } else {
                                    Icons.Outlined.Home
                                },
                                contentDescription = null,
                            )
                        },
                        label = { Text("Home", maxLines = 1) },
                        colors = navItemColors,
                    )
                    NavigationBarItem(
                        selected = state.selectedTab == MainTab.TEAM,
                        onClick = { viewModel.selectTab(MainTab.TEAM) },
                        icon = {
                            Icon(
                                if (state.selectedTab == MainTab.TEAM) {
                                    Icons.Filled.Groups
                                } else {
                                    Icons.Outlined.Groups
                                },
                                contentDescription = null,
                            )
                        },
                        label = { Text("Team", maxLines = 1) },
                        colors = navItemColors,
                    )
                    NavigationBarItem(
                        selected = state.selectedTab == MainTab.LEAGUE,
                        onClick = { viewModel.selectTab(MainTab.LEAGUE) },
                        icon = {
                            Icon(
                                if (state.selectedTab == MainTab.LEAGUE) {
                                    Icons.Filled.Leaderboard
                                } else {
                                    Icons.Outlined.Leaderboard
                                },
                                contentDescription = null,
                            )
                        },
                        label = { Text("League", maxLines = 1) },
                        colors = navItemColors,
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = viewModel::openTalentHub,
                        icon = {
                            Icon(
                                if (state.showReturnToTalentHub) {
                                    Icons.Filled.School
                                } else {
                                    Icons.Outlined.School
                                },
                                contentDescription = null,
                            )
                        },
                        label = {
                            Text(
                                "Talent Hub",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = navItemColors,
                    )
                    NavigationBarItem(
                        selected = state.selectedTab == MainTab.MENU,
                        onClick = { viewModel.selectTab(MainTab.MENU) },
                        icon = {
                            Icon(
                                if (state.selectedTab == MainTab.MENU) {
                                    Icons.Filled.Menu
                                } else {
                                    Icons.Outlined.Menu
                                },
                                contentDescription = null,
                            )
                        },
                        label = { Text("Menu", maxLines = 1) },
                        colors = navItemColors,
                    )
                }
            }
        },
    ) { padding ->
        MainTabContentHost(
            selectedTab = state.selectedTab,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) { tab ->
            when (tab) {
                MainTab.HOME -> HomePanel(state, viewModel, Modifier.fillMaxSize())
                MainTab.TEAM -> TeamPanel(state, viewModel, Modifier.fillMaxSize())
                MainTab.LEAGUE -> LeaguePanel(state, viewModel, Modifier.fillMaxSize())
                MainTab.MENU -> MenuPanel(state, viewModel, Modifier.fillMaxSize())
            }
        }
    }

    if (state.showExitConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissExitConfirm,
            title = { Text("Exit to Main Menu?") },
            text = {
                Text(
                    "Are you sure you want to return to main menu? Any progress from the " +
                        "beginning of the season will be lost.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmExit) { Text("Yes, Exit") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissExitConfirm) { Text("Cancel") }
            },
        )
    }
    if (state.showSeasonSummary) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSeasonSummary,
            title = { Text(state.seasonSummaryTitle) },
            text = {
                Text(
                    state.seasonSummaryMessage,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissSeasonSummary) { Text("OK") }
            },
        )
    }
    if (state.showInjuryDialog && state.injuryReport != null) {
        InjuryReportSheet(state.injuryReport!!, viewModel)
    }
    if (state.showPlayersLeavingDialog && state.playersLeavingDialog != null) {
        PlayersLeavingDialog(state.playersLeavingDialog!!, viewModel)
    }
    if (state.showRecruitingClassDialog) {
        RecruitingClassDialog(state.recruitingClassRows, viewModel::dismissRecruitingClassDialog)
    }
    if (state.showSaveDialog) {
        SaveDialog(state, viewModel)
    }
    if (state.confirmOverwriteSlot != null) {
        val slot = state.confirmOverwriteSlot!!
        val info = state.saveSlotInfos.getOrNull(slot)
        AlertDialog(
            onDismissRequest = viewModel::dismissOverwriteConfirm,
            title = { Text(if (info?.status == achijones.footballcoach.save.SlotStatus.CORRUPT) "Replace damaged save?" else "Overwrite Save?") },
            text = {
                Text(
                    "Are you sure you want to overwrite this save file?\n\n" +
                        (info?.let { achijones.footballcoach.ui.util.SaveSlots.label(it) } ?: ""),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmOverwriteSave) { Text("Yes, Overwrite") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissOverwriteConfirm) { Text("Cancel") }
            },
        )
    }
    state.confirmDeleteSlot?.let { slot ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteSlot,
            title = { Text("Delete slot ${slot + 1}?") },
            text = { Text("This permanently removes the career in this slot.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteSlot) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteSlot) { Text("Cancel") }
            },
        )
    }
    if (state.showChooseSlotForNewCareer) {
        AlertDialog(
            onDismissRequest = viewModel::dismissChooseSlotForNewCareer,
            title = { Text("Choose a save slot") },
            text = {
                Column {
                    Text("All slots are full. Pick one to overwrite for this new career.")
                    state.saveSlotInfos.forEach { info ->
                        TextButton(
                            onClick = { viewModel.chooseSlotForNewCareer(info.index) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(achijones.footballcoach.ui.util.SaveSlots.label(info))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissChooseSlotForNewCareer) { Text("Cancel") }
            },
        )
    }
    if (state.showRankingsDialog) {
        RankingsDialog(state, viewModel)
    }
    if (state.showLeagueHistoryDialog) {
        LeagueHistoryDialog(state, viewModel)
    }
    if (state.showTeamHistoryDialog) {
        TeamHistoryDialog(state, viewModel)
    }
    if (state.showRenameDialog && state.renameDialog != null) {
        RenameDialog(state.renameDialog!!, viewModel)
    }
    if (state.showPlayerCareer && state.playerCareer != null) {
        PlayerCareerSheet(state.playerCareer!!, viewModel::dismissPlayerCareer)
    }
    if (state.showGameDialog && state.gameDialog != null) {
        GameDetailSheet(state.gameDialog!!, viewModel)
    }
}

@Composable
private fun HomePanel(state: MainUiState, viewModel: MainViewModel, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        SegmentRow(
            labels = listOf("Stats", "Roster", "Games"),
            selected = state.homeSegment.ordinal,
            onSelect = { viewModel.selectHomeSegment(HomeSegment.entries[it]) },
        )
        TabContentTransition(
            targetState = state.homeSegment,
            modifier = Modifier.fillMaxSize(),
            label = "homeSegmentContent",
        ) { segment ->
                ContentList(
                    state = state,
                    segment = segment,
                    stats = state.homeStats,
                    roster = state.homeRoster,
                    schedule = state.homeSchedule,
                    scrollToIndex = state.scrollScheduleToIndex,
                    onConsumeScroll = viewModel::consumeScrollSchedule,
                    onPlayerClick = viewModel::openPlayerCareer,
                    onGameClick = viewModel::openGameDialog,
                    onOpponentClick = { it?.let(viewModel::examineTeam) },
                    onStatClick = { mode -> viewModel.openRankingsDialog(mode) },
                )
        }
    }
}

@Composable
private fun LeaguePanel(state: MainUiState, viewModel: MainViewModel, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        SpinnerDropdown(
            label = "Conference",
            options = state.confNames,
            selectedIndex = state.selectedConfIndex,
            onSelect = viewModel::selectConfIndex,
            showConferenceLogos = true,
        )
        SpinnerDropdown(
            label = "Team",
            options = state.browseTeamOptions.map { it.label },
            selectedIndex = state.selectedBrowseTeamIndex,
            onSelect = viewModel::selectBrowseTeamIndex,
            teamLogoOptions = state.browseTeamOptions,
        )
        SegmentRow(
            labels = listOf("Stats", "Roster", "Games"),
            selected = state.browseSegment.ordinal,
            onSelect = { viewModel.selectBrowseSegment(BrowseSegment.entries[it]) },
        )
        TabContentTransition(
            targetState = state.browseSegment,
            modifier = Modifier.fillMaxSize(),
            label = "browseSegmentContent",
        ) { segment ->
            Column(Modifier.fillMaxSize()) {
                if (segment == BrowseSegment.ROSTER) {
                    PosFilterDropdown(state.browseRosterPosFilter, viewModel::setBrowseRosterFilter)
                }
                ContentList(
                    state = state,
                    segment = when (segment) {
                        BrowseSegment.STATS -> HomeSegment.STATS
                        BrowseSegment.ROSTER -> HomeSegment.ROSTER
                        BrowseSegment.GAMES -> HomeSegment.GAMES
                    },
                    stats = state.browseStats,
                    roster = state.browseRoster,
                    schedule = state.browseSchedule,
                    scrollToIndex = null,
                    onConsumeScroll = {},
                    onPlayerClick = viewModel::openPlayerCareer,
                    onGameClick = viewModel::openGameDialog,
                    onOpponentClick = { it?.let(viewModel::examineTeam) },
                    onStatClick = { mode -> viewModel.openRankingsDialog(mode) },
                )
            }
        }
    }
}

@Composable
private fun TeamPanel(state: MainUiState, viewModel: MainViewModel, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        SegmentRow(
            labels = listOf("Depth Chart", "Strategy"),
            selected = state.teamSegment.ordinal,
            onSelect = { viewModel.selectTeamSegment(TeamPanelSegment.entries[it]) },
        )
        TabContentTransition(
            targetState = state.teamSegment,
            modifier = Modifier.fillMaxSize(),
            label = "teamSegmentContent",
        ) { segment ->
            when (segment) {
                TeamPanelSegment.DEPTH_CHART -> {
                    DepthChartPanel(state = state, viewModel = viewModel)
                }
                TeamPanelSegment.STRATEGY -> {
                    Column(modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            "SYSTEM",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                        SpinnerDropdown(
                            label = "Offense Philosophy",
                            options = state.offPhilosophyNames,
                            selectedIndex = state.offPhilosophyIndex,
                            onSelect = viewModel::setOffPhilosophy,
                        )
                        SpinnerDropdown(
                            label = "Defense System",
                            options = state.defSystemNames,
                            selectedIndex = state.defSystemIndex,
                            onSelect = viewModel::setDefSystem,
                        )
                        Text(
                            "QB UNDER PRESSURE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        )
                        Text(
                            "How your QB reacts when the pocket collapses.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        state.pressureSlotLabels.forEachIndexed { slotIndex, label ->
                            val selected = state.pressureResponseIndices.getOrElse(slotIndex) { 0 }
                            SpinnerDropdown(
                                label = label,
                                options = state.pressureResponseNames,
                                selectedIndex = selected,
                                onSelect = { responseIndex ->
                                    viewModel.setPressureResponse(slotIndex, responseIndex)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class MenuGridItem(
    val label: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val onClick: () -> Unit,
)

@Composable
private fun MenuPanel(state: MainUiState, viewModel: MainViewModel, modifier: Modifier) {
    when (state.menuDestination) {
        MenuDestination.AWARDS -> {
            Column(modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = viewModel::closeMenuAwards) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Menu",
                        )
                    }
                    Text(
                        "Awards",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                AwardsPanel(state, viewModel, Modifier.fillMaxSize())
            }
        }
        MenuDestination.GRID -> {
            val gridItems = listOf(
                MenuGridItem(
                    label = "Awards",
                    icon = Icons.Filled.EmojiEvents,
                    gradient = listOf(Color(0xFFFFB300), Color(0xFFFF8A50)),
                    onClick = viewModel::openMenuAwards,
                ),
                MenuGridItem(
                    label = "Schedule",
                    icon = Icons.Filled.CalendarMonth,
                    gradient = listOf(Color(0xFF26A69A), Color(0xFF00897B)),
                    onClick = viewModel::openScheduleScreen,
                ),
                MenuGridItem(
                    label = "Save",
                    icon = Icons.Filled.Save,
                    gradient = listOf(Color(0xFF64B5F6), Color(0xFF1976D2)),
                    onClick = viewModel::openSaveDialog,
                ),
                MenuGridItem(
                    label = "Rankings",
                    icon = Icons.Filled.Leaderboard,
                    gradient = listOf(Color(0xFFFF8A50), Color(0xFFE65100)),
                    onClick = viewModel::openRankingsDialog,
                ),
                MenuGridItem(
                    label = "League History",
                    icon = Icons.Filled.History,
                    gradient = listOf(Color(0xFF7E57C2), Color(0xFF4527A0)),
                    onClick = viewModel::openLeagueHistoryDialog,
                ),
                MenuGridItem(
                    label = "Team History",
                    icon = Icons.Filled.Groups,
                    gradient = listOf(Color(0xFF42A5F5), Color(0xFF1565C0)),
                    onClick = viewModel::openTeamHistoryDialog,
                ),
                MenuGridItem(
                    label = "Settings",
                    icon = Icons.Filled.Settings,
                    gradient = listOf(Color(0xFF66BB6A), Color(0xFF2E7D32)),
                    onClick = viewModel::openRenameDialog,
                ),
            )
            Column(
                modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Menu",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                MenuTeamCard(
                    teamName = state.menuTeamName,
                    abbr = state.menuTeamAbbr,
                    record = state.menuTeamRecord,
                    onClick = viewModel::openRenameDialog,
                )
                MenuAppIconGrid(items = gridItems)
                MenuListRow(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    label = "Exit to Main Menu",
                    onClick = viewModel::requestExit,
                )
            }
        }
    }
}

@Composable
private fun MenuTeamCard(
    teamName: String,
    abbr: String,
    record: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (teamName.isNotBlank() && abbr.isNotBlank()) {
            TeamLogo(teamName = teamName, abbr = abbr, size = 48.dp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                teamName.ifBlank { "Your Team" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (record.isNotBlank()) "Record $record" else abbr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MenuAppIconGrid(items: List<MenuGridItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items.chunked(4).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { item ->
                    MenuAppIcon(
                        item = item,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MenuAppIcon(item: MenuGridItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(item.gradient))
                .clickable(onClick = item.onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = Color.White,
                modifier = Modifier.size(30.dp),
            )
        }
        Text(
            item.label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MenuListRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ContentList(
    state: MainUiState,
    segment: HomeSegment,
    stats: List<StatRowUi>,
    roster: List<RosterRowUi>,
    schedule: List<ScheduleRowUi>,
    scrollToIndex: Int?,
    onConsumeScroll: () -> Unit,
    onPlayerClick: (Int) -> Unit,
    onGameClick: (Int) -> Unit,
    onOpponentClick: (String?) -> Unit,
    onStatClick: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(scrollToIndex, segment, schedule.size) {
        scrollToIndex?.let { scheduleIndex ->
            if (segment == HomeSegment.GAMES && schedule.isNotEmpty()) {
                // Hero occupies index 0; schedule rows start at 1.
                listState.scrollToItem(scheduleIndex + 1)
            } else {
                listState.scrollToItem(scheduleIndex)
            }
            onConsumeScroll()
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (segment) {
            HomeSegment.STATS -> teamStatsItems(stats, onStatClick)
            HomeSegment.ROSTER -> rosterItems(roster, onPlayerClick)
            HomeSegment.GAMES -> scheduleItems(schedule, onGameClick, onOpponentClick)
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

