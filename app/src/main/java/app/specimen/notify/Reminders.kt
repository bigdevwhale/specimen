package app.specimen.notify

import app.specimen.ui.Words
import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.specimen.MainActivity
import app.specimen.R
import app.specimen.container
import app.specimen.domain.Due
import app.specimen.domain.weekOf
import app.specimen.localized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Once a day the bureau sends one message, most important first: a sign to show a vet, life support due today,
 * an experiment ready to be concluded, or – on Monday – last week's reports.
 */
object Reminders {
    private const val CHANNEL = "station"

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        CoroutineScope(Dispatchers.Main).launch {
            val hour = runCatching { ctx.container.settings.now().hour }.getOrDefault(10)
            var at = LocalDateTime.of(LocalDate.now(), LocalTime.of(hour, 0))
            if (at.isBefore(LocalDateTime.now())) at = at.plusDays(1)
            val ms = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            am.setInexactRepeating(AlarmManager.RTC, ms, AlarmManager.INTERVAL_DAY, pending(ctx))
        }
    }

    private fun pending(ctx: Context) = PendingIntent.getBroadcast(ctx, 1, Intent(ctx, Look::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    suspend fun look(app: Context) {
        val c = app.container
        val s = c.settings.now()
        if (!s.remind || !s.onboarded) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val lab = c.lab()
        val ctx = app.localized()
        val today = lab.day
        val news = mutableListOf<Triple<String, String, String>>() // key, title, text
        lab.here.forEach { sp ->
            lab.anomalies(sp).filter { it.urgent }.firstOrNull()?.let {
                news += Triple("anomaly:${sp.id}:$today", ctx.getString(R.string.n_anomaly, sp.name), Words.anomaly(ctx, it))
            }
        }
        lab.here.forEach { sp ->
            val due = lab.dues(sp.id).filter { it.state == Due.State.TODAY || it.state == Due.State.OVERDUE }
            if (due.isNotEmpty()) news += Triple("due:${sp.id}:$today", ctx.getString(R.string.n_due, sp.name),
                due.take(3).joinToString(", ") { it.name.of(ctx.resources.configuration.locales[0].language == "ru").lowercase() }.replaceFirstChar { it.uppercase() })
        }
        lab.trials.filter { it.open && today - it.start >= it.days }.forEach { t ->
            news += Triple("trial:${t.id}", ctx.getString(R.string.n_trial), t.subject)
        }
        if (LocalDate.now().dayOfWeek == DayOfWeek.MONDAY) {
            val last = weekOf(today) - 7
            val ready = lab.here.filter { last in lab.reportWeeks(it.id) }
            if (ready.isNotEmpty()) news += Triple("report:$last", ctx.getString(R.string.n_report), ready.joinToString(", ") { it.name })
        }
        val fresh = news.filter { it.first !in s.told }
        val n = fresh.firstOrNull() ?: return
        val nm = app.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, ctx.getString(R.string.channel), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(app, 0, Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE)
        val note = NotificationCompat.Builder(app, CHANNEL).setSmallIcon(R.drawable.ic_notify).setContentTitle(n.second).setContentText(n.third)
            .setStyle(NotificationCompat.BigTextStyle().bigText(n.third)).setContentIntent(open).setAutoCancel(true).build()
        runCatching { NotificationManagerCompat.from(app).notify(7, note) }
        c.settings.update { st -> st.copy(told = st.told + n.first) }
    }

    class Look : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val done = goAsync()
            CoroutineScope(Dispatchers.Main).launch {
                try { look(context.applicationContext) } finally { done.finish() }
            }
        }
    }

    /** Alarms don't survive a restart. */
    class Boot : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) schedule(context)
        }
    }
}
