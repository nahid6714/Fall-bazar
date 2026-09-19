package com.folbazar.admin.data

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Renders the order receipt as a single self-contained HTML string. The
 * design (logo mark, green theme, section cards, product table, wave
 * footer) is identical across all paper sizes - only the `size-*` CSS class
 * on <body> changes, which switches the header from a two-column layout to
 * a stacked one and shrinks paddings/fonts for the narrow thermal widths.
 */
object ReceiptHtmlBuilder {

    private fun esc(text: String?): String = (text ?: "").let {
        it.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    private fun money(v: Double): String = String.format(Locale.US, "%.2f", v)

    fun build(order: Order, items: List<OrderItem>, size: ReceiptPaperSize): String {
        val widthPx = size.widthCssPx
        val minHeightCss = size.heightCssPx?.let { "min-height:${it}px;" } ?: ""
        val dateStr = order.createdAt?.take(10)
            ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())

        val area = listOfNotNull(order.upazila, order.district, order.division)
            .filter { it.isNotBlank() }.joinToString(", ")

        val customerRows = buildString {
            fun row(label: String, value: String?) {
                if (value.isNullOrBlank()) return
                append("<div class=\"kv\"><span class=\"k\">${esc(label)}</span><span class=\"sep\">:</span>&nbsp;<span class=\"v\">${esc(value)}</span></div>")
            }
            row("নাম", order.customer)
            row("ফোন", order.phone)
            row("ঠিকানা", order.address)
            row("এলাকা", area.ifBlank { null })
            row("ডেলিভারি এরিয়া", order.deliveryArea)
        }

        val itemRows = if (items.isEmpty()) {
            "<tr><td colspan=\"4\" style=\"color:#6b7d70;\">পণ্যের বিস্তারিত পাওয়া যায়নি</td></tr>"
        } else {
            items.joinToString("") { it2 ->
                val name = esc(it2.productName) +
                    (it2.variantLabel?.takeIf { v -> v.isNotBlank() }?.let { v -> " (${esc(v)})" } ?: "")
                "<tr><td>$name</td><td class=\"num\">${it2.quantity}</td>" +
                    "<td class=\"num\">${money(it2.unitPrice)}</td>" +
                    "<td class=\"num\">${money(it2.lineTotal)}</td></tr>"
            }
        }

        val discountRow = if (order.discount > 0) {
            """<div class="trow"><span class="lbl">ডিসকাউন্ট</span><span class="val">- ৳ ${money(order.discount)}</span></div>"""
        } else ""

        val extraPayLines = buildString {
            if (!order.trxId.isNullOrBlank()) append("<div class=\"kv\"><span class=\"k\">TrxID</span><span class=\"sep\">:</span>&nbsp;<span class=\"v\">${esc(order.trxId)}</span></div>")
            if (!order.couponCode.isNullOrBlank()) append("<div class=\"kv\"><span class=\"k\">কুপন কোড</span><span class=\"sep\">:</span>&nbsp;<span class=\"v\">${esc(order.couponCode)}</span></div>")
        }

        val noteBlock = if (!order.orderNote.isNullOrBlank()) {
            """<div class="card"><h2>${iconNote()}নোট</h2><div class="kv" style="flex-direction:column;"><span class="v">${esc(order.orderNote)}</span></div></div>"""
        } else ""

        return """
<!DOCTYPE html>
<html lang="bn"><head><meta charset="UTF-8">
<meta name="viewport" content="width=${widthPx}, initial-scale=1, maximum-scale=1, user-scalable=no">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Hind+Siliguri:wght@400;500;600;700&display=swap" rel="stylesheet">
<style>
$CSS
</style></head>
<body class="${size.cssClass}">
<div class="paper" id="paper" style="width:${widthPx}px;${minHeightCss}">
  <div class="head">
    <div class="brand">
      ${leafLogo()}
      <div class="name"><b>ফল বাজার</b><span>তাজা ফলে সুস্থ জীবন</span></div>
    </div>
    <div class="meta">
      <h1>অর্ডার রসিদ</h1>
      <div class="line">অর্ডার নং&nbsp; <b>#${esc(order.orderNumber)}</b></div>
      <div class="line">তারিখ&nbsp; <b>${esc(dateStr)}</b></div>
      <div class="thanks-pill">${iconCart()}<span>ধন্যবাদ, আমাদের সাথে থাকার জন্য!</span></div>
    </div>
  </div>

  <div class="divider-row"></div>

  <div class="card">
    <h2>${iconUser()}কাস্টমার ও ঠিকানা</h2>
    $customerRows
  </div>

  <div class="card">
    <h2>${iconBag()}পণ্য তালিকা</h2>
    <table class="items">
      <thead><tr><th>পণ্যের নাম</th><th class="num">পরিমাণ</th><th class="num">দাম (৳)</th><th class="num">মোট (৳)</th></tr></thead>
      <tbody>$itemRows</tbody>
    </table>
  </div>

  <div class="totals">
    <div class="trow"><span class="lbl">${iconReceipt()}সাবটোটাল</span><span class="val">৳ ${money(order.subtotal)}</span></div>
    <div class="trow"><span class="lbl">${iconTruck()}ডেলিভারি চার্জ</span><span class="val">৳ ${money(order.deliveryCharge)}</span></div>
    $discountRow
  </div>

  <div class="grand"><span class="lbl">${iconWallet()}সর্বমোট</span><span class="val">৳ ${money(order.total)}</span></div>

  <div class="card pay-card">
    <h2>${iconCard()}পেমেন্ট পদ্ধতি</h2>
    <div class="method">${esc(order.paymentTitle ?: order.paymentMethod)}</div>
    $extraPayLines
  </div>

  $noteBlock

  <div class="footer">
    <div class="footer-msg">
      <p class="big">ধন্যবাদ!</p>
      <div class="small">আবার আসবেন ${iconHeart()}</div>
    </div>
    <div class="wave-wrap">
      <svg viewBox="0 0 500 120" preserveAspectRatio="none">
        <path d="M0,50 C90,10 160,90 250,55 C340,20 420,85 500,45 L500,120 L0,120 Z" fill="#bfe3c9"/>
        <path d="M0,70 C100,35 170,100 260,68 C350,36 430,95 500,65 L500,120 L0,120 Z" fill="#8dcaa1"/>
      </svg>
    </div>
    <div class="wave-strap">${iconLeaf()}<span>তাজা ফল&nbsp;•&nbsp;ভালো স্বাস্থ্য&nbsp;•&nbsp;সুখী জীবন</span></div>
  </div>
</div>
</body></html>
""".trimIndent()
    }

