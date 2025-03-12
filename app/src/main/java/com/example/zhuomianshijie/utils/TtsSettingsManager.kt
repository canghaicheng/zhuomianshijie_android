package com.example.zhuomianshijie.utils

import android.content.Context
import TtsSetting
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

class TtsSettingsManager(private val context: Context) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val settingsFile = File(context.filesDir, "tts_settings.json")
    private val currentSettingFile = File(context.filesDir, "current_tts_setting.json")

    // 保存TTS设置列表
    fun saveTtsSettings(settings: List<TtsSetting>): Boolean {
        return try {
            settingsFile.writeText(gson.toJson(settings))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 加载TTS设置列表
    fun loadTtsSettings(): List<TtsSetting> {
        return try {
            if (!settingsFile.exists()) return emptyList()
            val type = object : TypeToken<List<TtsSetting>>() {}.type
            gson.fromJson(settingsFile.readText(), type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 保存当前选中的TTS设置
    fun saveCurrentSetting(setting: TtsSetting): Boolean {
        return try {
            currentSettingFile.writeText(gson.toJson(setting))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 加载当前选中的TTS设置
    fun loadCurrentSetting(): TtsSetting? {
        return try {
            if (!currentSettingFile.exists()) return null
            gson.fromJson(currentSettingFile.readText(), TtsSetting::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // 添加一个方法来处理HTTP请求
    fun handleTtsSettingsRequest(): String {
        val currentSetting = loadCurrentSetting()
        return if (currentSetting != null) {
            gson.toJson(currentSetting)
        } else {
            "{}"
        }
    }
} 