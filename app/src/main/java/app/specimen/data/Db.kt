package app.specimen.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/*
 * The station's files. A specimen (the pet) has observations (mood, weight, appetite, symptoms, notes),
 * experiments (a new food, a medicine, a move – each with a hypothesis and an outcome), life-support protocols
 * (vaccines, claws, litter) with a log of when they were done, and field photos tagged with behaviours.
 * Every row carries a [uid] and an [updated] stamp so two phones can later be merged row by row.
 */

fun uid(): String = UUID.randomUUID().toString()
fun now(): Long = System.currentTimeMillis()

@Entity
data class Specimen(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The catalogue number painted on the file: «ОБР-001». */
    val num: Int,
    val name: String,
    /** A [app.specimen.domain.Planet] key. */
    val planet: String,
    /** A breed key from breeds.tsv. */
    val breed: String,
    /** 0 unknown, 1 male, 2 female. */
    val sex: Int = 0,
    /** Epoch day of birth, 0 if unknown. */
    val born: Long = 0,
    /** Epoch day it came to live here. */
    val arrived: Long,
    val neutered: Boolean = false,
    /** The shot used as its portrait, 0 = the bureau's pictogram. */
    val portrait: Long = 0,
    /** Epoch day the mission ended (the pet died or left), 0 while it lives here. */
    val gone: Long = 0,
    val goneNote: String = "",
    val uid: String = uid(),
    val updated: Long = now(),
) {
    val here get() = gone == 0L
}

/** What an observation records. */
object Ob {
    /** value = a [app.specimen.domain.Mood] ordinal. */
    const val MOOD = 1
    /** value = kilograms. */
    const val WEIGHT = 2
    /** value: 0 refused, 1 poor, 2 as usual, 3 greedy. */
    const val APPETITE = 3
    /** value: 0 normal, 1 soft, 2 loose, 3 none (strains or nothing). */
    const val STOOL = 4
    /** value = a [app.specimen.domain.Symptom] ordinal. */
    const val SYMPTOM = 5
    /** Body condition score 1–9. */
    const val CONDITION = 6
    /** A free field note in [Obs.note]. */
    const val NOTE = 7
}

@Entity
data class Obs(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val specimen: Long,
    val day: Long,
    /** Minutes since midnight. */
    val at: Int = 0,
    val kind: Int,
    val value: Float = 0f,
    val note: String = "",
    val uid: String = uid(),
    val updated: Long = now(),
)

@Entity
data class Trial(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val specimen: Long,
    /** A [app.specimen.domain.TrialKind] ordinal. */
    val kind: Int,
    /** What was tried: «Корм Purina Pro Plan с индейкой», «Милбемакс». */
    val subject: String,
    /** The expected [app.specimen.domain.Outcome] ordinal. */
    val guess: Int,
    val start: Long,
    /** How many days to watch before concluding. */
    val days: Int,
    /** The [app.specimen.domain.Outcome] ordinal, -1 while it runs. */
    val outcome: Int = -1,
    /** [app.specimen.domain.Reaction] names seen, comma-separated. */
    val reactions: String = "",
    val note: String = "",
    val ended: Long = 0,
    val uid: String = uid(),
    val updated: Long = now(),
) {
    val open get() = outcome < 0
    val reactionList get() = reactions.split(',').filter { it.isNotBlank() }
}

@Entity
data class Protocol(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val specimen: Long,
    /** A key from protocols.tsv, or «u» for the researcher's own. */
    val key: String,
    /** The researcher's own name for it (only for «u»). */
    val title: String = "",
    /** Every so many days; 0 = once or as needed. */
    val every: Int,
    /** The epoch day it was last done, 0 if unknown. */
    val last: Long = 0,
    val note: String = "",
    val off: Boolean = false,
    val uid: String = uid(),
    val updated: Long = now(),
) {
    /** The epoch day it is due again, null if unknown or not recurring. */
    val due: Long? get() = if (last == 0L || every <= 0) null else last + every
}

@Entity
data class Done(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val protocol: Long,
    val specimen: Long,
    val day: Long,
    /** Was it overdue when done. */
    val late: Boolean = false,
    val note: String = "",
    val uid: String = uid(),
    val updated: Long = now(),
)

