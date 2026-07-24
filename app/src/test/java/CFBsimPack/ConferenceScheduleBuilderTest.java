package CFBsimPack;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConferenceScheduleBuilderTest {

    private static final String FIRST_NAMES =
            "Alex,Blake,Casey,Drew,Evan,Frankie,Gray,Hayden";
    private static final String LAST_NAMES =
            "Adams,Baker,Clark,Davis,Evans,Foster,Green,Hill";

    @Test
    public void conferenceGameTargetBySize() {
        assertEquals(0, ConferenceScheduleBuilder.conferenceGameTarget(1));
        assertEquals(1, ConferenceScheduleBuilder.conferenceGameTarget(2));
        assertEquals(8, ConferenceScheduleBuilder.conferenceGameTarget(9)); // odd size → even degree
        assertEquals(9, ConferenceScheduleBuilder.conferenceGameTarget(10));
        assertEquals(9, ConferenceScheduleBuilder.conferenceGameTarget(14)); // capped at max 9
        assertEquals(8, ConferenceScheduleBuilder.conferenceGameTarget(15)); // odd → even below cap
    }

    @Test
    public void oddConferencesNeverGetOddTargets() {
        for (int size = 3; size <= 20; size += 2) {
            int target = ConferenceScheduleBuilder.conferenceGameTarget(size);
            assertTrue("size " + size + " produced odd target " + target, target % 2 == 0);
            assertTrue(target <= size - 1);
            assertTrue(target <= 9);
        }
    }

    @Test
    public void pickLateRivalryRoundPrefersLatestWeekInBand() {
        Set<Integer> selected = new HashSet<>();
        selected.add(0);
        selected.add(2);
        selected.add(4);
        selected.add(5);
        selected.add(6);
        selected.add(7);
        // Conference weeks: 0,1,3,4,6,7,9,10,12
        int[] weekForSlot = {0, 1, 3, 4, 6, 7, 9, 10, 12};
        // Ordered rounds → weeks 0,1,3,4,6,7 → calendar …,7,8
        // Latest in band 8–11 is calendar week 8 → round 7
        assertEquals(7, ConferenceScheduleBuilder.pickLateRivalryRound(selected, weekForSlot));
    }

    @Test
    public void strongestInConfRivalryLandsInWeeks8To11() throws Exception {
        Path asset = Paths.get("src/main/assets/fbs_2026.csv");
        if (!Files.exists(asset)) {
            asset = Paths.get("app/src/main/assets/fbs_2026.csv");
        }
        String teamsCsv = new String(Files.readAllBytes(asset), StandardCharsets.UTF_8);
        League league = new League(FIRST_NAMES, LAST_NAMES, teamsCsv, false);
        league.prepareSeasonSchedule();

        Team ala = league.findTeamAbbr("ALA");
        Team aub = league.findTeamAbbr("AUB");
        assertNotNull(ala);
        assertNotNull(aub);
        assertEquals(ala.conference, aub.conference);
        Rivalry forward = ala.rivalryWith("AUB");
        Rivalry back = aub.rivalryWith("ALA");
        assertNotNull(forward);
        assertNotNull(back);
        assertTrue(Math.min(forward.strength, back.strength) >= Rivalry.SEAT_THRESHOLD);

        int week = -1;
        for (int w = 0; w < League.REGULAR_SEASON_WEEKS; w++) {
            Game game = ala.gameSchedule.get(w);
            if (game == null) {
                continue;
            }
            Team opp = game.homeTeam == ala ? game.awayTeam : game.homeTeam;
            if (opp == aub) {
                week = w;
                break;
            }
        }
        assertTrue("ALA–AUB in-conf game missing", week >= 0);
        int calendarWeek = week + 1;
        assertTrue(
                "Expected rivalry week in 8–11 but was " + calendarWeek,
                calendarWeek >= 8 && calendarWeek <= 11);
    }
}
