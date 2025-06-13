package com.example.termproject

import com.example.termproject.tflite.TextClassificationHelper
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.termproject.databinding.ActivityWriteBinding
import java.text.SimpleDateFormat
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.example.termproject.retrofit.NetworkService
import kotlinx.coroutines.launch
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.label.Category
import java.util.*
import com.example.termproject.retrofit.retrofitObject
import com.example.termproject.model.papagoRequest
import com.example.termproject.model.papagoResponse
import com.example.termproject.model.papagoErrorResponse
import okhttp3.ResponseBody
import retrofit2.Callback
import retrofit2.Call
import retrofit2.Converter
import retrofit2.Response

class WriteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWriteBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private lateinit var database: FirebaseFirestore
    private lateinit var classifierHelper: TextClassificationHelper
    private var onSaving : Int = 1 //중복 save를 막기 위한 semaphore같은 역할

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriteBinding.inflate(layoutInflater)
        database = FirebaseFirestore.getInstance()
        onSaving = 1
        setContentView(binding.root)

        val loadDraft = intent.getBooleanExtra("loadDraft",false)
        if(loadDraft){      //임시 저장된 일기가 있으니 불러 와야 함
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val diaryRef = database.collection("draft").document("Draft")
                    val draft = diaryRef.get().await()
                    val title : String? = draft.get("title") as? String
                    val content : String? = draft.get("content") as? String
                    val location : String? = draft.get("location") as? String
                    withContext(Dispatchers.Main){
                        if(title != null && title.isNotBlank()) binding.diaryTitle.setText(title)
                        if(content != null && content.isNotBlank()) binding.diaryContent.setText(content)
                        if(location != null && location.isNotBlank()) binding.editLocation.setText(location)
                    }
                }
                catch(e : Exception) {
                    Log.e("WriteActivity", "임시 저장본 로드 실패: ${e.message}", e)
                }
            }
        }

        val spf : SharedPreferences = getSharedPreferences("appPrefs", Context.MODE_PRIVATE)
        val editor = spf.edit()

        // 위치 정보 제공자 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()

        // 완료 버튼 클릭 시 - 일기 저장
        binding.saveButton.setOnClickListener {
            if(onSaving-->0) {
                translationRequest(binding.diaryContent.text.toString())
                Log.v("test","save button 이벤트")
            }
            else Toast.makeText(this, "일기 저장중, 잠시만 기다려 주세요!", Toast.LENGTH_SHORT).show()
        }

        // 임시 저장 버튼 클릭 시 - 일기 임시 저장 처음 들어올 때 이 임시 저장된 일기를 바로 불러옴
        binding.draftButton.setOnClickListener {
            editor.putBoolean("draftExist", true)    //임시 저장된 일기가 있는지 나타내는 플래그
            editor.apply()
            draftDiary()
        }

        // 삭제 버튼
        binding.deleteButton.setOnClickListener {
            editor.putBoolean("draftExist",false)
            editor.apply()
            finish()        // 현재 화면 종료
        }

        classifierHelper = TextClassificationHelper( //모델 추론용 클래스
            context = this@WriteActivity,
            listener = object : TextClassificationHelper.TextResultsListener {
                //추론 성공 시 콜백 함수
                override fun onResult(results: List<Category>, inferenceTime: Long) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        Log.v("test", "model classify result: " + results.toString())
                        val negativeScore = results[0].score
                        val emotionString: String =
                            when {
                                negativeScore < 0.2 -> "긍정"
                                negativeScore < 0.4 -> "다소 긍정"
                                negativeScore < 0.6 -> "중립"
                                negativeScore < 0.8 -> "다소 부정"
                                else -> "부정"
                            }
                        saveDiary(emotionString)
                    }
                }
                //실패시 콜백 함수
                override fun onError(error: String) {
                    Log.v("test", "error occurred!")
                    onSaving = 1 //저장 실패 onSaving 초기화
                }
            })
    }
    // 현재 위치 받아오기
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1000)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? -> currentLocation = location}
    }

    // 일기 데이터 저장 함수
    private fun saveDiary(emotion:String){
        val title = binding.diaryTitle.text.toString().trim()
        val content = binding.diaryContent.text.toString().trim()
        val locationText = binding.editLocation.text.toString().trim() // 입력된 위치 가져오기

        if (title.isEmpty() || content.isEmpty() || locationText.isEmpty()) {
            Toast.makeText(this, "제목, 내용, 장소를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val diaryId = UUID.randomUUID().toString()
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        val diaryData = mapOf(
            "id" to diaryId,
            "title" to title,
            "content" to content,
            "date" to date,
            "location" to locationText, // 사용자가 입력한 주소
            "emotion" to emotion,    //getEmotion에서 추론해서 갖고 온 감정
            "latitude" to currentLocation?.latitude.toString(),   //위도
            "longitude" to currentLocation?.longitude.toString() //경도
        )

        val diaryRef = database.collection("diaries").document(diaryId)
        diaryRef.set(diaryData).addOnSuccessListener {
            Toast.makeText(this, "일기 저장 완료!", Toast.LENGTH_SHORT).show()
            val spf : SharedPreferences = getSharedPreferences("appPrefs", Context.MODE_PRIVATE)
            val editor = spf.edit()
            editor.putBoolean("draftExist",false)   //일기가 저장 되었으므로 이제 임시 저장이 필요없음
            editor.apply()
            val intent = Intent(this, DetailActivity::class.java).apply {
                putExtra("title", diaryData["title"])
                putExtra("content", diaryData["content"])
                putExtra("date", diaryData["date"])
                putExtra("location", diaryData["location"])
                putExtra("emotion", diaryData["emotion"])
            }
            binding.diaryTitle.setText("")
            binding.diaryContent.setText("")
            binding.editLocation.setText("")
            startActivity(intent)
            finish()    //뒤로 돌아오는 버튼으로 돌아올 시 그냥 메인으로 보냄
        }.addOnFailureListener {
            Toast.makeText(this, "저장 실패: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    //일기 임시 저장 함수
    private fun draftDiary() {
        val title = binding.diaryTitle.text.toString().trim()
        val content = binding.diaryContent.text.toString().trim()
        val locationText = binding.editLocation.text.toString().trim() // 입력된 위치 가져오기

        if (title.isEmpty() || content.isEmpty() || locationText.isEmpty()) {
            Toast.makeText(this, "제목, 내용, 장소를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val diaryData = mapOf(
            "title" to title,
            "content" to content,
            "location" to locationText, // 사용자가 입력한 주소
        )

        val diaryRef = database.collection("draft").document("Draft")
        diaryRef.set(diaryData).addOnSuccessListener {
            Toast.makeText(this, "임시 저장 완료!", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "임시 저장 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            onSaving = 1 //저장 실패 onSaving 초기화
        }
    }

    private fun getEmotion(text: String) {
        classifierHelper.classify(text)
        Log.v("test","getEmotion 일어남!")
    }

    //papago api로 http request를 보내는 함수
    private fun translationRequest(textTotranslate: String) {
        val request = papagoRequest(text = textTotranslate)
        val networkService = retrofitObject.retrofit.create(NetworkService::class.java)
        //api id, key 그리고 body 순서의 매개변수
        val responseList = networkService.translate(BuildConfig.NAVER_API_ID,BuildConfig.NAVER_API_KEY,request)
        //리퀘스트 실패 시 응답을 에러 응답으로 변환하기 위한 converter
        val errorConverter: Converter<ResponseBody, papagoErrorResponse> =
            retrofitObject.retrofit.responseBodyConverter(papagoErrorResponse::class.java, arrayOfNulls(0))

        responseList.enqueue(object : Callback<papagoResponse> {
            override fun onResponse(call: Call<papagoResponse>,     //성공 콜백 함수
                                    response: Response<papagoResponse>){
                Log.v("test","파파고 번역 성공!")
                if(response.isSuccessful){
                    val responseBody = response.body()
                    val translatedResult = responseBody?.message?.result?.translatedText
                    if (translatedResult != null) {
                        getEmotion(translatedResult)
                        Log.v("test","성공 get emotion!")
                    } else {
                        Log.v("test","번역된 텍스트를 찾을 수 없습니다.")
                        onSaving = 1 //저장 실패 onSaving 초기화
                    }
                }
                else {
                    val errorResponse: papagoErrorResponse? = errorConverter.convert(response.errorBody())
                    if (errorResponse != null) { //converter 변환 성공
                        val apiErrorCode = errorResponse.error.errorCode // Papago 오류 코드
                        val apiErrorMessage = errorResponse.error.message // Papago 오류 메시지
                        if(apiErrorCode=="N2MT05"){     //error code가 N2MT05이면 이미 영어라는 뜻
                            getEmotion(textTotranslate)
                            Log.v("test","N2MT05 get emotion!")
                        }
                        else {
                            Log.e("test", "API 오류 코드: $apiErrorCode, 메시지: $apiErrorMessage")
                            onSaving = 1 //저장 실패 onSaving 초기화
                        }
                    }
                    else{ //converter 변환 실패
                        val rawErrorBody = response.errorBody()?.string()
                        Log.e("test", "Error body 파싱 실패 또는 내용 없음: $rawErrorBody")
                        onSaving = 1 //저장 실패 onSaving 초기화
                    }
                }
            }
            override fun onFailure(call: Call<papagoResponse>, t: Throwable){ //실패 콜백 함수
                Log.v("test","파파고 번역 실패!")
                call.cancel()
                onSaving = 1 //저장 실패 onSaving 초기화
            }
        })
    }

}
