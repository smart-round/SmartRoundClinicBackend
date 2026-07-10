package ke.co.smartroundclinic.consultation.presentation.validation

import io.ktor.server.plugins.requestvalidation.RequestValidationConfig

// No consultation-specific request bodies need validation anymore — chat/calls connect directly
// over (doctorId, patientId) path parameters rather than a request body.
fun RequestValidationConfig.registerConsultationValidators() {
}
