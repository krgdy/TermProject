package com.example.termproject.model

// 일기 데이터 클래스
data class Diary (
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val date: String = "",
    val location: String = "",
    val emotion: String = ""
)