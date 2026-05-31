package ke.co.smartroundclinic.scheduling.presentation.validation

import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult
import ke.co.smartroundclinic.scheduling.presentation.dto.request.BookAppointmentReq
import ke.co.smartroundclinic.scheduling.presentation.dto.request.UpdateScheduleDayReq
import ke.co.smartroundclinic.scheduling.presentation.dto.request.UpsertScheduleReq

fun RequestValidationConfig.registerSchedulingValidators() {

    validate<BookAppointmentReq> { req ->
        val errors = buildList {
            if (req.doctorId.isNullOrBlank()) add("doctorId is required")
            if (req.date.isNullOrBlank()) add("date is required")
            else if (!isValidDate(req.date)) add("date must be a valid ISO date (YYYY-MM-DD)")
            if (req.slotStart.isNullOrBlank()) add("slotStart is required")
            else if (!isValidTime(req.slotStart)) add("slotStart must be in HH:mm format")
            if (req.transactionRef.isNullOrBlank()) add("transactionRef is required")
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    validate<UpsertScheduleReq> { req ->
        val errors = buildList {
            if (req.dayOfWeek < 0 || req.dayOfWeek > 6) add("dayOfWeek must be 0-6 (0=Monday, 6=Sunday)")
            if (req.windowStart.isNullOrBlank()) add("windowStart is required")
            else if (!isValidTime(req.windowStart)) add("windowStart must be in HH:mm format")
            if (req.windowEnd.isNullOrBlank()) add("windowEnd is required")
            else if (!isValidTime(req.windowEnd)) add("windowEnd must be in HH:mm format")
            if (!req.windowStart.isNullOrBlank() && !req.windowEnd.isNullOrBlank() &&
                isValidTime(req.windowStart) && isValidTime(req.windowEnd)
            ) {
                if (timeToMinutes(req.windowEnd) <= timeToMinutes(req.windowStart)) {
                    add("windowEnd must be after windowStart")
                }
            }
            if (req.slotDuration < 1 || req.slotDuration > 60) add("slotDuration must be between 1 and 60 minutes")
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    validate<UpdateScheduleDayReq> { req ->
        val errors = buildList {
            req.windowStart?.let { if (!isValidTime(it)) add("windowStart must be in HH:mm format") }
            req.windowEnd?.let { if (!isValidTime(it)) add("windowEnd must be in HH:mm format") }
            req.slotDuration?.let { if (it < 1 || it > 60) add("slotDuration must be between 1 and 60 minutes") }
            if (req.windowStart == null && req.windowEnd == null && req.slotDuration == null &&
                req.breakBlocks == null && req.isActive == null && req.timezone == null
            ) {
                add("at least one field must be provided")
            }
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
}

private fun isValidTime(time: String): Boolean =
    Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(time)

private fun isValidDate(date: String): Boolean =
    Regex("^\\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\\d|3[01])$").matches(date)

private fun timeToMinutes(time: String): Int {
    val (h, m) = time.split(":").map { it.toInt() }
    return h * 60 + m
}
