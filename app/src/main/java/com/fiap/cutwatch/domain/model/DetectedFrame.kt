package com.fiap.cutwatch.domain.model

import android.graphics.Bitmap

data class DetectedFrame(
    val bitmap: Bitmap,
    val timeMs: Long,
    val detected: Boolean
)
