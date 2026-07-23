package achijones.footballcoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import achijones.footballcoach.ui.FootballCoachApp
import achijones.footballcoach.ui.theme.FcTheme

class FootballCoachActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FcTheme {
                FootballCoachApp()
            }
        }
    }
}
