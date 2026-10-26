package com.example.vid2.presentation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vid2.data.repository.UserRepositoryImpl
import com.example.vid2.data.storage.SharedPrefUserStorage
import com.example.vid2.data.storage.UserStorage
import com.example.vid2.databinding.ActivityMainBinding
import com.example.vid2.domain.models.SaveUserNameParam
import com.example.vid2.domain.usecase.GetUserNameUseCase
import com.example.vid2.domain.usecase.SaveUserNameUseCase
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    val mainVM by viewModel<MainVM>()      // { MainVMFactory(this) }


    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =  ActivityMainBinding.inflate(layoutInflater)

        mainVM.resultLiveData.observe(this) {
            binding.dataTextView.text = it

        }

        setContentView(binding.root)
        binding.sendButton.setOnClickListener {

            mainVM.save(binding.dataEditText.text.toString())
        }
        binding.receiveButton.setOnClickListener {
             mainVM.load()
        }

    }
}