package ke.co.smartroundclinic.consultation.presentation.validation

import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult
import ke.co.smartroundclinic.consultation.presentation.dto.request.CallActionReq

fun RequestValidationConfig.registerConsultationValidators() {
    validate<CallActionReq> { req ->
        if (req.callId.isNullOrBlank()) ValidationResult.Invalid("callId is required")
        else ValidationResult.Valid
    }
}
