package CFBsimPack;

import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GeoCatalogTest {

    @Before
    public void reset() {
        GeoCatalog.resetForTests();
    }

    @Test
    public void loadsSchoolsAndPlaces() {
        GeoCatalog geo = GeoCatalog.get();
        assertNotNull(geo.school("ALA"));
        assertTrue(geo.school("ALA").lat > 30);
        assertNotNull(geo.sampleHometown(new Random(1L)));
    }

    @Test
    public void haversineBirminghamTuscaloosaReasonable() {
        double miles = GeoCatalog.haversineMiles(33.52, -86.80, 33.21, -87.57);
        assertTrue(miles > 40 && miles < 70);
    }

    @Test
    public void distanceMultiplierCloserIsCheaper() {
        assertTrue(GeoCatalog.distanceMultiplier(25) < GeoCatalog.distanceMultiplier(500));
    }

    @Test
    public void sampleHometownDeterministic() {
        GeoCatalog geo = GeoCatalog.get();
        GeoCatalog.Place a = geo.sampleHometown(new Random(42L));
        GeoCatalog.Place b = geo.sampleHometown(new Random(42L));
        assertEquals(a.geoidfq, b.geoidfq);
    }
}
