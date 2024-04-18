package com.cs496.mercurymessaging.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.cs496.mercurymessaging.R
import com.cs496.mercurymessaging.databinding.ActivityAddUserBinding

class AddUserActivity: AppCompatActivity() {
    lateinit var binding: ActivityAddUserBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddUserBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun onAddClick(v: View) {

    }
}