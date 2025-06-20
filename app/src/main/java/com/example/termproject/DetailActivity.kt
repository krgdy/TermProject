package com.example.termproject

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.termproject.databinding.ActivityDetailBinding

class DetailActivity : AppCompatActivity() {

    // activity_detail.xml과 연결될 ViewBinding
    private lateinit var binding: ActivityDetailBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 바인딩 초기화
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent로부터 데이터 받기
        val title    = intent.getStringExtra("title")    ?: "(제목 없음)"
        val content  = intent.getStringExtra("content")  ?: "(내용 없음)"
        val date     = intent.getStringExtra("date")     ?: "(날짜 없음)"
        val location = intent.getStringExtra("location") ?: "(장소 없음)"
        val emotion  = intent.getStringExtra("emotion")  ?: "(감정 없음)"
        val imageUrl = intent.getStringExtra("imageUrl") ?: ""

        // 받은 데이터를 UI에 표시
        binding.tvDetailTitle.text    = title
        binding.tvDetailDate.text     = "날짜: $date"
        binding.tvDetailText.text     = content
        binding.tvDetailLocation.text = "위치: $location"
        binding.tvDetailEmotion.text  = "감정: $emotion"

        // imageUrl이 비어있지 않으면 Glide로 로드
        if (imageUrl.isNotBlank()) {
            Glide.with(this)
                .load(imageUrl)
                .into(binding.imgDetailPhoto)
        } else {
            // URL이 없으면 ImageView 숨기기
            binding.imgDetailPhoto.visibility = View.GONE
        }
    }
}
