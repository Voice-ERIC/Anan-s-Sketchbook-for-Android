package com.example.sketchbook

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var spinnerEmotion: Spinner
    private lateinit var previewImage: ImageView
    private lateinit var checkOverlay: CheckBox
    private lateinit var radioAlign: RadioGroup
    private lateinit var radioValign: RadioGroup
    private lateinit var btnStart: Button

    private var serviceRunning = false

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { tryStartFloatingService() }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { tryStartFloatingService() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spinnerEmotion = findViewById(R.id.spinnerEmotion)
        previewImage = findViewById(R.id.previewImage)
        checkOverlay = findViewById(R.id.checkOverlay)
        radioAlign = findViewById(R.id.radioAlign)
        radioValign = findViewById(R.id.radioValign)
        btnStart = findViewById(R.id.btnStart)

        val adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, Emotions.ALL
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEmotion.adapter = adapter

        spinnerEmotion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                loadPreviewImage((parent?.getItemAtPosition(pos) as Emotion).fileName)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        loadPreviewImage(Emotions.ALL.first().fileName)

        btnStart.setOnClickListener {
            if (serviceRunning) {
                stopService(Intent(this, FloatingWindowService::class.java))
                serviceRunning = false
                btnStart.setText(R.string.start_overlay)
            } else {
                if (hasOverlayPermission()) {
                    tryStartFloatingService()
                } else {
                    requestOverlayPermission()
                }
            }
        }
    }

    private fun loadPreviewImage(emotionFile: String) {
        try {
            assets.open("BaseImages/$emotionFile.png").use { input ->
                val bitmap = BitmapFactory.decodeStream(input)
                previewImage.setImageBitmap(bitmap)
            }
        } catch (_: Exception) {
            previewImage.setImageBitmap(null)
        }
    }

    private fun tryStartFloatingService() {
        if (!hasOverlayPermission()) {
            Toast.makeText(this, "需要悬浮窗权限才能启动", Toast.LENGTH_LONG).show()
            return
        }
        if (needsNotificationPermission() && !hasNotificationPermission()) {
            requestNotificationPermission()
            return
        }
        startFloatingService()
    }

    private fun buildConfig(): AppConfig {
        val emotion = (spinnerEmotion.selectedItem as Emotion).label
        val useOverlay = checkOverlay.isChecked
        val align = when (radioAlign.checkedRadioButtonId) {
            R.id.radioLeft -> "left"
            R.id.radioRight -> "right"
            else -> "center"
        }
        val valign = when (radioValign.checkedRadioButtonId) {
            R.id.radioTop -> "top"
            R.id.radioBottom -> "bottom"
            else -> "middle"
        }
        return AppConfig(
            emotionLabel = emotion,
            useOverlay = useOverlay,
            align = align,
            valign = valign,
        )
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingWindowService::class.java).apply {
            putExtra("config", buildConfig())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        serviceRunning = true
        btnStart.setText(R.string.stop_overlay)
        Toast.makeText(this, "悬浮窗已启动", Toast.LENGTH_SHORT).show()
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true
    }

    private fun requestOverlayPermission() {
        AlertDialog.Builder(this)
            .setTitle("需要悬浮窗权限")
            .setMessage("为了显示悬浮窗，请在权限设置中允许「显示在其他应用上层」")
            .setPositiveButton("去设置") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    private fun hasNotificationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    private fun requestNotificationPermission() {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
