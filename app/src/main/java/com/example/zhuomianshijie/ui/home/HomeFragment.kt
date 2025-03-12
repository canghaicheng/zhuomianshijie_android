package com.example.zhuomianshijie.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.zhuomianshijie.MainActivity
import com.example.zhuomianshijie.databinding.FragmentHomeBinding
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.zhuomianshijie.R

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())
    private var retryCount = 0
    private val maxRetries = 10

    // 添加权限请求启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Log.d("HomeFragment", "所有音频权限已授予")
            // 权限授予后重新加载WebView
            binding.webView.reload()
        } else {
            Log.d("HomeFragment", "部分音频权限被拒绝")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // 请求音频相关权限
        requestAudioPermissions()

        // 配置 WebView
        binding.webView.apply {
            // 启用硬件加速
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            settings.javaScriptEnabled = true  // 启用 JavaScript
            settings.domStorageEnabled = true  // 启用 DOM storage API
            settings.setSupportZoom(true)      // 支持缩放
            // 启用WebGL相关设置
            settings.apply {
                // 允许混合内容(HTTP和HTTPS)
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                
                // 允许通用访问
                allowContentAccess = true
                allowFileAccess = true
                
                // 设置媒体播放不需要用户手势触发
                mediaPlaybackRequiresUserGesture = false
                
                // 启用数据库
                databaseEnabled = true
                
                // 设置缓存模式
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            
            // 设置 WebViewClient 以在 WebView 内部处理导航
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    view.loadUrl(url)
                    return true
                }
            }
            
            // 添加WebChromeClient以支持WebGL和控制台日志以及麦克风权限
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    Log.d("WebView", "${consoleMessage.message()} -- From line ${consoleMessage.lineNumber()} of ${consoleMessage.sourceId()}")
                    return true
                }
                
                // 添加权限请求处理，允许WebView访问麦克风
                override fun onPermissionRequest(request: PermissionRequest) {
                    Log.d("WebView", "权限请求: ${request.resources.joinToString()}")
                    activity?.runOnUiThread {
                        request.grant(request.resources)
                    }
                }
            }
        }

        // 检查服务器状态并加载页面
        checkServerAndLoadPage(homeViewModel)

        return root
    }

    // 添加请求音频权限的方法
    private fun requestAudioPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
        
        // 检查是否已经有权限
        val allPermissionsGranted = permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
        
        // 如果没有所有权限，则请求
        if (!allPermissionsGranted) {
            requestPermissionLauncher.launch(permissions)
        } else {
            Log.d("HomeFragment", "已有所有音频权限")
        }
    }

    // 添加选项菜单处理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                // 刷新WebView
                binding.webView.reload()
                Log.d("HomeFragment", "刷新网页")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkServerAndLoadPage(homeViewModel: HomeViewModel) {
        // 如果MainActivity中的标志已经表明服务器在运行，直接加载
        if (MainActivity.isServerRunning) {
            Log.d("HomeFragment", "服务器已运行，直接加载页面")
            homeViewModel.url.observe(viewLifecycleOwner) { url ->
                binding.webView.loadUrl(url)
            }
            return
        }

        // 否则尝试连接服务器检查状态
        thread {
            try {
                val url = URL("http://127.0.0.1:3000/")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 1000
                connection.readTimeout = 1000
                connection.requestMethod = "HEAD"
                
                val responseCode = connection.responseCode
                connection.disconnect()
                
                handler.post {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        Log.d("HomeFragment", "服务器检查成功，加载页面")
                        homeViewModel.url.observe(viewLifecycleOwner) { url ->
                            binding.webView.loadUrl(url)
                        }
                    } else {
                        retryServerCheck(homeViewModel)
                    }
                }
            } catch (e: Exception) {
                Log.d("HomeFragment", "服务器检查异常: ${e.message}")
                handler.post {
                    retryServerCheck(homeViewModel)
                }
            }
        }
    }

    private fun retryServerCheck(homeViewModel: HomeViewModel) {
        if (retryCount < maxRetries) {
            retryCount++
            Log.d("HomeFragment", "重试检查服务器状态 ($retryCount/$maxRetries)")
            handler.postDelayed({
                checkServerAndLoadPage(homeViewModel)
            }, 500) // 每500毫秒重试一次
        } else {
            Log.d("HomeFragment", "达到最大重试次数，尝试直接加载")
            // 达到最大重试次数后，尝试直接加载
            homeViewModel.url.observe(viewLifecycleOwner) { url ->
                binding.webView.loadUrl(url)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}