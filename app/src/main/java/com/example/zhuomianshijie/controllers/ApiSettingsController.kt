package com.example.zhuomianshijie.controllers

import android.content.Context
import ApiSettingsManager
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status

class ApiSettingsController(private val context: Context) : NanoHTTPD(8080) {
    private val apiSettingsManager = ApiSettingsManager(context)

    override fun serve(session: IHTTPSession): Response {
        // 添加CORS头部的通用方法
        fun addCorsHeaders(response: Response): Response {
            response.addHeader("Access-Control-Allow-Origin", "*")
            response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
            response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
            response.addHeader("Access-Control-Max-Age", "86400")
            return response
        }

        // 处理OPTIONS预检请求
        if (session.method == Method.OPTIONS) {
            val response = newFixedLengthResponse(Status.OK, MIME_PLAINTEXT, "")
            return addCorsHeaders(response)
        }

        return when (session.uri) {
            "/api/settings" -> {
                when (session.method) {
                    Method.GET -> {
                        try {
                            val jsonResponse = apiSettingsManager.handleApiSettingsRequest()
                            val response = newFixedLengthResponse(
                                Status.OK,
                                "application/json",
                                jsonResponse
                            )
                            return addCorsHeaders(response)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            val response = newFixedLengthResponse(
                                Status.INTERNAL_ERROR,
                                MIME_PLAINTEXT,
                                "获取API设置失败: ${e.message}"
                            )
                            return addCorsHeaders(response)
                        }
                    }
                    else -> {
                        val response = newFixedLengthResponse(
                            Status.METHOD_NOT_ALLOWED, 
                            MIME_PLAINTEXT, 
                            "不支持的方法"
                        )
                        return addCorsHeaders(response)
                    }
                }
            }
            else -> {
                val response = newFixedLengthResponse(
                    Status.NOT_FOUND, 
                    MIME_PLAINTEXT, 
                    "未找到"
                )
                return addCorsHeaders(response)
            }
        }
    }
}