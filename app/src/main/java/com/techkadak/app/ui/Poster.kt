package com.techkadak.app.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.techkadak.app.Config
import com.techkadak.app.R
import com.techkadak.app.data.AppItem
import com.techkadak.app.data.Catalog
import java.io.File
import java.io.FileOutputStream

/**
 * "Poster share karo": phone pe hi banta hai (koi server nahi). Tech Kadak style — lime / black / white,
 * app ka apna icon, naam, Hinglish one-liner, "Tech Kadak app pe FREE" + techkadak.github.io.
 * Sirf app ka apna icon — kisi aur brand ka logo nahi.
 */
enum class PosterFormat(val w: Int, val h: Int, val hi: String, val en: String) {
    STORY(1080, 1920, "Status / Story (9:16)", "Status / Story (9:16)"),
    SQUARE(1080, 1080, "Post (1:1)", "Post (1:1)"),
}

object Poster {
    private const val LIME = 0xFF9BFF2E.toInt()
    private const val BLACK = 0xFF070707.toInt()
    private const val INK = 0xFF0A0A0A.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()

    private fun font(ctx: Context, id: Int, fallback: Typeface): Typeface =
        runCatching { ResourcesCompat.getFont(ctx, id) }.getOrNull() ?: fallback

    /** One-liner: app.what ka pehla vakya, ~120 chars tak. */
    fun oneLiner(app: AppItem): String {
        val w = app.what.trim().replace(Regex("\\s+"), " ")
        val first = Regex("^(.{25,140}?[.!?।])(\\s|$)").find(w)?.groupValues?.get(1) ?: w
        return if (first.length <= 130) first else first.take(127).trimEnd(' ', ',', ';', ':', '—', '-') + "…"
    }

    fun render(ctx: Context, app: AppItem, icon: Bitmap?, f: PosterFormat): Bitmap {
        val bmp = Bitmap.createBitmap(f.w, f.h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val display = font(ctx, R.font.unbounded_900, Typeface.DEFAULT_BOLD)
        val display7 = font(ctx, R.font.unbounded_700, Typeface.DEFAULT_BOLD)
        val body = font(ctx, R.font.dmsans_400, Typeface.DEFAULT)
        val bodyB = font(ctx, R.font.dmsans_700, Typeface.DEFAULT_BOLD)
        val W = f.w.toFloat(); val H = f.h.toFloat()
        val story = f == PosterFormat.STORY

        // ── Background: black + lime glows + faint dotted grid ──
        c.drawColor(BLACK)
        val glow = Paint(Paint.ANTI_ALIAS_FLAG)
        glow.shader = RadialGradient(W * 0.92f, H * 0.08f, W * 0.85f, intArrayOf(0x669BFF2E, 0x00000000), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, W, H, glow)
        glow.shader = RadialGradient(W * 0.05f, H * 0.95f, W * 0.7f, intArrayOf(0x339BFF2E, 0x00000000), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, W, H, glow)
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1AFFFFFF }
        var gy = 40f
        while (gy < H) { var gx = 40f; while (gx < W) { c.drawCircle(gx, gy, 2.2f, dot); gx += 54f }; gy += 54f }
        // Big ghost word
        val ghost = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = display; color = 0x129BFF2E; textSize = if (story) 300f else 230f }
        c.save(); c.rotate(-90f, W - 60f, H * 0.5f)
        c.drawText("KADAK", W - 60f - ghost.measureText("KADAK") / 2f, H * 0.5f, ghost)
        c.restore()

