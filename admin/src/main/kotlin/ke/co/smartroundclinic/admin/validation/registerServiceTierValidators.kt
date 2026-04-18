package ke.co.smartroundclinic.admin.validation

import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateServiceTierReq
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateSpecialityReq
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateSubSpecialityReq
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdateServiceTierReq
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdateSpecialityReq
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdateSubSpecialityReq

fun RequestValidationConfig.registerAdminValidators() {

    validate<CreateServiceTierReq> { req ->
        val errors = buildList {
            if (req.name.isNullOrBlank())          add("name is required")
            if (req.tierPrice <= 0)                add("tierPrice must be greater than 0")
            if (req.consultationDuration <= 0)     add("consultationDuration must be greater than 0")
            if (req.gracePeriod < 0)               add("gracePeriod cannot be negative")
            if (req.chatAccessWindow <= 0)         add("chatAccessWindow must be greater than 0")
            if (req.followUpWindow < 0)            add("followUpWindow cannot be negative")
            if (req.followUpFee < 0)               add("followUpFee cannot be negative")
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    validate<UpdateServiceTierReq> { req ->
        val allNull = req.name == null && req.tierPrice == null && req.consultationDuration == null &&
            req.gracePeriod == null && req.chatAccessWindow == null &&
            req.followUpWindow == null && req.followUpFee == null
        if (allNull) return@validate ValidationResult.Invalid("At least one field must be provided for update")

        val errors = buildList {
            req.name?.let              { if (it.isBlank()) add("name cannot be blank") }
            req.tierPrice?.let         { if (it <= 0) add("tierPrice must be greater than 0") }
            req.consultationDuration?.let { if (it <= 0) add("consultationDuration must be greater than 0") }
            req.gracePeriod?.let       { if (it < 0) add("gracePeriod cannot be negative") }
            req.chatAccessWindow?.let  { if (it <= 0) add("chatAccessWindow must be greater than 0") }
            req.followUpWindow?.let    { if (it < 0) add("followUpWindow cannot be negative") }
            req.followUpFee?.let       { if (it < 0) add("followUpFee cannot be negative") }
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    validate<CreateSpecialityReq> { req ->
        val errors = buildList {
            if (req.serviceTierId.isNullOrBlank()) add("serviceTierId is required")
            if (req.title.isNullOrBlank())       add("title is required")
            if (req.description.isNullOrBlank()) add("description is required")
            if (req.color.isNullOrBlank())       add("color is required")
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    validate<UpdateSpecialityReq> { req ->
        val allNull = req.title == null && req.serviceTierId == null && req.description == null &&
            req.color == null && req.iconUrl == null
        if (allNull) return@validate ValidationResult.Invalid("At least one field must be provided for update")

        val errors = buildList {
            req.serviceTierId?.let { if (it.isBlank()) add("serviceTierId cannot be blank") }
            req.title?.let       { if (it.isBlank()) add("title cannot be blank") }
            req.description?.let { if (it.isBlank()) add("description cannot be blank") }
            req.color?.let       { if (it.isBlank()) add("color cannot be blank") }
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    validate<CreateSubSpecialityReq> { req ->
        val errors = buildList {
            if (req.title.isNullOrBlank())       add("title is required")
            if (req.description.isNullOrBlank()) add("description is required")
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    validate<UpdateSubSpecialityReq> { req ->
        val allNull = req.title == null && req.description == null &&
            req.color == null && req.iconUrl == null
        if (allNull) return@validate ValidationResult.Invalid("At least one field must be provided for update")

        val errors = buildList {
            req.title?.let       { if (it.isBlank()) add("title cannot be blank") }
            req.description?.let { if (it.isBlank()) add("description cannot be blank") }
            req.color?.let       { if (it.isBlank()) add("color cannot be blank") }
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
}
