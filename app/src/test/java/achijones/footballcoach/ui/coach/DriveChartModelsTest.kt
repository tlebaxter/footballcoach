package achijones.footballcoach.ui.coach

import CFBsimPack.engine.ConceptFamily
import CFBsimPack.engine.DepthBand
import CFBsimPack.engine.PlayLogEntry
import CFBsimPack.engine.Playbook
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DriveChartModelsTest {

    @Test
    fun previewDepthYardsMatchesEngineBands() {
        assertEquals(6, previewDepthYards(DepthBand.SHORT))
        assertEquals(12, previewDepthYards(DepthBand.MEDIUM))
        assertEquals(22, previewDepthYards(DepthBand.DEEP))
        assertEquals(4, previewDepthYards(DepthBand.NONE))
    }

    @Test
    fun buildDriveSegmentsUsesDrivePlaysAndPreview() {
        val plays = listOf(
            entry(before = 25, after = 33, gained = 8, family = ConceptFamily.RUN, name = "Inside Zone"),
            entry(before = 33, after = 33, gained = 0, family = ConceptFamily.PASS, name = "Slants", line = "incomplete"),
        )
        val offense = Playbook.offenseById("gun_four_verts")
        assertNotNull(offense)
        val segments = buildDriveSegments(
            drivePlays = plays,
            yardLine = 33,
            selectedOffense = offense!!,
            showPreview = true,
        )
        assertEquals(3, segments.size)
        assertEquals(DriveSegmentKind.COMPLETED, segments[0].kind)
        assertEquals(25, segments[0].startYard)
        assertEquals(33, segments[0].endYard)
        assertEquals(8, segments[0].yardsGained)
        assertEquals("incomplete", segments[1].logLine)
        val preview = segments[2]
        assertEquals(DriveSegmentKind.PREVIEW, preview.kind)
        assertEquals(PREVIEW_SEGMENT_ID, preview.id)
        assertEquals(33, preview.startYard)
        assertEquals(55, preview.endYard) // DEEP = +22
    }

    @Test
    fun buildDriveSegmentsOmitsPreviewWhenDisabled() {
        val offense = Playbook.defaultOffense()
        val segments = buildDriveSegments(
            drivePlays = listOf(entry(20, 25, 5, ConceptFamily.RUN, "Zone")),
            yardLine = 25,
            selectedOffense = offense,
            showPreview = false,
        )
        assertEquals(1, segments.size)
        assertEquals(DriveSegmentKind.COMPLETED, segments[0].kind)
    }

    @Test
    fun driveSegmentColorByFamilyAndLoss() {
        val run = DriveSegment(0, 20, 28, DriveSegmentKind.COMPLETED, ConceptFamily.RUN, 8, "", "Run")
        val pass = DriveSegment(1, 28, 40, DriveSegmentKind.COMPLETED, ConceptFamily.PASS, 12, "", "Pass")
        val rpo = DriveSegment(2, 40, 45, DriveSegmentKind.COMPLETED, ConceptFamily.RPO, 5, "", "RPO")
        val loss = DriveSegment(3, 45, 42, DriveSegmentKind.COMPLETED, ConceptFamily.RUN, -3, "", "Stuff")
        val special = DriveSegment(4, 20, 20, DriveSegmentKind.COMPLETED, ConceptFamily.SPECIAL, 0, "", "Kneel")

        assertEquals(Color(0xFF22C55E), driveSegmentColor(run))
        assertEquals(Color(0xFF3B82F6), driveSegmentColor(pass))
        assertEquals(Color(0xFF3B82F6), driveSegmentColor(rpo))
        assertEquals(Color(0xFFEF4444), driveSegmentColor(loss))
        assertEquals(Color(0xFF9CA3AF), driveSegmentColor(special))
    }

    @Test
    fun zeroYardSegmentGetsMinimumVisualSpan() {
        val seg = DriveSegment(
            id = 0,
            startYard = 40,
            endYard = 40,
            kind = DriveSegmentKind.COMPLETED,
            family = ConceptFamily.PASS,
            yardsGained = 0,
            logLine = "incomplete",
            conceptName = "Slants",
        )
        val (left, right) = segmentAbsoluteFractions(seg, possessionHome = true, homeDefendsLeft = true)
        assertTrue(abs(right - left) >= MIN_SEGMENT_FRACTION - 1e-4f)
    }

    @Test
    fun hitTestPrefersCompletedOverPreviewAndIgnoresPreviewTaps() {
        val completed = DriveSegment(
            id = 0,
            startYard = 25,
            endYard = 35,
            kind = DriveSegmentKind.COMPLETED,
            family = ConceptFamily.RUN,
            yardsGained = 10,
            logLine = "gain of 10",
            conceptName = "Zone",
        )
        val preview = DriveSegment(
            id = PREVIEW_SEGMENT_ID,
            startYard = 35,
            endYard = 47,
            kind = DriveSegmentKind.PREVIEW,
            family = ConceptFamily.PASS,
            yardsGained = 12,
            logLine = "",
            conceptName = "Mesh",
        )
        val segments = listOf(completed, preview)
        // Attack right: yard 30 → 0.30
        val hit = hitTestDriveSegment(segments, 0.30f, possessionHome = true, homeDefendsLeft = true)
        assertNotNull(hit)
        assertEquals(0, hit!!.id)
        assertEquals("gain of 10", hit.logLine)

        val previewHit = hitTestDriveSegment(segments, 0.40f, possessionHome = true, homeDefendsLeft = true)
        assertNull(previewHit)
    }

    private fun entry(
        before: Int,
        after: Int,
        gained: Int,
        family: ConceptFamily,
        name: String,
        line: String = "$name for $gained",
    ): PlayLogEntry = PlayLogEntry(
        "Q1 15:00",
        1,
        1,
        10,
        before,
        gained,
        after,
        "id_$name",
        name,
        family,
        "cover_3",
        "Cover 3",
        line,
        true,
        null,
    )
}
