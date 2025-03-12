import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

class ApiSettingsManager(private val context: Context) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val settingsFile = File(context.filesDir, "api_settings.json")
    private val currentSettingFile = File(context.filesDir, "current_api_setting.json")

    // 保存API设置列表
    fun saveApiSettings(settings: List<ApiSetting>): Boolean {
        return try {
            settingsFile.writeText(gson.toJson(settings))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 加载API设置列表
    fun loadApiSettings(): List<ApiSetting> {
        return try {
            if (!settingsFile.exists()) return emptyList()
            val type = object : TypeToken<List<ApiSetting>>() {}.type
            gson.fromJson(settingsFile.readText(), type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 保存当前选中的API设置
    fun saveCurrentSetting(setting: ApiSetting): Boolean {
        return try {
            currentSettingFile.writeText(gson.toJson(setting))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 加载当前选中的API设置
    fun loadCurrentSetting(): ApiSetting? {
        return try {
            if (!currentSettingFile.exists()) return null
            gson.fromJson(currentSettingFile.readText(), ApiSetting::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    // 添加一个方法来处理HTTP请求
    fun handleApiSettingsRequest(): String {
        val currentSetting = loadCurrentSetting()
        return if (currentSetting != null) {
            gson.toJson(currentSetting)
        } else {
            "{}"
        }
    }
}