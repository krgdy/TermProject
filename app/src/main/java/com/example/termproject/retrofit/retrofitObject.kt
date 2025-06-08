package com.example.termproject.retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object retrofitObject  { //singleton으로 만든 retrofit instance
    val baseURL = "https://papago.apigw.ntruss.com"

    val retrofit: Retrofit
        get() = Retrofit.Builder()
            .baseUrl(baseURL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}