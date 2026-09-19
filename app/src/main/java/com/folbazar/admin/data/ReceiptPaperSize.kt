package com.folbazar.admin.data

import android.print.PrintAttributes

/**
 * The paper sizes the order receipt can be exported to. A4/A5 use the real,
 * fixed ISO page size (so the PDF behaves like a normal printable document -
 * extra blank space at the bottom if the order is short). The thermal sizes
 * use a fixed *width* but an auto-calculated height, matching how a real POS
 * receipt/roll printer works (no fixed page length).
 *
 * The exact same HTML/CSS design (logo, green theme, cards, icons, wave
 * footer) is reused for every size - only the width-driven layout (see the
 * `size-*` CSS classes in ReceiptHtmlBuilder) adapts so it stays readable at
 * 58mm.
 */
enum class ReceiptPaperSize(
    val label: String,
    val cssClass: String,
    val widthMm: Float,
    /** Fixed physical height in mm, or null when the height should hug the content. */
    val heightMm: Float?
) {
    A4("A4", "size-a4", 210f, 297f),
    A5("A5", "size-a5", 148f, 210f),
    THERMAL_80("থার্মাল ৮০মিমি", "size-t80", 80f, null),
    THERMAL_58("থার্মাল ৫৮মিমি", "size-t58", 58f, null);

    val isAutoHeight: Boolean get() = heightMm == null

    /** CSS px for this size's width at the standard 96 CSS-px-per-inch used by print engines. */
    val widthCssPx: Int get() = mmToCssPx(widthMm)

    /** CSS px for a fixed height, or null when auto. */
    val heightCssPx: Int? get() = heightMm?.let { mmToCssPx(it) }

    /**
     * Builds the [PrintAttributes.MediaSize] to hand to WebView's print adapter.
     * For auto-height sizes, [measuredHeightMm] (from a prior content measurement
     * pass) is used as the page length; a small floor avoids a degenerate 0-height page.
     */
    fun mediaSize(measuredHeightMm: Float = 0f): PrintAttributes.MediaSize {
        if (this == A4) return PrintAttributes.MediaSize.ISO_A4
        if (this == A5) return PrintAttributes.MediaSize.ISO_A5
        val h = (heightMm ?: measuredHeightMm).coerceAtLeast(30f)
        return PrintAttributes.MediaSize(
            "receipt_${name.lowercase()}",
            label,
            mmToMils(widthMm),
            mmToMils(h)
        )
    }

    companion object {
        fun mmToMils(mm: Float): Int = ((mm / 25.4f) * 1000f).toInt().coerceAtLeast(1)
        fun mmToCssPx(mm: Float): Int = ((mm / 25.4f) * 96f).toInt().coerceAtLeast(1)
        fun cssPxToMm(px: Float): Float = (px / 96f) * 25.4f
    }
}
