package ke.co.smartroundclinic.admin.validation

import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateCommissionRateReq
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateServiceTierReq
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdateCommissionRateReq
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdateServiceTierReq

fun RequestValidationConfig.registerAdminValidators() {

    validate<CreateCommissionRateReq> { req ->
        val errors = buildList {
            if (req.commissionRate < 0 || req.commissionRate > 100) add("commissionRate must be between 0 and 100")
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    validate<UpdateCommissionRateReq> { req ->
        if (req.commissionRate == null) return@validate ValidationResult.Invalid("commissionRate is required for update")
        val errors = buildList {
            req.commissionRate.let { if (it < 0 || it > 100) add("commissionRate must be between 0 and 100") }
        }
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

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

}
