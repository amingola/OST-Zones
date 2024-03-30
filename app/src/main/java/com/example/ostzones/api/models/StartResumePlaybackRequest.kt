package com.example.ostzones.api.models

import com.google.gson.annotations.SerializedName

data class StartResumePlaybackRequest(
    @SerializedName("context_uri") val contextUri: String?,
    @SerializedName("position_ms") val positionMs: Long?,
    val offset: Offset?
)

data class Offset(
    val position: Long
)
