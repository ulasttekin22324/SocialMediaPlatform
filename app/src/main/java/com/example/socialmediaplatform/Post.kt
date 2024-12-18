package com.example.socialmediaplatform

data class Post(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val content: String = "",
    val imageUrl: String = "",
    var likes: Int = 0,
    val comments: Map<String, Comment> = emptyMap()
)
