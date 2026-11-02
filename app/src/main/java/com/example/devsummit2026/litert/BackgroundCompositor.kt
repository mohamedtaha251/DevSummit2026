package com.example.devsummit2026.litert

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader

/**
 * 4 simple, stable background modes:
 * - NONE: Natural selfie camera feed
 * - BLUR: Google Meet bokeh blur
 * - OFFICE: Warm modern office interior
 * - STUDIO: Sleek Google Meet neon studio gradient
 */
enum class BackgroundMode(val label: String) {
    NONE("Original"),
    BLUR("Blur"),
    OFFICE("Office"),
    STUDIO("Studio")
}

/**
 * High-performance, rock-solid compositing engine.
 * Uses double-buffering to eliminate screen tearing and flickering completely:
 * the background thread renders into a private back buffer, and only swaps to the
 * front buffer once drawing is 100% complete.
 */
class BackgroundCompositor {

    // Double buffering: UI only sees frontBitmap after it is completely drawn
    private var frontBitmap: Bitmap? = null
    private var backBitmap: Bitmap? = null
    private var backCanvas: Canvas? = null
    private val dstRect = Rect()

    private val filterPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val maskPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    // Cached preset background bitmaps (created once per resolution)
    private var cachedWidth = 0
    private var cachedHeight = 0
    private var officeBg: Bitmap? = null
    private var studioBg: Bitmap? = null

    // Pre-allocated downsampled blur bitmap to avoid GC allocation during streaming
    private var blurBitmap: Bitmap? = null
    private var blurCanvas: Canvas? = null

    @Synchronized
    fun composite(
        cameraBitmap: Bitmap,
        maskBitmap: Bitmap?,
        mode: BackgroundMode
    ): Bitmap {
        if (mode == BackgroundMode.NONE || maskBitmap == null) {
            return cameraBitmap
        }

        val width = cameraBitmap.width
        val height = cameraBitmap.height

        // Ensure double buffers match resolution
        if (frontBitmap == null || frontBitmap?.width != width || frontBitmap?.height != height) {
            frontBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            backBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            backCanvas = Canvas(backBitmap!!)
        }

        val canvas = backCanvas ?: return cameraBitmap
        dstRect.set(0, 0, width, height)

        ensurePresetBackgrounds(width, height)

        // Clear previous frame in back buffer
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)

        // Step 1: Draw the selected background layer
        when (mode) {
            BackgroundMode.BLUR -> {
                val blurred = getBlurredBitmap(cameraBitmap)
                canvas.drawBitmap(blurred, null, dstRect, filterPaint)
            }
            BackgroundMode.OFFICE -> {
                officeBg?.let { canvas.drawBitmap(it, null, dstRect, filterPaint) }
            }
            BackgroundMode.STUDIO -> {
                studioBg?.let { canvas.drawBitmap(it, null, dstRect, filterPaint) }
            }
            BackgroundMode.NONE -> return cameraBitmap
        }

        // Step 2: Draw the masked foreground person on an isolated Skia layer
        val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawBitmap(cameraBitmap, 0f, 0f, null)
        canvas.drawBitmap(maskBitmap, null, dstRect, maskPaint)
        canvas.restoreToCount(saveCount)

        // Step 3: Swap front and back buffers atomically
        // Now frontBitmap holds the 100% completed frame, safe for UI rendering
        val completed = backBitmap!!
        backBitmap = frontBitmap
        frontBitmap = completed
        backCanvas = Canvas(backBitmap!!)

        return completed
    }

    /**
     * Reuses a fixed 32x48 bitmap for bokeh blur with zero allocations per frame.
     * Upsampling with bilinear filtering expands it into a soft background blur.
     */
    private fun getBlurredBitmap(source: Bitmap): Bitmap {
        val downW = 32
        val downH = 48
        val current = blurBitmap
        val small = if (current != null && current.width == downW && current.height == downH) {
            current
        } else {
            val bmp = Bitmap.createBitmap(downW, downH, Bitmap.Config.ARGB_8888)
            blurBitmap = bmp
            blurCanvas = Canvas(bmp)
            bmp
        }
        val canvas = blurCanvas ?: Canvas(small)
        canvas.drawBitmap(source, null, Rect(0, 0, downW, downH), filterPaint)
        return small
    }

    private fun ensurePresetBackgrounds(width: Int, height: Int) {
        if (cachedWidth == width && cachedHeight == height && officeBg != null) {
            return
        }

        cachedWidth = width
        cachedHeight = height

        officeBg = generateOfficeBackground(width, height)
        studioBg = generateStudioBackground(width, height)
    }

    private fun generateOfficeBackground(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // Neutral professional office wall
        val wallPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h.toFloat(),
                Color.rgb(228, 232, 238), Color.rgb(190, 198, 210), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), wallPaint)

        // Soft ambient window light from top right
        val windowGlow = Paint().apply {
            shader = RadialGradient(w * 0.85f, h * 0.25f, w * 0.5f,
                Color.argb(100, 255, 250, 235), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
        c.drawCircle(w * 0.85f, h * 0.25f, w * 0.5f, windowGlow)

        // Wooden bookshelf on left side
        val woodPaint = Paint().apply { color = Color.rgb(90, 58, 38) }
        c.drawRect(0f, 0f, w * 0.28f, h.toFloat(), woodPaint)

        val shelfPaint = Paint().apply { color = Color.rgb(65, 38, 22) }
        val shelfCount = 5
        for (i in 1..shelfCount) {
            val y = h * (i.toFloat() / (shelfCount + 1))
            c.drawRect(0f, y - 5f, w * 0.28f, y + 5f, shelfPaint)
        }

        // Colorful book rows
        val bookColors = intArrayOf(
            Color.rgb(180, 50, 45), Color.rgb(40, 100, 160),
            Color.rgb(55, 130, 80), Color.rgb(210, 160, 40)
        )
        val bookPaint = Paint()
        for (i in 1..shelfCount) {
            val y = h * (i.toFloat() / (shelfCount + 1))
            var startX = 14f
            while (startX < w * 0.24f) {
                val bookW = 16f
                val bookH = 45f + ((startX * 7) % 25)
                bookPaint.color = bookColors[(startX.toInt() % bookColors.size)]
                c.drawRect(startX, y - bookH, startX + bookW, y - 5f, bookPaint)
                startX += bookW + 4f
            }
        }

        return bmp
    }

    private fun generateStudioBackground(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // Google Meet style sleek studio gradient (Dark Navy to Violet to Cyan)
        val gradientPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(),
                intArrayOf(
                    Color.rgb(15, 23, 42),
                    Color.rgb(99, 24, 155),
                    Color.rgb(56, 189, 248)
                ),
                floatArrayOf(0.0f, 0.55f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), gradientPaint)

        // Subtle soft center illumination
        val spotPaint = Paint().apply {
            shader = RadialGradient(w * 0.5f, h * 0.4f, w * 0.65f,
                Color.argb(60, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
        c.drawCircle(w * 0.5f, h * 0.4f, w * 0.65f, spotPaint)

        return bmp
    }
}
