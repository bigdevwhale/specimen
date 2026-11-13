package app.specimen.export

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import app.specimen.R
import app.specimen.data.Ob
import app.specimen.data.Specimen
import app.specimen.domain.Lab
import app.specimen.domain.Outcome
import app.specimen.domain.Reaction
import app.specimen.domain.Symptom
import app.specimen.domain.TrialKind
import app.specimen.localized
import app.specimen.ui.Words
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A plain extract from the file for a vet: who the animal is, breed predispositions, weight, what was done and
 * when, medicines and vaccines with reactions, symptoms of the last two months, foods that went badly.
 * No bureau jokes here – it is for a busy person.
 */
object VetPdf {
    private const val W = 595
    private const val H = 842
    private const val M = 48f

    suspend fun write(context: Context, lab: Lab, sp: Specimen): File = withContext(Dispatchers.IO) {
        val ctx = context.localized()
        val doc = PdfDocument()
        val sans = ResourcesCompat.getFont(ctx, R.font.rubik_regular) ?: Typeface.SANS_SERIF
        val bold = ResourcesCompat.getFont(ctx, R.font.rubik_bold) ?: Typeface.DEFAULT_BOLD
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = sans; textSize = 10f; color = 0xFF222222.toInt() }
        val muted = Paint(text).apply { color = 0xFF777777.toInt(); textSize = 8.5f }
        val head = Paint(text).apply { typeface = bold; textSize = 11.5f }
        val title = Paint(text).apply { typeface = bold; textSize = 18f }
        val line = Paint().apply { color = 0xFFCCCCCC.toInt(); strokeWidth = 0.7f }

