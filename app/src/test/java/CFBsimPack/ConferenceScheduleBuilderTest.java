package CFBsimPack;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConferenceScheduleBuilderTest {

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
}
