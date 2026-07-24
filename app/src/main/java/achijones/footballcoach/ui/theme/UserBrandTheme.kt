package achijones.footballcoach.ui.theme

import CFBsimPack.Team
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Compose-readable identity of the user's committed team brand.
 * Unset while the team picker is active or before a career is loaded.
 */
object UserBrandTheme {
    var teamName: String? by mutableStateOf(null)
        private set
    var abbr: String? by mutableStateOf(null)
        private set

    val isSet: Boolean
        get() = !teamName.isNullOrBlank() && !abbr.isNullOrBlank()

    fun set(name: String, teamAbbr: String) {
        teamName = name
        abbr = teamAbbr
    }

    fun setFrom(team: Team?) {
        if (team == null) {
            clear()
            return
        }
        set(team.name, team.abbr)
    }

    fun clear() {
        teamName = null
        abbr = null
    }
}
