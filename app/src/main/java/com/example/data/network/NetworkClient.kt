package com.example.data.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    val openFoodFacts: OpenFoodFactsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        // Open Food Facts asks apps to identify themselves
                        val request = chain.request().newBuilder()
                            .header("User-Agent", "Wellplate/1.0 (Android)")
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            )
            .build()
            .create(OpenFoodFactsApi::class.java)
    }
}