@Entity
data class Shot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val specimen: Long,
    val day: Long,
    /** Behaviour keys, comma-separated, first is the main one. */
    val tags: String = "",
    val note: String = "",
    val uid: String = uid(),
    val updated: Long = now(),
) {
    val tagList get() = tags.split(',').filter { it.isNotBlank() }
}

@Dao
interface StationDao {
    @Query("SELECT * FROM Specimen ORDER BY num") fun specimens(): Flow<List<Specimen>>
    @Query("SELECT * FROM Specimen ORDER BY num") suspend fun specimensNow(): List<Specimen>
    @Query("SELECT * FROM Specimen WHERE id = :id") suspend fun specimen(id: Long): Specimen?
    @Query("SELECT COALESCE(MAX(num), 0) FROM Specimen") suspend fun lastNum(): Int

    @Query("SELECT * FROM Obs ORDER BY day, at, id") fun obs(): Flow<List<Obs>>
    @Query("SELECT * FROM Obs ORDER BY day, at, id") suspend fun obsNow(): List<Obs>

    @Query("SELECT * FROM Trial ORDER BY start DESC, id DESC") fun trials(): Flow<List<Trial>>
    @Query("SELECT * FROM Trial ORDER BY start DESC, id DESC") suspend fun trialsNow(): List<Trial>
    @Query("SELECT * FROM Trial WHERE id = :id") suspend fun trial(id: Long): Trial?

    @Query("SELECT * FROM Protocol ORDER BY id") fun protocols(): Flow<List<Protocol>>
    @Query("SELECT * FROM Protocol ORDER BY id") suspend fun protocolsNow(): List<Protocol>
    @Query("SELECT * FROM Protocol WHERE id = :id") suspend fun protocol(id: Long): Protocol?

    @Query("SELECT * FROM Done ORDER BY day, id") fun done(): Flow<List<Done>>
    @Query("SELECT * FROM Done ORDER BY day, id") suspend fun doneNow(): List<Done>

    @Query("SELECT * FROM Shot ORDER BY day DESC, id DESC") fun shots(): Flow<List<Shot>>
    @Query("SELECT * FROM Shot ORDER BY day DESC, id DESC") suspend fun shotsNow(): List<Shot>
    @Query("SELECT * FROM Shot WHERE id = :id") suspend fun shot(id: Long): Shot?

    @Insert suspend fun add(s: Specimen): Long
    @Insert suspend fun add(o: Obs): Long
    @Insert suspend fun add(t: Trial): Long
    @Insert suspend fun add(p: Protocol): Long
    @Insert suspend fun add(d: Done): Long
    @Insert suspend fun add(s: Shot): Long

    @Update suspend fun save(s: Specimen)
    @Update suspend fun save(o: Obs)
    @Update suspend fun save(t: Trial)
    @Update suspend fun save(p: Protocol)
    @Update suspend fun save(s: Shot)

    @Delete suspend fun drop(s: Specimen)
    @Delete suspend fun drop(o: Obs)
    @Delete suspend fun drop(t: Trial)
    @Delete suspend fun drop(p: Protocol)
    @Delete suspend fun drop(d: Done)
    @Delete suspend fun drop(s: Shot)

    @Query("DELETE FROM Obs WHERE specimen = :id") suspend fun dropObs(id: Long)
    @Query("DELETE FROM Trial WHERE specimen = :id") suspend fun dropTrials(id: Long)
    @Query("DELETE FROM Protocol WHERE specimen = :id") suspend fun dropProtocols(id: Long)
    @Query("DELETE FROM Done WHERE specimen = :id") suspend fun dropDone(id: Long)
    @Query("DELETE FROM Done WHERE protocol = :id") suspend fun dropDoneOf(id: Long)
}

@Database(entities = [Specimen::class, Obs::class, Trial::class, Protocol::class, Done::class, Shot::class], version = 1, exportSchema = true)
abstract class StationDb : RoomDatabase() {
    abstract fun dao(): StationDao

    companion object {
        fun create(context: Context): StationDb =
            Room.databaseBuilder(context, StationDb::class.java, "station.db").build()
    }
}
