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
import com.wavecat.mivlgu.client.Para
import com.wavecat.mivlgu.client.ScheduleGetResult
import com.wavecat.mivlgu.client.WeekType

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
            repository.getAllCachedGroups().forEach { group ->
                add(group to repository.loadTimetableCache(group))
            }
        }

        val currentWeek = repository.lastWeekNumber ?: 0

        for ((name, timetable) in data) {
            if (timetable == MainRepository.emptyScheduleGetResult)
                continue

            for (day in timetable.disciplines)
                for (classes in day.value)
                    for (week in classes.value)
                        for (para in week.value)
                            para.extraData = buildExtraData(
                                data = data,
                                currentWeek = currentWeek,
                                obtainGroup = name,
                                obtainIndex = classes.key.toInt() - 1,
                                obtainDay = day.key,
                                obtainPara = para
                            )

            repository.saveTimetableCache(name, timetable)
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
        obtainPara: Para
    ): ParaExtraData? {
        if (obtainIndex == 0)
            return null

        val obtainBuilding = obtainPara.aud.split("/").last()

        for ((group, timetable) in data) {
            if (group == obtainGroup) continue

            val day = timetable.disciplines[obtainDay] ?: continue
            val klass = day[obtainIndex.toString()] ?: continue

            for (week in klass.values) {
                for (para in week) {
                    if (checkPara(para, currentWeek)) {
                        val extraData = ParaExtraData()

                        if (para.aud == obtainPara.aud) {
                            extraData.prevName = para.name
                            extraData.prevGroupName = group
                        }

                        val prevBuilding = para.aud.split("/").last()

                        if (para.name == obtainPara.name && prevBuilding != obtainBuilding)
                            extraData.prevBuilding = para.aud

                        if (extraData.isEmpty())
                            continue

                        return extraData
                    }
                }
            }
        }

        return null
    }

    private fun checkRange(
        numberWeek: String,
        weekType: WeekType,
        currentWeek: Int
    ): Boolean {
        val parts = numberWeek
            .split("-")
            .map { it.toInt() }

        val weekStart = parts[0]
        val weekEnd = parts[1]

        val isEven = currentWeek % 2 == 0

        return (currentWeek in weekStart..weekEnd &&
                (weekType == WeekType.ALL ||
                        (isEven && weekType == WeekType.EVEN) ||
                        (!isEven && weekType == WeekType.ODD))
                )
    }

    private fun checkWeeks(
        underGroup: String,
        weekType: WeekType,
        currentWeek: Int
    ): Boolean {
        for (part in underGroup.split(",", "/")) {
            if (part.contains("-") &&
                checkRange(part, weekType, currentWeek)
            )
                return true
            else if (part.toIntOrNull() == currentWeek)
                return true
        }

        return false
    }

    private fun checkPara(para: Para, currentWeek: Int): Boolean =
        if (para.underGroup.isNullOrEmpty())
            checkWeeks(para.numberWeek, para.typeWeek, currentWeek)
        else
            ((!para.underGroup1.isNullOrEmpty() && checkWeeks(
                para.underGroup1,
                para.typeWeek,
                currentWeek
            )) || (!para.underGroup2.isNullOrEmpty() && checkWeeks(
                para.underGroup2,
                para.typeWeek,
                currentWeek
            )))

    companion object {
        private const val CHANNEL_ID = "analysis"
    }
}