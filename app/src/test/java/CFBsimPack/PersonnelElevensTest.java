package CFBsimPack;

import CFBsimPack.engine.OffenseConcept;
import CFBsimPack.engine.Playbook;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PersonnelElevensTest {

    private static final String FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J";
    private static final String LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T";

    @Test
    public void iForm21PutsFbEvenIfPhilosophyIsEleven() throws Exception {
        League league = createLeague();
        Team t = league.teamList.get(0);
        t.setOffPhilosophy(OffensivePhilosophy.AIR_RAID);
        assertEquals("10", t.offPhilosophy.defaultPersonnel);

        OffenseConcept power = Playbook.offenseById("i_dive");
        assertEquals("21", power.personnel);

        OnFieldEleven eleven = OnFieldEleven.forOffense(t, power.personnel);
        int fbSlots = 0;
        for (RoleTag role : eleven.roles) {
            if (role == RoleTag.FB) fbSlots++;
        }
        assertTrue("I-Form 21 must include an FB slot", fbSlots >= 1);
        assertEquals(11, eleven.players.size());
    }

    @Test
    public void empty10HasNoFb() throws Exception {
        League league = createLeague();
        Team t = league.teamList.get(0);
        OnFieldEleven eleven = OnFieldEleven.forOffense(t, "10");
        int fbSlots = 0;
        for (RoleTag role : eleven.roles) {
            if (role == RoleTag.FB) fbSlots++;
        }
        assertEquals(0, fbSlots);
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
