package CFBsimPack;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RosterStatusTransferReasonTest {

    @Test
    public void rosterStatusFromStringAliases() {
        assertEquals(RosterStatus.PWO, RosterStatus.fromString("pwo"));
        assertEquals(RosterStatus.SCHOLARSHIP, RosterStatus.fromString("SCHOLARSHIP"));
        assertEquals(RosterStatus.SCHOLARSHIP_PLUS_NIL, RosterStatus.fromString("NIL"));
        assertEquals(RosterStatus.SCHOLARSHIP_PLUS_NIL, RosterStatus.fromString("Scholarship+NIL"));
        assertEquals(RosterStatus.SCHOLARSHIP_PLUS_NIL, RosterStatus.fromString("SCHOLARSHIP_PLUS_NIL"));
        assertEquals(RosterStatus.SCHOLARSHIP, RosterStatus.fromString(null));
        assertEquals(RosterStatus.SCHOLARSHIP, RosterStatus.fromString("unknown"));
    }

    @Test
    public void rosterStatusScholarshipHelpers() {
        assertFalse(RosterStatus.PWO.usesScholarship());
        assertTrue(RosterStatus.SCHOLARSHIP.usesScholarship());
        assertTrue(RosterStatus.SCHOLARSHIP_PLUS_NIL.usesScholarship());
        assertEquals("Scholarship+NIL", RosterStatus.SCHOLARSHIP_PLUS_NIL.displayName());
        assertEquals("PWO", RosterStatus.PWO.chipLabel());
        assertEquals("Scholarship", RosterStatus.SCHOLARSHIP.chipLabel());
        assertEquals("Sch+NIL", RosterStatus.SCHOLARSHIP_PLUS_NIL.chipLabel());
    }

    @Test
    public void transferReasonFromStringAndIsIssue() {
        assertEquals(TransferReason.NONE, TransferReason.fromString(null));
        assertEquals(TransferReason.NONE, TransferReason.fromString(""));
        assertEquals(TransferReason.PLAYING_TIME, TransferReason.fromString("playing_time"));
        assertEquals(TransferReason.BETTER_FIT, TransferReason.fromString("not_a_reason"));
        assertFalse(TransferReason.NONE.isIssue());
        assertTrue(TransferReason.PLAYING_TIME.isIssue());
        assertTrue(TransferReason.TITLE_CHASE.isIssue());
    }
}
