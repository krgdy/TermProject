package com.example.termproject

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.termproject.databinding.ActivityWriteBinding
import java.text.SimpleDateFormat
import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

class WriteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWriteBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private lateinit var database: FirebaseFirestore

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
            saveDiary()
        }

        // 임시 저장 버튼 클릭 시 - 일기 임시 저장 처음 들어올 때 이 임시 저장된 일기를 바로 불러옴
        binding.draftButton.setOnClickListener {
            editor.putBoolean("draftExist",true)    //임시 저장된 일기가 있는지 나타내는 플래그
            editor.apply()
            draftDiary()
        }

        // 삭제 버튼
        binding.deleteButton.setOnClickListener {
            editor.putBoolean("draftExist",false)
            editor.apply()
            finish()        // 현재 화면 종료
        }
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
    private fun saveDiary(){
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
            "emotion" to "긍정"
        )

        val diaryRef = database.collection("diaries").document(diaryId)
        diaryRef.set(diaryData).addOnSuccessListener {
            Toast.makeText(this, "일기 저장 완료!", Toast.LENGTH_SHORT).show()
            finish()
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
        }
    }

}
