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
//좌표가 포함된 일기 데이터 클래스 (mapActivity)
data class DiaryLatLang (
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val date: String = "",
    val location: String = "",
    val emotion: String = "",
    val latitude: String = "null",      //toString을 하고 받으므로 잘못되면 "null"
    val longitude: String = "null"
)