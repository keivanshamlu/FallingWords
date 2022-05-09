package com.shamlou.fallingwords.data.models

import com.google.gson.annotations.SerializedName

data class ResponseWord(
    @SerializedName("text_eng")
    val text_eng: String,
    @SerializedName("text_spa")
    val text_spa: String,
    val isCorrect: Boolean = true
)