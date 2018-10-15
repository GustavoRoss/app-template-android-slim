package br.com.melgarejo.apptemplateslim.data.remote.client

import br.com.melgarejo.apptemplateslim.data.remote.client.ApiProviders.apiServices
import br.com.melgarejo.apptemplateslim.data.remote.exception.RequestException
import br.com.melgarejo.apptemplateslim.domain.utility.Constants.AVATAR_KEY
import br.com.melgarejo.apptemplateslim.domain.utility.Constants.IMAGE_MEDIA_TYPE
import br.com.melgarejo.apptemplateslim.domain.utility.Constants.PLATFORM_CONSTANT
import io.reactivex.Single
import io.reactivex.SingleTransformer
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException

object ApiClient {

    fun signIn(email: String, password: String, token: String) = makeRequest(apiServices.signIn(email, password, token, PLATFORM_CONSTANT))
    fun signInWithFacebook(accessToken: String) = makeRequest(apiServices.signInWithFacebook(accessToken))
    fun sendPasswordRecovery(email: String) = justVerifyErrors(apiServices.sendPasswordRecovery(email))
    fun signUp(fields: Map<String, String?>) = Single.just(fields)
            .map { buildSignUpMultipartBody(it) }
            .flatMap { makeRequest(apiServices.signUp(it)) }

    /**
     * Functions that composes Rx Streams in all requests
     */

    private fun <T> justVerifyErrors(request: Single<Response<T>>) = request.compose(verifyResponseException())
            .compose(verifyRequestException())
            .ignoreElement()

    private fun <T> verifyResponseException() = SingleTransformer<Response<T>, Response<T>> { upstream ->
        upstream.doOnSuccess { response ->
            if (!response.isSuccessful) {
                throw RequestException.httpError(response.code(), response.errorBody())
            }
        }
    }

    private fun <T> verifyRequestException() = SingleTransformer<Response<T>, Response<T>> { upstream ->
        upstream.onErrorResumeNext { t ->
            when (t) {
                is RequestException -> Single.error(t)
                is SocketTimeoutException -> Single.error(RequestException.timeoutError(t))
                is IOException -> Single.error(RequestException.networkError(t))
                else -> Single.error(RequestException.unexpectedError(t))
            }
        }
    }

    private fun <T> unwrap(): SingleTransformer<Response<T>, T> {
        return SingleTransformer { upstream ->
            upstream.map<T>(Response<T>::body)
        }
    }

    private fun <T> makeRequest(request: Single<Response<T>>) = request.compose(verifyResponseException())
            .compose(verifyRequestException())
            .compose(unwrap())

    private fun buildSignUpMultipartBody(fields: Map<String, String?>): MultipartBody {
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
        for ((key, value) in fields) {
            if (AVATAR_KEY == key) {
                if (value == null) continue
                val file = File(value)
                builder.addFormDataPart(
                        key,
                        file.name,
                        RequestBody.create(MediaType.parse(IMAGE_MEDIA_TYPE), file)
                )
            } else {
                value?.let { builder.addFormDataPart(key, it) }
            }
        }
        return builder.build()
    }
}
