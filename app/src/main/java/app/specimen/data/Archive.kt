package app.specimen.data

import android.content.Context
import android.content.Intent
import app.specimen.AppContainer
import app.specimen.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.system.exitProcess

/**
 * The whole station in one zip: the database file and the photos. Restoring replaces both and restarts the app,
 * since the open database can't be swapped under Room.
 */
object Archive {
    private const val DB = "station.db"
    private const val MARK = "specimen-archive"

    suspend fun export(c: AppContainer, out: OutputStream) = withContext(Dispatchers.IO) {
        // Fold the write-ahead log into the main file so one file holds everything.
        c.db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        val db = c.app.getDatabasePath(DB)
        ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry(MARK)); z.write("1".toByteArray()); z.closeEntry()
            z.putNextEntry(ZipEntry(DB)); db.inputStream().use { it.copyTo(z) }; z.closeEntry()
            c.photos.dir.listFiles()?.filter { it.isFile }?.forEach { f ->
                z.putNextEntry(ZipEntry("photos/${f.name}")); f.inputStream().use { it.copyTo(z) }; z.closeEntry()
            }
        }
    }

    /** Unpacks into a staging folder first; only a real archive replaces the station. */
    suspend fun import(c: AppContainer, input: InputStream): Boolean = withContext(Dispatchers.IO) {
        val stage = File(c.app.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        var marked = false
        ZipInputStream(input).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                val name = e.name
                if (name.contains("..") || e.isDirectory) continue
                if (name == MARK) { marked = true; continue }
                if (name != DB && !name.startsWith("photos/")) continue
                val f = File(stage, name).apply { parentFile?.mkdirs() }
                f.outputStream().use { z.copyTo(it) }
            }
        }
        val db = File(stage, DB)
        if (!marked || !db.exists() || db.length() < 1024) return@withContext false
        c.db.close()
        val target = c.app.getDatabasePath(DB)
        listOf("", "-wal", "-shm").forEach { File(target.path + it).delete() }
        db.copyTo(target, overwrite = true)
        c.photos.dir.listFiles()?.forEach { it.delete() }
        File(stage, "photos").listFiles()?.forEach { it.copyTo(File(c.photos.dir, it.name), overwrite = true) }
        stage.deleteRecursively()
        true
    }

    fun restart(ctx: Context) {
        ctx.startActivity(Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        exitProcess(0)
    }
}
