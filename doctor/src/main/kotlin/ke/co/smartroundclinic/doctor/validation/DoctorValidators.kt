package ke.co.smartroundclinic.doctor.validation

import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult
import ke.co.smartroundclinic.doctor.presentation.dto.request.AddPaymentDetailsReq
import ke.co.smartroundclinic.doctor.presentation.dto.request.SubmitRatingReq
import ke.co.smartroundclinic.doctor.presentation.dto.request.UpdatePaymentDetailsReq
import ke.co.smartroundclinic.doctor.presentation.dto.request.UpdateRatingReq

fun RequestValidationConfig.registerDoctorValidators() {

    validate<SubmitRatingReq> { req ->
        val errors = buildList {
            if (req.appointmentId.isNullOrBlank()) add("appointmentId is required")
            if (req.doctorId.isNullOrBlank()) add("doctorId is required")
            if (req.rating !in 1..5) add("rating must be between 1 and 5")
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    validate<UpdateRatingReq> { req ->
        if (req.rating == null && req.comment == null)
            return@validate ValidationResult.Invalid("At least one field must be provided for update")
        val errors = buildList {
            req.rating?.let { if (it !in 1..5) add("rating must be between 1 and 5") }
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    validate<AddPaymentDetailsReq> { req ->
        val errors = buildList {
            if (req.bankName.isBlank())      add("bankName is required")
            if (req.branchName.isBlank())    add("branchName is required")
            if (req.bankCode.isBlank())      add("bankCode is required")
            if (req.branchCode.isBlank())    add("branchCode is required")
            if (req.accountNumber.isBlank()) add("accountNumber is required")
            if (req.accountName.isBlank())   add("accountName is required")
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    validate<UpdatePaymentDetailsReq> { req ->
        val allNull = req.bankName == null && req.branchName == null &&
            req.bankCode == null && req.branchCode == null &&
            req.accountNumber == null && req.accountName == null
        if (allNull) return@validate ValidationResult.Invalid("At least one field must be provided for update")

        val errors = buildList {
            req.bankName?.let      { if (it.isBlank()) add("bankName cannot be blank") }
            req.branchName?.let    { if (it.isBlank()) add("branchName cannot be blank") }
            req.bankCode?.let      { if (it.isBlank()) add("bankCode cannot be blank") }
            req.branchCode?.let    { if (it.isBlank()) add("branchCode cannot be blank") }
            req.accountNumber?.let { if (it.isBlank()) add("accountNumber cannot be blank") }
            req.accountName?.let   { if (it.isBlank()) add("accountName cannot be blank") }
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
}