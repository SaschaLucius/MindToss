package lukulent.mindtoss.app.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.flow.first
import lukulent.mindtoss.app.data.HistoryRepository
import lukulent.mindtoss.app.data.SettingsRepository
import lukulent.mindtoss.app.data.model.SendStatus
import lukulent.mindtoss.app.network.MailFormat
import lukulent.mindtoss.app.network.ResendApi
import lukulent.mindtoss.app.network.ResendErrorMapper
import java.util.concurrent.TimeUnit

class SendMailWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val to = inputData.getString("to") ?: return Result.failure()
        val historyId = inputData.getString("history_id") ?: return Result.failure()

        val historyRepo = HistoryRepository(applicationContext)
        val entry = historyRepo.getEntry(historyId)
        if (entry == null) {
            return Result.failure()
        }

        val settingsRepo = SettingsRepository(applicationContext)
        val apiKey = settingsRepo.apiKey.first()
        val from = settingsRepo.effectiveSenderEmail.first()

        if (apiKey.isBlank()) {
            markFailed(historyRepo, historyId, "API-Key fehlt. Bitte in den Einstellungen prüfen.")
            return Result.failure()
        }

        val parts = MailFormat.subjectAndBody(entry.content)
        val result = ResendApi.sendEmail(
            apiKey = apiKey,
            from = from,
            to = to,
            subject = parts.subject,
            body = parts.body,
        )

        if (result.isSuccess) {
            val resendId = result.getOrNull()
            historyRepo.updateEntry(historyId) {
                it.copy(status = SendStatus.SUCCESS, resendId = resendId, errorMessage = null)
            }
            return Result.success()
        }

        val raw = result.exceptionOrNull()?.message
        val mapped = ResendErrorMapper.map(raw)
        val attemptsExhausted = runAttemptCount >= MAX_ATTEMPTS
        if (ResendErrorMapper.isPermanentFailure(raw) || attemptsExhausted) {
            markFailed(historyRepo, historyId, mapped.message)
            return Result.failure()
        }
        return Result.retry()
    }

    private suspend fun markFailed(
        historyRepo: HistoryRepository,
        historyId: String,
        message: String,
    ) {
        historyRepo.updateEntry(historyId) {
            it.copy(status = SendStatus.FAILED, errorMessage = message)
        }
    }

    companion object {
        private const val MAX_ATTEMPTS = 5

        fun enqueue(
            context: Context,
            to: String,
            historyId: String,
        ) {
            val data = workDataOf(
                "to" to to,
                "history_id" to historyId,
            )

            val request = OneTimeWorkRequestBuilder<SendMailWorker>()
                .setInputData(data)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30L,
                    TimeUnit.SECONDS,
                )
                .addTag("send_mail")
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
