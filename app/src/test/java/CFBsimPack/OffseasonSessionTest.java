package CFBsimPack;

import org.junit.After;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OffseasonSessionTest {

    @After
    public void tearDown() {
        OffseasonSession.clear();
    }

    @Test
    public void phaseFromStringParsesAndDefaults() {
        assertEquals(OffseasonSession.Phase.RETENTION, OffseasonSession.phaseFromString(null));
        assertEquals(OffseasonSession.Phase.RETENTION, OffseasonSession.phaseFromString("nope"));
        assertEquals(OffseasonSession.Phase.PORTAL, OffseasonSession.phaseFromString("portal"));
        assertEquals(OffseasonSession.Phase.HS, OffseasonSession.phaseFromString("HS"));
    }

    @Test
    public void beginReadyAndClear() throws Exception {
        assertFalse(OffseasonSession.ready());
        OffseasonSession.begin(null, null);
        assertFalse(OffseasonSession.ready());

        League league = createLeague();
        LeagueOffseason off = new LeagueOffseason(league);
        OffseasonSession.begin(league, off, OffseasonSession.Phase.PORTAL);
        assertTrue(OffseasonSession.ready());
        assertEquals(OffseasonSession.Phase.PORTAL, OffseasonSession.phase);
        assertEquals(league, OffseasonSession.league);

        OffseasonSession.clear();
        assertFalse(OffseasonSession.ready());
        assertNull(OffseasonSession.league);
        assertNull(OffseasonSession.offseason);
        assertEquals(OffseasonSession.Phase.RETENTION, OffseasonSession.phase);
    }

    @Test
    public void phaseLabels() {
        assertEquals("Retention", OffseasonSession.phaseLabel(OffseasonSession.Phase.RETENTION));
        assertEquals("Portal", OffseasonSession.phaseLabel(OffseasonSession.Phase.PORTAL));
        assertEquals("Schedule", OffseasonSession.phaseLabel(OffseasonSession.Phase.SCHEDULE));
        assertEquals("HS", OffseasonSession.phaseLabel(OffseasonSession.Phase.HS));
    }

    private static League createLeague() throws Exception {
        Path csvPath = Paths.get("app/src/main/assets/fbs_2026.csv");
        if (!Files.exists(csvPath)) {
            csvPath = Paths.get("src/main/assets/fbs_2026.csv");
        }
        String csv = new String(Files.readAllBytes(csvPath), StandardCharsets.UTF_8);
        return new League("A,B", "C,D", csv);
    }
}
