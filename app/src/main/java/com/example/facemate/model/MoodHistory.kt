package com.example.facemate.model

import java.util.Date

data class MoodHistory(
    val mood: String = "",
    val note: String = "",
    val timestamp: Date? = null,
    val docId: String = ""  // required for deletion
)