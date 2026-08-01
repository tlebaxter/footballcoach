package achijones.footballcoach.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import achijones.footballcoach.ui.components.TabContentTransition
import achijones.footballcoach.ui.components.TeamLogo
import achijones.footballcoach.ui.icons.EmojiEvents
import achijones.footballcoach.ui.theme.FcBackground
import achijones.footballcoach.ui.theme.FcOvrElite
import kotlinx.coroutines.launch

@Composable
fun AwardsPanel(state: MainUiState, viewModel: MainViewModel, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        SegmentRow(
            labels = buildList {
                add("Honors")
                if (state.awardsBowlsUnlocked) add("Bowls")
            },
            selected = if (state.awardsSegment == AwardsSegment.HONORS) 0 else 1,
            onSelect = {
                viewModel.selectAwardsSegment(
                    if (it == 0) AwardsSegment.HONORS else AwardsSegment.BOWLS,
                )
            },
        )
        TabContentTransition(
            targetState = state.awardsSegment,
            modifier = Modifier.fillMaxSize(),
            label = "awardsSegmentContent",
        ) { segment ->
            when (segment) {
                AwardsSegment.HONORS -> AwardsHonorsContent(state, viewModel)
                AwardsSegment.BOWLS -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        SpinnerDropdown(
                            label = "View",
                            options = state.bowlSpinnerOptions,
                            selectedIndex = state.selectedBowlOption,
                            onSelect = viewModel::selectBowlOption,
                        )
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.bowlRows) { bowl ->
                                BowlCard(bowl)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AwardsHonorsContent(state: MainUiState, viewModel: MainViewModel) {
    val pages = state.awardPages
    if (pages.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AwardsSectionLabel(state.awardsSectionLabel.ifBlank { "Historic Awards" })
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                items(state.awardRows) { row ->
                    AwardNomineeRow(row)
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = state.selectedAwardCategory.coerceIn(pages.indices),
        pageCount = { pages.size },
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.selectedAwardCategory, pages.size) {
        val target = state.selectedAwardCategory.coerceIn(0, pages.lastIndex)
        if (pagerState.settledPage != target && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(target)
        }
    }
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != state.selectedAwardCategory) {
            viewModel.selectAwardCategory(pagerState.settledPage)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SpinnerDropdown(
            label = "Category",
            options = pages.map { it.categoryLabel },
            selectedIndex = state.selectedAwardCategory.coerceIn(pages.indices),
            onSelect = viewModel::selectAwardCategory,
        )
        AwardPageDots(
            count = pages.size,
            selected = pagerState.currentPage,
            onSelect = { page ->
                viewModel.selectAwardCategory(page)
                scope.launch { pagerState.animateScrollToPage(page) }
            },
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            beyondViewportPageCount = 1,
        ) { pageIndex ->
            AwardCategoryPage(
                page = pages[pageIndex],
                onPlayerClick = viewModel::openPlayerCareer,
            )
        }
    }
}

@Composable
private fun AwardPageDots(
    count: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    if (count <= 1) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val active = index == selected
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (active) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) FcOvrElite else FcOvrElite.copy(alpha = 0.28f),
                    )
                    .clickable { onSelect(index) },
            )
        }
    }
}

@Composable
private fun AwardCategoryPage(
    page: AwardCategoryPageUi,
    onPlayerClick: (Int) -> Unit,
) {
    val stageBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2A2208),
            FcBackground,
            FcBackground,
        ),
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(stageBrush)
            .padding(horizontal = 4.dp),
    ) {
        if (!page.potyHeader.isNullOrBlank()) {
            AwardSpotlightHero(
                header = page.potyHeader,
                subhead = page.potySubhead,
                stats = page.potyStats,
                teamName = page.potyTeamName,
                abbr = page.potyAbbr,
                playerKey = page.potyPlayerKey,
                onPlayerClick = onPlayerClick,
            )
        }
        AwardsSectionLabel(page.sectionLabel)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            items(page.rows) { row ->
                AwardNomineeRow(row, onPlayerClick = onPlayerClick)
            }
        }
    }
}

@Composable
private fun AwardsSectionLabel(label: String) {
    if (label.isBlank()) return
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 1.6.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        color = FcOvrElite.copy(alpha = 0.78f),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
    )
}

