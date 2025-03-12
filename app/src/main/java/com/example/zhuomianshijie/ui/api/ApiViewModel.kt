package com.example.zhuomianshijie.ui.api

import ApiSetting
import ApiSettingsManager
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class ApiViewModel(application: Application) : AndroidViewModel(application) {
    private val apiSettingsManager = ApiSettingsManager(application)
    
    private val _apiSettings = MutableLiveData<List<ApiSetting>>()
    val apiSettings: LiveData<List<ApiSetting>> = _apiSettings
    
    private val _currentSetting = MutableLiveData<ApiSetting?>()
    val currentSetting: LiveData<ApiSetting?> = _currentSetting

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _apiSettings.value = apiSettingsManager.loadApiSettings()
        _currentSetting.value = apiSettingsManager.loadCurrentSetting()
    }

    fun saveSetting(setting: ApiSetting) {
        val settings = _apiSettings.value?.toMutableList() ?: mutableListOf()
        val existingIndex = settings.indexOfFirst { it.name == setting.name }
        
        if (existingIndex >= 0) {
            settings[existingIndex] = setting
        } else {
            settings.add(setting)
        }

        if (apiSettingsManager.saveApiSettings(settings)) {
            _apiSettings.value = settings
            _currentSetting.value = setting
            apiSettingsManager.saveCurrentSetting(setting)
        }
    }

    // 从返回Unit修改为返回ApiSetting?
    fun copySetting(setting: ApiSetting): ApiSetting? {
        val newName = "${setting.name}_复制"
        val newSetting = setting.copy(name = newName)
        
        // 保存新设置
        saveSetting(newSetting)
        
        // 返回新创建的设置
        return newSetting
    }

    fun deleteSetting(setting: ApiSetting) {
        val settings = _apiSettings.value?.toMutableList() ?: mutableListOf()
        if (settings.remove(setting)) {
            if (apiSettingsManager.saveApiSettings(settings)) {
                _apiSettings.value = settings
                if (_currentSetting.value == setting) {
                    _currentSetting.value = settings.firstOrNull()
                    _currentSetting.value?.let { apiSettingsManager.saveCurrentSetting(it) }
                }
            }
        }
    }

    fun setCurrentSetting(setting: ApiSetting) {
        if (apiSettingsManager.saveCurrentSetting(setting)) {
            _currentSetting.value = setting
        }
    }
    
    // 添加更新设置的方法
    fun updateSetting(setting: ApiSetting) {
        val settings = _apiSettings.value?.toMutableList() ?: mutableListOf()
        val index = settings.indexOfFirst { it.name == currentSetting.value?.name }
        
        if (index >= 0) {
            // 更新设置
            settings[index] = setting
            if (apiSettingsManager.saveApiSettings(settings)) {
                _apiSettings.value = settings
                _currentSetting.value = setting
                apiSettingsManager.saveCurrentSetting(setting)
            }
        }
    }
}