package com.example.zhuomianshijie.ui.tts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import TtsSetting
import com.example.zhuomianshijie.utils.TtsSettingsManager

class TtsViewModel(application: Application) : AndroidViewModel(application) {

    private val ttsSettingsManager = TtsSettingsManager(application)
    
    private val _ttsSettings = MutableLiveData<List<TtsSetting>>()
    val ttsSettings: LiveData<List<TtsSetting>> = _ttsSettings
    
    private val _currentSetting = MutableLiveData<TtsSetting?>()
    val currentSetting: LiveData<TtsSetting?> = _currentSetting
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        val settings = ttsSettingsManager.loadTtsSettings()
        _ttsSettings.value = settings
        
        // 加载当前选中的设置
        val currentSetting = ttsSettingsManager.loadCurrentSetting()
        _currentSetting.value = currentSetting
    }
    
    fun setCurrentSetting(setting: TtsSetting) {
        _currentSetting.value = setting
        ttsSettingsManager.saveCurrentSetting(setting)
    }
    
    fun saveSetting(setting: TtsSetting) {
        val settings = _ttsSettings.value?.toMutableList() ?: mutableListOf()
        
        // 检查是否已存在同名设置，如果存在则更新
        val existingIndex = settings.indexOfFirst { it.name == setting.name }
        if (existingIndex >= 0) {
            settings[existingIndex] = setting
        } else {
            settings.add(setting)
        }
        
        // 保存设置列表
        ttsSettingsManager.saveTtsSettings(settings)
        
        // 更新LiveData
        _ttsSettings.value = settings
        
        // 设置为当前选中的设置
        setCurrentSetting(setting)
    }
    
    fun copySetting(setting: TtsSetting): TtsSetting? {
        val settings = _ttsSettings.value?.toMutableList() ?: mutableListOf()
        
        // 创建副本并生成新名称
        var newName = "${setting.name}_复制"
        
        val copiedSetting = setting.copy(name = newName)
        settings.add(copiedSetting)
        
        // 保存设置列表
        ttsSettingsManager.saveTtsSettings(settings)
        
        // 更新LiveData
        _ttsSettings.value = settings
        
        return copiedSetting
    }
    
    fun deleteSetting(setting: TtsSetting) {
        val settings = _ttsSettings.value?.toMutableList() ?: mutableListOf()
        
        // 移除设置
        settings.removeIf { it.name == setting.name }
        
        // 保存设置列表
        ttsSettingsManager.saveTtsSettings(settings)
        
        // 更新LiveData
        _ttsSettings.value = settings
        
        // 如果删除的是当前选中的设置，则清除当前选中
        if (_currentSetting.value?.name == setting.name) {
            _currentSetting.value = settings.firstOrNull()
            settings.firstOrNull()?.let {
                ttsSettingsManager.saveCurrentSetting(it)
            }
        }
    }
    
    // 添加更新设置的方法
    fun updateSetting(setting: TtsSetting) {
        val settings = _ttsSettings.value?.toMutableList() ?: mutableListOf()
        val index = settings.indexOfFirst { it.name == currentSetting.value?.name }
        
        if (index >= 0) {
            // 更新设置
            settings[index] = setting
            
            // 保存设置列表
            ttsSettingsManager.saveTtsSettings(settings)
            
            // 更新LiveData
            _ttsSettings.value = settings
            
            // 更新当前设置
            _currentSetting.value = setting
            ttsSettingsManager.saveCurrentSetting(setting)
        }
    }
}