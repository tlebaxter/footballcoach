package CFBsimPack;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PlayerSeasonRecordTest {

    @Test
    public void saveTokenRoundTrip() {
        PlayerSeasonRecord original = new PlayerSeasonRecord();
        original.seasonYear = 2026;
        original.teamAbbr = "ABC";
        original.teamName = "Alpha:Beta";
        original.classYear = 3;
        original.gamesPlayed = 12;
        original.wins = 9;
        original.wonHeisman = true;
        original.wonAllAmerican = false;
        original.wonAllConference = true;
        original.position = "QB";
        original.passAtt = 300;
        original.passComp = 200;
        original.passYards = 3200;
        original.passTd = 28;
        original.passInt = 7;
        original.sacked = 15;
        original.rushAtt = 40;
        original.rushYards = 120;
        original.rushTd = 2;
        original.rushFumbles = 1;
        original.targets = 0;
        original.receptions = 0;
        original.recYards = 0;
        original.recTd = 0;
        original.drops = 0;
        original.recFumbles = 0;
        original.xpAtt = 0;
        original.xpMade = 0;
        original.fgAtt = 0;
        original.fgMade = 0;
        original.prAtt = 5;
        original.prYards = 88;
        original.prTd = 1;
        original.krAtt = 12;
        original.krYards = 310;
        original.krTd = 0;
        original.fairCatches = 2;
        original.puntAtt = 0;
        original.puntYards = 0;

        String token = original.toSaveToken();
        PlayerSeasonRecord loaded = PlayerSeasonRecord.fromSaveToken(token);
        assertNotNull(loaded);
        assertEquals(2026, loaded.seasonYear);
        assertEquals("ABC", loaded.teamAbbr);
        assertEquals("Alpha:Beta", loaded.teamName);
        assertEquals(3, loaded.classYear);
        assertEquals(12, loaded.gamesPlayed);
        assertEquals(9, loaded.wins);
        assertTrue(loaded.wonHeisman);
        assertFalse(loaded.wonAllAmerican);
        assertTrue(loaded.wonAllConference);
        assertEquals("QB", loaded.position);
        assertEquals(3200, loaded.passYards);
        assertEquals(28, loaded.passTd);
        assertEquals(7, loaded.passInt);
        assertEquals(120, loaded.rushYards);
        assertEquals(5, loaded.prAtt);
        assertEquals(88, loaded.prYards);
        assertEquals(1, loaded.prTd);
        assertEquals(12, loaded.krAtt);
        assertEquals(310, loaded.krYards);
    }

    @Test
    public void defenseSaveTokenRoundTrip() {
        PlayerSeasonRecord original = new PlayerSeasonRecord();
        original.seasonYear = 2027;
        original.teamAbbr = "DEF";
        original.teamName = "Defense U";
        original.classYear = 4;
        original.gamesPlayed = 13;
        original.wins = 10;
        original.position = "EDGE";
        original.puntAtt = 0;
        original.puntYards = 0;
        original.tackles = 55;
        original.tfl = 14;
        original.sacksDef = 11;
        original.defInt = 0;
        original.passDef = 2;
        original.forcedFumbles = 3;
        original.fumbleRec = 1;

        PlayerSeasonRecord loaded = PlayerSeasonRecord.fromSaveToken(original.toSaveToken());
        assertNotNull(loaded);
        assertEquals(55, loaded.tackles);
        assertEquals(14, loaded.tfl);
        assertEquals(11, loaded.sacksDef);
        assertEquals(0, loaded.defInt);
        assertEquals(2, loaded.passDef);
        assertEquals(3, loaded.forcedFumbles);
        assertEquals(1, loaded.fumbleRec);
        assertTrue(loaded.summaryLine().contains("55 Tck"));
    }

    @Test
    public void oldTokenWithoutDefenseFieldsLoadsZeros() {
        PlayerSeasonRecord original = new PlayerSeasonRecord();
        original.seasonYear = 2025;
        original.teamAbbr = "OLD";
        original.teamName = "Old School";
        original.classYear = 2;
        original.gamesPlayed = 11;
        original.wins = 4;
        original.position = "CB";
        original.puntAtt = 0;
        original.puntYards = 0;
        String full = original.toSaveToken();
        // Strip trailing defense fields (7 ints after puntYards)
        String[] parts = full.split(":");
        StringBuilder old = new StringBuilder();
        for (int i = 0; i <= 36; i++) {
            if (i > 0) old.append(':');
            old.append(parts[i]);
        }
        PlayerSeasonRecord loaded = PlayerSeasonRecord.fromSaveToken(old.toString());
        assertNotNull(loaded);
        assertEquals(0, loaded.tackles);
        assertEquals(0, loaded.sacksDef);
        assertEquals(0, loaded.defInt);
    }

    @Test
    public void fromSaveTokenRejectsShortTokens() {
        assertNull(PlayerSeasonRecord.fromSaveToken("too:short"));
    }

    @Test
    public void classStrMapsYears() {
        PlayerSeasonRecord r = new PlayerSeasonRecord();
        r.classYear = 1;
        assertEquals("Fr", r.classStr());
        r.classYear = 4;
        assertEquals("Sr", r.classStr());
        r.classYear = 5;
        assertEquals("Grad", r.classStr());
    }
}
