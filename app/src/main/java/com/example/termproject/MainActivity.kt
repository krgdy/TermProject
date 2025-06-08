package com.example.termproject

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.termproject.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val spf : SharedPreferences = getSharedPreferences("appPrefs", Context.MODE_PRIVATE)
        val draftExist = spf.getBoolean("draftExist",false)

        binding.writeButton.setOnClickListener {
            val intent = Intent(this, WriteActivity::class.java)
            intent.putExtra("loadDraft",draftExist)
            startActivity(intent)
        }
        binding.listButton.setOnClickListener {
            val intent = Intent(this, ListActivity::class.java)
            startActivity(intent)
        }
        binding.mapButton.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }
    }
}