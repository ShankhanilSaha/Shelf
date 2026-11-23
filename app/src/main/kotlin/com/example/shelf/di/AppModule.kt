package com.example.shelf.di

import com.example.shelf.BuildConfig
import com.example.shelf.feature.auth.GoogleSignInConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    /** Read from google-services.json at build time; empty without a Firebase configuration. */
    @Provides
    fun provideGoogleSignInConfig(): GoogleSignInConfig = GoogleSignInConfig(BuildConfig.GOOGLE_WEB_CLIENT_ID)
}
