package br.com.melgarejo.apptemplateslim.data.remote.deserializer

import br.com.melgarejo.apptemplateslim.data.remote.entity.ApiError
import com.google.gson.Gson
import okhttp3.ResponseBody

object ApiErrorsDeserializer {
    fun deserialize(responseBody: ResponseBody?): ApiError? {
        return Gson()
            .fromJson(responseBody?.string(), ApiError::class.java)
    }
}
