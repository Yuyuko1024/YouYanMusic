package com.youyuan.music.compose.di

import android.content.Context
import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.constants.AppConstants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideApiClient(
        @ApplicationContext context: Context
    ): ApiClient {
        return ApiClient.getInstance(
            context = context,
            baseUrl = AppConstants.APP_API_ENDPOINT,
            isDebug = true
        )
    }
}
