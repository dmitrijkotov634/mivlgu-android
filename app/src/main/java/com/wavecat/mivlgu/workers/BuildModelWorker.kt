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
import com.wavecat.mivlgu.MainRepository
import com.wavecat.mivlgu.ParaExtraData
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.client.ScheduleGetResult
import com.wavecat.mivlgu.client.models.Para
import com.wavecat.mivlgu.workers.Navigator.getBuildingNumber
import kotlin.time.DurationUnit

class BuildModelWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val repository = MainRepository(appContext)

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

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(applicationContext.getString(R.string.building))
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_baseline_schedule_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT

        if (notificationsGranted)
            notificationManager.notify(id.hashCode(), notification)

        val data = buildList {
            repository.retrieveAllCachedGroups().forEach { group ->
                add(group to repository.retrieveTimetableCache(group))
            }
        }

        val currentWeek = repository.cachedWeekNumber ?: 0

        for ((name, timetable) in data) {
            if (timetable == MainRepository.emptyScheduleGetResult)
                continue

            println("Build model for $name")

            for (day in timetable.disciplines)
                for (classes in day.value)
                    for (week in classes.value)
                        for (para in week.value) {
                            val extraData = ParaExtraData()

                            buildExtraData(
                                data = data,
                                currentWeek = currentWeek,
                                obtainGroup = name,
                                obtainIndex = classes.key.toInt() - 1,
                                obtainDay = day.key,
                                obtainPara = para,
                                extraData
                            )

                            if (!extraData.isEmpty())
                                para.extraData = extraData
                        }

            repository.cacheTimetableData(name, timetable)
        }

        repository.extraDataVersion = currentWeek

        notificationManager.cancel(id.hashCode())

        return Result.success()
    }

    private fun buildExtraData(
        data: List<Pair<String, ScheduleGetResult>>,
        currentWeek: Int,
        obtainGroup: String,
        obtainIndex: Int,
        obtainDay: String,
        obtainPara: Para,
        extraData: ParaExtraData
    ) {
        if (obtainIndex == 0)
            return

        val obtainBuilding = Navigator.parse(obtainPara.audience).getBuildingNumber() ?: -1

        for ((group, timetable) in data) {
            val day = timetable.disciplines[obtainDay] ?: continue
            val klass = day[obtainIndex.toString()] ?: continue

            klass.values.forEach { week ->
                for (para in week) {
                    if (para.isLessonToday(currentWeek)) {
                        if (group == obtainGroup) {
                            val navigatorResult =
                                Navigator.compare(obtainPara.audience, para.audience)

                            if (navigatorResult is Navigator.Result.Success) {
                                val minutes = navigatorResult.duration.toInt(DurationUnit.MINUTES)

                                if (minutes >= 2)
                                    extraData.routeTime = minutes
                            }

                            return@forEach
                        }

                        if (para.audience == obtainPara.audience) {
                            extraData.prevName = para.name
                            extraData.prevGroupName = group
                        }

                        val prevBuilding =
                            Navigator.parse(para.audience).getBuildingNumber() ?: continue

                        if (para.name == obtainPara.name &&
                            !Navigator.isCombinedBuilding(prevBuilding, obtainBuilding)
                        )
                            extraData.prevBuilding = para.audience
                    }
                }
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "analysis"
    }
}