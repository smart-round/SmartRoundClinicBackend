package ke.co.smartroundclinic.doctor

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ke.co.smartroundclinic.doctor.domain.service.CertificationService
import ke.co.smartroundclinic.doctor.domain.service.ComplianceService
import ke.co.smartroundclinic.doctor.domain.service.LocalBankService
import ke.co.smartroundclinic.doctor.domain.service.PaymentDetailsService
import ke.co.smartroundclinic.doctor.domain.service.PractitionerLicenceService
import ke.co.smartroundclinic.doctor.domain.service.PractitionerProfileService
import ke.co.smartroundclinic.doctor.presentation.controller.certificationController
import ke.co.smartroundclinic.doctor.presentation.controller.complianceController
import ke.co.smartroundclinic.doctor.presentation.controller.localBankController
import ke.co.smartroundclinic.doctor.presentation.controller.paymentDetailsController
import ke.co.smartroundclinic.doctor.presentation.controller.practitionerLicenceController
import ke.co.smartroundclinic.doctor.presentation.controller.practitionerProfileController
import ke.co.smartroundclinic.doctor.validation.registerDoctorValidators
import org.koin.ktor.ext.inject
import kotlin.getValue

fun Application.doctorModule() {
    registerDoctorValidators()
    val profileService: PractitionerProfileService by inject()
    val certificationService: CertificationService by inject()
    val licenceService: PractitionerLicenceService by inject()
    val complianceService: ComplianceService by inject()
    val paymentDetailsService: PaymentDetailsService by inject()
    val localBankService: LocalBankService by inject()
    routing {
        practitionerProfileController(profileService)
        certificationController(certificationService)
        complianceController(complianceService)
        practitionerLicenceController(licenceService)
        paymentDetailsController(paymentDetailsService)
        localBankController(localBankService)
    }
}