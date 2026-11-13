package app.specimen.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    /** 0: night, 1: day, 2: as the system. */
    val theme: Int = 2,
    /** The researcher's name for the signature under reports. */
    val researcher: String = "",
    /** Daily reminders on. */
    val remind: Boolean = true,
    /** Hour of the daily reminder. */
    val hour: Int = 10,
    /** Keys already told by notifications, so each is told once. */
    val told: Set<String> = emptySet(),
    /** Screens whose first-visit tip was already shown. */
    val tips: Set<String> = emptySet(),
    val tipsOff: Boolean = false,
    /** Lessons whose quiz was answered right. */
    val lessons: Set<String> = emptySet(),
    /** Lessons whose field task was done. */
    val tasks: Set<String> = emptySet(),
    /** Report weeks opened («specimen:weekStart»). */
    val read: Set<String> = emptySet(),
    /** Report designs bought with grants. */
    val skins: Set<String> = emptySet(),
    /** The design reports are typed in. */
    val skin: String = "typed",
    /** Grants spent on designs. */
    val spent: Int = 0,
    /** Experiments the ethics committee turned down (a badge, of sorts). */
    val refused: Int = 0,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.set(k: Preferences.Key<String>) = this[k]?.split(',')?.filter { it.isNotBlank() }?.toSet() ?: emptySet()

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        theme = this[THEME] ?: 2,
        researcher = this[RESEARCHER] ?: "",
        remind = this[REMIND] ?: true,
        hour = this[HOUR] ?: 10,
        told = set(TOLD),
        tips = set(TIPS),
        tipsOff = this[TIPS_OFF] ?: false,
        lessons = set(LESSONS),
        tasks = set(TASKS),
        read = set(READ),
        skins = set(SKINS),
        skin = this[SKIN] ?: "typed",
        spent = this[SPENT] ?: 0,
        refused = this[REFUSED] ?: 0,
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[THEME] = s.theme
            it[RESEARCHER] = s.researcher
            it[REMIND] = s.remind
            it[HOUR] = s.hour
            it[TOLD] = s.told.toList().takeLast(300).joinToString(",")
            it[TIPS] = s.tips.joinToString(",")
            it[TIPS_OFF] = s.tipsOff
            it[LESSONS] = s.lessons.joinToString(",")
            it[TASKS] = s.tasks.joinToString(",")
            it[READ] = s.read.toList().takeLast(400).joinToString(",")
            it[SKINS] = s.skins.joinToString(",")
            it[SKIN] = s.skin
            it[SPENT] = s.spent
            it[REFUSED] = s.refused
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val THEME = intPreferencesKey("theme")
        val RESEARCHER = stringPreferencesKey("researcher")
        val REMIND = booleanPreferencesKey("remind")
        val HOUR = intPreferencesKey("hour")
        val TOLD = stringPreferencesKey("told")
        val TIPS = stringPreferencesKey("tips")
        val TIPS_OFF = booleanPreferencesKey("tips_off")
        val LESSONS = stringPreferencesKey("lessons")
        val TASKS = stringPreferencesKey("tasks")
        val READ = stringPreferencesKey("read")
        val SKINS = stringPreferencesKey("skins")
        val SKIN = stringPreferencesKey("skin")
        val SPENT = intPreferencesKey("spent")
        val REFUSED = intPreferencesKey("refused")
    }
}
