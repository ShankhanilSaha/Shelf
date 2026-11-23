package com.example.shelf.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.shelf.core.common.result.ShelfError
import com.example.shelf.core.common.result.ShelfException
import com.example.shelf.core.data.repository.AuthRepository
import com.example.shelf.core.model.AuthState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Runs [LibrarySynchronizer] for the signed-in account. Without a Firebase configuration or a
 * signed-in user it does nothing and succeeds. Network failures are retried with the exponential
 * backoff set by [SyncScheduler]; other failures wait for the next scheduled sync.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val gateway: SyncGateway,
    private val authRepository: AuthRepository,
    private val synchronizer: LibrarySynchronizer,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!gateway.isAvailable) return Result.success()
        val user = (authRepository.authState.first() as? AuthState.SignedIn)?.user ?: return Result.success()
        return try {
            synchronizer.sync(user.uid)
            Result.success()
        } catch (e: ShelfException) {
            if (e.error == ShelfError.Network && runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
    }
}
