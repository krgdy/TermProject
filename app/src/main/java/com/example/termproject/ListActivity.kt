package com.example.termproject

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.termproject.adapter.DiaryAdapter
import com.example.termproject.databinding.ActivityListBinding
import com.example.termproject.model.Diary
import com.google.firebase.firestore.FirebaseFirestore

class ListActivity : AppCompatActivity() {

    // 뷰 바인딩 및 Firebase, 어댑터 관련 변수 선언
    private lateinit var binding: ActivityListBinding
    private lateinit var database: FirebaseFirestore
    private lateinit var adapter: DiaryAdapter
    private val diaryList = mutableListOf<Diary>()  // RecyclerView에 사용할 데이터 리스트

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firestore 인스턴스 초기화
        database = FirebaseFirestore.getInstance()

        // RecyclerView 세팅
        binding.recyclerDiaryList.layoutManager = LinearLayoutManager(this)

        // 어댑터 초기화 + 항목 클릭 시 상세 보기로 이동
        adapter = DiaryAdapter(diaryList) { selectedDiary ->
            val intent = Intent(this, DetailActivity::class.java).apply {
                putExtra("title", selectedDiary.title)
                putExtra("content", selectedDiary.content)
                putExtra("date", selectedDiary.date)
                putExtra("location", selectedDiary.location)
                putExtra("emotion", selectedDiary.emotion)
            }
            startActivity(intent)
        }

        // 어댑터 연결
        binding.recyclerDiaryList.adapter = adapter

        // Firebase에서 일기 목록 불러오기
        loadDiaryList()
    }

    // Firestore에서 diaries 컬렉션의 데이터를 가져와 RecyclerView에 반영하는 함수
    private fun loadDiaryList() {
        database.collection("diaries")
            .get()
            .addOnSuccessListener { result ->
                diaryList.clear()  // 기존 리스트 초기화
                for (document in result) {
                    val diary = document.toObject(Diary::class.java)
                    diaryList.add(diary)
                }
                adapter.notifyDataSetChanged()  // UI에 데이터 갱신
            }
            .addOnFailureListener {
                Toast.makeText(this, "일기 불러오기 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
