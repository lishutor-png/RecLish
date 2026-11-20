package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.MainActivity
import com.example.audio.AudioCaptureSource
import com.example.audio.AudioPreferences
import com.example.audio.RecordingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class FloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: FrameLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var updateJob: Job? = null

    // UI elements inside overlay
    private lateinit var collapsedLayout: LinearLayout
    private lateinit var expandedLayout: LinearLayout
    private lateinit var tvTimer: TextView
    private lateinit var tvBadge: TextView
    private lateinit var tvExpandedTimer: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvHelperTip: TextView
    private lateinit var ivStatusDot: ImageView
    private lateinit var btnToggleRecord: Button
    private lateinit var btnStopSave: Button
    private lateinit var btnCancelRecord: Button
    private lateinit var btnOpenApp: Button
    private lateinit var btnMinimize: Button
    private lateinit var btnCloseOverlay: ImageView

    private var isExpanded = false
    private var isRecording = false
    private var isPaused = false

    companion object {
        const val ACTION_SHOW = "com.example.service.FLOATING_SHOW"
        const val ACTION_HIDE = "com.example.service.FLOATING_HIDE"
        var isOverlayRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE -> {
                stopSelf()
            }
            else -> {
                if (Settings.canDrawOverlays(this)) {
                    showOverlay()
                } else {
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlay() {
        if (overlayView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(16)
            y = dpToPx(140)
        }

        overlayView = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
        }

        buildOverlayViews()
        setupDragAndTouch()

        windowManager?.addView(overlayView, layoutParams)
        isOverlayRunning = true

        startStateObserver()
    }

    private fun buildOverlayViews() {
        val root = overlayView ?: return

        // 1. COLLAPSED VIEW (Sleek Compact Pill)
        collapsedLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), dpToPx(7), dpToPx(12), dpToPx(7))
            background = createRoundedBackground(Color.parseColor("#F00F172A"), Color.parseColor("#38BDF8"), dpToPx(24))
            elevation = dpToPx(10).toFloat()

            // Status dot
            ivStatusDot = ImageView(this@FloatingOverlayService).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(10), dpToPx(10)).apply {
                    marginEnd = dpToPx(8)
                }
                setImageDrawable(createCircleDrawable(Color.parseColor("#10B981")))
            }
            addView(ivStatusDot)

            // Timer display
            tvTimer = TextView(this@FloatingOverlayService).apply {
                text = "00:00"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = android.graphics.Typeface.MONOSPACE
            }
            addView(tvTimer)

            // Rec badge
            tvBadge = TextView(this@FloatingOverlayService).apply {
                text = " 🔊 Tab Siap"
                setTextColor(Color.parseColor("#38BDF8"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(dpToPx(4), 0, 0, 0)
            }
            addView(tvBadge)
        }

        // 2. EXPANDED VIEW (Full Floating Control Card)
        expandedLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(14))
            background = createRoundedBackground(Color.parseColor("#F80F172A"), Color.parseColor("#6366F1"), dpToPx(18))
            elevation = dpToPx(14).toFloat()
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(dpToPx(270), FrameLayout.LayoutParams.WRAP_CONTENT)

            // Header row with Title and Close (X) button
            val headerRow = LinearLayout(this@FloatingOverlayService).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dpToPx(6)
                }

                val titleCol = LinearLayout(this@FloatingOverlayService).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val titleText = TextView(this@FloatingOverlayService).apply {
                    text = "RecLish Audio Tab"
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                titleCol.addView(titleText)

                tvStatus = TextView(this@FloatingOverlayService).apply {
                    text = "Standby (Siaga)"
                    setTextColor(Color.parseColor("#38BDF8"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                }
                titleCol.addView(tvStatus)

                addView(titleCol)

                // Timer on top right of expanded view
                tvExpandedTimer = TextView(this@FloatingOverlayService).apply {
                    text = "00:00"
                    setTextColor(Color.parseColor("#38BDF8"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                    setPadding(0, 0, dpToPx(8), 0)
                }
                addView(tvExpandedTimer)

                btnCloseOverlay = ImageView(this@FloatingOverlayService).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(24), dpToPx(24))
                    setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    setColorFilter(Color.parseColor("#94A3B8"))
                    setOnClickListener {
                        stopSelf()
                    }
                }
                addView(btnCloseOverlay)
            }
            addView(headerRow)

            // Helper tip when Idle
            tvHelperTip = TextView(this@FloatingOverlayService).apply {
                text = "💡 Putar audio di Chrome, lalu ketuk 'Mulai Rekam' di bawah:"
                setTextColor(Color.parseColor("#94A3B8"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dpToPx(8)
                }
            }
            addView(tvHelperTip)

            // Action Buttons Row (Mulai / Jeda / Lanjut / Simpan / Batal)
            val buttonRow = LinearLayout(this@FloatingOverlayService).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dpToPx(8)
                }

                // 1. Toggle Record Button (Mulai / Jeda / Lanjut)
                btnToggleRecord = Button(this@FloatingOverlayService).apply {
                    text = "▶ Mulai Rekam"
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    background = createRoundedBackground(Color.parseColor("#10B981"), Color.TRANSPARENT, dpToPx(10))
                    layoutParams = LinearLayout.LayoutParams(0, dpToPx(38), 1f).apply {
                        marginEnd = dpToPx(4)
                    }
                    setOnClickListener {
                        handleToggleRecord()
                    }
                }
                addView(btnToggleRecord)

                // 2. Save Button
                btnStopSave = Button(this@FloatingOverlayService).apply {
                    text = "💾 Simpan"
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    background = createRoundedBackground(Color.parseColor("#2563EB"), Color.TRANSPARENT, dpToPx(10))
                    layoutParams = LinearLayout.LayoutParams(0, dpToPx(38), 1f).apply {
                        marginEnd = dpToPx(4)
                    }
                    visibility = View.GONE
                    setOnClickListener {
                        handleStopRecording()
                    }
                }
                addView(btnStopSave)

                // 3. Cancel Button (New!)
                btnCancelRecord = Button(this@FloatingOverlayService).apply {
                    text = "✕ Batal"
                    setTextColor(Color.parseColor("#FCA5A5"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    background = createRoundedBackground(Color.parseColor("#331E293B"), Color.parseColor("#EF4444"), dpToPx(10))
                    layoutParams = LinearLayout.LayoutParams(0, dpToPx(38), 0.9f)
                    visibility = View.GONE
                    setOnClickListener {
                        handleCancelRecording()
                    }
                }
                addView(btnCancelRecord)
            }
            addView(buttonRow)

            // Bottom row: Open RecLish App + Minimize
            val bottomRow = LinearLayout(this@FloatingOverlayService).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

                btnOpenApp = Button(this@FloatingOverlayService).apply {
                    text = "Buka RecLish"
                    setTextColor(Color.parseColor("#E2E8F0"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    background = createRoundedBackground(Color.parseColor("#334155"), Color.TRANSPARENT, dpToPx(8))
                    layoutParams = LinearLayout.LayoutParams(0, dpToPx(34), 1.2f).apply {
                        marginEnd = dpToPx(6)
                    }
                    setOnClickListener {
                        val appIntent = Intent(this@FloatingOverlayService, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(appIntent)
                    }
                }
                addView(btnOpenApp)

                btnMinimize = Button(this@FloatingOverlayService).apply {
                    text = "Kecilkan"
                    setTextColor(Color.parseColor("#94A3B8"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    background = createRoundedBackground(Color.parseColor("#1E293B"), Color.TRANSPARENT, dpToPx(8))
                    layoutParams = LinearLayout.LayoutParams(0, dpToPx(34), 1f)
                    setOnClickListener {
                        toggleExpanded(false)
                    }
                }
                addView(btnMinimize)
            }
            addView(bottomRow)
        }

        root.addView(collapsedLayout)
        root.addView(expandedLayout)
    }

    private fun handleToggleRecord() {
        val selectedSource = AudioPreferences.getSource(this)
        val selectedFormat = AudioPreferences.getFormat(this)

        if (selectedSource == AudioCaptureSource.BROWSER_INTERNAL && !AudioRecorderService.isMediaProjectionReady) {
            val appIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                action = MainActivity.ACTION_REQUEST_AUDIO_CAPTURE
            }
            startActivity(appIntent)
            Toast.makeText(this, "Izinkan akses perekaman audio sistem di RecLish!", Toast.LENGTH_SHORT).show()
            return
        }

        val service = AudioRecorderService.instance
        if (service == null) {
            // Start recording explicitly only when clicked
            val startIntent = Intent(this, AudioRecorderService::class.java).apply {
                action = AudioRecorderService.ACTION_START
                putExtra(AudioRecorderService.EXTRA_FORMAT, selectedFormat)
                putExtra(AudioRecorderService.EXTRA_SOURCE, selectedSource.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(startIntent)
            } else {
                startService(startIntent)
            }
            Toast.makeText(this, "Merekam suara dari browser...", Toast.LENGTH_SHORT).show()
        } else {
            val state = service.getRecorderState().value
            when (state.status) {
                RecordingStatus.RECORDING -> {
                    val pauseIntent = Intent(this, AudioRecorderService::class.java).apply {
                        action = AudioRecorderService.ACTION_PAUSE
                    }
                    startService(pauseIntent)
                }
                RecordingStatus.PAUSED -> {
                    val resumeIntent = Intent(this, AudioRecorderService::class.java).apply {
                        action = AudioRecorderService.ACTION_RESUME
                    }
                    startService(resumeIntent)
                }
                else -> {
                    val startIntent = Intent(this, AudioRecorderService::class.java).apply {
                        action = AudioRecorderService.ACTION_START
                        putExtra(AudioRecorderService.EXTRA_FORMAT, selectedFormat)
                        putExtra(AudioRecorderService.EXTRA_SOURCE, selectedSource.name)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(startIntent)
                    } else {
                        startService(startIntent)
                    }
                    Toast.makeText(this, "Merekam suara dari browser...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleStopRecording() {
        val stopIntent = Intent(this, AudioRecorderService::class.java).apply {
            action = AudioRecorderService.ACTION_STOP
        }
        startService(stopIntent)
        Toast.makeText(this, "Rekaman MP3 disimpan ke memori HP!", Toast.LENGTH_SHORT).show()
    }

    private fun handleCancelRecording() {
        val cancelIntent = Intent(this, AudioRecorderService::class.java).apply {
            action = AudioRecorderService.ACTION_CANCEL
        }
        startService(cancelIntent)
        Toast.makeText(this, "Rekaman dibatalkan (tidak disimpan)", Toast.LENGTH_SHORT).show()
    }

    private fun toggleExpanded(expand: Boolean) {
        isExpanded = expand
        if (expand) {
            collapsedLayout.visibility = View.GONE
            expandedLayout.visibility = View.VISIBLE
        } else {
            expandedLayout.visibility = View.GONE
            collapsedLayout.visibility = View.VISIBLE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragAndTouch() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        collapsedLayout.setOnTouchListener { _, event ->
            val lp = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = lp.x
                    initialY = lp.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                    }
                    if (isDragging) {
                        lp.x = initialX + dx
                        lp.y = initialY + dy
                        windowManager?.updateViewLayout(overlayView, lp)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Tap toggles to expanded control card
                        toggleExpanded(true)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun startStateObserver() {
        updateJob?.cancel()
        updateJob = scope.launch {
            while (isActive) {
                val service = AudioRecorderService.instance
                if (service != null) {
                    val state = service.getRecorderState().value
                    isRecording = state.status == RecordingStatus.RECORDING
                    isPaused = state.status == RecordingStatus.PAUSED

                    val totalSecs = state.durationMs / 1000
                    val mins = totalSecs / 60
                    val secs = totalSecs % 60
                    val timeStr = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

                    tvTimer.text = timeStr
                    tvExpandedTimer.text = timeStr

                    if (isRecording) {
                        ivStatusDot.setImageDrawable(createCircleDrawable(Color.parseColor("#EF4444")))
                        tvBadge.text = " 🔴 Rec"
                        tvBadge.setTextColor(Color.parseColor("#EF4444"))

                        // In recording state: show Jeda, Simpan, Batal
                        btnToggleRecord.text = "⏸ Jeda"
                        btnToggleRecord.background = createRoundedBackground(Color.parseColor("#F59E0B"), Color.TRANSPARENT, dpToPx(10))
                        btnStopSave.visibility = View.VISIBLE
                        btnCancelRecord.visibility = View.VISIBLE
                        tvHelperTip.visibility = View.GONE

                        tvStatus.text = "Sedang Merekam..."
                        tvStatus.setTextColor(Color.parseColor("#EF4444"))
                        tvExpandedTimer.setTextColor(Color.parseColor("#EF4444"))
                    } else if (isPaused) {
                        ivStatusDot.setImageDrawable(createCircleDrawable(Color.parseColor("#F59E0B")))
                        tvBadge.text = " ⏸ Jeda"
                        tvBadge.setTextColor(Color.parseColor("#F59E0B"))

                        // In paused state: show Lanjut, Simpan, Batal
                        btnToggleRecord.text = "▶ Lanjut"
                        btnToggleRecord.background = createRoundedBackground(Color.parseColor("#10B981"), Color.TRANSPARENT, dpToPx(10))
                        btnStopSave.visibility = View.VISIBLE
                        btnCancelRecord.visibility = View.VISIBLE
                        tvHelperTip.visibility = View.GONE

                        tvStatus.text = "Rekaman Dijeda"
                        tvStatus.setTextColor(Color.parseColor("#F59E0B"))
                        tvExpandedTimer.setTextColor(Color.parseColor("#F59E0B"))
                    } else {
                        // Standby state: only show Mulai Rekam
                        ivStatusDot.setImageDrawable(createCircleDrawable(Color.parseColor("#10B981")))
                        tvBadge.text = " 🔊 Tab Siap"
                        tvBadge.setTextColor(Color.parseColor("#38BDF8"))
                        tvTimer.text = "Siap"
                        tvExpandedTimer.text = "00:00"

                        btnToggleRecord.text = "▶ Mulai Rekam Audio Tab"
                        btnToggleRecord.background = createRoundedBackground(Color.parseColor("#10B981"), Color.TRANSPARENT, dpToPx(10))
                        btnStopSave.visibility = View.GONE
                        btnCancelRecord.visibility = View.GONE
                        tvHelperTip.visibility = View.VISIBLE

                        tvStatus.text = "Standby (Siaga)"
                        tvStatus.setTextColor(Color.parseColor("#38BDF8"))
                        tvExpandedTimer.setTextColor(Color.parseColor("#38BDF8"))
                    }
                } else {
                    // Service is null -> Standby mode
                    isRecording = false
                    isPaused = false
                    tvTimer.text = "Siap"
                    tvExpandedTimer.text = "00:00"
                    ivStatusDot.setImageDrawable(createCircleDrawable(Color.parseColor("#10B981")))
                    tvBadge.text = " 🔊 Tab Siap"
                    tvBadge.setTextColor(Color.parseColor("#38BDF8"))

                    btnToggleRecord.text = "▶ Mulai Rekam Audio Tab"
                    btnToggleRecord.background = createRoundedBackground(Color.parseColor("#10B981"), Color.TRANSPARENT, dpToPx(10))
                    btnStopSave.visibility = View.GONE
                    btnCancelRecord.visibility = View.GONE
                    tvHelperTip.visibility = View.VISIBLE

                    tvStatus.text = "Standby (Siaga)"
                    tvStatus.setTextColor(Color.parseColor("#38BDF8"))
                    tvExpandedTimer.setTextColor(Color.parseColor("#38BDF8"))
                }
                delay(200)
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun createRoundedBackground(fillColor: Int, strokeColor: Int, radiusPx: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fillColor)
            cornerRadius = radiusPx.toFloat()
            if (strokeColor != Color.TRANSPARENT) {
                setStroke(dpToPx(1), strokeColor)
            }
        }
    }

    private fun createCircleDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
        }
        isOverlayRunning = false
    }
}
