package app.specimen.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.specimen.MainActivity
import app.specimen.R
import app.specimen.container
import app.specimen.localized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The two phones keeping in step with the app closed: a quiet foreground service that listens for the other phone,
 * so whatever was logged there is already here when you look. Only when the person turned it on, only while paired,
 * and only at home: on a network where the two phones have met before (until they've met, any Wi-Fi will do).
 */
class LinkService : Service() {
    private var scope: CoroutineScope? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val c = container
        val started = runCatching {
            ServiceCompat.startForeground(this, ID, note(home = true), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        }.isSuccess
        if (!started) { stopSelf(); return START_NOT_STICKY }
        if (scope == null) {
            val s = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            scope = s
            s.launch {
                c.homeNet.here.combine(c.settings.settings.map { it.homes }) { here, homes ->
                    here != null && (homes.isEmpty() || here in homes)
                }.distinctUntilChanged().collect { home ->
                    if (home) c.link.start("service") else c.link.stop("service")
                    runCatching { ServiceCompat.startForeground(this@LinkService, ID, note(home), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE) }
                }
            }
        }
        return START_STICKY
    }

    private fun note(home: Boolean): Notification {
        val l = localized()
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL, l.getString(R.string.link_channel), NotificationManager.IMPORTANCE_MIN))
        val open = PendingIntent.getActivity(this, 2, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(l.getString(if (home) R.string.link_note_home else R.string.link_note_away))
            .setContentText(l.getString(R.string.link_note_text))
            .setContentIntent(open)
            .setOngoing(true).setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    override fun onDestroy() {
        scope?.cancel()
        scope = null
        container.link.stop("service")
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "link"
        private const val ID = 11
        fun intent(context: Context) = Intent(context, LinkService::class.java)

        /** Starts or stops the service to match the settings. */
        fun sync(context: Context, on: Boolean) {
            if (on) runCatching { ContextCompat.startForegroundService(context, intent(context)) }
            else context.stopService(intent(context))
        }
    }
}

/** After a restart the link goes back to listening, if it was. */
class LinkBoot : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val c = context.container
        val done = goAsync()
        c.scope.launch {
            val s = c.settings.settings.first()
            if (s.paired && s.hum) LinkService.sync(context, true)
            done.finish()
        }
    }
}
