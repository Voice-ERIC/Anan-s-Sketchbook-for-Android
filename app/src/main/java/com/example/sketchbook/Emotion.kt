package com.example.sketchbook

data class Emotion(val label: String, val fileName: String) {
    override fun toString(): String = label
}

object Emotions {
    val ALL = listOf(
        Emotion("普通", "base"),
        Emotion("开心", "开心"),
        Emotion("生气", "生气"),
        Emotion("无语", "无语"),
        Emotion("脸红", "脸红"),
        Emotion("病娇", "病娇"),
        Emotion("闭眼", "闭眼"),
        Emotion("难受", "难受"),
        Emotion("害怕", "害怕"),
        Emotion("激动", "激动"),
        Emotion("惊讶", "惊讶"),
        Emotion("哭泣", "哭泣"),
    )

    fun fromLabel(label: String): Emotion =
        ALL.firstOrNull { it.label == label } ?: ALL.first()
}
