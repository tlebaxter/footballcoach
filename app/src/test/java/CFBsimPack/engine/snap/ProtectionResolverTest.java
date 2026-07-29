package CFBsimPack.engine.snap;

import CFBsimPack.League;
import CFBsimPack.OnFieldEleven;
import CFBsimPack.Player;
import CFBsimPack.Team;
import CFBsimPack.engine.playdef.ProtectionScheme;
import CFBsimPack.engine.playdef.ProtectionType;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ProtectionResolverTest {

    private static final String FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J";
    private static final String LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T";

    @Test
    public void weakLtVsEliteEdgeProducesEarlierPressure() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);

        double earlyWeak = medianEarliest(offense, defense, true, 11L);
        double earlyStrong = medianEarliest(offense, defense, false, 11L);
        assertTrue("weak=" + earlyWeak + " strong=" + earlyStrong, earlyWeak < earlyStrong);
    }

    @Test
    public void emptyProtectionIsRiskierThanMaxProtect() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);
        OnFieldEleven off = OnFieldEleven.forOffense(offense, "10");
        OnFieldEleven def = OnFieldEleven.forDefense(defense);

        ProtectionResolver resolver = new ProtectionResolver(new Random(99L));
        SituationMods sit = new SituationMods(0, 0, false, false, false, 0, false);
        double emptySum = 0;
        double maxSum = 0;
        int n = 80;
        for (int i = 0; i < n; i++) {
            ProtectionResolver r = new ProtectionResolver(new Random(500L + i));
            ProtectionResult empty = r.resolve(off, def,
                    new ProtectionScheme(ProtectionType.EMPTY_FIVE, 1.0, Collections.emptySet(), false),
                    sit, null, 1.0, 0, 1.0);
            ProtectionResult max = r.resolve(off, def, ProtectionScheme.maxProtect(),
                    sit, null, 1.0, 0, 1.0);
            emptySum += empty.earliestPressureSec;
            maxSum += max.earliestPressureSec;
        }
        assertTrue("empty=" + (emptySum / n) + " max=" + (maxSum / n), emptySum / n < maxSum / n);
    }

    private double medianEarliest(Team offense, Team defense, boolean weakenLt, long seed) throws Exception {
        OnFieldEleven off = OnFieldEleven.forOffense(offense, "11");
        OnFieldEleven def = OnFieldEleven.forDefense(defense);
        // Adjust LT / EDGE ratings on roster players used by elevens
        for (int i = 0; i < off.players.size(); i++) {
            Player p = off.players.get(i);
            if (off.roles.get(i) == CFBsimPack.RoleTag.OL && p.ratings != null) {
                if (weakenLt) {
                    p.ratings.pbk = 30;
                    p.ratings.stre = 35;
                } else {
                    p.ratings.pbk = 90;
                    p.ratings.stre = 88;
                }
                p.applyRatings(p.ratings);
            }
        }
        for (int i = 0; i < def.players.size(); i++) {
            Player p = def.players.get(i);
            if (def.roles.get(i) == CFBsimPack.RoleTag.EDGE && p.ratings != null) {
                p.ratings.prs = 95;
                p.ratings.spd = 90;
                p.applyRatings(p.ratings);
            }
        }
        double[] samples = new double[60];
        for (int i = 0; i < samples.length; i++) {
            ProtectionResult res = new ProtectionResolver(new Random(seed + i)).resolve(
                    off, def, ProtectionScheme.infer(null, false, false),
                    new SituationMods(0, 0, false, false, false, 0, false),
                    null, 1.0, 0, 1.0);
            assertNotNull(res);
            samples[i] = res.earliestPressureSec;
        }
        java.util.Arrays.sort(samples);
        return samples[samples.length / 2];
    }

    private League createLeague() throws IOException {
        Path csvPath = Paths.get("src/main/assets/fbs_2026.csv");
        if (!Files.exists(csvPath)) {
            csvPath = Paths.get("app/src/main/assets/fbs_2026.csv");
        }
        String csv = new String(Files.readAllBytes(csvPath), StandardCharsets.UTF_8);
        League league = new League(FIRST_NAMES, LAST_NAMES, csv);
        league.userTeam = league.teamList.get(0);
        return league;
    }
}
