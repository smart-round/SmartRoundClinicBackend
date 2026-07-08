package ke.co.smartroundclinic.patient.validation

import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult
import ke.co.smartroundclinic.patient.presentation.dto.request.CreatePersonalInformationReq
import ke.co.smartroundclinic.patient.presentation.dto.request.SubmitPatientRatingReq
import ke.co.smartroundclinic.patient.presentation.dto.request.UpdatePatientRatingReq
import ke.co.smartroundclinic.patient.presentation.dto.request.UpdatePersonalInformationReq

fun RequestValidationConfig.registerPatientValidators() {

    validate<SubmitPatientRatingReq> { req ->
        val errors = buildList {
            if (req.appointmentId.isNullOrBlank()) add("appointmentId is required")
            if (req.patientId.isNullOrBlank()) add("patientId is required")
            if (req.rating !in 1..5) add("rating must be between 1 and 5")
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    validate<UpdatePatientRatingReq> { req ->
        if (req.rating == null && req.comment == null)
            return@validate ValidationResult.Invalid("At least one field must be provided for update")
        val errors = buildList {
            req.rating?.let { if (it !in 1..5) add("rating must be between 1 and 5") }
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    validate<CreatePersonalInformationReq> { req ->
        val errors = buildList {
            if (req.phoneNumber.isNullOrBlank())  add("phoneNumber is required")
            if (req.countryCode.isNullOrBlank())  add("countryCode is required")
            if (req.bloodGroup.isNullOrBlank())   add("bloodGroup is required")
            if (req.dateOfBirth.isNullOrBlank())  add("dateOfBirth is required")
            if (req.weight != null && req.weightIn == null) add("weightIn is required when weight is provided")
            if (req.height != null && req.heightIn == null) add("heightIn is required when height is provided")
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    validate<UpdatePersonalInformationReq> { req ->
        val allNull = req.phoneNumber == null && req.countryCode == null && req.bloodGroup == null &&
            req.dateOfBirth == null && req.weight == null && req.weightIn == null &&
            req.height == null && req.heightIn == null && req.maritalStatus == null
        if (allNull) return@validate ValidationResult.Invalid("At least one field must be provided for update")
        val errors = buildList {
            if (req.weight != null && req.weightIn == null) add("weightIn is required when weight is provided")
            if (req.height != null && req.heightIn == null) add("heightIn is required when height is provided")
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
}
