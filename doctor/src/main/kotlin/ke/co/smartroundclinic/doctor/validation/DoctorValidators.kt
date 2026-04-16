package ke.co.smartroundclinic.doctor.validation

import io.ktor.server.application.Application
import ke.co.smartroundclinic.doctor.presentation.dto.request.AddPaymentDetailsReq
import ke.co.smartroundclinic.doctor.presentation.dto.request.UpdatePaymentDetailsReq
import ke.co.smartroundclinic.infra.plugins.registerValidator

fun Application.registerDoctorValidators() {

    registerValidator<AddPaymentDetailsReq> { req ->
        require(req.bankName.isNotBlank())      { "bankName is required" }
        require(req.branchName.isNotBlank())    { "branchName is required" }
        require(req.bankCode.isNotBlank())      { "bankCode is required" }
        require(req.branchCode.isNotBlank())    { "branchCode is required" }
        require(req.accountNumber.isNotBlank()) { "accountNumber is required" }
        require(req.accountName.isNotBlank())   { "accountName is required" }
    }

    registerValidator<UpdatePaymentDetailsReq> { req ->
        val allNull = req.bankName == null && req.branchName == null &&
            req.bankCode == null && req.branchCode == null &&
            req.accountNumber == null && req.accountName == null
        require(!allNull) { "At least one field must be provided for update" }
        req.bankName?.let     { require(it.isNotBlank()) { "bankName cannot be blank" } }
        req.branchName?.let   { require(it.isNotBlank()) { "branchName cannot be blank" } }
        req.bankCode?.let     { require(it.isNotBlank()) { "bankCode cannot be blank" } }
        req.branchCode?.let   { require(it.isNotBlank()) { "branchCode cannot be blank" } }
        req.accountNumber?.let { require(it.isNotBlank()) { "accountNumber cannot be blank" } }
        req.accountName?.let  { require(it.isNotBlank()) { "accountName cannot be blank" } }
    }
}
