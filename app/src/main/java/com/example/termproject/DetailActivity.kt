package com.example.termproject

// 일기 리스트에서 일기 클릭 시 상세보기 화면

import android.view.View
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
        binding.tvDetailDate.text     = date
        binding.tvDetailText.text     = content
        binding.tvDetailLocation.text = location

        // imageUrl이 비어있지 않으면 Glide로 로드
        if (imageUrl.isNotBlank()) {
            Glide.with(this)
                .load(imageUrl)
                .into(binding.imgDetailPhoto)
        } else {
            // URL이 없으면 ImageView 숨기기
            binding.imgDetailPhoto.visibility = View.GONE
        }

        val chip = binding.tvDetailEmotion  // 레이아웃 id 가 tvDetailEmotion 이지만 Chip 입니다
        chip.text = emotion
        when (emotion) {
            "긍정" -> chip.apply {
                chipIconTint = ColorStateList.valueOf(
                    ContextCompat.getColor(context, android.R.color.white)
                )
                chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.emotion_positive)
                )
                chipStrokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.emotion_positive)
                )
            }

            "부정" -> chip.apply {
                chipIconTint = ColorStateList.valueOf(
                    ContextCompat.getColor(context, android.R.color.white)
                )
                chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.emotion_negative)
                )
                chipStrokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.emotion_negative)
                )
            }

            else -> chip.apply {
                chipIcon = null
                chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, android.R.color.darker_gray)
                )
                chipStrokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, android.R.color.darker_gray)
                )
            }
        }
    }
}
