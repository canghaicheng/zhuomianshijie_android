package com.example.zhuomianshijie

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.zhuomianshijie.controllers.ApiSettingsController
import com.example.zhuomianshijie.controllers.SttSettingsController
import com.example.zhuomianshijie.controllers.TtsSettingsController
import com.example.zhuomianshijie.databinding.ActivityMainBinding
import com.example.zhuomianshijie.ui.home.HomeFragment
import com.google.android.material.navigation.NavigationView
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    // 添加ApiSettingsController变量声明
    private lateinit var apiSettingsController: ApiSettingsController
    private lateinit var ttsSettingsController: TtsSettingsController
    private lateinit var sttSettingsController: SttSettingsController

    companion object {
        // 服务器状态
        @JvmStatic
        var isServerRunning = false
        
        // 确保 Node.js 只启动一次
        @JvmStatic
        private var _startedNodeAlready = false

        init {
            System.loadLibrary("native-lib")
            System.loadLibrary("node")
        }
    }

    // 声明 native 方法
    private external fun startNodeWithArguments(arguments: Array<String>): Int
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        
        // 复制 Node.js 项目文件
        copyNodeJsProject()

        // 启动 Node.js
        if (!_startedNodeAlready) {
            _startedNodeAlready = true
            Thread {
                try {
                    Log.d("NodeJS", "开始启动 Node.js 服务器...")
                    
                    // 使用应用内私有目录
                    val nodeProjectDir = File(applicationContext.filesDir, "nodejs-project")
                    
                    // 确保目录和文件存在
                    val mainJs = File(nodeProjectDir, "main.js")
                    if (!mainJs.exists()) {
                        Log.e("NodeJS", "main.js 不存在: ${mainJs.absolutePath}")
                        return@Thread
                    }
                    
                    // 临时创建.node_repl_history文件防止权限错误
                    File(applicationContext.filesDir, ".node_repl_history").createNewFile()
                    
                    // 设置必要的环境变量
                    System.setProperty("NODE_PATH", nodeProjectDir.absolutePath)
                    System.setProperty("HOME", applicationContext.filesDir.absolutePath)
                    System.setProperty("TMPDIR", applicationContext.cacheDir.absolutePath)
                    
                    // 输出诊断信息
                    Log.d("NodeJS", "NODE_PATH: ${System.getProperty("NODE_PATH")}")
                    Log.d("NodeJS", "HOME: ${System.getProperty("HOME")}")
                    Log.d("NodeJS", "TMPDIR: ${System.getProperty("TMPDIR")}")
                    
                    // 检查main.js内容
                    try {
                        val content = mainJs.readText()
                        Log.d("NodeJS", "main.js 内容长度: ${content.length}字节")
                        Log.d("NodeJS", "main.js 前50个字符: ${content.take(50)}...")
                    } catch (e: Exception) {
                        Log.e("NodeJS", "读取main.js内容失败", e)
                    }
                    
                    // 切换到nodejs项目目录 - 通过cd命令行参数
                    val currentDir = "cd \"${nodeProjectDir.absolutePath}\" && "
                    Log.d("NodeJS", "工作目录命令: $currentDir")
                    
                     // 严格按照官方文档方式启动
                     Log.i("NodeJS", "准备启动Node.js，使用main.js...")
                     val result = startNodeWithArguments(arrayOf(
                         "node", 
                         mainJs.absolutePath  // 使用 main.js 的绝对路径
                     ))
                    Log.d("NodeJS", "Node.js 启动结果: $result")
                    
                    if (result == 0) {
                        isServerRunning = true
                        Log.i("NodeJS", "Node.js启动成功，设置状态标志")
                        
                    } else {
                        Log.e("NodeJS", "Node.js启动失败，返回码: $result")
                    }
                } catch (e: Exception) {
                    Log.e("NodeJS", "Node.js 服务器启动异常", e)
                }
            }.start()
            
            // 启动HTTP服务器
            Thread {
                try {
                    apiSettingsController = ApiSettingsController(this)
                    apiSettingsController.start()
                    Log.d("MainActivity", "API设置HTTP服务器已启动在端口8080")
                } catch (e: Exception) {
                    Log.e("MainActivity", "启动API设置HTTP服务器失败", e)
                }

                try {
                    ttsSettingsController = TtsSettingsController(this)
                    ttsSettingsController.start()
                    Log.d("MainActivity", "TTS设置HTTP服务器已启动在端口8081")
                } catch (e: Exception) {
                    Log.e("MainActivity", "启动TTS设置HTTP服务器失败", e)
                }

                try {
                    sttSettingsController = SttSettingsController(this)
                    sttSettingsController.start()
                    Log.d("MainActivity", "STT设置HTTP服务器已启动在端口8082")
                } catch (e: Exception) {
                    Log.e("MainActivity", "启动STT设置HTTP服务器失败", e)
                }

            }.start()
        }
       
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_api, R.id.nav_tts, R.id.nav_stt
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止HTTP服务器
        if (::apiSettingsController.isInitialized) {
            apiSettingsController.stop()
        }
        if (::ttsSettingsController.isInitialized) {
            ttsSettingsController.stop()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                // 将刷新事件传递给当前Fragment处理
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
                val currentFragment = navHostFragment?.childFragmentManager?.fragments?.get(0)
                if (currentFragment is HomeFragment) {
                    currentFragment.onOptionsItemSelected(item)
                    return true
                }
                return super.onOptionsItemSelected(item)
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    // 复制 Node.js 项目文件到应用私有目录
    private fun copyNodeJsProject() {
        try {
            // 添加调试日志
            Log.d("NodeJS", "assets目录内容:")
            assets.list("")?.forEach { 
                Log.d("NodeJS", "- $it")
            }
            
            // 检查nodejs-project目录是否存在
            Log.d("NodeJS", "检查nodejs-project目录:")
            assets.list("nodejs-project")?.forEach {
                Log.d("NodeJS", "- $it")
            }
            
            // 打印目标目录
            val nodeDir = File(applicationContext.filesDir, "nodejs-project")
            Log.d("NodeJS", "目标目录路径: ${nodeDir.absolutePath}")
            
            // 创建目标目录
            nodeDir.deleteRecursively() // 先清除旧文件
            nodeDir.mkdirs()
            
            // 递归复制所有文件
            copyAssetFolder("nodejs-project", nodeDir.absolutePath)
            
            Log.d("NodeJS", "Node.js 项目文件复制成功: ${nodeDir.absolutePath}")
            // 列出复制的文件
            nodeDir.listFiles()?.forEach { 
                Log.d("NodeJS", "复制的文件: ${it.name}")
            }
        } catch (e: Exception) {
            Log.e("NodeJS", "复制过程异常", e)
            // 打印更详细的错误信息
            Log.e("NodeJS", "assets 目录内容:")
            try {
                assets.list("")?.forEach {
                    Log.d("NodeJS", "- $it")
                }
            } catch (e: Exception) {
                Log.e("NodeJS", "无法列出 assets 目录", e)
            }
        }
    }

    // 递归复制 assets 目录中的文件和文件夹
    private fun copyAssetFolder(srcFolder: String, destPath: String) {
        try {
            val files = assets.list(srcFolder)
            if (files == null || files.isEmpty()) {
                // 如果是文件，直接复制
                copyAssetFile(srcFolder, destPath)
                return
            }

            // 创建目标文件夹
            File(destPath).mkdirs()

            // 递归复制子文件和文件夹
            files.forEach { filename ->
                val srcPath = if (srcFolder.isEmpty()) filename else "$srcFolder/$filename"
                val destFilePath = "$destPath/$filename"
                
                if (assets.list(srcPath)?.isNotEmpty() == true) {
                    // 是文件夹，递归复制
                    copyAssetFolder(srcPath, destFilePath)
                } else {
                    // 是文件，直接复制
                    copyAssetFile(srcPath, destFilePath)
                }
            }
        } catch (e: Exception) {
            Log.e("NodeJS", "复制文件夹失败: $srcFolder", e)
            throw e
        }
    }

    // 复制单个文件
    private fun copyAssetFile(srcFile: String, destPath: String) {
        try {
            assets.open(srcFile).use { input ->
                File(destPath).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("NodeJS", "复制文件成功: $srcFile -> $destPath")
        } catch (e: Exception) {
            Log.e("NodeJS", "复制文件失败: $srcFile", e)
            throw e
        }
    }


}