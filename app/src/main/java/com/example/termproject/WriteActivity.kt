package com.example.termproject

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.termproject.databinding.ActivityWriteBinding
import java.text.SimpleDateFormat
import android.Manifest
import android.location.Location
import com.google.firebase.database.FirebaseDatabase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.*

class WriteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWriteBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 위치 정보 제공자 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()

        // 완료 버튼 클릭 시 - 일기 저장
        binding.saveButton.setOnClickListener {
            saveDiary()
        }


        binding.draftButton.setOnClickListener {

        }

        // 삭제 버튼
        binding.deleteButton.setOnClickListener {
            finish() // 현재 화면 종료
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
    private fun saveDiary() {
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

        val database = FirebaseDatabase.getInstance()
        val diaryRef = database.getReference("diaries").child(diaryId)
        diaryRef.setValue(diaryData).addOnSuccessListener {
            Toast.makeText(this, "일기 저장 완료!", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "저장 실패: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}