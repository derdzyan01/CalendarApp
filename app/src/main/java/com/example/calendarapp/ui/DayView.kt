package com.example.calendarapp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import com.example.calendarapp.model.Event
import java.time.LocalTime
import kotlin.math.max

class DayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val hourHeight = 120f
    private val timeLineWidth = 120f
    private val verticalPadding = 20f
    private val eventPadding = 4f
    private val eventRounding = 8f
    private val eventTextSize = 24f
    private val timeTextSize = 28f

    private val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = timeTextSize
        textAlign = Paint.Align.CENTER
    }
    private val eventPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val eventTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = eventTextSize
    }
    private val currentTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = 3f
    }

    private var events: List<Event> = emptyList()
    private val eventRects = mutableMapOf<Event, RectF>()

    interface OnEventClickListener {
        fun onEventClick(event: Event)
    }
    private var eventClickListener: OnEventClickListener? = null
    fun setOnEventClickListener(listener: OnEventClickListener?) {
        this.eventClickListener = listener
        Log.d("DayView", "OnEventClickListener set: ${listener != null}")
    }

    private val gestureDetector: GestureDetectorCompat

    init {
        Log.d("DayView", "Initializing DayView")
        gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(event: MotionEvent): Boolean {
                Log.d("DayView", "GestureDetector: onSingleTapUp detected at X=${event.x}, Y=${event.y}")
                handleTap(event.x, event.y)
                return true
            }
            override fun onDown(e: MotionEvent): Boolean {
                Log.d("DayView", "GestureDetector: onDown")
                return true
            }
        })
    }

    fun setEvents(newEvents: List<Event>) {
        Log.d("DayView", "setEvents called with ${newEvents.size} events")
        this.events = newEvents.sortedBy { it.startTime }
        calculateLayout()
        invalidate()
    }

    private fun calculateLayout() {
        eventRects.clear()
        if (width == 0 || height == 0) {
            Log.w("DayView", "calculateLayout skipped: View dimensions are zero.")
            return
        }
        Log.d("DayView", "calculateLayout running with width: $width")

        val minuteHeight = hourHeight / 60f

        events.forEach { event ->
            val startMinutes = event.startTime.toLocalTime().toSecondOfDay() / 60f
            val endMinutes = event.endTime.toLocalTime().toSecondOfDay() / 60f
            val startY = verticalPadding + startMinutes * minuteHeight
            val endY = verticalPadding + endMinutes * minuteHeight
            val effectiveEndY = max(startY + 1, endY)
            val left = timeLineWidth + eventPadding
            val right = width.toFloat() - eventPadding
            val rect = RectF(left, startY, right, effectiveEndY)
            eventRects[event] = rect
            Log.d("DayView", "Calculated rect for event ${event.id}: $rect")
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d("DayView", "onSizeChanged: New dimensions w=$w, h=$h")
        calculateLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val totalContentHeight = (hourHeight * 24) + (2 * verticalPadding)
        val measuredHeight = resolveSize(totalContentHeight.toInt(), heightMeasureSpec)
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
        Log.d("DayView", "onMeasure: Measured dimensions w=$measuredWidth, h=$measuredHeight")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGridBackground(canvas)
        drawEvents(canvas)
        drawCurrentTimeIndicator(canvas)
    }

    private fun drawGridBackground(canvas: Canvas) {
        canvas.drawLine(timeLineWidth, 0f, timeLineWidth, height.toFloat(), hourPaint)
        for (hour in 0..23) {
            val y = verticalPadding + hour * hourHeight
            canvas.drawLine(timeLineWidth, y, width.toFloat(), y, hourPaint)
            val timeText = String.format("%02d:00", hour)
            val textBounds = android.graphics.Rect()
            timePaint.getTextBounds(timeText, 0, timeText.length, textBounds)
            val textY = y + textBounds.height() / 2f + timePaint.descent()
            canvas.drawText(timeText, timeLineWidth / 2, textY, timePaint)
        }
        val bottomY = verticalPadding + 24 * hourHeight
        canvas.drawLine(timeLineWidth, bottomY, width.toFloat(), bottomY, hourPaint)
    }

    private fun drawEvents(canvas: Canvas) {
        eventRects.forEach { (event, rect) ->
            eventPaint.color = event.color
            canvas.drawRoundRect(rect, eventRounding, eventRounding, eventPaint)
            eventTextPaint.color = getContrastColor(event.color)
            val availableTextWidth = rect.width() - (2 * eventPadding)
            val availableTextHeight = rect.height() - (2 * eventPadding)

            if (availableTextHeight > eventTextSize) {
                val textToDraw = event.title
                val maxChars = eventTextPaint.breakText(textToDraw, true, availableTextWidth, null)
                var clippedText = textToDraw.substring(0, maxChars)
                if (maxChars < textToDraw.length) {
                    val ellipsis = "..."
                    val ellipsisWidth = eventTextPaint.measureText(ellipsis)
                    val fitChars = eventTextPaint.breakText(textToDraw, true, availableTextWidth - ellipsisWidth, null)
                    if (fitChars < maxChars && fitChars > 0) {
                        clippedText = textToDraw.substring(0, fitChars) + ellipsis
                    } else if (fitChars == 0 && availableTextWidth > ellipsisWidth) {
                        clippedText = ellipsis
                    } else if (fitChars == maxChars) {
                        clippedText += ellipsis
                    }
                }
                val textX = rect.left + eventPadding
                val textY = rect.top + eventPadding - eventTextPaint.fontMetrics.top
                canvas.drawText(clippedText, textX, textY, eventTextPaint)
            }
        }
    }

    private fun drawCurrentTimeIndicator(canvas: Canvas) {
        val minuteHeight = hourHeight / 60f
        val currentTime = LocalTime.now()
        val currentMinutes = currentTime.toSecondOfDay() / 60f
        val currentY = verticalPadding + currentMinutes * minuteHeight

        if (currentY >= verticalPadding && currentY <= (hourHeight * 24) + verticalPadding) {
            Log.d("DayView", "Drawing current time line at Y=$currentY")
            canvas.drawLine(timeLineWidth, currentY, width.toFloat(), currentY, currentTimePaint)
            canvas.drawCircle(timeLineWidth, currentY, 8f, currentTimePaint)
        } else {
            Log.d("DayView", "Current time line Y=$currentY is outside drawable bounds.")
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val consumed = gestureDetector.onTouchEvent(event)
        if (consumed) {
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun handleTap(touchX: Float, touchY: Float) {
        Log.d("DayView", "handleTap: Checking ${eventRects.size} rectangles for hit at X=$touchX, Y=$touchY.")
        for ((e, rect) in eventRects.entries.reversed()) {
            Log.d("DayView", "handleTap: Checking event ID ${e.id} with rect: $rect")
            if (rect.contains(touchX, touchY)) {
                Log.d("DayView", "handleTap: >>> HIT DETECTED on event ${e.id}! <<<")
                if (eventClickListener == null) {
                    Log.e("DayView", "handleTap: Listener is NULL!")
                }
                eventClickListener?.onEventClick(e)
                performClick()
                return
            }
        }
        Log.d("DayView", "handleTap: No hit detected on any event rect.")
    }

    private fun getContrastColor(color: Int): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
        return if (luminance > 0.5) Color.BLACK else Color.WHITE
    }
}