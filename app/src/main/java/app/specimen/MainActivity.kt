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
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val seeding by container.seeding.collectAsStateWithLifecycle()
            // Wait for settings so the welcome doesn't flash for someone already here.
            settings?.takeIf { !seeding }?.let { s ->
                BureauTheme(s.theme) { StationRoot(s) }
            }
        }
    }
}