        val pad = 84f
        // ── Top brand pill: TK logo + TECH KADAK ──
        val pillH = 92f
        val logo = runCatching { BitmapFactory.decodeResource(ctx.resources, R.drawable.tk_logo) }.getOrNull()
        val brand = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = display; textSize = 38f; color = INK; letterSpacing = 0.06f }
        val bw = brand.measureText("TECH KADAK")
        val pillW = pillH + 22f + bw + 44f
        val top = if (story) 110f else 70f
        val pill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LIME }
        c.drawRoundRect(RectF(pad, top, pad + pillW, top + pillH), pillH / 2, pillH / 2, pill)
        if (logo != null) {
            val r = RectF(pad + 8f, top + 8f, pad + pillH - 8f, top + pillH - 8f)
            val sh = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = BitmapShader(Bitmap.createScaledBitmap(logo, r.width().toInt(), r.height().toInt(), true), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).also { s -> s.setLocalMatrix(Matrix().apply { setTranslate(r.left, r.top) }) } }
            c.drawOval(r, sh)
        }
        c.drawText("TECH KADAK", pad + pillH + 14f, top + pillH / 2 + 14f, brand)
        // tag on right
        val tagP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = bodyB; textSize = 32f; color = LIME; letterSpacing = 0.12f }
        val tag = if (Catalog.isOpenSource(app)) "OPEN SOURCE" else "FREE APP"
        c.drawText(tag, W - pad - tagP.measureText(tag), top + pillH / 2 + 11f, tagP)

        // ── Icon ──
        val iconSize = if (story) 400f else 300f
        val iconTop = if (story) 420f else 200f
        val iconLeft = if (story) (W - iconSize) / 2 else pad
        val ir = RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
        val rad = iconSize * 0.24f
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x339BFF2E; maskFilter = BlurMaskFilter(60f, BlurMaskFilter.Blur.NORMAL) }
        c.drawRoundRect(RectF(ir.left - 30, ir.top - 30, ir.right + 30, ir.bottom + 30), rad + 30, rad + 30, ring)
        val tile = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF161616.toInt() }
        c.drawRoundRect(ir, rad, rad, tile)
        if (icon != null) {
            val path = Path().apply { addRoundRect(ir, rad, rad, Path.Direction.CW) }
            c.save(); c.clipPath(path)
            val inset = iconSize * 0.0f
            c.drawBitmap(icon, null, RectF(ir.left + inset, ir.top + inset, ir.right - inset, ir.bottom - inset), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            c.restore()
        } else {
            val ip = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = display; color = LIME; textSize = iconSize * 0.36f }
            val t = app.initials
            c.drawText(t, ir.centerX() - ip.measureText(t) / 2, ir.centerY() + ip.textSize * 0.36f, ip)
        }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 6f; color = LIME }
        c.drawRoundRect(ir, rad, rad, border)

        // ── Name ──
        val nameP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = display; color = WHITE }
        val textLeft = if (story) pad else ir.right + 48f
        val textW = (W - pad - textLeft).toInt()
        var size = if (story) 120f else 84f
        val name = app.name.substringBefore(" (").trim()
        var nameL: StaticLayout
        while (true) {
            nameP.textSize = size
            nameL = layout(name, nameP, textW, if (story) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL, 0.95f)
            val longestWord = name.split(' ').maxOf { nameP.measureText(it) }
            if ((nameL.lineCount <= 2 && longestWord <= textW) || size < 48f) break
            size -= 6f
        }
        val nameTop = if (story) ir.bottom + 100f else ir.top + (iconSize - nameL.height) / 2 - 20f
        c.save(); c.translate(textLeft, nameTop); nameL.draw(c); c.restore()

        // ── Category · price chips ──
        val chipP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = bodyB; textSize = 34f; color = LIME }
        val chips = listOfNotNull(Catalog.shortName(app.categories.firstOrNull() ?: "").ifBlank { null }, app.pricing.uppercase().take(14))
        var chipY = if (story) nameTop + nameL.height + 40f else (ir.bottom + 60f)
        run {
            val gap = 18f; val ch = 70f
            val widths = chips.map { chipP.measureText(it) + 56f }
            var x = if (story) (W - (widths.sum() + gap * (widths.size - 1))) / 2 else pad
            chips.forEachIndexed { i, t ->
                val r = RectF(x, chipY, x + widths[i], chipY + ch)
                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (i == chips.lastIndex) LIME else 0x00000000 }
                c.drawRoundRect(r, ch / 2, ch / 2, fill)
                c.drawRoundRect(r, ch / 2, ch / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = LIME })
                chipP.color = if (i == chips.lastIndex) INK else LIME
                c.drawText(t, x + 28f, chipY + ch / 2 + 12f, chipP)
                x += widths[i] + gap
            }
            chipY += ch
        }

        // ── One-liner (CTA se pehle hi fit ho — chhota karo / "…") ──
        val ctaH = if (story) 150f else 118f
        val ctaBottom = H - (if (story) 230f else 120f)
        val lineP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = body; color = 0xE6FFFFFF.toInt(); textSize = if (story) 50f else 42f }
        val lineW = (W - pad * 2).toInt()
        val lineTop = chipY + (if (story) 60f else 40f)
        val avail = (ctaBottom - ctaH - (if (story) 70f else 36f)) - lineTop
        val align = if (story) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL
        var maxLines = if (story) 5 else 3
        var lineL = layout(oneLiner(app), lineP, lineW, align, 1.12f, maxLines)
        while (lineL.height > avail && (lineP.textSize > 32f || maxLines > 1)) {
            if (lineP.textSize > 32f) lineP.textSize -= 3f else maxLines -= 1
            lineL = layout(oneLiner(app), lineP, lineW, align, 1.12f, maxLines)
        }
        if (lineL.height <= avail) { c.save(); c.translate(pad, lineTop); lineL.draw(c); c.restore() }

        // ── Bottom CTA ──
        val cta = RectF(pad, ctaBottom - ctaH, W - pad, ctaBottom)
        c.drawRoundRect(cta, ctaH / 2, ctaH / 2, pill)
        val ctaP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = display; color = INK; textSize = if (story) 50f else 42f }
        val ctaText = "TECH KADAK APP PE FREE"
        while (ctaP.measureText(ctaText) > cta.width() - 80f) ctaP.textSize -= 2f
        c.drawText(ctaText, cta.centerX() - ctaP.measureText(ctaText) / 2, cta.centerY() + ctaP.textSize * 0.36f, ctaP)
        val urlP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = display7; color = WHITE; textSize = if (story) 44f else 36f; letterSpacing = 0.02f }
        val host = Config.SITE_BASE.removePrefix("https://").trimEnd('/')
        c.drawText(host, W / 2 - urlP.measureText(host) / 2, ctaBottom + (if (story) 100f else 70f), urlP)
        if (story) {
            val small = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = body; color = 0x99FFFFFF.toInt(); textSize = 32f }
            val s = "Sirf official download links • No fake ratings"
            c.drawText(s, W / 2 - small.measureText(s) / 2, ctaBottom + 160f, small)
        }
        return bmp
    }

    private fun layout(text: String, p: TextPaint, w: Int, align: Layout.Alignment, spacing: Float, maxLines: Int = Int.MAX_VALUE): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, p, w).setAlignment(align).setLineSpacing(0f, spacing).setIncludePad(false)
            .setMaxLines(maxLines).setEllipsize(android.text.TextUtils.TruncateAt.END).build()

    fun save(ctx: Context, app: AppItem, f: PosterFormat, bmp: Bitmap): File {
        val dir = File(ctx.cacheDir, "posters").apply { mkdirs() }
        val file = File(dir, "techkadak-${app.slug}-${f.name.lowercase()}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    fun shareIntent(ctx: Context, app: AppItem, file: File): Intent {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", file)
        val text = "${app.name} — Tech Kadak app pe free 🔥\n${app.pageUrl(Config.SITE_BASE)}"
        val send = Intent(Intent.ACTION_SEND).setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_TEXT, text)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri("poster", uri)
        return Intent.createChooser(send, tr("Poster share karo", "Share poster")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