@Composable
private fun AwardSpotlightHero(
    header: String,
    subhead: String?,
    stats: String?,
    teamName: String?,
    abbr: String?,
    playerKey: Int?,
    onPlayerClick: (Int) -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val wash = Brush.verticalGradient(
        colors = listOf(
            FcOvrElite.copy(alpha = 0.22f),
            Color(0xFF1A1608),
        ),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .clip(shape)
            .background(wash)
            .border(1.dp, FcOvrElite.copy(alpha = 0.45f), shape)
            .then(
                if (playerKey != null) {
                    Modifier.clickable { onPlayerClick(playerKey) }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.EmojiEvents,
            contentDescription = null,
            tint = FcOvrElite,
            modifier = Modifier.size(28.dp),
        )
        if (!teamName.isNullOrBlank() && !abbr.isNullOrBlank()) {
            TeamLogo(teamName = teamName, abbr = abbr, size = 44.dp)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "PLAYER OF THE YEAR",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                color = FcOvrElite.copy(alpha = 0.85f),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = header,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            subhead?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            stats?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (playerKey != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "View player",
                tint = FcOvrElite.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun AwardNomineeRow(
    row: AwardRowUi,
    onPlayerClick: ((Int) -> Unit)? = null,
) {
    if (row.isMessage) {
        Text(
            text = row.title.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                .padding(12.dp),
        )
        return
    }

    val showHistoric = row.playerKey == null && !row.title.isNullOrBlank() && row.playerName.isNullOrBlank()
    if (showHistoric) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .border(
                    width = 0.5.dp,
                    color = FcOvrElite.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(0.dp),
                )
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!row.teamName.isNullOrBlank() && !row.abbr.isNullOrBlank()) {
                    TeamLogo(teamName = row.teamName, abbr = row.abbr, size = 28.dp)
                }
                Text(
                    text = row.title.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (row.highlightUser) FcOvrElite else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            row.subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        return
    }

    val medal = awardMedalColors(row.rankNum)
    val clickable = row.playerKey != null && onPlayerClick != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .then(
                if (row.highlightUser) {
                    Modifier.background(FcOvrElite.copy(alpha = 0.08f))
                } else {
                    Modifier
                },
            )
            .then(
                if (clickable) {
                    Modifier.clickable { onPlayerClick.invoke(row.playerKey!!) }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (row.highlightUser) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(36.dp)
                    .background(FcOvrElite),
            )
        }
        when {
            medal != null && !row.rankLabel.isNullOrBlank() -> {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(medal.bg)
                        .border(1.dp, medal.border, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = row.rankLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = medal.fg,
                    )
                }
            }
            !row.teamName.isNullOrBlank() && !row.abbr.isNullOrBlank() -> {
                TeamLogo(teamName = row.teamName, abbr = row.abbr, size = 28.dp)
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            val headline = when {
                !row.playerName.isNullOrBlank() -> {
                    listOfNotNull(row.position, row.playerName).joinToString(" ")
                }
                !row.title.isNullOrBlank() -> row.title
                else -> row.abbr.orEmpty()
            }
            Text(
                text = headline,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = listOfNotNull(
                row.abbr?.let { ab ->
                    buildString {
                        append(ab)
                        row.yearLabel?.let { append(" · $it") }
                    }
                },
                row.metaLine,
            ).filter { it.isNotBlank() }
            if (meta.isNotEmpty()) {
                Text(
                    text = meta.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            row.statsLine?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (clickable) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "View player",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(0.5.dp)
            .background(FcOvrElite.copy(alpha = 0.14f)),
    )
}

private data class AwardMedalColors(
    val bg: Color,
    val fg: Color,
    val border: Color,
)

private fun awardMedalColors(rankNum: Int): AwardMedalColors? = when {
    rankNum == 1 -> AwardMedalColors(
        bg = Color(0xFF3E2E00),
        fg = FcOvrElite,
        border = FcOvrElite.copy(alpha = 0.7f),
    )
    rankNum == 2 -> AwardMedalColors(
        bg = Color(0xFF2A2A2E),
        fg = Color(0xFFC0C0C0),
        border = Color(0xFFC0C0C0).copy(alpha = 0.65f),
    )
    rankNum == 3 -> AwardMedalColors(
        bg = Color(0xFF2E1C10),
        fg = Color(0xFFCD7F32),
        border = Color(0xFFCD7F32).copy(alpha = 0.65f),
    )
    rankNum > 3 -> AwardMedalColors(
        bg = Color(0xFF2A2A2A),
        fg = Color(0xFFB0B0B0),
        border = Color(0xFF3A3A3A),
    )
    else -> null
}

@Composable
private fun BowlCard(bowl: BowlRowUi) {
    val cardShape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                if (bowl.isUserInvolved) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .border(
                1.dp,
                if (bowl.isUserInvolved) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                },
                cardShape,
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = bowl.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BowlSide(
                name = bowl.awayName,
                abbr = bowl.awayAbbr,
                rank = bowl.awayRank,
                record = bowl.awayRecord,
                modifier = Modifier.weight(1f),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(
                    text = bowl.score,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (bowl.played) "FINAL" else "PREVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BowlSide(
                name = bowl.homeName,
                abbr = bowl.homeAbbr,
                rank = bowl.homeRank,
                record = bowl.homeRecord,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BowlSide(
    name: String?,
    abbr: String,
    rank: Int?,
    record: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TeamLogo(
            teamName = name,
            abbr = abbr,
            size = 44.dp,
        )
        Text(
            text = abbr,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val meta = buildList {
            rank?.let { add("#$it") }
            record?.let { add("($it)") }
        }.joinToString(" ")
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

