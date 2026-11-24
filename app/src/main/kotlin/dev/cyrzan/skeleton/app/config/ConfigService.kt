package dev.cyrzan.skeleton.app.config

import retrofit2.http.GET

internal interface ConfigService {

    @GET("v1/config")
    suspend fun getConfig(): AppConfigDto
}
