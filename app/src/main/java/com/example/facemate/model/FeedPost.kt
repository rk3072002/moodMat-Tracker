package com.example.facemate.model

    data class FeedPost(
        val text: String = "",
        var likes: Int = 0,
        val timestamp: Long = 0L,
        val userId: String = "",
        val likedUsers: Map<String, Boolean>? = null // Optional for parsing
    )


