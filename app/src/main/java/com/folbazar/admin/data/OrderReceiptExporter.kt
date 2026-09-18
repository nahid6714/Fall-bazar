package com.folbazar.admin.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Renders a single order as a clean, printable receipt and exports it either as a
 * PNG image or a PDF file. Both formats share the exact same layout logic (the
 * layout is computed once, then simply "played back" onto whichever Canvas the
 * target format provides) so the image and the PDF always look identical.
 */
private data class RLine(
    val text: String,
    val y: Float,
    val x: Float,
    val paint: Paint,
    val align: Paint.Align = Paint.Align.LEFT
)

private class RLayout(val width: Int, val height: Int, val lines: List<RLine>, val dividers: List<Float>)

object OrderReceiptExporter {

    private const val WIDTH = 760f
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = WIDTH - MARGIN * 2
    private val BRAND = Color.parseColor("#DF2D4D")
    private val MUTED = Color.parseColor("#777777")
    private val FAINT = Color.parseColor("#9AA0A6")
    private val DIVIDER = Color.parseColor("#E3E3E3")

    private fun paint(size: Float, bold: Boolean = false, color: Int = Color.BLACK) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; isFakeBoldText = bold; this.color = color
    }

    private fun money(v: Double): String = String.format(Locale.US, "%.2f", v)

    private fun wrap(text: String, p: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.trim().split(Regex("\\s+"))
        val out = mutableListOf<String>()
        var cur = StringBuilder()
        for (w in words) {
            val trial = if (cur.isEmpty()) w else "$cur $w"
            if (p.measureText(trial) <= maxWidth) cur = StringBuilder(trial)
            else {
                if (cur.isNotEmpty()) out.add(cur.toString())
                cur = StringBuilder(w)
            }
        }
        if (cur.isNotEmpty()) out.add(cur.toString())
        return out.ifEmpty { listOf("") }
    }

    private fun build(order: Order, items: List<OrderItem>): RLayout {
        val lines = mutableListOf<RLine>()
        val dividers = mutableListOf<Float>()
        var y = 0f

        val titleP = paint(34f, bold = true, color = BRAND)
        val subP = paint(17f, color = MUTED)
        val sectionP = paint(19f, bold = true)
        val labelP = paint(16f, color = MUTED)
        val valueP = paint(16f, bold = true)
        val colHeadP = paint(13f, color = FAINT)
        val itemP = paint(16f)
        val totalLabelP = paint(17f)
        val totalValueP = paint(17f, bold = true)
        val grandLabelP = paint(21f, bold = true)
        val grandValueP = paint(21f, bold = true, color = BRAND)
        val footerP = paint(12f, color = FAINT)

        y += 52f
        lines += RLine("ফল বাজার", y, MARGIN, titleP)
        y += 26f
        lines += RLine("অর্ডার রশিদ  •  #${order.orderNumber}", y, MARGIN, subP)
        y += 21f
        val dateStr = order.createdAt?.take(10) ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        lines += RLine("তারিখ: $dateStr", y, MARGIN, subP)
        y += 22f
        dividers += y; y += 28f

        lines += RLine("কাস্টমার ও ঠিকানা", y, MARGIN, sectionP)
        y += 26f
        fun kv(label: String, value: String?) {
            if (value.isNullOrBlank()) return
            val wrapped = wrap(value, valueP, CONTENT_WIDTH - 150f)
            lines += RLine(label, y, MARGIN, labelP)
            wrapped.forEachIndexed { idx, ln ->
                lines += RLine(ln, y + idx * 20f, MARGIN + 140f, valueP)
            }
            y += wrapped.size * 20f + 6f
        }
        kv("নাম", order.customer)
        kv("ফোন", order.phone)
        kv("ঠিকানা", order.address)
        kv("এলাকা", listOfNotNull(order.upazila, order.district, order.division).filter { it.isNotBlank() }.joinToString(", ").ifBlank { null })
        kv("ডেলিভারি এরিয়া", order.deliveryArea)

        y += 4f
        dividers += y; y += 28f

        lines += RLine("পণ্য তালিকা", y, MARGIN, sectionP)
        y += 24f
        val qtyX = MARGIN + CONTENT_WIDTH - 210f
        val priceX = MARGIN + CONTENT_WIDTH - 130f
        val totalX = MARGIN + CONTENT_WIDTH
        lines += RLine("পণ্যের নাম", y, MARGIN, colHeadP)
        lines += RLine("পরিমাণ", y, qtyX, colHeadP)
        lines += RLine("দাম", y, priceX, colHeadP)
        lines += RLine("মোট", y, totalX, colHeadP, Paint.Align.RIGHT)
        y += 18f
        dividers += y; y += 20f

        if (items.isEmpty()) {
            lines += RLine("পণ্যের বিস্তারিত পাওয়া যায়নি", y, MARGIN, labelP)
            y += 26f
        } else {
            items.forEach { it2 ->
                val label = it2.productName + (it2.variantLabel?.takeIf { v -> v.isNotBlank() }?.let { v -> " ($v)" } ?: "")
                val nameLines = wrap(label, itemP, qtyX - MARGIN - 12f)
                nameLines.forEachIndexed { idx, ln -> lines += RLine(ln, y + idx * 20f, MARGIN, itemP) }
                lines += RLine("${it2.quantity}", y, qtyX, itemP)
                lines += RLine(money(it2.unitPrice), y, priceX, itemP)
                lines += RLine("৳${money(it2.lineTotal)}", y, totalX, itemP, Paint.Align.RIGHT)
                y += nameLines.size * 20f + 10f
            }
        }

        y += 4f
        dividers += y; y += 26f

        fun totalRow(label: String, valueText: String, lp: Paint = totalLabelP, vp: Paint = totalValueP) {
            lines += RLine(label, y, MARGIN, lp)
            lines += RLine(valueText, y, totalX, vp, Paint.Align.RIGHT)
            y += 25f
        }
        totalRow("সাবটোটাল", "৳ ${money(order.subtotal)}")
        totalRow("ডেলিভারি চার্জ", "৳ ${money(order.deliveryCharge)}")
        if (order.discount > 0) totalRow("ডিসকাউন্ট", "- ৳ ${money(order.discount)}")
        y += 2f
        dividers += y; y += 30f
        lines += RLine("সর্বমোট", y, MARGIN, grandLabelP)
        lines += RLine("৳ ${money(order.total)}", y, totalX, grandValueP, Paint.Align.RIGHT)
        y += 38f

        dividers += y; y += 26f
        lines += RLine("পেমেন্ট পদ্ধতি: ${order.paymentTitle ?: order.paymentMethod}", y, MARGIN, labelP)
        y += 20f
        if (!order.trxId.isNullOrBlank()) { lines += RLine("TrxID: ${order.trxId}", y, MARGIN, labelP); y += 20f }
        if (!order.couponCode.isNullOrBlank()) { lines += RLine("কুপন কোড: ${order.couponCode}", y, MARGIN, labelP); y += 20f }
        if (!order.orderNote.isNullOrBlank()) {
            val noteLines = wrap("নোট: ${order.orderNote}", labelP, CONTENT_WIDTH)
            noteLines.forEach { lines += RLine(it, y, MARGIN, labelP); y += 20f }
        }

        y += 18f
        lines += RLine("ধন্যবাদ ফল বাজারে অর্ডার করার জন্য", y, MARGIN, footerP)
        y += 36f

        return RLayout(WIDTH.toInt(), y.toInt(), lines, dividers)
    }

    private fun paint(layout: RLayout, canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        val dividerPaint = Paint().apply { color = DIVIDER; strokeWidth = 1.5f }
        layout.dividers.forEach { canvas.drawLine(MARGIN, it, WIDTH - MARGIN, it, dividerPaint) }
        layout.lines.forEach { l ->
            val p = Paint(l.paint).apply { textAlign = l.align }
            canvas.drawText(l.text, l.x, l.y, p)
        }
    }

    fun renderBitmap(order: Order, items: List<OrderItem>): Bitmap {
        val layout = build(order, items)
        val bmp = Bitmap.createBitmap(layout.width, layout.height, Bitmap.Config.ARGB_8888)
        paint(layout, Canvas(bmp))
        return bmp
    }

    fun renderPdf(order: Order, items: List<OrderItem>): PdfDocument {
        val layout = build(order, items)
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(layout.width, layout.height, 1).create())
        paint(layout, page.canvas)
        doc.finishPage(page)
        return doc
    }

    private fun exportsDir(context: Context): File = File(context.cacheDir, "exports").apply { mkdirs() }

    fun saveImage(context: Context, bitmap: Bitmap, orderNumber: String): File {
        val file = File(exportsDir(context), "order-$orderNumber.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    fun savePdf(context: Context, doc: PdfDocument, orderNumber: String): File {
        val file = File(exportsDir(context), "order-$orderNumber.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    /** Opens the system share sheet, which also lets the user "Save to Downloads"/Drive/etc. */
    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "রশিদ সেভ / শেয়ার করুন"))
    }
}
