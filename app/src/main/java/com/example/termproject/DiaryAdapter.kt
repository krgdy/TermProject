package com.example.termproject.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.termproject.databinding.ItemDiaryBinding
import com.example.termproject.model.Diary

// RecyclerView.Adapter: 일기 데이터를 리스트 형태로 보여주는 어댑터 클래스
class DiaryAdapter(
    private val diaryList: List<Diary>,                         // 보여줄 일기 데이터 리스트
    private val onItemClick: (Diary) -> Unit                    // 항목 클릭 시 실행할 동작 (람다)
) : RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder>() {

    // ViewHolder: item_diary.xml과 데이터를 연결해주는 내부 클래스
    inner class DiaryViewHolder(val binding: ItemDiaryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // 실제 데이터를 뷰에 바인딩하는 함수
        fun bind(diary: Diary) {
            binding.tvTitle.text = diary.title                  // 제목 표시
            binding.tvDate.text = diary.date                    // 날짜 표시
            binding.tvEmotion.text = "감정: ${diary.emotion}"   // 감정 표시

            // 항목 전체 클릭 시 처리할 이벤트 연결
            binding.root.setOnClickListener {
                onItemClick(diary)                              // 클릭 시 선택된 일기 데이터를 넘김
            }
        }
    }

    // ViewHolder를 생성할 때 호출됨 (item_diary.xml을 inflate)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        val binding = ItemDiaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DiaryViewHolder(binding)
    }

    // 리스트에서 각 항목에 데이터를 바인딩할 때 호출됨
    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        val diary = diaryList[position]
        holder.bind(diary)     // ViewHolder의 bind() 함수 호출
    }

    // 전체 리스트 항목 개수 반환
    override fun getItemCount(): Int = diaryList.size
}
