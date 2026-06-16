package com.example.infrastructure.adapters.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.provider.MediaStore
import com.example.core.domain.Habit
import com.example.core.domain.StreakStats

object ShareCardGenerator {

    fun generateAndSaveShareCard(
        context: Context,
        habit: Habit,
        stats: StreakStats,
        isCyberpunk: Boolean
    ): Boolean {
        val width = 800
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColor = if (isCyberpunk) 0xFF0D1220.toInt() else 0xFF22162B.toInt()
        val primaryColor = if (isCyberpunk) 0xFF22C55E.toInt() else 0xFFF97316.toInt()
        val secondaryColor = if (isCyberpunk) 0xFF8B5CF6.toInt() else 0xFFEC4899.toInt()
        val textColor = 0xFFF9FAFB.toInt()
        val mutedColor = 0xFF6B7280.toInt()

        canvas.drawColor(bgColor)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        paint.color = primaryColor
        paint.strokeWidth = 8f
        paint.style = Paint.Style.STROKE
        canvas.drawRect(20f, 20f, width - 20f, height - 20f, paint)

        paint.reset()
        paint.isAntiAlias = true
        paint.color = textColor
        paint.textSize = 36f
        paint.isFakeBoldText = true
        canvas.drawText("⚡ HabitEngine ⚡", 60f, 80f, paint)

        paint.reset()
        paint.isAntiAlias = true
        paint.color = secondaryColor
        paint.textSize = 24f
        canvas.drawText("OFFLINE RESOLUTION LEDGER", 60f, 120f, paint)

        paint.reset()
        paint.isAntiAlias = true
        paint.color = textColor
        paint.textSize = 32f
        paint.isFakeBoldText = true
        val wrappedRoutine = if (habit.routineText.length > 35) {
            habit.routineText.take(35) + "..."
        } else {
            habit.routineText
        }
        canvas.drawText("Habit: $wrappedRoutine", 60f, 200f, paint)

        paint.reset()
        paint.isAntiAlias = true
        paint.color = primaryColor
        paint.textSize = 48f
        paint.isFakeBoldText = true
        canvas.drawText("${stats.currentStreak} Days", 60f, 300f, paint)

        paint.reset()
        paint.isAntiAlias = true
        paint.color = textColor
        paint.textSize = 24f
        canvas.drawText("Current Streak", 60f, 340f, paint)

        paint.reset()
        paint.isAntiAlias = true
        paint.color = secondaryColor
        paint.textSize = 48f
        paint.isFakeBoldText = true
        canvas.drawText("${stats.longestStreak} Days", 450f, 300f, paint)

        paint.reset()
        paint.isAntiAlias = true
        paint.color = textColor
        paint.textSize = 24f
        canvas.drawText("Longest Streak", 450f, 340f, paint)

        paint.reset()
        paint.isAntiAlias = true
        paint.color = textColor
        paint.textSize = 28f
        canvas.drawText("Completion Rate: ${stats.completionPercentage}%", 60f, 440f, paint)

        paint.reset()
        paint.isAntiAlias = true
        paint.color = mutedColor
        paint.textSize = 20f
        canvas.drawText("Verified Locally • sudegoraankit/habitengine", 60f, 530f, paint)

        return saveBitmapToMediaStore(context, bitmap)
    }

    private fun saveBitmapToMediaStore(context: Context, bitmap: Bitmap): Boolean {
        val resolver = context.contentResolver
        val filename = "HabitEngine_${System.currentTimeMillis()}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/HabitEngine")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        return try {
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri).use { stream ->
                    if (stream != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                    }
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
