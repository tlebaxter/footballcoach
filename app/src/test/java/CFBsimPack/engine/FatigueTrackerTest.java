package CFBsimPack.engine;

import CFBsimPack.League;
import CFBsimPack.OnFieldEleven;
import CFBsimPack.Player;
import CFBsimPack.PlayerRatings;
import CFBsimPack.RoleTag;
import CFBsimPack.Team;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FatigueTrackerTest {

    private static final String FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J";
    private static final String LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T";

    @Test
    public void factorBandsMatchEnergyThresholds() {
        FatigueTracker ft = new FatigueTracker();
        Player p = player("RB");
        assertEquals(1.0, ft.factor(p), 0.001);

        ft.setEnergy(p, 70);
        assertEquals(1.0, ft.factor(p), 0.001);

        ft.setEnergy(p, 69);
        assertEquals(0.92, ft.factor(p), 0.001);

        ft.setEnergy(p, 40);
        assertEquals(0.92, ft.factor(p), 0.001);

        ft.setEnergy(p, 39);
        assertEquals(0.82, ft.factor(p), 0.001);
    }

    @Test
    public void fatiguedCopyScalesSkillKeysNotMeta() {
        FatigueTracker ft = new FatigueTracker();
        Player p = player("WR");
        p.ratings.spd = 100;
        p.ratings.hnd = 80;
        p.ratings.pot = 90;
        p.ratings.footIq = 88;
        ft.setEnergy(p, 30);

        PlayerRatings copy = ft.fatiguedCopy(p);
        assertEquals(82, copy.spd);
        assertEquals(66, copy.hnd);
        assertEquals(90, copy.pot);
        assertEquals(88, copy.footIq);
        assertEquals(100, p.ratings.spd);
    }

    @Test
    public void afterSnapDrainsBothElevensAndRecoversBench() throws Exception {
        League league = createLeague();
        Team offense = league.teamList.get(0);
        Team defense = league.teamList.get(1);
        FatigueTracker ft = new FatigueTracker();

        OnFieldEleven off = OnFieldEleven.forOffense(offense, "11", ft);
        OnFieldEleven def = OnFieldEleven.forDefense(defense, ft);
        Player offStarter = off.firstWithRole(RoleTag.RB);
        Player defStarter = def.players.get(0);
        Player benchRb = offense.getRB(1);
        assertTrue(offStarter != null && defStarter != null && benchRb != null);

        ft.setEnergy(offStarter, 80);
        ft.setEnergy(defStarter, 80);
        ft.setEnergy(benchRb, 50);

        ft.afterSnap(off, def);

        assertTrue("offense on-field should drain", ft.energyOf(offStarter) < 80);
        assertTrue("defense on-field should drain", ft.energyOf(defStarter) < 80);
        assertEquals("bench recovers +3", 53, ft.energyOf(benchRb));
    }

    @Test
    public void autoSubSitsTiredRbForFresherBackup() throws Exception {
        League league = createLeague();
        Team t = league.teamList.get(0);
        assertTrue(t.teamRBs.size() >= 2);

        Player rb1 = t.getRB(0);
        Player rb2 = t.getRB(1);
        FatigueTracker ft = new FatigueTracker();
        ft.setEnergy(rb1, 30);
        ft.setEnergy(rb2, 80);

        OnFieldEleven eleven = OnFieldEleven.forOffense(t, "11", ft);
        Player onFieldRb = eleven.firstWithRole(RoleTag.RB);
        assertEquals("tired RB1 should sit for fresh RB2", rb2, onFieldRb);
    }

    @Test
    public void keepsTiredStarterWhenBackupIsNotFreshEnough() throws Exception {
        League league = createLeague();
        Team t = league.teamList.get(0);
        assertTrue(t.teamRBs.size() >= 2);
        FatigueTracker ft = new FatigueTracker();
        for (Player rb : t.teamRBs) {
            ft.setEnergy(rb, 45);
        }
        ft.setEnergy(t.getRB(0), 30);

        OnFieldEleven eleven = OnFieldEleven.forOffense(t, "11", ft);
        assertEquals("no backup at FRESH_ENERGY — starter stays", t.getRB(0), eleven.firstWithRole(RoleTag.RB));
    }

    @Test
    public void leastTiredFallbackWhenEntireDepthIsGassed() throws Exception {
        League league = createLeague();
        Team t = league.teamList.get(0);
        assertTrue(t.teamRBs.size() >= 2);
        FatigueTracker ft = new FatigueTracker();
        for (Player rb : t.teamRBs) {
            ft.setEnergy(rb, 20);
        }
        ft.setEnergy(t.getRB(0), 25);
        ft.setEnergy(t.getRB(1), 35);

        OnFieldEleven eleven = OnFieldEleven.forOffense(t, "11", ft);
        assertEquals(t.getRB(1), eleven.firstWithRole(RoleTag.RB));
    }

    @Test
    public void fatiguedCompositesAreSofterThanFresh() throws Exception {
        League league = createLeague();
        Team home = league.teamList.get(0);
        FatigueTracker ft = new FatigueTracker();
        for (Player ol : home.teamOLs) {
            ft.setEnergy(ol, 30);
        }
        int freshOl = OnFieldEleven.forOffense(home, "11", null).olPassComposite();
        int fatiguedOl = OnFieldEleven.forOffense(home, "11", ft).olPassComposite();
        assertTrue("fatigued OL composite should be softer than fresh", fatiguedOl < freshOl);
    }

    @Test
    public void playResolverRatingAppliesFactor() {
        Player qb = player("QB");
        qb.ratings.tha = 100;
        FatigueTracker ft = new FatigueTracker();
        ft.setEnergy(qb, 30);

        PlayResolver resolver = new PlayResolver(new java.util.Random(1L));
        resolver.setFatigueTracker(ft);

        // Mirror PlayResolver.rating: round(raw * factor)
        int expected = (int) Math.round(100 * ft.factor(qb));
        assertEquals(82, expected);
        assertEquals(82, ft.fatiguedCopy(qb).tha);
    }

    private static Player player(String pos) {
        Player p = new Player();
        p.position = pos;
        p.ratings = new PlayerRatings();
        p.ratings.spd = 80;
        p.ratings.endu = 60;
        return p;
    }

    private static League createLeague() throws IOException {
        Path csvPath = Paths.get("src/main/assets/fbs_2026.csv");
        if (!Files.exists(csvPath)) {
            csvPath = Paths.get("app/src/main/assets/fbs_2026.csv");
        }
        String csv = new String(Files.readAllBytes(csvPath), StandardCharsets.UTF_8);
        return new League(FIRST_NAMES, LAST_NAMES, csv);
    }
}
