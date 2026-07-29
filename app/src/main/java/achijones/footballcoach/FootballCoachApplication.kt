package achijones.footballcoach

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import CFBsimPack.GameSession
import achijones.footballcoach.save.CareerPersistence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FootballCoachApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    val league = GameSession.getLeague() ?: return
                    if (!GameSession.hasActiveSaveSlot()) return
                    appScope.launch {
                        withContext(NonCancellable) {
                            CareerPersistence.saveActive(this@FootballCoachApplication, league)
                        }
                    }
                }
            },
        )
    }
}
