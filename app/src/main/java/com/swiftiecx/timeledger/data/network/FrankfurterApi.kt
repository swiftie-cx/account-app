package com.swiftiecx.timeledger.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// 定义 API 响应的数据结构
data class ExchangeRateResponse(
    val amount: Double,
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)

// 定义 API 接口
interface FrankfurterApiService {
    // 获取以 CNY 为基准的最新汇率
    // 例如：https://api.frankfurter.app/latest?from=CNY
    @GET("latest")
    suspend fun getLatestRates(@Query("from") baseCurrency: String = "CNY"): ExchangeRateResponse
}

// 单例对象，用于创建 Retrofit 实例
object RetrofitClient {
    private const val BASE_URL = "https://api.frankfurter.app/"

    val api: FrankfurterApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FrankfurterApiService::class.java)
    }
}