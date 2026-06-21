package ke.co.smartroundclinic.notification.config

import ke.co.smartroundclinic.infra.EnvLoader

private fun require(key: String): String =
    EnvLoader.get(key) ?: throw IllegalStateException("$key is required")

data class EmailConfig(
    val apiKey: String = require("RESEND_API_KEY"),
    val baseUrl: String = require("RESEND_BASE_URL"),
    val onboardingEmail: String = require("RESEND_ONBOARDING_EMAIL"),
    val onboardingTemplateId: String = require("RESEND_ONBOARDING_TEMPLATE_ID"),
    val accountVerificationEmail: String = require("RESEND_ACCOUNT_VERIFICATION_EMAIL"),
    val accountVerificationTemplateId: String = require("RESEND_ACCOUNT_VERIFICATION_TEMPLATE_ID"),
    val doctorAccountVerificationSuccessTemplateId: String = require("RESEND_DOCTOR_ACCOUNT_VERIFICATION_SUCCESS_TEMPLATE_ID"),
    val patientAccountVerificationSuccessTemplateId: String = require("RESEND_PATIENT_ACCOUNT_VERIFICATION_SUCCESS_TEMPLATE_ID"),
    val passwordResetTemplateId: String = require("RESEND_RESET_EMAIL"),
    val passwordResetRequestTemplateId: String = require("RESEND_PASSWORD_RESET_REQUEST_TEMPLATE_ID"),
    val passwordResetConfirmationTemplateId: String = require("RESEND_PASSWORD_RESET_CONFIRMATION_TEMPLATE_ID"),
    val resendDoctorApplicationRequestTemplateId: String = require("RESEND_DOCTOR_APPLICATION_REQUEST_TEMPLATE_ID"),
    val resendDoctorApplicationRequestApprovedTemplateId: String = require("RESEND_DOCTOR_APPLICATION_REQUEST_APPROVED_TEMPLATE_ID"),
    val resendDoctorApplicationRequestRejectedTemplateId: String = require("RESEND_DOCTOR_APPLICATION_REQUEST_REJECTED_TEMPLATE_ID"),
    val suspendUserEmail: String = require("RESEND_SUSPEND_USER_EMAIL"),
    val suspendUserTemplateId: String = require("RESEND_SUSPEND_USER_TEMPLATE_ID"),
)
