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
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
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
import com.google.firebase.storage.FirebaseStorage
import okhttp3.ResponseBody
import retrofit2.Callback
import retrofit2.Call
import retrofit2.Converter
import retrofit2.Response
import java.io.File

class WriteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWriteBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private lateinit var database: FirebaseFirestore
    private lateinit var classifierHelper: TextClassificationHelper
    private var onSaving : Int = 1 //중복 save를 막기 위한 semaphore같은 역할
    private var photoUri: Uri? = null
    private var photoFile: File? = null
    private val storage = FirebaseStorage.getInstance()
    private var selectedAddress: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriteBinding.inflate(layoutInflater)
        database = FirebaseFirestore.getInstance()
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
                    withContext(Dispatchers.Main) {
                        if (title?.isNotBlank() == true) binding.diaryTitle.setText(title)
                        if (content?.isNotBlank() == true) binding.diaryContent.setText(content)
                        if (location?.isNotBlank() == true) {
                            selectedAddress = location
                            Toast.makeText(this@WriteActivity, "불러온 위치: $location", Toast.LENGTH_SHORT).show()
                        }
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
        binding.btnCamera.setOnClickListener {
            takePicture()
        }

        binding.btnGallery.setOnClickListener {
            pickImageFromGallery()
        }
    }

    // 사진 촬영 함수 - 카메라 앱
    private fun takePicture() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 3000)
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        photoFile = File.createTempFile("IMG_", ".jpg", cacheDir)
        photoUri = FileProvider.getUriForFile(this, "${packageName}.provider", photoFile!!)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        startActivityForResult(intent, 2001)
    }

    // 사진 선택 함수 - 갤러리 앱
    private fun pickImageFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), 3001)
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 3002)
            }
        }

        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 2002)
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
        val locationText = selectedAddress?.trim() ?: ""

        if (title.isEmpty() || content.isEmpty() || locationText.isEmpty()) {
            Toast.makeText(this, "제목, 내용, 위치를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val diaryId = UUID.randomUUID().toString()
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        val diaryData = mutableMapOf(
            "id" to diaryId,
            "title" to title,
            "content" to content,
            "date" to date,
            "location" to locationText, // 사용자가 입력한 주소
            "emotion" to emotion    //getEmotion에서 추론해서 갖고 온 감정
        )

        // 사진이 있을 경우 Storage 업로드 후 URL 저장
        if (photoUri != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val photoRef = storage.reference.child("images/$diaryId.jpg")
                    photoRef.putFile(photoUri!!).await()
                    val downloadUrl = photoRef.downloadUrl.await()
                    diaryData["imageUrl"] = downloadUrl.toString()

                    // Firestore 저장도 함께 이 coroutine 안에서 실행
                    database.collection("diaries").document(diaryId)
                        .set(diaryData)
                        .addOnSuccessListener {
                            runOnUiThread {
                                Toast.makeText(this@WriteActivity, "일기 저장 완료!", Toast.LENGTH_SHORT).show()
                                getSharedPreferences("appPrefs", Context.MODE_PRIVATE)
                                    .edit().putBoolean("draftExist", false).apply()
                                finish()
                            }
                        }
                        .addOnFailureListener {
                            runOnUiThread {
                                Toast.makeText(this@WriteActivity, "저장 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        }

                } catch (e: Exception) {
                    Log.e("WriteActivity", "사진 업로드 실패: ${e.message}")
                }
            }
        } else {
            // 사진이 없을 경우 Firestore만 저장
            database.collection("diaries").document(diaryId)
                .set(diaryData)
                .addOnSuccessListener {
                    Toast.makeText(this@WriteActivity, "일기 저장 완료!", Toast.LENGTH_SHORT).show()
                    getSharedPreferences("appPrefs", Context.MODE_PRIVATE)
                        .edit().putBoolean("draftExist", false).apply()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this@WriteActivity, "저장 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            selectedAddress = data.getStringExtra("selectedAddress")
            Toast.makeText(this, "선택된 위치: $selectedAddress", Toast.LENGTH_SHORT).show()
        }

        if (requestCode == 2001 && resultCode == RESULT_OK && photoUri != null) {
            binding.imagePreview.setImageURI(photoUri)
        }

        if (requestCode == 2002 && resultCode == RESULT_OK && data != null) {
            photoUri = data.data
            binding.imagePreview.setImageURI(photoUri)
        }
    }


    //일기 임시 저장 함수
    private fun draftDiary() {
        val title = binding.diaryTitle.text.toString().trim()
        val content = binding.diaryContent.text.toString().trim()
        val locationText = selectedAddress?.trim() ?: "" // 입력된 위치 가져오기

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        } else {
            when (requestCode) {
                3000 -> takePicture()
                3001, 3002 -> pickImageFromGallery()
            }
        }
    }

}