    // ---- small inline icon helpers (kept tiny & dependency-free) ----
    private fun leafLogo() = """<svg viewBox="0 0 48 48" fill="none"><path d="M24 44c-10-2-17-10-17-20 0-6 3-11 8-14 1 6 5 10 11 12 5-8 4-16-1-22 10 2 16 10 16 19 0 14-8 23-17 25z" fill="#2f8f52"/><path d="M24 44c6-4 10-11 10-19 0-4-1-7-3-10-3 6-8 9-13 8 1 6 3 12 6 21z" fill="#175c36"/></svg>"""
    private fun iconCart() = """<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6"/></svg>"""
    private fun iconUser() = """<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>"""
    private fun iconBag() = """<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><path d="M3 6h18"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>"""
    private fun iconReceipt() = """<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/><path d="M9 13h6"/><path d="M9 17h6"/></svg>"""
    private fun iconTruck() = """<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="1" y="3" width="15" height="13" rx="1"/><path d="M16 8h4l3 3v5h-7z"/><circle cx="5.5" cy="18.5" r="1.8"/><circle cx="18.5" cy="18.5" r="1.8"/></svg>"""
    private fun iconWallet() = """<svg viewBox="0 0 24 24" fill="none" stroke="#eafcef" stroke-width="2"><path d="M19 7H5a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2z"/><path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/><circle cx="16" cy="13" r="1.6"/></svg>"""
    private fun iconCard() = """<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20"/></svg>"""
    private fun iconLeaf() = """<svg viewBox="0 0 24 24" fill="none" stroke="#eafcef" stroke-width="2"><path d="M12 22c-5-4-8-8-8-12a8 8 0 0 1 16 0c0 4-3 8-8 12z"/><path d="M12 4c0 3-2 4-2 7"/></svg>"""
    private fun iconHeart() = """<svg viewBox="0 0 24 24" fill="#e0294f" width="12" height="12"><path d="M12 21s-7-4.35-10-9.28C.5 8.5 2 5 5.5 5c2 0 3.5 1.2 4.5 2.8C11 6.2 12.5 5 14.5 5 18 5 19.5 8.5 22 11.72 19 16.65 12 21 12 21z"/></svg>"""
    private fun iconNote() = """<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/><path d="M9 13h6"/><path d="M9 17h6"/></svg>"""

