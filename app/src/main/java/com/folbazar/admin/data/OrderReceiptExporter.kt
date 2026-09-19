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
            attach(activity, webView, size.widthCssPx)
            webView.loadHtmlSuspend(html)

            val contentHeightPx = webView.measureContentHeightPx()
            val pageHeightMm = if (size.isAutoHeight) {
                ReceiptPaperSize.cssPxToMm(contentHeightPx.toFloat()) + 4f
            } else {
                size.heightMm ?: ReceiptPaperSize.cssPxToMm(contentHeightPx.toFloat())
            }

            val layoutHeightPx = maxOf(contentHeightPx, size.heightCssPx ?: contentHeightPx, 1)
            webView.measure(
                View.MeasureSpec.makeMeasureSpec(size.widthCssPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(layoutHeightPx, View.MeasureSpec.EXACTLY)
            )
            webView.layout(0, 0, size.widthCssPx, layoutHeightPx)

            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

            val file = File(exportsDir(activity), "order-${order.orderNumber}-${size.name.lowercase()}.pdf")
            webView.writePdfToFile(size.widthMm, pageHeightMm, contentHeightPx, file)
            file
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
            attach(activity, webView, size.widthCssPx)
            webView.loadHtmlSuspend(html)

            val widthPx = size.widthCssPx
            val heightPx = size.heightCssPx ?: webView.measureContentHeightPx()

            webView.measure(
                View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
            )
            webView.layout(0, 0, widthPx, heightPx)
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

            val bmp = Bitmap.createBitmap(widthPx * scale, heightPx * scale, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.WHITE)
            canvas.scale(scale.toFloat(), scale.toFloat())
            webView.draw(canvas)

            val file = File(exportsDir(activity), "order-${order.orderNumber}-${size.name.lowercase()}.png")
            FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
            file
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
        setBackgroundColor(Color.WHITE)
    }

    /** Adds the WebView to the activity's content root, positioned off-screen so it never flashes on screen. */
    private fun attach(activity: Activity, webView: WebView, widthPx: Int) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content)
        val lp = FrameLayout.LayoutParams(widthPx, FrameLayout.LayoutParams.WRAP_CONTENT)
        webView.layoutParams = lp
        webView.translationX = -100000f
        root.addView(webView, lp)
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
     * Writes the rendered WebView directly to a PDF.
     *
     * We intentionally do not use PrintDocumentAdapter here: recent Android SDKs
     * expose LayoutResultCallback/WriteResultCallback constructors as package-private,
     * so app code can no longer instantiate those callback classes. PdfDocument gives
     * us the same result without relying on hidden/package-private framework APIs.
     */
    private fun WebView.writePdfToFile(
        widthMm: Float,
        pageHeightMm: Float,
        contentHeightCssPx: Int,
        outFile: File
    ) {
        val pageWidthPt = mmToPdfPoints(widthMm)
        val pageHeightPt = mmToPdfPoints(pageHeightMm)
        val fixedPageHeightCssPx = ReceiptPaperSize.mmToCssPx(pageHeightMm)
        val autoHeight = pageHeightMm > 0f && contentHeightCssPx <= fixedPageHeightCssPx + 2
        val pageCount = if (autoHeight) 1 else {
            val pageHeight = fixedPageHeightCssPx.coerceAtLeast(1)
            ((contentHeightCssPx + pageHeight - 1) / pageHeight).coerceAtLeast(1)
        }

        val document = PdfDocument()
        try {
            val scaleX = pageWidthPt.toFloat() / width
            val pageHeightCssPx = fixedPageHeightCssPx.coerceAtLeast(1)

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
                if (!autoHeight) {
                    canvas.translate(0f, -(pageNumber * pageHeightCssPx).toFloat())
                }
                draw(canvas)
                canvas.restore()
                document.finishPage(page)
            }

            FileOutputStream(outFile).use { output ->
                document.writeTo(output)
            }
        } finally {
            document.close()
        }
    }

    private fun mmToPdfPoints(mm: Float): Int =
        ((mm / 25.4f) * 72f).toInt().coerceAtLeast(1)

}
