package CFBsimPack;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Team picker must resolve by abbr, not teamList index: setTeamRanks() reorders
 * teamList in place after picker UI models are built.
 */
public class TeamPickerSelectionTest {

    private static final String FIRST_NAMES =
            "Alex,Blake,Casey,Drew,Evan,Frankie,Gray,Hayden";
    private static final String LAST_NAMES =
            "Adams,Baker,Clark,Davis,Evans,Foster,Green,Hill";

    @Test
    public void setTeamRanksInvalidatesTeamListIndexesButAbbrLookupStaysCorrect() throws Exception {
        League league = createLeague();
        assertEquals(0, league.currentWeek);

        Map<String, Integer> indexByAbbr = new HashMap<>();
        for (int i = 0; i < league.teamList.size(); i++) {
            indexByAbbr.put(league.teamList.get(i).abbr, i);
        }

        Team selected = null;
        for (Team t : league.teamList) {
            if ("ALA".equals(t.abbr)) {
                selected = t;
                break;
            }
        }
        assertNotNull(selected);

        league.setTeamRanks();

        int staleIndexes = 0;
        for (Map.Entry<String, Integer> entry : indexByAbbr.entrySet()) {
            Team atStaleIndex = league.teamList.get(entry.getValue());
            if (!entry.getKey().equals(atStaleIndex.abbr)) {
                staleIndexes++;
            }
        }
        assertTrue("setTeamRanks should reorder teamList", staleIndexes > 0);

        Team byAbbr = null;
        for (Team t : league.teamList) {
            if ("ALA".equals(t.abbr)) {
                byAbbr = t;
                break;
            }
        }
        assertNotNull(byAbbr);
        assertSame(selected, byAbbr);
        assertEquals("ALA", byAbbr.abbr);
    }

    private static League createLeague() throws IOException {
        Path asset = Paths.get("src/main/assets/fbs_2026.csv");
        if (!Files.exists(asset)) {
            asset = Paths.get("app/src/main/assets/fbs_2026.csv");
        }
        String teamsCsv = new String(Files.readAllBytes(asset), StandardCharsets.UTF_8);
        return new League(FIRST_NAMES, LAST_NAMES, teamsCsv);
    }
}
