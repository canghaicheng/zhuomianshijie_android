package com.example.zhuomianshijie.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    private val _url = MutableLiveData<String>().apply {
        value = "http://127.0.0.1:3000/home"
    }
    val url: LiveData<String> = _url
}