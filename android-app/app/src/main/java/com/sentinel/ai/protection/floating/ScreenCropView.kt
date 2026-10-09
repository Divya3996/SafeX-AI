package com.sentinel.ai.protection.floating

import android.content.Context
import android.graphics.*
import android.view.*
import com.sentinel.ai.core.model.*
import kotlin.math.*

/** Crop and line selection share one fit/zoom transform, including preview letterboxing. */
class ScreenCropView(context: Context) : View(context) {
    var bitmap: Bitmap? = null
        set(value) { field = value; invalidate() }
    var selection = ScreenSelection.FULL
        set(value) { field = value; invalidate() }
    var lines = emptyList<ExtractedLine>()
    var selectedLines = emptySet<Int>()
    var onSelection: (ScreenSelection) -> Unit = {}
    var onLine: (Int) -> Unit = {}
    var reviewMode = false
    var panMode = false
    var zoomed = false
        set(value) { field = value; focusX = (selection.left + selection.right) / 2; focusY = (selection.top + selection.bottom) / 2; invalidate() }
    private var focusX = .5f
    private var focusY = .5f
    private val fitted = RectF()
    private val selectedRect = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var down = PointF()
    private var original = selection
    private var handle = -1
    private var panStart = PointF()
    private var panMoved = false
    private fun imageRect(): RectF {
        val image = bitmap ?: return RectF()
        val scale = min(width.toFloat() / image.width, height.toFloat() / image.height) * if (zoomed) 2 else 1
        val w = image.width * scale; val h = image.height * scale
        val left = if (zoomed) width / 2f - focusX * w else (width - w) / 2
        val top = if (zoomed) height / 2f - focusY * h else (height - h) / 2
        fitted.set(left, top, left + w, top + h)
        return fitted
    }
    private fun normalized(x: Float, y: Float): PointF {
        val r = imageRect()
        return PointF(((x - r.left) / max(1f, r.width())).coerceIn(0f, 1f), ((y - r.top) / max(1f, r.height())).coerceIn(0f, 1f))
    }
    private fun rectangle(s: ScreenSelection): RectF {
        val r = imageRect()
        selectedRect.set(r.left + s.left * r.width(), r.top + s.top * r.height(), r.left + s.right * r.width(), r.top + s.bottom * r.height())
        return selectedRect
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val image = bitmap?.takeUnless { it.isRecycled } ?: return
        val saved = canvas.save()
        canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawColor(Color.rgb(4, 17, 24))
        paint.style = Paint.Style.FILL; paint.color = Color.WHITE
        canvas.drawBitmap(image, null, imageRect(), paint)
        if (reviewMode) {
            selectedLines.forEach { index ->
                lines.getOrNull(index)?.region?.let { region ->
                    val r = imageRect()
                    paint.color = Color.argb(110, 39, 211, 182)
                    canvas.drawRect(r.left + region.left.toFloat() / image.width * r.width(), r.top + region.top.toFloat() / image.height * r.height(),
                        r.left + region.right.toFloat() / image.width * r.width(), r.top + region.bottom.toFloat() / image.height * r.height(), paint)
                }
            }
        } else {
            val box = rectangle(selection)
            paint.color = Color.argb(165, 0, 0, 0)
            canvas.drawRect(0f, 0f, width.toFloat(), max(0f, box.top), paint)
            canvas.drawRect(0f, min(height.toFloat(), box.bottom), width.toFloat(), height.toFloat(), paint)
            canvas.drawRect(0f, box.top, max(0f, box.left), box.bottom, paint)
            canvas.drawRect(min(width.toFloat(), box.right), box.top, width.toFloat(), box.bottom, paint)
            paint.color = Color.rgb(39, 211, 182); paint.style = Paint.Style.STROKE; paint.strokeWidth = resources.displayMetrics.density * 2
            canvas.drawRect(box, paint)
            paint.style = Paint.Style.FILL
            val radius = resources.displayMetrics.density * 8
            canvas.drawCircle(box.left, box.top, radius, paint); canvas.drawCircle(box.right, box.top, radius, paint)
            canvas.drawCircle(box.left, box.bottom, radius, paint); canvas.drawCircle(box.right, box.bottom, radius, paint)
        }
        canvas.restoreToCount(saved)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val image = bitmap ?: return false
        val point = normalized(event.x, event.y)
        if (panMode && zoomed) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { down = PointF(event.x, event.y); panStart = down; panMoved = false; parent?.requestDisallowInterceptTouchEvent(true) }
                MotionEvent.ACTION_MOVE -> {
                    if (hypot(event.x - panStart.x, event.y - panStart.y) > ViewConfiguration.get(context).scaledTouchSlop) panMoved = true
                    val r = imageRect(); val minX = (width / (2f * r.width())).coerceAtMost(.5f); val minY = (height / (2f * r.height())).coerceAtMost(.5f)
                    focusX = (focusX - (event.x - down.x) / r.width()).coerceIn(minX, 1 - minX)
                    focusY = (focusY - (event.y - down.y) / r.height()).coerceIn(minY, 1 - minY); down = PointF(event.x, event.y); invalidate()
                }
                MotionEvent.ACTION_UP -> { parent?.requestDisallowInterceptTouchEvent(false); if (reviewMode && !panMoved) selectAt(event.x, event.y) }
                MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
            }
            return true
        }
        if (reviewMode) {
            if (!imageRect().contains(event.x, event.y)) return false
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                selectAt(event.x, event.y)
            }
            return true
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                down = point; original = selection
                val box = rectangle(selection)
                val corners = listOf(PointF(box.left, box.top), PointF(box.right, box.top), PointF(box.left, box.bottom), PointF(box.right, box.bottom))
                handle = corners.indices.minBy { hypot(corners[it].x - event.x, corners[it].y - event.y) }
                if (hypot(corners[handle].x - event.x, corners[handle].y - event.y) > resources.displayMetrics.density * 32) handle = if (box.contains(event.x, event.y)) 4 else 5
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val minSize = .02f
                selection = when (handle) {
                    0 -> original.copy(left = point.x.coerceAtMost(original.right - minSize), top = point.y.coerceAtMost(original.bottom - minSize))
                    1 -> original.copy(right = point.x.coerceAtLeast(original.left + minSize), top = point.y.coerceAtMost(original.bottom - minSize))
                    2 -> original.copy(left = point.x.coerceAtMost(original.right - minSize), bottom = point.y.coerceAtLeast(original.top + minSize))
                    3 -> original.copy(right = point.x.coerceAtLeast(original.left + minSize), bottom = point.y.coerceAtLeast(original.top + minSize))
                    4 -> {
                        val dx = (point.x - down.x).coerceIn(-original.left, 1 - original.right)
                        val dy = (point.y - down.y).coerceIn(-original.top, 1 - original.bottom)
                        ScreenSelection(original.left + dx, original.top + dy, original.right + dx, original.bottom + dy)
                    }
                    else -> {
                        val left = min(down.x, point.x).coerceAtMost(1f - minSize)
                        val top = min(down.y, point.y).coerceAtMost(1f - minSize)
                        ScreenSelection(left, top, max(down.x, point.x).coerceIn(left + minSize, 1f), max(down.y, point.y).coerceIn(top + minSize, 1f))
                    }
                }
                onSelection(selection); return true
            }
            MotionEvent.ACTION_UP -> { parent?.requestDisallowInterceptTouchEvent(false); performClick(); return true }
            MotionEvent.ACTION_CANCEL -> { selection = original; onSelection(selection); parent?.requestDisallowInterceptTouchEvent(false); return true }
        }
        return true
    }
    private fun selectAt(x: Float, y: Float) {
        val image = bitmap ?: return
        if (!imageRect().contains(x, y)) return
        val p = normalized(x, y)
        val index = lines.indexOfFirst { it.region?.let { r -> p.x * image.width >= r.left && p.x * image.width <= r.right && p.y * image.height >= r.top && p.y * image.height <= r.bottom } == true }
        if (index >= 0) { onLine(index); performClick() }
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}
