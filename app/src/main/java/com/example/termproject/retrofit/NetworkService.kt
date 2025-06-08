package com.example.termproject.retrofit


import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header
import retrofit2.http.Headers
import com.example.termproject.model.papagoResponse
import com.example.termproject.model.papagoRequest
import retrofit2.Call

interface NetworkService {
    @Headers("Content-Type: application/json")
    @POST("nmt/v1/translation")
    fun translate(  //Id와 Key 값은 동적이어서 매개변수로 넣어줘야 함
        @Header("X-NCP-APIGW-API-KEY-ID") apiId : String,
        @Header("X-NCP-APIGW-API-KEY")  apiKey : String,
        @Body request: papagoRequest
    ): Call<papagoResponse>
}