package com.wavecat.mivlgu.workers

import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.times

object Navigator {
    sealed interface Destination {
        data object Invalid : Destination
        data class Special(val name: String) : Destination
        data class Audience(val audienceNumber: String, val buildingNumber: Int) : Destination
    }

    fun parse(audienceString: String): Destination = runCatching {
        val parts = audienceString.trim().split(" ")

        return if (parts.size > 1 && parts[1].contains("/")) {
            val audienceParts = parts[1].split("/")
            Destination.Audience(audienceParts[0], audienceParts[1].toInt())
        } else {
            Destination.Special(audienceString)
        }
    }.getOrElse {
        println("Invalid audience: $audienceString")
        Destination.Invalid
    }

    sealed interface Result {
        enum class Failure : Result {
            INVALID_DESTINATION,
            UNKNOWN_BUILDING,
            UNKNOWN_DISTANCE,
        }

        data class Success(val duration: Duration) : Result
    }

    private val specialDestinationBuildings = mapOf("Зал" to 3)
    private val specialDestinationFloors = mapOf("Зал" to 1)

    private val buildingsDistance = mapOf(
        (2 to 10) to 16.minutes,
        (2 to 3) to 2.minutes,
        (2 to 8) to 16.minutes,
        (2 to 4) to 14.minutes,
        (2 to 5) to 19.minutes,
        (2 to 9) to 8.minutes,
        (2 to 2) to 0.minutes,

        (10 to 10) to 0.minutes,
        (10 to 3) to 18.minutes,
        (10 to 8) to 8.minutes,
        (10 to 4) to 20.minutes,
        (10 to 5) to 4.minutes,
        (10 to 9) to 7.minutes,
        (10 to 2) to 16.minutes,

        (3 to 10) to 18.minutes,
        (3 to 3) to 0.minutes,
        (3 to 8) to 18.minutes,
        (3 to 4) to 16.minutes,
        (3 to 5) to 21.minutes,
        (3 to 9) to 10.minutes,
        (3 to 2) to 2.minutes,

        (4 to 10) to 20.minutes,
        (4 to 3) to 16.minutes,
        (4 to 8) to 25.minutes,
        (4 to 4) to 0.minutes,
        (4 to 5) to 29.minutes,
        (4 to 9) to 20.minutes,
        (4 to 2) to 14.minutes,

        (5 to 10) to 4.minutes,
        (5 to 3) to 21.minutes,
        (5 to 8) to 5.minutes,
        (5 to 4) to 29.minutes,
        (5 to 5) to 0.minutes,
        (5 to 9) to 11.minutes,
        (5 to 2) to 19.minutes,

        (9 to 10) to 7.minutes,
        (9 to 3) to 10.minutes,
        (9 to 8) to 8.minutes,
        (9 to 4) to 20.minutes,
        (9 to 5) to 11.minutes,
        (9 to 9) to 0.minutes,
        (9 to 2) to 8.minutes,

        (8 to 10) to 8.minutes,
        (8 to 3) to 18.minutes,
        (8 to 8) to 0.minutes,
        (8 to 4) to 25.minutes,
        (8 to 5) to 5.minutes,
        (8 to 9) to 8.minutes,
        (8 to 2) to 16.minutes,
    )

    private val BETWEEN_FLOOR_WALKING = 30.seconds

    private fun Destination.getFloorNumber(): Int? = when (this) {
        is Destination.Special -> specialDestinationFloors[name]
        is Destination.Audience -> audienceNumber.substring(0, 1).toInt()
        Destination.Invalid -> null
    }

    fun Destination.getBuildingNumber(): Int? = when (this) {
        is Destination.Special -> specialDestinationBuildings[name]
        is Destination.Audience -> buildingNumber
        Destination.Invalid -> null
    }

    private fun compare(destinationFrom: Destination, destinationTo: Destination): Result {
        if (destinationTo is Destination.Invalid || destinationFrom is Destination.Invalid)
            return Result.Failure.INVALID_DESTINATION

        val buildingNumber1 =
            destinationFrom.getBuildingNumber() ?: return Result.Failure.UNKNOWN_BUILDING

        val buildingNumber2 =
            destinationTo.getBuildingNumber() ?: return Result.Failure.UNKNOWN_BUILDING

        var distance =
            buildingsDistance[buildingNumber1 to buildingNumber2]
                ?: return Result.Failure.UNKNOWN_DISTANCE

        val floorNumber1 =
            destinationFrom.getFloorNumber() ?: return Result.Failure.UNKNOWN_BUILDING

        val floorNumber2 =
            destinationTo.getFloorNumber() ?: return Result.Failure.UNKNOWN_BUILDING

        if (buildingNumber1 != buildingNumber2) {
            distance += floorNumber1 * BETWEEN_FLOOR_WALKING
            distance += floorNumber2 * BETWEEN_FLOOR_WALKING
        } else {
            distance += abs(floorNumber1 - floorNumber2) * BETWEEN_FLOOR_WALKING
        }

        return Result.Success(distance)
    }

    fun isCombinedBuilding(buildingNumber1: Int, buildingNumber2: Int): Boolean =
        (buildingNumber1 == 2 && buildingNumber2 == 3) ||
                (buildingNumber1 == 3 && buildingNumber2 == 2) ||
                (buildingNumber1 == buildingNumber2)

    fun compare(audienceFrom: String, audienceTo: String): Result =
        compare(parse(audienceTo), parse(audienceFrom))
}