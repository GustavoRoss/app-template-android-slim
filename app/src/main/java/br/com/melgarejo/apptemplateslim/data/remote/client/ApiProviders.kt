package br.com.melgarejo.apptemplateslim.data.remote.client

import br.com.melgarejo.apptemplateslim.BuildConfig
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.DateFormat

object ApiProviders {
    val apiServices: ApiService by lazy { apiServiceSingleton ?: buildApiServices() }

    private val apiEndpoint by lazy { BuildConfig.API_ENDPOINT + BuildConfig.API_VERSION }
    private val retrofit by lazy { buildRetrofit() }
    private val authInterceptor by lazy { AuthInterceptor() }
    private var apiServiceSingleton: ApiService? = null

    private fun buildApiServices(): ApiService {
        with(retrofit.create(ApiService::class.java)) {
            apiServiceSingleton = this
            return this
        }
    }

    private fun buildRetrofit() = Retrofit.Builder()
            .client(okHttpClientBuilder().build())
            .baseUrl(apiEndpoint)
            .addConverterFactory(
                    GsonConverterFactory.create(
                            GsonBuilder()
                                    .serializeNulls()
                                    .setDateFormat(DateFormat.FULL)
                                    .create()
                    )
            )
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()

    private fun okHttpClientBuilder() = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().setLevel(resolveLevelInterceptor()))
            .addInterceptor(authInterceptor)

    private fun resolveLevelInterceptor() =
            if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
}