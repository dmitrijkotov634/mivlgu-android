package com.wavecat.mivlgu.workers

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wavecat.mivlgu.Constant
import com.wavecat.mivlgu.MainRepository
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.client.HttpClient
import com.wavecat.mivlgu.client.Parser
import kotlinx.coroutines.delay
import java.util.Calendar

class PreloadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val repository = MainRepository(appContext)

    private val parser = Parser()
    private val httpClient = HttpClient()

    private var notificationsGranted = true
    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsGranted = ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }

        val notificationManager = NotificationManagerCompat.from(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = applicationContext.getString(R.string.notification_channel)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            notificationManager.createNotificationChannel(channel)
        }

        val calendar = Calendar.getInstance()

        val groups = buildList {
            Constant.facultiesIds.forEach { id ->
                val facultyGroups = parser.pickGroups(
                    id,
                    Constant.getSemester(calendar),
                    Constant.getYear(calendar)
                )
                repository.cacheFacultyData(id, facultyGroups)
                addAll(facultyGroups)
            }
        }

        repository.cachedWeekNumber = parser.getWeekNumber()

        for ((index, group) in groups.withIndex()) {
            httpClient.scheduleGetJson(
                group,
                Constant.getSemester(calendar),
                Constant.getYear(calendar)
            ).let {
                repository.cacheTimetableData(group, it)
            }

            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle(applicationContext.getString(R.string.preload, group))
                .setProgress(groups.size, index, false)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_baseline_schedule_24)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT

            if (notificationsGranted)
                notificationManager.notify(id.hashCode(), notification)

            delay(100)
        }

        notificationManager.cancel(id.hashCode())

        return Result.success()
    }

    companion object {
        private const val CHANNEL_ID = "analysis"
    }
}