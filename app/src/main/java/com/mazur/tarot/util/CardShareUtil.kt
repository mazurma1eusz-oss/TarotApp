package com.mazur.tarot.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.mazur.tarot.R
import com.mazur.tarot.data.model.TarotCard
import com.mazur.tarot.data.repository.ReadingDetails
import java.io.File
import java.io.FileOutputStream

// Stały format grafiki udostępniania - 9:16 (Stories/Reels).
private const val CANVAS_WIDTH = 1080
private const val CANVAS_HEIGHT = 1920

private const val SIDE_PADDING = 90f
private const val CONTENT_WIDTH = 900f // 1080 - 2*90

private const val CARD_BORDER_WIDTH = 6f
private const val CARD_CORNER_RADIUS = 24f
private const val THUMB_GAP = 24f
private const val THUMB_CARD_WIDTH = 280f
private const val THUMB_CARD_HEIGHT = 470f
private const val THUMB_ROW_Y = 240f

private const val HERO_CARD_X = 270f
private const val HERO_CARD_Y = 220f
private const val HERO_CARD_W = 540f
private const val HERO_CARD_H = 900f

private const val COLOR_OVERLAY_A = 0x99
private const val COLOR_OVERLAY_R = 0x09
private const val COLOR_OVERLAY_G = 0x07
private const val COLOR_OVERLAY_B = 0x18

private const val COLOR_BORDER = "#D4AF37"
private const val COLOR_TEXT = "#F2EEFF"
private const val COLOR_LABEL = "#C9C2E6"
private const val COLOR_BRAND = "#8B5CF6"

private const val CAPTION_TEXT_SIZE = 38f
private const val THUMB_NAME_TEXT_SIZE = 26f
private const val QUESTION_TEXT_SIZE = 32f
private const val BODY_TEXT_SIZE = 42f
private const val FOOTER_TEXT_SIZE = 28f
private const val FOOTER_Y = 1840f

private const val MAX_BODY_LINES = 6

/**
 * Generuje graficzny podgląd karty lub całego odczytu (Dziennik) do udostępnienia - stały
 * kadr 1080x1920 (9:16): tło = prawdziwe zdjęcie aplikacji (main_bg) rozciągnięte na cały
 * kadr + ciemny overlay dla czytelności tekstu (nigdy pusty/jednolity kolor), karta(y) w
 * stałych, jawnie zdefiniowanych ramkach (CAŁA bitmapa karty, bez cropa - patrz [drawCard]),
 * potem czysty tekst odczytu (bez tagów [WGLĄD]/[PRZESŁANIE]) i stopka marki na stałym Y.
 * Renderowanie ([renderCardBitmap]/[renderReadingBitmap]) jest rozdzielone od faktycznego
 * udostępnienia ([shareBitmap]), żeby UI mogło najpierw pokazać DOKŁADNIE tę samą bitmapę
 * w podglądzie, zanim otworzy systemowy selektor.
 */
object CardShareUtil {

    fun renderCardBitmap(context: Context, card: TarotCard, isReversed: Boolean): Bitmap {
        val label = card.name + if (isReversed) " " + context.getString(R.string.reversed_suffix) else ""
        val bodyText = if (isReversed) card.descriptionReversedGeneral else card.descriptionGeneral
        return renderShareBitmap(
            context = context,
            entries = listOf(ShareCardEntry(card.imageResName, isReversed, label)),
            question = null,
            bodyText = bodyText,
        )
    }

    fun renderReadingBitmap(context: Context, reading: ReadingDetails, includeQuestion: Boolean): Bitmap {
        val entries = reading.drawnCards.map {
            ShareCardEntry(
                imageResName = it.card.imageResName,
                isReversed = it.isReversed,
                displayName = it.card.name + if (it.isReversed) " " + context.getString(R.string.reversed_suffix) else "",
            )
        }
        return renderShareBitmap(
            context = context,
            entries = entries,
            question = if (includeQuestion) reading.question else null,
            bodyText = extractShareText(context, reading),
        )
    }

