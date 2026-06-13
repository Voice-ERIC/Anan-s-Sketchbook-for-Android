package com.example.sketchbook

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppConfig(
    val emotionLabel: String = "普通",
    val useOverlay: Boolean = true,
    val align: String = "center",
    val valign: String = "middle",
    val textBoxLeft: Int = 119,
    val textBoxTop: Int = 450,
    val textBoxRight: Int = 398,
    val textBoxBottom: Int = 625,
    val lineSpacing: Float = 0.15f,
) : Parcelable
