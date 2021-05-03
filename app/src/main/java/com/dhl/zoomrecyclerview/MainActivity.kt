package com.dhl.zoomrecyclerview

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dhl.zoomrecyclerview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvText.setOnClickListener {
            startActivity(Intent(this, ListActivity::class.java))
        }

        binding.tvText2.setOnClickListener {
            val dialog = BDialog(MainActivity@this)
            dialog.show()
        }
    }
}