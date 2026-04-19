package ke.co.smartroundclinic.support.validation

import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult
import ke.co.smartroundclinic.support.presentation.dto.request.AssignTicketReq
import ke.co.smartroundclinic.support.presentation.dto.request.CreateIssueCategoryReq
import ke.co.smartroundclinic.support.presentation.dto.request.CreateTicketReq
import ke.co.smartroundclinic.support.presentation.dto.request.UpdateIssueCategoryReq

fun RequestValidationConfig.registerSupportValidators() {

    validate<AssignTicketReq> { req ->
        val errors = buildList {
            if (req.adminUserId.isNullOrBlank()) add("adminUserId is required")
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    validate<CreateIssueCategoryReq> { req ->
        val errors = buildList {
            if (req.name.isNullOrBlank()) add("name is required")
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    validate<UpdateIssueCategoryReq> { req ->
        if (req.name == null && req.description == null && req.status == null)
            return@validate ValidationResult.Invalid("At least one field must be provided for update")
        val errors = buildList {
            req.name?.let { if (it.isBlank()) add("name cannot be blank") }
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    validate<CreateTicketReq> { req ->
        val errors = buildList {
            if (req.issueCategoryId.isNullOrBlank()) add("issueCategoryId is required")
            if (req.title.isNullOrBlank())           add("title is required")
            if (req.description.isNullOrBlank())     add("description is required")
            if (req.complainantName.isNullOrBlank()) add("complainantName is required")
            if (req.complainantEmail.isNullOrBlank()) add("complainantEmail is required")
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
}