    /** Buduje podpis towarzyszący grafice (styl "wiadomości") - wymienia wylosowane karty i
     * zaprasza odbiorcę do pobrania aplikacji, z linkiem wprost do Google Play. */
    fun buildShareCaption(context: Context, cardNames: List<String>): String {
        val cardsText = cardNames.joinToString(", ")
        val playStoreUrl = context.getString(R.string.play_store_url)
        return context.getString(R.string.share_caption_template, cardsText, playStoreUrl)
    }

    /** Zapisuje [bitmap] do cache i otwiera systemowy selektor udostępniania - obraz (image/png)
     * plus opcjonalny [shareText] (podpis w stylu wiadomości, patrz [buildShareCaption]) jako
     * EXTRA_TEXT, żeby aplikacje czatowe pokazały go razem z grafiką. */
    fun shareBitmap(context: Context, bitmap: Bitmap, chooserTitle: String, fileName: String = "tarot_share.png", shareText: String? = null) {
        val file = saveBitmapToCache(context, bitmap, fileName)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            shareText?.let { putExtra(Intent.EXTRA_TEXT, it) }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    // ---- Renderer ----------------------------------------------------------------------

    private data class ShareCardEntry(val imageResName: String, val isReversed: Boolean, val displayName: String)

    private fun renderShareBitmap(
        context: Context,
        entries: List<ShareCardEntry>,
        question: String?,
        bodyText: String,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBackground(context, canvas)

        val frames = cardFrames(entries.size)
        entries.forEachIndexed { index, entry ->
            drawCardFrame(context, canvas, entry.imageResName, entry.isReversed, frames[index])
        }

        var cursorY = frames.maxOf { it.bottom } + 20f
        cursorY = if (entries.size == 1) {
            drawCenteredText(canvas, entries.first().displayName, CAPTION_TEXT_SIZE, COLOR_TEXT, CANVAS_WIDTH / 2f, cursorY, CONTENT_WIDTH)
        } else {
            drawCardNames(canvas, entries, frames, cursorY)
        }
        cursorY += 20f

        question?.takeIf { it.isNotBlank() }?.let {
            cursorY = drawCenteredText(canvas, "„$it”", QUESTION_TEXT_SIZE, "#EDE7FF", CANVAS_WIDTH / 2f, cursorY, CONTENT_WIDTH)
            cursorY += 24f
        }

        val bodyLayout = buildBodyLayout(bodyText, BODY_TEXT_SIZE, COLOR_TEXT, CONTENT_WIDTH)
        canvas.save()
        canvas.translate(SIDE_PADDING, cursorY)
        bodyLayout.draw(canvas)
        canvas.restore()

        drawBrandFooter(canvas)

        return bitmap
    }

    /** Tło grafiki: prawdziwe zdjęcie tła aplikacji (main_bg) rozciągnięte SCALE_FILL na cały
     * kadr 1080x1920 + półprzezroczysty ciemny overlay dla czytelności tekstu - nigdy pusty
     * jednolity kolor. */
    private fun drawBackground(context: Context, canvas: Canvas) {
        val bg = BitmapFactory.decodeResource(context.resources, R.drawable.main_bg)
        if (bg != null) {
            val src = Rect(0, 0, bg.width, bg.height)
            val dst = Rect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT)
            canvas.drawBitmap(bg, src, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }
        val overlayColor = Color.argb(COLOR_OVERLAY_A, COLOR_OVERLAY_R, COLOR_OVERLAY_G, COLOR_OVERLAY_B)
        canvas.drawColor(overlayColor)
    }

    /** Ramki na karty: 1 karta -> duża, wyśrodkowana hero-ramka (dokładne współrzędne ze
     * specyfikacji). 2+ kart -> rząd stałej wysokości Y, karty ~280x470 wyśrodkowane poziomo
     * (zmniejszane proporcjonalnie tylko jeśli więcej niż 3 nie zmieściłyby się w [CONTENT_WIDTH]). */
    private fun cardFrames(count: Int): List<RectF> {
        if (count <= 1) {
            return listOf(RectF(HERO_CARD_X, HERO_CARD_Y, HERO_CARD_X + HERO_CARD_W, HERO_CARD_Y + HERO_CARD_H))
        }
        val maxRowWidth = CONTENT_WIDTH
        val naturalWidth = (maxRowWidth - THUMB_GAP * (count - 1)) / count
        val cardWidth = minOf(THUMB_CARD_WIDTH, naturalWidth)
        val cardHeight = cardWidth * (THUMB_CARD_HEIGHT / THUMB_CARD_WIDTH)
        val rowWidth = cardWidth * count + THUMB_GAP * (count - 1)
        val startX = (CANVAS_WIDTH - rowWidth) / 2f
        return (0 until count).map { i ->
            val left = startX + i * (cardWidth + THUMB_GAP)
            RectF(left, THUMB_ROW_Y, left + cardWidth, THUMB_ROW_Y + cardHeight)
        }
    }

    /** Rysuje CAŁĄ [bitmap] (src = pełny prostokąt bitmapy: (0,0,bitmap.width,bitmap.height) -
     * ŻADNEGO mniejszego src, ŻADNEGO cropa od (0,0)) wewnątrz ramki [dst]. Gdy proporcje
     * bitmapy różnią się od proporcji [dst], obraz jest dopasowany (letterbox) i wyśrodkowany
     * WEWNĄTRZ [dst] - nigdy ucięty ani rozciągnięty w nierównych proporcjach. */
    private fun drawCard(bitmap: Bitmap, canvas: Canvas, dst: RectF) {
        val src = Rect(0, 0, bitmap.width, bitmap.height)
        val srcAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val dstAspect = dst.width() / dst.height()

        val fit = if (srcAspect > dstAspect) {
            // Bitmapa relatywnie szersza niż ramka -> dopasuj do szerokości ramki, wyśrodkuj w pionie.
            val fitHeight = dst.width() / srcAspect
            val top = dst.top + (dst.height() - fitHeight) / 2f
            RectF(dst.left, top, dst.right, top + fitHeight)
        } else {
            // Bitmapa relatywnie wyższa niż ramka -> dopasuj do wysokości ramki, wyśrodkuj w poziomie.
            val fitWidth = dst.height() * srcAspect
            val left = dst.left + (dst.width() - fitWidth) / 2f
            RectF(left, dst.top, left + fitWidth, dst.bottom)
        }

        canvas.drawBitmap(bitmap, src, fit, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    /** Dekoduje PEŁNĄ bitmapę karty (BitmapFactory.decodeResource - natywna rozdzielczość
     * assetu, żadnego wcześniejszego skalowania/cropowania przez Drawable.toBitmap), rysuje ją
     * przez [drawCard] wewnątrz zaokrąglonej, oprawionej złotą ramką [dst]. Odwrócona karta
     * jest obracana o 180 stopni WEWNĄTRZ własnej ramki - klip jest ustawiany PRZED obrotem,
     * a obrót o 180 stopni wokół środka prostokąta jest symetryczny (nie przesuwa obszaru
     * przycięcia), więc karta nigdy nie wychodzi poza swoją ramkę ani nie jest ucięta. */
    private fun drawCardFrame(context: Context, canvas: Canvas, imageResName: String, isReversed: Boolean, dst: RectF) {
        val resId = CardImageResolver.resolve(context, imageResName)
        val bitmap = BitmapFactory.decodeResource(context.resources, resId) ?: return

        val clipPath = Path().apply { addRoundRect(dst, CARD_CORNER_RADIUS, CARD_CORNER_RADIUS, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(clipPath)
        if (isReversed) {
            canvas.rotate(180f, dst.centerX(), dst.centerY())
        }
        drawCard(bitmap, canvas, dst)
        canvas.restore()

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_BORDER)
            style = Paint.Style.STROKE
            strokeWidth = CARD_BORDER_WIDTH
        }
        canvas.drawRoundRect(dst, CARD_CORNER_RADIUS, CARD_CORNER_RADIUS, borderPaint)
    }

    /** Nazwy kart pod rzędem miniatur (2+ kart) - jedna wyśrodkowana etykieta pod każdą ramką. */
    private fun drawCardNames(canvas: Canvas, entries: List<ShareCardEntry>, frames: List<RectF>, y: Float): Float {
        var maxBottom = y
        entries.forEachIndexed { index, entry ->
            val frame = frames[index]
            val bottom = drawCenteredText(canvas, entry.displayName, THUMB_NAME_TEXT_SIZE, COLOR_LABEL, frame.centerX(), y, frame.width())
            maxBottom = maxOf(maxBottom, bottom)
        }
        return maxBottom
    }

    /** Rysuje jednowierszowy/wielowierszowy tekst wyśrodkowany na [centerX], zaczynając od [y],
     * o maksymalnej szerokości [maxWidth]. Zwraca Y tuż pod narysowanym tekstem. */
    private fun drawCenteredText(canvas: Canvas, text: String, textSize: Float, color: String, centerX: Float, y: Float, maxWidth: Float): Float {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.parseColor(color)
            this.textSize = textSize
        }
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, maxWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.1f)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(centerX - maxWidth / 2f, y)
        layout.draw(canvas)
        canvas.restore()
        return y + layout.height
    }

    /** Buduje tekst odczytu wyśrodkowany w [maxWidth], max [MAX_BODY_LINES] linii - reszta jest
     * przycinana (na granicy linii, nie znaku) i kończona wielokropkiem zamiast pomniejszania
     * czcionki, żeby tekst na grafice był zawsze czytelny. */
    private fun buildBodyLayout(text: String, textSize: Float, color: String, maxWidth: Float): StaticLayout {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.parseColor(color)
            this.textSize = textSize
        }
        fun layoutFor(t: String) = StaticLayout.Builder
            .obtain(t, 0, t.length, paint, maxWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.25f)
            .setIncludePad(false)
            .build()

        var layout = layoutFor(text)
        var attempts = 0
        var source = text
        while (layout.lineCount > MAX_BODY_LINES && attempts < 3) {
            val cut = layout.getLineEnd(MAX_BODY_LINES - 1)
            source = source.substring(0, cut.coerceAtMost(source.length)).trimEnd().trimEnd('.', ',', ';', ':', '…') + "…"
            layout = layoutFor(source)
            attempts++
        }
        return layout
    }

    private fun drawBrandFooter(canvas: Canvas) {
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_BRAND)
            textSize = FOOTER_TEXT_SIZE
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✦ Tarot Mistyczny ✦", CANVAS_WIDTH / 2f, FOOTER_Y, brandPaint)
    }

    /** Wyciąga czysty tekst "Przesłania" z odpowiedzi AI (bez tagów [WGLĄD]/[PRZESŁANIE] i bez
     * gwiazdek Markdown) - dla zwykłych odczytów bez AI (fallback) po prostu łączy opisy kart. */
    private fun extractShareText(context: Context, reading: ReadingDetails): String {
        val raw = reading.aiResponse
        val text = if (raw != null) {
            val sections = AiResponseFormatter.parse(raw, context)
            val messageHeading = context.getString(R.string.ai_heading_message)
            val message = sections.firstOrNull { it.heading == messageHeading }
            message?.body ?: sections.lastOrNull()?.body ?: raw
        } else {
            reading.drawnCards.joinToString(" ") { it.activeDescription }
        }
        return text.replace("*", "").trim()
    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String): File {
        val dir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        return file
    }
}
