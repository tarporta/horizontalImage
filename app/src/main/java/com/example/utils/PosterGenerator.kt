package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PosterGenerator {

    /**
     * Helper to load, scale, and apply soft visual vignette (feathering) to an image bitmap.
     * Feathering blends custom image edges smoothly to white, matching the poster's background.
     */
    fun loadFeatheredBitmap(path: String, targetSize: Int, feather: Float): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        try {
            val original = android.graphics.BitmapFactory.decodeFile(path) ?: return null
            val size = targetSize.coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(original, size, size, true)
            if (scaled != original) {
                original.recycle()
            }
            if (feather <= 0f) {
                return scaled
            }
            // Create a mutable copy to apply radial feather brush
            val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            canvas.drawBitmap(scaled, 0f, 0f, null)
            scaled.recycle()

            // Draw white radial gradient centered over the bitmap
            val paint = Paint().apply {
                isAntiAlias = true
                val cx = size / 2f
                val cy = size / 2f
                val radius = size / 2f
                val colors = intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.WHITE)
                // Stops move inward as feather increases (feathering gets wider)
                val stops = floatArrayOf(0f, (1f - feather).coerceIn(0f, 1f), 1f)
                shader = android.graphics.RadialGradient(
                    cx, cy, radius,
                    colors, stops,
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Generates a 1080x1920 poster bitmap.
     */
    fun generatePosterBitmap(
        textA: String,
        emojiA: String,
        textB: String,
        emojiB: String,
        fontSizeSp: Float,
        textColorHex: String,
        isVerticalLayout: Boolean,
        footerText: String,
        showFooter: Boolean,
        borderStyle: String, // "none", "thin", "bold", "double"
        operatorSizeSp: Float = 18f,
        operatorWeight: String = "light",
        mysterySizeSp: Float = 24f,
        mysteryWeight: String = "bold",
        customFontPath: String? = null,
        imagePathA: String? = null,
        imagePathB: String? = null,
        imageSizeA: Float = 120f,
        imageSizeB: Float = 120f,
        featherA: Float = 0f,
        featherB: Float = 0f
    ): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Load custom fonts if supplied, otherwise standard defaults
        val baseTypeface = if (customFontPath != null) {
            try {
                Typeface.createFromFile(customFontPath)
            } catch (e: Exception) {
                Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
        } else {
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val regularTypeface = if (customFontPath != null) {
            try {
                Typeface.createFromFile(customFontPath)
            } catch (e: Exception) {
                Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
        } else {
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        // 1. Draw crisp white background
        canvas.drawColor(Color.WHITE)

        // Parse text color
        val textColor = try {
            Color.parseColor(textColorHex)
        } catch (e: Exception) {
            Color.BLACK
        }

        // 2. Draw border decorations if any
        val borderPaint = Paint().apply {
            color = textColor
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        when (borderStyle) {
            "thin" -> {
                borderPaint.strokeWidth = 6f
                canvas.drawRect(40f, 40f, width - 40f, height - 40f, borderPaint)
            }
            "bold" -> {
                borderPaint.strokeWidth = 16f
                canvas.drawRect(40f, 40f, width - 40f, height - 40f, borderPaint)
            }
            "double" -> {
                borderPaint.strokeWidth = 6f
                canvas.drawRect(40f, 40f, width - 40f, height - 40f, borderPaint)
                canvas.drawRect(52f, 52f, width - 52f, height - 52f, borderPaint)
            }
        }

        // Scale factors relative to the baseline 40f SP
        val scale = fontSizeSp / 40f

        // High-fidelity graphic paints
        val emojiPaint = Paint().apply {
            textSize = 160f * scale
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val textPaint = Paint().apply {
            color = textColor
            textSize = 72f * scale
            isAntiAlias = true
            typeface = baseTypeface
            textAlign = Paint.Align.CENTER
        }

        val operatorTypeface = when (operatorWeight) {
            "light" -> {
                if (customFontPath != null) regularTypeface
                else Typeface.create("sans-serif-light", Typeface.NORMAL)
            }
            "bold" -> {
                if (customFontPath != null) baseTypeface
                else Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            else -> regularTypeface
        }

        val operatorPaint = Paint().apply {
            color = Color.parseColor("#94A3B8") // Elegant Slate-400 for operators (+, =, etc.)
            textSize = operatorSizeSp * 3.5f
            isAntiAlias = true
            typeface = operatorTypeface
            textAlign = Paint.Align.CENTER
        }

        val dashedBorderPaint = Paint().apply {
            color = Color.parseColor("#94A3B8") // slate-400
            style = Paint.Style.STROKE
            strokeWidth = 6f * scale
            isAntiAlias = true
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(24f * scale, 16f * scale), 0f)
        }

        val mysteryBoxBgPaint = Paint().apply {
            color = Color.parseColor("#F8FAFC") // slate-50
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val mysteryTypeface = when (mysteryWeight) {
            "bold" -> baseTypeface
            "black" -> {
                if (customFontPath != null) baseTypeface
                else Typeface.create("sans-serif-black", Typeface.BOLD)
            }
            else -> regularTypeface
        }

        val mysteryTextPaint = Paint().apply {
            color = Color.parseColor("#94A3B8") // slate-400
            textSize = mysterySizeSp * 3.5f
            isAntiAlias = true
            typeface = mysteryTypeface
            textAlign = Paint.Align.CENTER
        }

        val centerX = width / 2f

        if (isVerticalLayout) {
            // Compute heights dynamically considering emojis vs custom images
            val heightA = if (imagePathA != null) imageSizeA * scale else 160f * scale
            val heightB = if (imagePathB != null) imageSizeB * scale else 160f * scale
            val labelHeight = 72f * scale
            val spacing = 24f * scale
            val opSpacing = 80f * scale
            val opHeight = operatorSizeSp * 3.5f
            val boxHeight = mysterySizeSp * 3.5f * 2.1f // Resizing mystery box dynamically

            val totalStackHeight = (
                heightA + spacing + labelHeight + // Element A
                opSpacing + opHeight + opSpacing + // Plus
                heightB + spacing + labelHeight + // Element B
                opSpacing + opHeight + opSpacing + // Equal
                boxHeight // Mystery Box
            )

            var currentY = (height - totalStackHeight) / 2f

            // Helper to center text vertically by calculating its font descent/ascent
            fun drawCenteredText(canvas: Canvas, text: String, cx: Float, cy: Float, paint: Paint) {
                val fm = paint.fontMetrics
                val textOffset = (fm.descent + fm.ascent) / 2f
                canvas.drawText(text, cx, cy - textOffset, paint)
            }

            // Element A
            val elementACY = currentY + (heightA / 2f)
            if (imagePathA != null) {
                val imgBitmap = loadFeatheredBitmap(imagePathA, (imageSizeA * scale).toInt(), featherA)
                if (imgBitmap != null) {
                    canvas.drawBitmap(imgBitmap, centerX - imgBitmap.width / 2f, elementACY - imgBitmap.height / 2f, null)
                    imgBitmap.recycle()
                }
            } else {
                drawCenteredText(canvas, emojiA, centerX, elementACY, emojiPaint)
            }
            currentY += heightA + spacing

            val textACY = currentY + (labelHeight / 2f)
            drawCenteredText(canvas, textA.uppercase(Locale.getDefault()), centerX, textACY, textPaint)
            currentY += labelHeight + opSpacing

            // Plus Operator
            val plusCY = currentY + (opHeight / 2f)
            drawCenteredText(canvas, "+", centerX, plusCY, operatorPaint)
            currentY += opHeight + opSpacing

            // Element B
            val elementBCY = currentY + (heightB / 2f)
            if (imagePathB != null) {
                val imgBitmap = loadFeatheredBitmap(imagePathB, (imageSizeB * scale).toInt(), featherB)
                if (imgBitmap != null) {
                    canvas.drawBitmap(imgBitmap, centerX - imgBitmap.width / 2f, elementBCY - imgBitmap.height / 2f, null)
                    imgBitmap.recycle()
                }
            } else {
                drawCenteredText(canvas, emojiB, centerX, elementBCY, emojiPaint)
            }
            currentY += heightB + spacing

            val textBCY = currentY + (labelHeight / 2f)
            drawCenteredText(canvas, textB.uppercase(Locale.getDefault()), centerX, textBCY, textPaint)
            currentY += labelHeight + opSpacing

            // Equal Operator
            val equalCY = currentY + (opHeight / 2f)
            drawCenteredText(canvas, "=", centerX, equalCY, operatorPaint)
            currentY += opHeight + opSpacing

            // Mystery Box containing ?
            val boxLeft = centerX - (boxHeight * 0.5f)
            val boxRight = centerX + (boxHeight * 0.5f)
            val boxTop = currentY
            val boxBottom = currentY + boxHeight
            val rx = boxHeight * 0.18f

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, rx, rx, mysteryBoxBgPaint)
                canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, rx, rx, dashedBorderPaint)
            } else {
                canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, mysteryBoxBgPaint)
                canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, dashedBorderPaint)
            }

            val boxCY = currentY + (boxHeight / 2f)
            drawCenteredText(canvas, "?", centerX, boxCY, mysteryTextPaint)

        } else {
            // Horizontal layout (Elements perfectly scaled and centered)
            val verticalCenterY = height / 2f

            // Center X positions spaced perfectly across 1080 width
            val xA = 180f
            val xPlus = 370f
            val xB = 540f
            val xEqual = 710f
            val xMystery = 900f

            fun drawCenteredText(canvas: Canvas, text: String, cx: Float, cy: Float, paint: Paint) {
                val fm = paint.fontMetrics
                val textOffset = (fm.descent + fm.ascent) / 2f
                canvas.drawText(text, cx, cy - textOffset, paint)
            }

            // Adjust horizontal specific font scales to fit neatly
            val hEmojiPaint = Paint(emojiPaint).apply { textSize = 140f * scale }
            val hTextPaint = Paint(textPaint).apply { textSize = 54f * scale }
            val hOperatorPaint = Paint(operatorPaint).apply { textSize = operatorSizeSp * 3.5f }

            val textYDelta = 45f * scale
            val textYDown = 65f * scale

            // Element A (Emoji or Custom Image + Text)
            val imgSizeA = imageSizeA * scale
            if (imagePathA != null) {
                val imgBitmap = loadFeatheredBitmap(imagePathA, imgSizeA.toInt(), featherA)
                if (imgBitmap != null) {
                    canvas.drawBitmap(imgBitmap, xA - imgBitmap.width / 2f, (verticalCenterY - textYDelta) - imgBitmap.height / 2f, null)
                    imgBitmap.recycle()
                }
            } else {
                drawCenteredText(canvas, emojiA, xA, verticalCenterY - textYDelta, hEmojiPaint)
            }
            drawCenteredText(canvas, textA.uppercase(Locale.getDefault()), xA, verticalCenterY + textYDown, hTextPaint)

            // Plus Operator
            drawCenteredText(canvas, "+", xPlus, verticalCenterY, hOperatorPaint)

            // Element B (Emoji or Custom Image + Text)
            val imgSizeB = imageSizeB * scale
            if (imagePathB != null) {
                val imgBitmap = loadFeatheredBitmap(imagePathB, imgSizeB.toInt(), featherB)
                if (imgBitmap != null) {
                    canvas.drawBitmap(imgBitmap, xB - imgBitmap.width / 2f, (verticalCenterY - textYDelta) - imgBitmap.height / 2f, null)
                    imgBitmap.recycle()
                }
            } else {
                drawCenteredText(canvas, emojiB, xB, verticalCenterY - textYDelta, hEmojiPaint)
            }
            drawCenteredText(canvas, textB.uppercase(Locale.getDefault()), xB, verticalCenterY + textYDown, hTextPaint)

            // Equal Operator
            drawCenteredText(canvas, "=", xEqual, verticalCenterY, hOperatorPaint)

            // Mystery Box containing ?
            val boxWidthHeight = mysterySizeSp * 3.5f * 2.0f
            val boxLeft = xMystery - (boxWidthHeight / 2f)
            val boxRight = xMystery + (boxWidthHeight / 2f)
            val boxTop = verticalCenterY - (boxWidthHeight / 2f)
            val boxBottom = verticalCenterY + (boxWidthHeight / 2f)
            val rx = boxWidthHeight * 0.18f

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, rx, rx, mysteryBoxBgPaint)
                canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, rx, rx, dashedBorderPaint)
            } else {
                canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, mysteryBoxBgPaint)
                canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, dashedBorderPaint)
            }

            drawCenteredText(canvas, "?", xMystery, verticalCenterY, mysteryTextPaint)
        }

        // 5. Optionally draw footer
        if (showFooter && footerText.isNotEmpty()) {
            val footerPaint = Paint().apply {
                color = textColor
                alpha = 150 // semi-transparent
                textSize = 32f
                isAntiAlias = true
                typeface = regularTypeface
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(footerText, (width / 2).toFloat(), (height - 100).toFloat(), footerPaint)
        }

        return bitmap
    }

    /**
     * Saves the poster bitmap into the Gallery.
     */
    fun savePosterToGallery(context: Context, bitmap: Bitmap): Result<String> {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "EmojiPoster_$timeStamp.png"
        var fos: OutputStream? = null
        var imageUri: Uri? = null
        val contentResolver = context.contentResolver

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/VerticalEmojiPoster")
                }
                imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    fos = contentResolver.openOutputStream(imageUri)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
                val dir = File(imagesDir, "VerticalEmojiPoster")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val image = File(dir, filename)
                fos = FileOutputStream(image)
                imageUri = Uri.fromFile(image)

                // Trigger media scanner on old devices
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = imageUri
                context.sendBroadcast(mediaScanIntent)
            }

            if (fos != null) {
                val success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.close()
                if (success) {
                    Result.success("포스터 이미지가 갤러리에 저장되었습니다!")
                } else {
                    Result.failure(Exception("이미지 압축에 실패했습니다."))
                }
            } else {
                Result.failure(Exception("파일 출력 스트림을 열 수 없습니다."))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Shares the poster bitmap to other applications.
     */
    fun sharePoster(context: Context, bitmap: Bitmap): Result<Unit> {
        return try {
            val cachePath = File(context.cacheDir, "shared_images")
            cachePath.mkdirs()

            val file = File(cachePath, "emoji_vertical_poster.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            if (contentUri != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    type = "image/png"
                }
                context.startActivity(Intent.createChooser(shareIntent, "포스터 공유하기"))
                Result.success(Unit)
            } else {
                Result.failure(Exception("공유용 Content URI 생성 불가"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