    @Suppress("MaxLineLength")
    private val CSS = """
    :root{--green-900:#12492b;--green-800:#175c36;--green-700:#1e7a45;--green-600:#2f8f52;--green-100:#e7f5ea;--green-50:#f4faf5;--divider:#dcecdf;--muted:#6b7d70;--ink:#1c2a20;--accent:#e0294f;}
    *{box-sizing:border-box;}
    html,body{margin:0;padding:0;background:#fff;}
    body{font-family:'Hind Siliguri',system-ui,sans-serif;color:var(--ink);}
    .paper{position:relative;background:#fff;padding:28px 26px 0;display:flex;flex-direction:column;margin:0 auto;}
    .paper::before{content:"";position:absolute;inset:0;box-shadow:inset 0 0 0 1px var(--divider);pointer-events:none;}
    .head{display:flex;align-items:flex-start;justify-content:space-between;gap:14px;}
    body.size-t58 .head, body.size-t80 .head{flex-direction:column;align-items:center;text-align:center;gap:8px;}
    .brand{display:flex;align-items:center;gap:8px;}
    body.size-t58 .brand, body.size-t80 .brand{flex-direction:column;gap:4px;}
    .brand svg{width:42px;height:42px;flex:none;}
    body.size-t58 .brand svg{width:32px;height:32px;}
    .brand .name{line-height:1.05;}
    .brand .name b{display:block;font-size:22px;font-weight:700;color:var(--green-800);letter-spacing:.2px;}
    body.size-t58 .brand .name b{font-size:16px;}
    body.size-t80 .brand .name b{font-size:18px;}
    .brand .name span{display:block;font-size:10.5px;color:var(--muted);margin-top:2px;}
    .meta{text-align:right;padding-left:14px;border-left:1px solid var(--divider);min-width:0;}
    body.size-t58 .meta, body.size-t80 .meta{border-left:none;padding-left:0;text-align:center;}
    .meta h1{margin:0 0 6px;font-size:19px;color:var(--green-800);font-weight:700;}
    body.size-t58 .meta h1{font-size:14px;}
    body.size-t80 .meta h1{font-size:16px;}
    .meta .line{font-size:12.5px;color:#3c4a40;margin:2px 0;}
    body.size-t58 .meta .line{font-size:10.5px;}
    .meta .line b{color:var(--ink);font-weight:600;}
    .thanks-pill{margin-top:10px;background:var(--green-100);color:var(--green-800);border-radius:9px;padding:8px 12px;font-size:11.5px;font-weight:600;display:flex;align-items:center;gap:7px;}
    body.size-t58 .thanks-pill, body.size-t80 .thanks-pill{margin-top:10px;font-size:10px;justify-content:center;}
    .thanks-pill svg{width:15px;height:15px;flex:none;}
    .divider-row{height:1px;background:var(--divider);margin:16px 0;}
    .card{background:var(--green-50);border-radius:11px;padding:14px 15px;margin-bottom:14px;}
    body.size-t58 .card, body.size-t80 .card{padding:10px;border-radius:8px;margin-bottom:10px;}
    .card h2{margin:0 0 10px;font-size:14.5px;font-weight:700;color:var(--green-800);display:flex;align-items:center;gap:7px;}
    body.size-t58 .card h2{font-size:12px;margin-bottom:7px;}
    .card h2 svg{width:16px;height:16px;flex:none;}
    .kv{display:flex;font-size:12.5px;padding:3px 0;gap:10px;}
    body.size-t58 .kv{font-size:10.5px;}
    .kv .k{width:88px;flex:none;color:var(--muted);}
    body.size-t58 .kv .k{width:64px;}
    .kv .sep{color:var(--muted);}
    .kv .v{font-weight:600;color:var(--ink);}
    table.items{width:100%;border-collapse:collapse;font-size:12px;}
    body.size-t58 table.items{font-size:10px;}
    table.items thead th{text-align:left;font-size:11px;font-weight:600;color:var(--green-800);background:var(--green-100);padding:7px 8px;white-space:nowrap;}
    body.size-t58 table.items thead th{font-size:9px;padding:5px 4px;}
    table.items thead th:first-child{border-radius:7px 0 0 7px;}
    table.items thead th:last-child{border-radius:0 7px 7px 0;text-align:right;}
    table.items thead th.num, table.items tbody td.num{text-align:right;}
    table.items tbody td{padding:8px;border-bottom:1px solid var(--divider);vertical-align:top;}
    body.size-t58 table.items tbody td{padding:5px 4px;}
    table.items tbody tr:last-child td{border-bottom:none;}
    .totals{padding-top:6px;}
    .trow{display:flex;align-items:center;justify-content:space-between;font-size:12.5px;padding:6px 0;}
    body.size-t58 .trow{font-size:10.5px;}
    .trow .lbl{display:flex;align-items:center;gap:7px;color:#3c4a40;}
    .trow .lbl svg{width:14px;height:14px;flex:none;color:var(--green-700);}
    .trow .val{font-weight:600;}
    .grand{margin:8px 0 14px;background:var(--green-800);border-radius:11px;padding:12px 16px;display:flex;align-items:center;justify-content:space-between;}
    body.size-t58 .grand, body.size-t80 .grand{padding:9px 11px;border-radius:8px;}
    .grand .lbl{color:#eafcef;font-size:14px;font-weight:700;display:flex;align-items:center;gap:7px;}
    body.size-t58 .grand .lbl{font-size:11px;}
    .grand .lbl svg{width:16px;height:16px;}
    .grand .val{color:#ff7d97;font-size:17px;font-weight:700;}
    body.size-t58 .grand .val{font-size:13px;}
    .pay-card .method{font-size:12.5px;font-weight:600;color:var(--ink);}
    body.size-t58 .pay-card .method{font-size:10.5px;}
    .footer{position:relative;margin-top:auto;padding-bottom:118px;}
    body.size-t58 .footer, body.size-t80 .footer{padding-bottom:78px;margin-top:10px;}
    .footer-msg{text-align:center;padding:18px 10px 6px;}
    .footer-msg .big{font-size:26px;font-weight:700;color:var(--green-800);margin:0;}
    body.size-t58 .footer-msg .big{font-size:18px;}
    .footer-msg .small{font-size:11.5px;color:var(--muted);margin-top:4px;display:flex;align-items:center;justify-content:center;gap:5px;}
    body.size-t58 .footer-msg .small{font-size:9.5px;}
    .footer-msg .small svg{width:12px;height:12px;}
    .wave-wrap{position:absolute;left:0;right:0;bottom:0;height:118px;overflow:hidden;}
    body.size-t58 .wave-wrap, body.size-t80 .wave-wrap{height:78px;}
    .wave-wrap svg{position:absolute;bottom:0;left:0;width:100%;height:100%;}
    .wave-strap{position:absolute;left:0;right:0;bottom:0;height:34px;background:var(--green-900);display:flex;align-items:center;justify-content:center;color:#eafcef;font-size:11px;font-weight:600;gap:6px;letter-spacing:.2px;}
    body.size-t58 .wave-strap{font-size:8.5px;height:26px;}
    .wave-strap svg{width:12px;height:12px;}
    """.trimIndent()
}
