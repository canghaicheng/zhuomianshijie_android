package com.example.zhuomianshijie.utils

import android.content.Context
import SttSetting
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

class SttSettingsManager(private val context: Context) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val settingsFile = File(context.filesDir, "stt_settings.json")
    private val currentSettingFile = File(context.filesDir, "current_stt_setting.json")

    // 保存STT设置列表
    fun saveSttSettings(settings: List<SttSetting>): Boolean {
        return try {
            settingsFile.writeText(gson.toJson(settings))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 加载STT设置列表
    fun loadSttSettings(): List<SttSetting> {
        return try {
            if (!settingsFile.exists()) return emptyList()
            val type = object : TypeToken<List<SttSetting>>() {}.type
            gson.fromJson(settingsFile.readText(), type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 保存当前选中的STT设置
    fun saveCurrentSetting(setting: SttSetting): Boolean {
        return try {
            currentSettingFile.writeText(gson.toJson(setting))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 加载当前选中的STT设置
    fun loadCurrentSetting(): SttSetting? {
        return try {
            if (!currentSettingFile.exists()) return null
            gson.fromJson(currentSettingFile.readText(), SttSetting::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // 添加一个方法来处理HTTP请求
    fun handleSttSettingsRequest(): String {
        val currentSetting = loadCurrentSetting()
        return if (currentSetting != null) {
            gson.toJson(currentSetting)
        } else {
            "{}"
        }
    }
}