        var pageNo = 0
        var page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, ++pageNo).create())
        var y = M
        fun newPage() { doc.finishPage(page); page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, ++pageNo).create()); y = M }
        fun ensure(h: Float) { if (y + h > H - M) newPage() }
        fun wrap(s: String, p: Paint, width: Float): List<String> {
            val out = mutableListOf<String>()
            s.split('\n').forEach { para ->
                var cur = ""
                para.split(' ').forEach { w ->
                    val t = if (cur.isEmpty()) w else "$cur $w"
                    if (p.measureText(t) > width && cur.isNotEmpty()) { out += cur; cur = w } else cur = t
                }
                out += cur
            }
            return out
        }
        fun para(s: String, p: Paint = text, indent: Float = 0f) {
            wrap(s, p, W - 2 * M - indent).forEach { l -> ensure(p.textSize * 1.4f); y += p.textSize * 1.35f; page.canvas.drawText(l, M + indent, y, p) }
        }
        fun section(s: String) {
            ensure(40f); y += 14f
            page.canvas.drawText(s.uppercase(), M, y + 11f, head); y += 16f
            page.canvas.drawLine(M, y, W - M, y, line); y += 2f
        }
        fun row(k: String, v: String) {
            val lines = wrap(v, text, W - 2 * M - 150f)
            ensure(lines.size * 14f)
            y += 13.5f
            page.canvas.drawText(k, M, y, muted)
            lines.forEachIndexed { i, l -> if (i > 0) y += 13.5f; page.canvas.drawText(l, M + 150f, y, text) }
        }

        val breed = lab.breed(sp)
        val planet = lab.planet(sp)
        page.canvas.drawText(ctx.getString(R.string.vet_title), M, y + 18f, title); y += 26f
        para(ctx.getString(R.string.vet_made, Words.dateFull(ctx, lab.day)), muted)

        section(ctx.getString(R.string.vet_animal))
        row(ctx.getString(R.string.vet_name), sp.name)
        row(ctx.getString(R.string.vet_species), Words.planetKind(ctx, planet) + " · " + breed.name.of(ctx.resources.configuration.locales[0].language == "ru"))
        if (sp.sex > 0) row(ctx.getString(R.string.k_sex), Words.sex(ctx, sp.sex) + if (sp.neutered) ", " + ctx.getString(R.string.neutered).lowercase() else "")
        if (sp.born > 0) row(ctx.getString(R.string.k_age), Words.age(ctx, sp.born, lab.day) + " (" + Words.dateFull(ctx, sp.born) + ")")
        row(ctx.getString(R.string.k_arrived), Words.dateFull(ctx, sp.arrived))
        val ru = ctx.resources.configuration.locales[0].language == "ru"
        val risks = breed.risks.mapNotNull { lab.cat.risk(it) }
        if (risks.isNotEmpty()) row(ctx.getString(R.string.vet_risks), risks.joinToString("; ") { it.name.of(ru) })

        val w = lab.weights(sp.id)
        if (w.isNotEmpty()) {
            section(ctx.getString(R.string.weight))
            w.takeLast(12).reversed().forEach { (d, v) -> row(Words.dateFull(ctx, d), Words.kg(ctx, v)) }
            lab.obsOf(sp.id).lastOrNull { it.kind == Ob.CONDITION }?.let { row(ctx.getString(R.string.ob_condition), "${it.value.toInt()}/9 · ${Words.dateFull(ctx, it.day)}") }
        }

        val done = lab.done.filter { it.specimen == sp.id }.sortedByDescending { it.day }
        if (done.isNotEmpty()) {
            section(ctx.getString(R.string.vet_done))
            done.take(30).forEach { d ->
                val p = lab.protocols.firstOrNull { it.id == d.protocol } ?: return@forEach
                val name = if (p.key == "u") p.title else lab.cat.proto(p.key)?.name?.of(ru) ?: p.key
                row(Words.dateFull(ctx, d.day), name + if (d.note.isNotBlank()) " – ${d.note}" else "")
            }
        }

        val meds = lab.trialsOf(sp.id).filter { it.kind == TrialKind.MED.ordinal || it.kind == TrialKind.VACCINE.ordinal || it.kind == TrialKind.VET.ordinal }
        if (meds.isNotEmpty()) {
            section(ctx.getString(R.string.vet_meds))
            meds.forEach { t ->
                val o = Outcome.entries.getOrNull(t.outcome)
                val rs = t.reactionList.mapNotNull { Reaction.of(it) }
                val v = buildString {
                    append(t.subject)
                    append(" (").append(Words.kind(ctx, TrialKind.entries[t.kind]).lowercase()).append(")")
                    if (o != null) append(" – ").append(Words.outcome(ctx, o).lowercase())
                    if (rs.isNotEmpty()) append("; ").append(rs.joinToString(", ") { Words.reaction(ctx, it).lowercase() })
                    if (t.note.isNotBlank()) append(". ").append(t.note)
                }
                row(Words.dateFull(ctx, t.start), v)
            }
        }

        val signs = lab.obsOf(sp.id).filter { it.kind == Ob.SYMPTOM && it.day >= lab.day - 60 }.reversed()
        if (signs.isNotEmpty()) {
            section(ctx.getString(R.string.vet_symptoms))
            signs.groupBy { it.day }.forEach { (d, list) -> row(Words.dateFull(ctx, d), list.mapNotNull { Symptom.entries.getOrNull(it.value.toInt()) }.joinToString(", ") { Words.symptom(ctx, it).lowercase() }) }
        }
        val app = lab.obsOf(sp.id).filter { it.kind == Ob.APPETITE && it.day >= lab.day - 14 }
        if (app.isNotEmpty()) row(ctx.getString(R.string.vet_appetite), app.takeLast(7).joinToString("; ") { Words.date(ctx, it.day) + " " + Words.appetite(ctx, it.value.toInt()).lowercase() })

        val badFood = lab.foods(sp.id).filter { it.bad || it.outcome == Outcome.REFUSED || it.outcome == Outcome.BAD }
        if (badFood.isNotEmpty()) {
            section(ctx.getString(R.string.vet_food))
            badFood.forEach { f -> row(Words.dateFull(ctx, f.last), f.name + (f.outcome?.let { " – " + Words.outcome(ctx, it).lowercase() } ?: "")) }
        }

        val notes = lab.obsOf(sp.id).filter { it.kind == Ob.NOTE && it.day >= lab.day - 30 }.reversed()
        if (notes.isNotEmpty()) {
            section(ctx.getString(R.string.vet_notes))
            notes.take(10).forEach { row(Words.dateFull(ctx, it.day), it.note) }
        }

        y += 18f
        para(ctx.getString(R.string.vet_footer), muted)
        doc.finishPage(page)
        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
        val f = File(dir, "${sp.name.replace(Regex("[^\\p{L}\\p{N}]+"), "_")}-vet-${Words.dateFull(ctx, lab.day).replace(' ', '_')}.pdf")
        f.outputStream().use { doc.writeTo(it) }
        doc.close()
        f
    }
}
