package com.example.sketchbook

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import java.io.File

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var inputView: View? = null
    private var config: AppConfig = AppConfig()
    private lateinit var renderer: SketchbookRenderer

    private var windowX = 0
    private var windowY = 200
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService<WindowManager>()!!
        renderer = SketchbookRenderer(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        @Suppress("DEPRECATION")
        val configExtra = intent?.getParcelableExtra<AppConfig>("config")
        config = configExtra ?: AppConfig()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("安安的素描本")
            .setContentText("悬浮窗运行中")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)

        if (!::overlayView.isInitialized) {
            createOverlay()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "悬浮窗服务", NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService<NotificationManager>()!!
            nm.createNotificationChannel(channel)
        }
    }

    private fun renderAndCopy(text: String) {
        if (text.isBlank()) {
            Toast.makeText(this, "请输入文字", Toast.LENGTH_SHORT).show()
            return
        }
        val emotion = Emotions.fromLabel(config.emotionLabel).fileName
        val bitmap = renderer.render(text, emotion, config)
        if (bitmap != null) {
            copyBitmapToClipboard(bitmap)
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "渲染失败，请检查资源文件", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showInputProxy(editText: EditText) {
        if (inputView != null) {
            (inputView as EditText).requestFocus()
            val imm = getSystemService<InputMethodManager>()!!
            @Suppress("DEPRECATION")
            imm.showSoftInput(inputView, InputMethodManager.SHOW_FORCED)
            return
        }

        val proxyEditText = EditText(this).apply {
            setBackgroundColor(0x00000000)
            setTextColor(0x00000000)
            setHintTextColor(0x00000000)
            textSize = 16f
            setSingleLine(false)
            maxLines = 4
            isCursorVisible = false
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val loc = IntArray(2)
        editText.getLocationOnScreen(loc)

        val params = WindowManager.LayoutParams(
            editText.width,
            editText.height,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = loc[0]
            y = loc[1]
        }

        proxyEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                editText.setText(s)
            }
        })

        proxyEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                removeInputProxy()
            }
        }

        windowManager.addView(proxyEditText, params)
        inputView = proxyEditText

        proxyEditText.post {
            proxyEditText.requestFocus()
            val imm = getSystemService<InputMethodManager>()!!
            @Suppress("DEPRECATION")
            imm.showSoftInput(proxyEditText, InputMethodManager.SHOW_FORCED)
        }
    }

    private fun removeInputProxy() {
        inputView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        inputView = null
    }

    private fun createOverlay() {
        val inflater = getSystemService<LayoutInflater>()!!
        overlayView = inflater.inflate(R.layout.floating_window, null)!!

        val editText = overlayView.findViewById<EditText>(R.id.editText)
        val btnGenerate = overlayView.findViewById<View>(R.id.btnGenerate)
        val btnClose = overlayView.findViewById<ImageButton>(R.id.btnClose)
        val dragHandle = overlayView.findViewById<LinearLayout>(R.id.dragHandle)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = windowX
            y = windowY
        }

        windowManager.addView(overlayView, params)

        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    removeInputProxy()
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    windowX = (initialX + (event.rawX - initialTouchX)).toInt()
                    windowY = (initialY + (event.rawY - initialTouchY)).toInt()
                    params.x = windowX
                    params.y = windowY
                    windowManager.updateViewLayout(overlayView, params)
                    true
                }
                else -> false
            }
        }

        editText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                showInputProxy(editText)
            }
            true
        }

        overlayView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                removeInputProxy()
                true
            } else {
                false
            }
        }

        btnClose.setOnClickListener {
            removeInputProxy()
            stopSelf()
        }

        btnGenerate.setOnClickListener {
            removeInputProxy()
            renderAndCopy(editText.text.toString())
        }

        editText.setOnEditorActionListener { _, _, _ ->
            renderAndCopy(editText.text.toString())
            true
        }
    }

    private fun copyBitmapToClipboard(bitmap: Bitmap) {
        val file = File(cacheDir, "sketchbook_output.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri: Uri = FileProvider.getUriForFile(
            this, "${packageName}.fileprovider", file
        )
        val clipboard = getSystemService<ClipboardManager>()!!
        val clipData = ClipData.newUri(contentResolver, "Sketchbook Image", uri)
        clipboard.setPrimaryClip(clipData)
    }

    override fun onDestroy() {
        removeInputProxy()
        if (::overlayView.isInitialized && overlayView.isAttachedToWindow) {
            try { windowManager.removeView(overlayView) } catch (_: Exception) {}
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "floating_window"
        private const val NOTIFICATION_ID = 1001
    }
}
