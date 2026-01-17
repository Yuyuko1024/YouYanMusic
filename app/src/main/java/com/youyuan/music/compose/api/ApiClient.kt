package com.youyuan.music.compose.api

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.getValue
import androidx.core.net.toUri

class ApiClient private constructor(
    private val context: Context,
    private val baseUrl: String,
    private val isDebug: Boolean = false
) {
    companion object {
        private const val DEFAULT_BASE_URL = "http://192.168.10.160:3000"

        private const val DEFAULT_TIMEOUT = 30L // seconds
        private const val DEFAULT_RETRY_COUNT = 3

        @Volatile
        private var INSTANCE: ApiClient? = null

        /**
         * 获取ApiClient单例实例
         */
        fun getInstance(context: Context, baseUrl: String, isDebug: Boolean = false): ApiClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiClient(context.applicationContext, baseUrl, isDebug).also { INSTANCE = it }
            }
        }

        /**
         * 销毁实例（主要用于测试或需要重新初始化的场景）
         */
        fun destroyInstance() {
            INSTANCE = null
        }

    }

    private val cookieManager: CookieManager by lazy {
        CookieManager(context)
    }

    private val gson: Gson by lazy {
        GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .create()
    }

    private val okHttp: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .cookieJar(cookieManager)
            .addInterceptor(RiskControlInterceptor())
            // 添加请求头拦截器，模拟浏览器请求
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
        if (isDebug) {
            // 添加日志拦截器作为 Network Interceptor，这样可以看到最终发送的请求（包含 cookie）
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addNetworkInterceptor(loggingInterceptor)
        }
        builder.build()
    }

    private class RiskControlInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val response = chain.proceed(chain.request())

            // 风控常见表现：HTTP 200，但 body 里是 {"code":-462,...}
            // 这里用 peekBody 读取一小段，不消耗原 body。
            val peek = try {
                response.peekBody(64L * 1024L).string()
            } catch (_: Exception) {
                null
            }

            if (!peek.isNullOrBlank()) {
                val hit = Regex("\\\"code\\\"\\s*:\\s*-462").containsMatchIn(peek)
                if (hit) {
                    val blockText = Regex("\\\"blockText\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                        .find(peek)
                        ?.groupValues
                        ?.getOrNull(1)
                    val message = Regex("\\\"message\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                        .find(peek)
                        ?.groupValues
                        ?.getOrNull(1)

                    val msg = listOfNotNull(blockText, message).firstOrNull()
                        ?: "检测到您的网络环境存在风险，请稍后再试"
                    throw IOException("RISK_CONTROL_-462:$msg")
                }
            }

            return response
        }
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * 创建API服务接口实例
     */
    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }

    /**
     * 清除所有cookies
     */
    fun clearCookies() {
        cookieManager.clearAll()
    }

    /**
     * 清除特定域名的cookies
     */
    fun clearCookiesForHost(host: String) {
        cookieManager.clearForHost(host)
    }
    
    /**
     * 手动保存 cookie 字符串（用于从响应 body 中获取的 cookie）
     */
    fun saveCookieString(cookieString: String) {
        val host = baseUrl.toUri().host
        Log.d("ApiClient", "saveCookieString: baseUrl=$baseUrl, host=$host")
        if (host == null) {
            Log.e("ApiClient", "saveCookieString: host is null!")
            return
        }
        cookieManager.saveCookieString(host, cookieString)
    }
    
    /**
     * 检查是否有已保存的 cookie
     */
    fun hasCookies(): Boolean {
        val host = baseUrl.toUri().host
        Log.d("ApiClient", "hasCookies: baseUrl=$baseUrl, host=$host")
        if (host == null) return false
        val result = cookieManager.hasCookiesForHost(host)
        Log.d("ApiClient", "hasCookies: result=$result")
        return result
    }
}