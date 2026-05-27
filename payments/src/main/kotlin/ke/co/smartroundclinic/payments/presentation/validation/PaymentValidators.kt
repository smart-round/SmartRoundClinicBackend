package ke.co.smartroundclinic.payments.presentation.validation

import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult
import ke.co.smartroundclinic.payments.presentation.dto.request.InitiatePaymentReq
import ke.co.smartroundclinic.payments.presentation.dto.request.UpdatePaymentStatusReq

fun RequestValidationConfig.registerPaymentValidators() {
    validate<InitiatePaymentReq> { req ->
        when {
            req.appointmentId.isNullOrBlank() -> ValidationResult.Invalid("appointmentId is required")
            req.doctorId.isNullOrBlank() -> ValidationResult.Invalid("doctorId is required")
            req.amount <= 0 -> ValidationResult.Invalid("amount must be greater than 0")
            else -> ValidationResult.Valid
        }
    }
    validate<UpdatePaymentStatusReq> { req ->
        when {
            req.status.isNullOrBlank() -> ValidationResult.Invalid("status is required")
            else -> ValidationResult.Valid
        }
    }
}
