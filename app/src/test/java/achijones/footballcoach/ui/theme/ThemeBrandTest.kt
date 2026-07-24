package achijones.footballcoach.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeBrandTest {

    @After
    fun tearDown() {
        UserBrandTheme.clear()
    }

    @Test
    fun userBrandTheme_setAndClear() {
        assertFalse(UserBrandTheme.isSet)
        UserBrandTheme.set("Alabama", "ALA")
        assertTrue(UserBrandTheme.isSet)
        assertEquals("Alabama", UserBrandTheme.teamName)
        assertEquals("ALA", UserBrandTheme.abbr)
        UserBrandTheme.clear()
        assertFalse(UserBrandTheme.isSet)
        assertEquals(null, UserBrandTheme.teamName)
        assertEquals(null, UserBrandTheme.abbr)
    }

    @Test
    fun onColorFor_picksDarkTextOnLightSurfaces() {
        assertEquals(Color(0xFF121212), onColorFor(Color.White))
        assertEquals(Color(0xFF121212), onColorFor(Color(0xFFD3BC8D))) // Army gold
    }

    @Test
    fun onColorFor_picksWhiteTextOnDarkSurfaces() {
        assertEquals(Color.White, onColorFor(Color(0xFF9E1B32))) // Alabama crimson
        assertEquals(Color.White, onColorFor(Color(0xFF0047BA))) // BYU blue
        assertEquals(Color.White, onColorFor(Color.Black))
    }

    @Test
    fun darkenBrand_reducesChannels() {
        val crimson = Color(0xFF9E1B32)
        val darkened = darkenBrand(crimson, factor = 0.45f)
        assertTrue(darkened.red < crimson.red)
        assertTrue(darkened.green < crimson.green)
        assertTrue(darkened.blue < crimson.blue)
        assertEquals(1f, darkened.alpha)
    }
}
