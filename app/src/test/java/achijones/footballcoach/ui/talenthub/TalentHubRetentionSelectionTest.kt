package achijones.footballcoach.ui.talenthub

import CFBsimPack.OffseasonSession
import CFBsimPack.RosterStatus
import CFBsimPack.TransferReason
import achijones.footballcoach.testing.LeagueFixtures
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TalentHubRetentionSelectionTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        LeagueFixtures.clearSessions()
    }

    @Test
    fun toggleRetainCheckboxPersistsAcrossReload() {
        val vm = readyRetentionVm()
        val row = firstCheckableRow(vm)
        val before = row.checked

        vm.toggleSuggestion(row.id)

        val after = vm.uiState.value.rows.first { it.id == row.id }
        assertEquals(!before, after.checked)
    }

    @Test
    fun customRetentionOfferPersistsAfterConfirm() {
        val vm = readyRetentionVm()
        val row = firstNonDraftStayRow(vm)

        vm.onRowTap(row.id)
        val sheet = vm.uiState.value.offerSheet
        assertNotNull(sheet)
        assertFalse(sheet!!.draftStay)

        val targetYears = (sheet.years % sheet.maxYears) + 1
        val targetStatus = if (sheet.status == RosterStatus.SCHOLARSHIP) {
            RosterStatus.SCHOLARSHIP_PLUS_NIL
        } else {
            RosterStatus.SCHOLARSHIP
        }
        vm.updateOfferYears(targetYears)
        vm.updateOfferStatus(targetStatus)
        vm.confirmOffer()

        assertTrue(vm.uiState.value.offerSheet == null)
        val updated = vm.uiState.value.rows.first { it.id == row.id }
        assertTrue(updated.checked)

        vm.onRowTap(updated.id)
        val reopened = vm.uiState.value.offerSheet
        assertNotNull(reopened)
        assertEquals(targetYears, reopened!!.years)
        assertEquals(targetStatus, reopened.status)
    }

    @Test
    fun retainCheckboxPersistsAcrossTabSwitch() {
        val vm = readyRetentionVm()
        val row = firstCheckableRow(vm)
        val before = row.checked

        vm.toggleSuggestion(row.id)
        val flipped = !before
        assertEquals(flipped, vm.uiState.value.rows.first { it.id == row.id }.checked)

        vm.selectTab(HubTab.PORTAL)
        vm.selectTab(HubTab.RETAIN)

        val restored = vm.uiState.value.rows.first { it.id == row.id }
        assertEquals(flipped, restored.checked)
    }

    private fun readyRetentionVm(): TalentHubViewModel {
        val (league, _) = LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.RETENTION)
        val user = league.userTeam
        var seeded = 0
        for (p in user.getAllPlayers()) {
            if (p.year >= 5) continue
            p.portalRiskTier = 2
            p.transferReason = TransferReason.PLAYING_TIME
            p.transferReasonText = "Playing time"
            p.retainedThisOffseason = false
            seeded++
            if (seeded >= 4) break
        }
        assertTrue("Need retain candidates", seeded >= 2)

        val vm = TalentHubViewModel(app)
        waitUntil { vm.uiState.value.ready && vm.uiState.value.rows.any { it.showCheck } }
        return vm
    }

    private fun firstCheckableRow(vm: TalentHubViewModel): TalentRowUi {
        val row = vm.uiState.value.rows.firstOrNull { it.showCheck && it.suggestionKey != null }
        assertNotNull("Expected a retain checkbox row", row)
        return row!!
    }

    private fun firstNonDraftStayRow(vm: TalentHubViewModel): TalentRowUi {
        val row = vm.uiState.value.rows.firstOrNull {
            it.showCheck && it.suggestionKey != null && it.statusLabel != "Stay"
        }
        assertNotNull("Expected a non-draft-stay retain row", row)
        return row!!
    }

    private fun waitUntil(predicate: () -> Boolean) {
        var waited = 0
        while (!predicate() && waited < 100) {
            Thread.sleep(20)
            waited++
        }
        assertTrue(predicate())
    }
}
