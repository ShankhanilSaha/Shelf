package com.example.shelf

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.example.shelf.core.common.dispatcher.ApplicationScope
import com.example.shelf.core.data.reminder.ReminderNotifier
import com.example.shelf.core.data.repository.AuthRepository
import com.example.shelf.core.data.repository.UserDataRepository
import com.example.shelf.core.data.sync.SyncScheduler
import com.example.shelf.core.model.AuthState
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class ShelfApplication :
    Application(),
    Configuration.Provider,
    SingletonImageLoader.Factory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /** The app's single OkHttp client (User-Agent, cache), created on first use. */
    @Inject
    lateinit var okHttpClient: Lazy<OkHttpClient>

    @Inject
    lateinit var reminderNotifier: ReminderNotifier

    @Inject
    lateinit var userDataRepository: UserDataRepository

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    /** WorkManager is initialised on demand with Hilt's factory so workers get injected. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        reminderNotifier.createChannel()
        applicationScope.launch { userDataRepository.refreshReminderSchedule() }
        // Keeps the periodic pull scheduled for a signed-in user (a no-op if it already is).
        applicationScope.launch {
            if (authRepository.authState.first() is AuthState.SignedIn) syncScheduler.schedulePeriodicSync()
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient.get() }))
        }
        .crossfade(true)
        .build()
}
