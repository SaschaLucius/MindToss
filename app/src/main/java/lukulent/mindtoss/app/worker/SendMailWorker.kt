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

        if (apiKey.isBlank()) return Result.failure()

        val parts = MailFormat.subjectAndBody(entry.content)
        val result = ResendApi.sendEmail(
            apiKey = apiKey,
            from = from,
            to = to,
            subject = parts.subject,
            body = parts.body,
        )

        return if (result.isSuccess) {
            val resendId = result.getOrNull()
            historyRepo.updateEntry(historyId) {
                it.copy(status = SendStatus.SUCCESS, resendId = resendId)
            }
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
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
