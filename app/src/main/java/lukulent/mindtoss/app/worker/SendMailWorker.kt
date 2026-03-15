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
import lukulent.mindtoss.app.data.model.HistoryEntry
import lukulent.mindtoss.app.data.model.MessageType
import lukulent.mindtoss.app.network.ResendApi
import java.util.UUID
import java.util.concurrent.TimeUnit

class SendMailWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val to = inputData.getString("to") ?: return Result.failure()
        val subject = inputData.getString("subject") ?: return Result.failure()
        val body = inputData.getString("body") ?: return Result.failure()
        val messageType = inputData.getString("message_type") ?: "NOTE"
        val content = inputData.getString("content") ?: ""

        val settingsRepo = SettingsRepository(applicationContext)
        val apiKey = settingsRepo.apiKey.first()
        val from = settingsRepo.effectiveSenderEmail.first()

        if (apiKey.isBlank()) return Result.failure()

        val result = ResendApi.sendEmail(
            apiKey = apiKey,
            from = from,
            to = to,
            subject = subject,
            body = body,
        )

        return if (result.isSuccess) {
            val historyRepo = HistoryRepository(applicationContext)
            historyRepo.addEntry(
                HistoryEntry(
                    id = UUID.randomUUID().toString(),
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    type = MessageType.valueOf(messageType),
                )
            )
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        fun enqueue(
            context: Context,
            to: String,
            subject: String,
            body: String,
            content: String,
            messageType: MessageType,
        ) {
            val data = workDataOf(
                "to" to to,
                "subject" to subject,
                "body" to body,
                "content" to content,
                "message_type" to messageType.name,
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
