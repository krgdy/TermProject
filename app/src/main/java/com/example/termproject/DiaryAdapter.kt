package com.example.termproject.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.termproject.R
import com.example.termproject.databinding.ItemDiaryBinding
import com.example.termproject.model.Diary

class DiaryAdapter(
    private val diaryList: List<Diary>,
    private val onItemClick: (Diary) -> Unit
) : RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder>() {

    inner class DiaryViewHolder(val binding: ItemDiaryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(diary: Diary) {
            binding.tvTitle.text = diary.title
            binding.tvDate.text = diary.date

            // 감정 표시 (이모지 + 텍스트)
            binding.tvEmotion.text = when (diary.emotion) {
                "긍정" -> "😊 긍정"
                "부정" -> "☹️ 부정"
                else   -> diary.emotion
            }

            // 감정에 따른 색상 설정
            val colorRes = when (diary.emotion) {
                "긍정" -> R.color.emotion_positive   // res/values/colors.xml 에 #4CAF50 정의
                "부정" -> R.color.emotion_negative   // res/values/colors.xml 에 #F44336 정의
                else   -> android.R.color.darker_gray
            }
            val ctx = binding.root.context
            binding.tvEmotion.setTextColor(ContextCompat.getColor(ctx, colorRes))

            // 클릭 이벤트
            binding.root.setOnClickListener { onItemClick(diary) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        val binding = ItemDiaryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DiaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        holder.bind(diaryList[position])
    }

    override fun getItemCount(): Int = diaryList.size
}
