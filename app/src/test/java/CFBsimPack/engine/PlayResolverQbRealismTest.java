package CFBsimPack.engine;

import CFBsimPack.League;
import CFBsimPack.OnFieldEleven;
import CFBsimPack.Player;
import CFBsimPack.Team;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PlayResolverQbRealismTest {

    private static final String FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J";
    private static final String LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T";

    @Test
    public void gunQbDrawAlwaysPicksQb() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Player qb = offense.getQB(0);
        assertNotNull(qb);

        OffenseConcept draw = Playbook.offenseById("gun_qb_draw");
        OnFieldEleven eleven = OnFieldEleven.forOffense(offense, draw.personnel);
        PlayResolver resolver = new PlayResolver(new Random(1L));

        for (int i = 0; i < 40; i++) {
            Player carrier = resolver.pickCarrier(eleven, offense, draw);
            assertEquals("QB Draw carrier must be QB", qb, carrier);
        }
    }

    @Test
    public void highSpdIncreasesRpoKeepShare() {
        Player mobile = bareQb(92, 88);
        Player pocket = bareQb(55, 50);
        OffenseConcept zoneRead = Playbook.offenseById("pistol_zone_read");
        assertNotNull(zoneRead);

        int[] mobileDist = countBranches(mobile, zoneRead, CoverageCall.COVER_3, 1111L, 800);
        int[] pocketDist = countBranches(pocket, zoneRead, CoverageCall.COVER_3, 1111L, 800);
        assertTrue(
                "mobile dist give/keep/throw=" + mobileDist[0] + "/" + mobileDist[1] + "/" + mobileDist[2]
                        + " pocket=" + pocketDist[0] + "/" + pocketDist[1] + "/" + pocketDist[2],
                mobileDist[1] > pocketDist[1]);
    }

    @Test
    public void spyReducesRpoKeepShare() {
        Player mobile = bareQb(90, 85);
        OffenseConcept zoneRead = Playbook.offenseById("pistol_zone_read");

        int coverKeeps = countKeeps(mobile, zoneRead, CoverageCall.COVER_3, 2222L, 800);
        int spyKeeps = countKeeps(mobile, zoneRead, CoverageCall.SPY, 2222L, 800);
        assertTrue("cover keeps=" + coverKeeps + " spy=" + spyKeeps, spyKeeps < coverKeeps);
    }

    private static int countKeeps(
            Player qb, OffenseConcept concept, CoverageCall cov, long seed, int trials) {
        return countBranches(qb, concept, cov, seed, trials)[1];
    }

    private static int[] countBranches(
            Player qb, OffenseConcept concept, CoverageCall cov, long seed, int trials) {
        int[] counts = new int[3];
        // Wide seed stride — consecutive Random seeds share nearly-identical first draws
        for (int i = 0; i < trials; i++) {
            PlayResolver resolver = new PlayResolver(new Random(seed + i * 1_000_003L));
            PlayResolver.RpoBranch branch = resolver.chooseRpoBranch(qb, concept, cov);
            counts[branch.ordinal()]++;
        }
        return counts;
    }

    private static Player bareQb(int spd, int elu) {
        Player qb = new Player();
        qb.position = "QB";
        qb.name = "Test QB";
        qb.ratings = new CFBsimPack.PlayerRatings();
        qb.ratings.spd = spd;
        qb.ratings.elu = elu;
        qb.ratings.thv = 70;
        qb.ratings.tha = 75;
        qb.ratings.thp = 75;
        qb.applyRatings(qb.ratings);
        return qb;
    }

    private League createLeague() throws IOException {
        Path csvPath = Paths.get("src/main/assets/fbs_2026.csv");
        if (!Files.exists(csvPath)) {
            csvPath = Paths.get("app/src/main/assets/fbs_2026.csv");
        }
        String csv = new String(Files.readAllBytes(csvPath), StandardCharsets.UTF_8);
        League league = new League(FIRST_NAMES, LAST_NAMES, csv);
        league.userTeam = league.teamList.get(0);
        league.userTeam.userControlled = true;
        return league;
    }
}
