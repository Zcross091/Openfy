package com.openfy.music.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Response
import java.util.concurrent.TimeUnit

class NewPipeDownloader(private val client: OkHttpClient) : Downloader() {

    override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): Response {
        val httpMethod = request.httpMethod()
        val dataToSend = request.dataToSend()

        val reqBuilder = Request.Builder()
            .url(request.url())
            .method(httpMethod, dataToSend?.toRequestBody(null))

        request.headers()?.forEach { (headerName, values) ->
            values.forEach { v ->
                reqBuilder.addHeader(headerName, v)
            }
        }

        val response = client.newCall(reqBuilder.build()).execute()
        val responseHeaders = response.headers.toMultimap()
        val responseBody = response.body?.string() ?: ""

        return Response(
            response.code,
            response.message,
            responseHeaders,
            responseBody,
            response.request.url.toString()
        )
    }

    companion object {
        private var initialized = false

        @Synchronized
        fun init() {
            if (initialized) return
            try {
                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()
                NewPipe.init(NewPipeDownloader(okHttpClient))
                initialized = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
