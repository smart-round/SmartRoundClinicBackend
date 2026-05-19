package ke.co.smartroundclinic.consultation.presentation.validation

import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult
import ke.co.smartroundclinic.consultation.presentation.dto.request.StartConsultationReq

fun RequestValidationConfig.registerConsultationValidators() {
    validate<StartConsultationReq> { req ->
        if (req.appointmentId.isNullOrBlank()) ValidationResult.Invalid("appointmentId is required")
        else ValidationResult.Valid
    }
}
