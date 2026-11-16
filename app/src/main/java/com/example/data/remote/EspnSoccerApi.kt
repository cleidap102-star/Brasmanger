package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface EspnSoccerApi {

    @GET("apis/site/v2/sports/soccer/{league}/teams")
    suspend fun getLeagueTeams(
        @Path("league") league: String
    ): EspnTeamsResponse

    @GET("apis/site/v2/sports/soccer/{league}/teams/{teamId}/roster")
    suspend fun getTeamRoster(
        @Path("league") league: String,
        @Path("teamId") teamId: String
    ): EspnRosterResponse

    companion object {
        private const val BASE_URL = "https://site.api.espn.com/"

        fun create(): EspnSoccerApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(EspnSoccerApi::class.java)
        }
    }
}
