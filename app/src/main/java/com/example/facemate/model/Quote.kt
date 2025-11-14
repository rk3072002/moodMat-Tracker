package com.example.facemate.model

data class Quote(
    val id: String = "",
    val text: String = "",
    val author: String = "",
    val likes: Int = 0,
    val dislikes: Int = 0,
    val isReadBy: Map<String, Boolean> = emptyMap()
)