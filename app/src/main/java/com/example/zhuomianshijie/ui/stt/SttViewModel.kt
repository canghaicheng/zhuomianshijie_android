package com.example.zhuomianshijie.ui.stt

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import SttSetting
import com.example.zhuomianshijie.utils.SttSettingsManager

class SttViewModel(application: Application) : AndroidViewModel(application) {

    private val sttSettingsManager = SttSettingsManager(application)
    
    private val _sttSettings = MutableLiveData<List<SttSetting>>()
    val sttSettings: LiveData<List<SttSetting>> = _sttSettings
    
    private val _currentSetting = MutableLiveData<SttSetting?>()
    val currentSetting: LiveData<SttSetting?> = _currentSetting
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        val settings = sttSettingsManager.loadSttSettings()
        _sttSettings.value = settings
        
        // 加载当前选中的设置
        val currentSetting = sttSettingsManager.loadCurrentSetting()
        _currentSetting.value = currentSetting
    }
    
    fun setCurrentSetting(setting: SttSetting) {
        _currentSetting.value = setting
        sttSettingsManager.saveCurrentSetting(setting)
    }
    
    fun saveSetting(setting: SttSetting) {
        val settings = _sttSettings.value?.toMutableList() ?: mutableListOf()
        
        // 检查是否已存在同名设置，如果存在则更新
        val existingIndex = settings.indexOfFirst { it.name == setting.name }
        if (existingIndex >= 0) {
            settings[existingIndex] = setting
        } else {
            settings.add(setting)
        }
        
        // 保存设置列表
        sttSettingsManager.saveSttSettings(settings)
        
        // 更新LiveData
        _sttSettings.value = settings
        
        // 设置为当前选中的设置
        setCurrentSetting(setting)
    }
    
    fun copySetting(setting: SttSetting): SttSetting? {
        val settings = _sttSettings.value?.toMutableList() ?: mutableListOf()
        
        // 创建副本并生成新名称
        var newName = "${setting.name}_复制"
        
        val copiedSetting = setting.copy(name = newName)
        settings.add(copiedSetting)
        
        // 保存设置列表
        sttSettingsManager.saveSttSettings(settings)
        
        // 更新LiveData
        _sttSettings.value = settings
        
        return copiedSetting
    }
    
    fun deleteSetting(setting: SttSetting) {
        val settings = _sttSettings.value?.toMutableList() ?: mutableListOf()
        
        // 移除设置
        settings.removeIf { it.name == setting.name }
        
        // 保存设置列表
        sttSettingsManager.saveSttSettings(settings)
        
        // 更新LiveData
        _sttSettings.value = settings
        
        // 如果删除的是当前选中的设置，则清除当前选中
        if (_currentSetting.value?.name == setting.name) {
            _currentSetting.value = settings.firstOrNull()
            settings.firstOrNull()?.let {
                sttSettingsManager.saveCurrentSetting(it)
            }
        }
    }
    
    // 添加更新设置的方法
    fun updateSetting(setting: SttSetting) {
        val settings = _sttSettings.value?.toMutableList() ?: mutableListOf()
        val index = settings.indexOfFirst { it.name == currentSetting.value?.name }
        
        if (index >= 0) {
            // 更新设置
            settings[index] = setting
            
            // 保存设置列表
            sttSettingsManager.saveSttSettings(settings)
            
            // 更新LiveData
            _sttSettings.value = settings
            
            // 更新当前设置
            _currentSetting.value = setting
            sttSettingsManager.saveCurrentSetting(setting)
        }
    }
}