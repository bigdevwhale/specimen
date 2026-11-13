package app.specimen

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.specimen.data.Settings
import app.specimen.ui.StationRoot
import app.specimen.ui.theme.BureauTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        invite(intent)
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val seeding by container.seeding.collectAsStateWithLifecycle()
            // Wait for settings so the welcome doesn't flash for someone already here.
            settings?.takeIf { !seeding }?.let { s ->
                BureauTheme(s.theme) { StationRoot(s) }
            }
        }
    }

    // The link to the paired phone runs while the app is in front.
    override fun onStart() { super.onStart(); container.link.start("app") }
    override fun onStop() { container.link.stop("app"); super.onStop() }

    override fun onNewIntent(intent: android.content.Intent) { super.onNewIntent(intent); invite(intent) }

    /** Debug builds only: `am start ... --es invite <text>` pairs without the camera, for testing two phones. */
    private fun invite(intent: android.content.Intent?) {
        if (!BuildConfig.DEBUG) return
        val i = intent?.getStringExtra("invite")?.let(app.specimen.sync.Invite::decode) ?: return
        // «any» as the device: both phones wait with the same secret and pair with whoever turns up.
        if (i.deviceId == "any") container.write {
            settings.update { it.copy(secret = android.util.Base64.encodeToString(i.secret, android.util.Base64.NO_WRAP), partnerId = "", partnerName = "") }
            link.refresh()
        } else container.write { pair(i) }
    }
}
