package com.jithesh.newsreader.data.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface FeedFetchService {
    @Streaming
    @GET
    suspend fun fetchFeed(@Url url: String): Response<ResponseBody>
}
