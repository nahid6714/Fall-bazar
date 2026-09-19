package com.folbazar.admin.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Renders a single order as a receipt (same design for every size - see
 * ReceiptHtmlBuilder) and exports it as a PDF or PNG at a chosen
 * [ReceiptPaperSize]. Both formats are produced by loading the receipt HTML
 * into an off-screen WebView, so the PDF/PNG always match each other and the
 * in-app preview pixel for pixel.
 */
object OrderReceiptExporter {

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    suspend fun exportPdf(
        activity: Activity,
        order: Order,
        items: List<OrderItem>,
        size: ReceiptPaperSize
    ): File = withContext(Dispatchers.Main) {
        val webView = createWebView(activity)
        try {
            val html = ReceiptHtmlBuilder.build(order, items, size)
            attach(activity, webView, size.widthCssPx, size.heightCssPx ?: 1200)
            webView.loadHtmlSuspend(html)

            val measuredHeight = webView.measureContentHeightPx()
            val contentHeightPx = maxOf(measuredHeight, size.heightCssPx ?: 1, 1)
            webView.scrollTo(0, 0)
            val pageHeightMm = size.heightMm ?: (
                ReceiptPaperSize.cssPxToMm(measuredHeight.coerceAtLeast(1).toFloat()) + 4f
            )

            webView.measure(
                View.MeasureSpec.makeMeasureSpec(size.widthCssPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(contentHeightPx, View.MeasureSpec.EXACTLY)
            )
            webView.layout(0, 0, size.widthCssPx, contentHeightPx)
            webView.requestLayout()
            // Give Chromium one render pass before we capture the view.
            // Wait for the WebView layout/paint pipeline (including font/layout reflow).
            delay(350)

            // Render the WebView to a bitmap first. Drawing the WebView directly to
            // PdfDocument can produce a blank page on some Android/WebView versions.
            // The bitmap path is reliable and also keeps PDF and PNG output identical.
            webView.scrollTo(0, 0)
            delay(150)
            val bitmap = renderWebView(webView, size.widthCssPx, contentHeightPx)
            try {
                val file = File(exportsDir(activity), "order-${order.orderNumber}-${size.name.lowercase()}.pdf")
                bitmap.writePdfToFile(size.widthMm, pageHeightMm, size.heightMm != null, file)
                file
            } finally {
                bitmap.recycle()
            }
        } finally {
            detach(webView)
        }
    }

    suspend fun exportImage(
        activity: Activity,
        order: Order,
        items: List<OrderItem>,
        size: ReceiptPaperSize,
        scale: Int = 2
    ): File = withContext(Dispatchers.Main) {
        val webView = createWebView(activity)
        try {
            val html = ReceiptHtmlBuilder.build(order, items, size)
            attach(activity, webView, size.widthCssPx, size.heightCssPx ?: 1200)
            webView.loadHtmlSuspend(html)

            val measuredHeight = webView.measureContentHeightPx()
            val heightPx = maxOf(measuredHeight, size.heightCssPx ?: 1, 1)
            webView.scrollTo(0, 0)
            delay(150)
            val bitmap = renderWebView(webView, size.widthCssPx, heightPx)
            try {
                val file = File(exportsDir(activity), "order-${order.orderNumber}-${size.name.lowercase()}.png")
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                file
            } finally {
                bitmap.recycle()
            }
        } finally {
            detach(webView)
        }
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

    fun findActivity(context: Context): Activity? {
        var c = context
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }

    // ---------------------------------------------------------------
    // WebView plumbing
    // ---------------------------------------------------------------

    private fun exportsDir(context: Context): File = File(context.cacheDir, "exports").apply { mkdirs() }

    private fun createWebView(activity: Activity): WebView = WebView(activity).apply {
        settings.javaScriptEnabled = true
        settings.useWideViewPort = false
        settings.loadWithOverviewMode = false
        settings.textZoom = 100
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.defaultFontSize = 16
        settings.defaultFixedFontSize = 16
        setInitialScale(100)
        setBackgroundColor(Color.WHITE)
    }

    /** Adds the WebView behind the visible Compose content so Chromium still rasterizes it. */
    private fun attach(activity: Activity, webView: WebView, widthPx: Int, heightPx: Int) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content)
        val lp = FrameLayout.LayoutParams(widthPx, heightPx.coerceAtLeast(1))
        // Keep the WebView behind the Compose UI instead of translating it far
        // off-screen. Some Android WebView implementations skip rasterization
        // for completely off-screen views, which resulted in blank PDF pages.
        root.addView(webView, 0, lp)
    }

    private fun renderWebView(webView: WebView, widthPx: Int, heightPx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        webView.draw(canvas)
        return bitmap
    }

    private fun detach(webView: WebView) {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
    }

    private suspend fun WebView.loadHtmlSuspend(html: String): Unit = suspendCancellableCoroutine { cont ->
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (cont.isActive) cont.resume(Unit)
            }
        }
        loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    /** Reads the receipt card's rendered height (in CSS px) so auto-height (thermal) pages can be sized exactly. */
    private suspend fun WebView.measureContentHeightPx(): Int = suspendCancellableCoroutine { cont ->
        post {
            evaluateJavascript("document.getElementById('paper').scrollHeight.toString();") { result ->
                val px = result?.trim('"')?.toDoubleOrNull()?.toInt() ?: 0
                if (cont.isActive) cont.resume(px)
            }
        }
    }

    /**
     * Writes the rendered receipt bitmap to a PDF.
     *
     * We intentionally do not use PrintDocumentAdapter here: recent Android SDKs
     * expose LayoutResultCallback/WriteResultCallback constructors as package-private.
     * Rendering to a bitmap first also avoids blank pages on some WebView versions.
     */
    private fun Bitmap.writePdfToFile(
        widthMm: Float,
        pageHeightMm: Float,
        fixedHeight: Boolean,
        outFile: File
    ) {
        val pageWidthPt = mmToPdfPoints(widthMm)
        val pageHeightPt = mmToPdfPoints(pageHeightMm)
        val pageHeightPx = ReceiptPaperSize.mmToCssPx(pageHeightMm).coerceAtLeast(1)
        val pageCount = if (fixedHeight) {
            ((height + pageHeightPx - 1) / pageHeightPx).coerceAtLeast(1)
        } else {
            1
        }

        val document = PdfDocument()
        try {
            val scaleX = pageWidthPt.toFloat() / width.toFloat()
            for (pageNumber in 0 until pageCount) {
                val pageInfo = PdfDocument.PageInfo.Builder(
                    pageWidthPt,
                    pageHeightPt,
                    pageNumber + 1
                ).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)
                canvas.save()
                canvas.scale(scaleX, scaleX)
                if (fixedHeight) {
                    canvas.translate(0f, -(pageNumber * pageHeightPx).toFloat())
                }
                canvas.drawBitmap(this, 0f, 0f, null)
                canvas.restore()
                document.finishPage(page)
            }
            FileOutputStream(outFile).use { document.writeTo(it) }
        } finally {
            document.close()
        }
    }

    private fun mmToPdfPoints(mm: Float): Int =
        ((mm / 25.4f) * 72f).toInt().coerceAtLeast(1)

}
