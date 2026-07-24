package CFBsimPack;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DepthChartCrossPosTest {

    private static final String FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J";
    private static final String LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T";

    @Test
    public void rbCanFillFbSlotViaOvr() throws Exception {
        League league = createLeague();
        Team t = league.teamList.get(0);
        t.teamFBs.clear();
        Player rb = t.getRB(0);
        assertNotNull(rb);
        rb.ratings.rbk = 88;
        rb.ratings.stre = 85;
        rb.applyRatings(rb.ratings);

        OnFieldEleven eleven = OnFieldEleven.forOffense(t, "21");
        boolean hasFbRole = false;
        for (RoleTag role : eleven.roles) {
            if (role == RoleTag.FB) hasFbRole = true;
        }
        assertTrue(hasFbRole);
        assertTrue(eleven.players.size() >= 10);
    }

    @Test
    public void edgeSlotCanFillFromLb() throws Exception {
        League league = createLeague();
        Team t = league.teamList.get(0);
        // Nickel has an EDGE slot; 4-3 uses DE only
        t.setDefSystem(DefensiveSystem.NICKEL);
        t.teamEDGEs.clear();
        Player lb = t.getLB(0);
        assertNotNull(lb);
        lb.ratings.prs = 90;
        lb.ratings.spd = 85;
        lb.applyRatings(lb.ratings);

        OnFieldEleven def = OnFieldEleven.forDefense(t);
        boolean edgeFilled = false;
        for (int i = 0; i < def.roles.size(); i++) {
            if (def.roles.get(i) == RoleTag.EDGE) {
                edgeFilled = def.players.get(i) != null;
            }
        }
        assertTrue(edgeFilled);
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
