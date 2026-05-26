package ke.co.smartroundclinic.notification.presentation.validation

import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult
import ke.co.smartroundclinic.notification.presentation.dto.request.CreateNotificationReq
import ke.co.smartroundclinic.notification.presentation.dto.request.RegisterDeviceTokenReq
import ke.co.smartroundclinic.notification.presentation.dto.request.SendPushNotificationReq

fun RequestValidationConfig.registerNotificationValidators() {

    validate<SendPushNotificationReq> { req ->
        val errors = buildList {
            if (req.title.isNullOrBlank()) add("title is required")
            if (req.body.isNullOrBlank())  add("body is required")
            if (req.recipientId == null && req.destination == null)
                add("either recipientId or destination must be provided")
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    validate<CreateNotificationReq> { req ->
        val errors = buildList {
            if (req.title.isNullOrBlank())   add("title is required")
            if (req.message.isNullOrBlank()) add("message is required")
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    validate<RegisterDeviceTokenReq> { req ->
        if (req.deviceToken.isNullOrBlank())
            ValidationResult.Invalid("deviceToken is required")
        else
            ValidationResult.Valid
    }
}
