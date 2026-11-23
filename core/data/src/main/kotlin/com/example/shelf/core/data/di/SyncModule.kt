package com.example.shelf.core.data.di

import android.content.Context
import com.example.shelf.core.data.auth.FirebaseAuthRepository
import com.example.shelf.core.data.auth.NoOpAuthRepository
import com.example.shelf.core.data.repository.AuthRepository
import com.example.shelf.core.data.sync.FirebaseSyncGateway
import com.example.shelf.core.data.sync.NoOpSyncGateway
import com.example.shelf.core.data.sync.SyncGateway
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/** Whether Firebase initialised, i.e. the build was made with an app/google-services.json. */
internal data class FirebaseStatus(val isConfigured: Boolean)

/**
 * Chooses the Firebase or the no-op implementations once, at injection time. The Firebase
 * objects are only created (through the providers) when Firebase is configured, because their
 * `getInstance()` throws otherwise. These are `@Provides` rather than `@Binds` because the choice
 * is made at run time.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object SyncModule {
    @Provides
    @Singleton
    fun provideFirebaseStatus(@ApplicationContext context: Context): FirebaseStatus =
        FirebaseStatus(isConfigured = FirebaseApp.getApps(context).isNotEmpty())

    @Provides
    @Singleton
    fun provideSyncGateway(status: FirebaseStatus, firebase: Provider<FirebaseSyncGateway>): SyncGateway =
        if (status.isConfigured) firebase.get() else NoOpSyncGateway

    @Provides
    @Singleton
    fun provideAuthRepository(
        status: FirebaseStatus,
        firebase: Provider<FirebaseAuthRepository>,
        noOp: Provider<NoOpAuthRepository>,
    ): AuthRepository = if (status.isConfigured) firebase.get() else noOp.get()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    /** Room is the single source of truth, so Firestore keeps no second copy on disk. */
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
            .build()
    }
}
