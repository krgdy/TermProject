package com.example.termproject.model

//papago api를 위한 http requset용 데이터 클래스
data class papagoRequest (
    val source : String = "auto",
    val target : String = "en",
    val text : String = ""
)
//마지막으로 가장 안쪽의 result에 해당하는 데이터 클래스
data class papagoTranslationResult(
    val srcLangType: String,
    val tarLangType: String,
    val translatedText: String
)

// 2. "message" 객체에 해당하는 데이터 클래스
data class papagoMessage(
    val result: papagoTranslationResult
)

// 3. 최상위 응답 객체에 해당하는 데이터 클래스
data class papagoResponse(
    val message: papagoMessage
)

/*
응답 예시 형식
{
    "message": { // 최상위 객체 안에 "message" 키가 있고, 그 값은 또 다른 객체
        "result": { // "message" 객체 안에 "result" 키가 있고, 그 값은 또 다른 객체
            "srcLangType": "ko",
            "tarLangType": "en",
            "translatedText": "Hello, I like to eat apple while riding a bicycle."
        }
    }
}
 */

//에러용 응답 데이터 클래스
data class papagoError(
    val errorCode: String,   //	N2MT02같은 코드도 있어서 문자열로 처리
    val message: String
)

data class papagoErrorResponse(
    val error: papagoError
)

/*
에러 응답 예시 형식
{
 "error":{
    "errorCode":"210",
    "message":"Permission Denied"
  }
 }
 */