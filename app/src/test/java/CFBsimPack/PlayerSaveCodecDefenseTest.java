package CFBsimPack;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PlayerSaveCodecDefenseTest {

    @Test
    public void defenseSeasonSuffixRoundTripsSkills() {
        Player edge = defender("Sack Artist", PositionGroup.EDGE);
        edge.gamesPlayed = 8;
        edge.statsWins = 5;
        edge.seasonSnaps = 412;
        edge.seasonStats.tackles = 44;
        edge.seasonStats.tfl = 9;
        edge.seasonStats.sacksDef = 7;
        edge.seasonStats.defInt = 1;
        edge.seasonStats.passDef = 3;
        edge.seasonStats.forcedFumbles = 2;
        edge.seasonStats.fumbleRec = 1;

        String suffix = PlayerSaveCodec.seasonSuffix(edge);
        assertTrue(suffix.startsWith("|SEASON,"));
        assertTrue(suffix.contains(",44,9,7,1,3,2,1,"));

        Player loaded = defender("Sack Artist", PositionGroup.EDGE);
        PlayerSaveCodec.loadSeasonFromSuffix(loaded, suffix);
        assertEquals(8, loaded.gamesPlayed);
        assertEquals(5, loaded.statsWins);
        assertEquals(412, loaded.seasonSnaps);
        assertEquals(44, loaded.seasonStats.tackles);
        assertEquals(9, loaded.seasonStats.tfl);
        assertEquals(7, loaded.seasonStats.sacksDef);
        assertEquals(1, loaded.seasonStats.defInt);
        assertEquals(3, loaded.seasonStats.passDef);
        assertEquals(2, loaded.seasonStats.forcedFumbles);
        assertEquals(1, loaded.seasonStats.fumbleRec);
    }

    @Test
    public void oldSkillLessDefenseSeasonStillLoadsSnapsAndInjury() {
        Player cb = defender("Legacy CB", PositionGroup.CB);
        // Old format: gp,wins,snaps,injury,ejected — no skill fields
        PlayerSaveCodec.loadSeasonFromSuffix(cb, "|SEASON,10,6,300,Knee:2,0");
        assertEquals(10, cb.gamesPlayed);
        assertEquals(6, cb.statsWins);
        assertEquals(300, cb.seasonSnaps);
        assertEquals(0, cb.seasonStats.tackles);
        assertEquals(0, cb.seasonStats.defInt);
        assertTrue(cb.isInjured);
        assertNotNull(cb.injury);
        assertEquals(2, cb.injury.getDuration());
        assertFalse(cb.isEjected);
    }

    @Test
    public void defenseCareerLineRoundTripsSkills() {
        Player lb = defender("Mike", PositionGroup.LB);
        lb.careerGamesPlayed = 30;
        lb.careerSnaps = 1800;
        lb.careerStats.tackles = 210;
        lb.careerStats.tfl = 28;
        lb.careerStats.sacksDef = 12;
        lb.careerStats.defInt = 4;
        lb.careerStats.passDef = 11;
        lb.careerStats.forcedFumbles = 5;
        lb.careerStats.fumbleRec = 3;
        lb.careerHeismans = 0;
        lb.careerAllAmerican = 1;
        lb.careerAllConference = 2;
        lb.careerWins = 22;

        // Real team lines append roster status after career CSV (needed for snaps heuristic).
        String line = PlayerSaveCodec.toLine(lb) + "," + lb.rosterStatusSave();
        String[] fields = line.split(",", -1);
        Player loaded = PlayerSaveCodec.fromFields(null, fields, false);
        assertNotNull(loaded);
        assertEquals(30, loaded.careerGamesPlayed);
        assertEquals(1800, loaded.careerSnaps);
        assertEquals(210, loaded.careerStats.tackles);
        assertEquals(28, loaded.careerStats.tfl);
        assertEquals(12, loaded.careerStats.sacksDef);
        assertEquals(4, loaded.careerStats.defInt);
        assertEquals(11, loaded.careerStats.passDef);
        assertEquals(5, loaded.careerStats.forcedFumbles);
        assertEquals(3, loaded.careerStats.fumbleRec);
        assertEquals(1, loaded.careerAllAmerican);
        assertEquals(2, loaded.careerAllConference);
        assertEquals(22, loaded.careerWins);
    }

    @Test
    public void oldDefenseCareerWithRosterDoesNotMisreadAwardsAsSkills() {
        Player template = defender("Legacy Career CB", PositionGroup.CB);
        String[] fields = buildOldDefenseCareerFields(template, 20, 900, 0, 0, 1, 12);
        Player loaded = PlayerSaveCodec.fromFields(null, fields, false);
        assertNotNull(loaded);
        assertEquals(20, loaded.careerGamesPlayed);
        assertEquals(900, loaded.careerSnaps);
        assertEquals(0, loaded.careerStats.tackles);
        assertEquals(0, loaded.careerStats.sacksDef);
        assertEquals(1, loaded.careerAllConference);
        assertEquals(12, loaded.careerWins);
    }

    /** Full player CSV with old skill-less defense career (games,snaps,awards,roster). */
    private static String[] buildOldDefenseCareerFields(
            Player template, int games, int snaps, int heis, int aa, int ac, int wins) {
        String modern = PlayerSaveCodec.toLine(template) + "," + template.rosterStatusSave();
        String[] f = modern.split(",", -1);
        // Modern career tail: games, snaps, 7 skills, heis, aa, ac, wins, roster (14 fields)
        int careerStart = f.length - 14;
        ArrayList<String> out = new ArrayList<>();
        for (int i = 0; i < careerStart; i++) {
            out.add(f[i]);
        }
        out.add(String.valueOf(games));
        out.add(String.valueOf(snaps));
        out.add(String.valueOf(heis));
        out.add(String.valueOf(aa));
        out.add(String.valueOf(ac));
        out.add(String.valueOf(wins));
        out.add(template.rosterStatusSave());
        return out.toArray(new String[0]);
    }

    private static Player defender(String name, PositionGroup group) {
        PlayerRatings bag = PlayerFactory.rollRatings(group, 3, 4, new Random(42L));
        return PlayerFactory.fromRatings(group, name, null, 3, bag, false);
    }
}
