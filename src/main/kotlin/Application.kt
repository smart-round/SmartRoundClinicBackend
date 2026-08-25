package ke.co

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ke.co.smartroundclinic.admin.adminModule
import ke.co.smartroundclinic.admin.validation.registerAdminValidators
import ke.co.smartroundclinic.admin.koin.adminModule as adminKoinModule
import ke.co.smartroundclinic.article.articleModule
import ke.co.smartroundclinic.article.koin.articleModule as articleKoinModule
import ke.co.smartroundclinic.auth.authModule
import ke.co.smartroundclinic.auth.koin.authModule as authKoinModule
import ke.co.smartroundclinic.doctor.doctorModule
import ke.co.smartroundclinic.doctor.koin.doctorModule as doctorKoinModule
import ke.co.smartroundclinic.doctor.validation.registerDoctorValidators
import ke.co.smartroundclinic.infra.configureInfraModule
import org.koin.ktor.ext.inject
import ke.co.smartroundclinic.admin.seeder.seedDefaultPolicyGroups
import ke.co.smartroundclinic.infra.syncPermissionCatalog
import ke.co.smartroundclinic.infra.koin.appConfigModule
import ke.co.smartroundclinic.infra.koin.databaseModule
import ke.co.smartroundclinic.infra.koin.httpModule
import ke.co.smartroundclinic.infra.koin.storageModule
import ke.co.smartroundclinic.infra.koin.redisModule
import ke.co.smartroundclinic.notification.notificationModule
import ke.co.smartroundclinic.notification.koin.notificationModule as notificationKoinModule
import ke.co.smartroundclinic.scheduling.schedulingModule
import ke.co.smartroundclinic.scheduling.koin.schedulingKoinModule
import ke.co.smartroundclinic.scheduling.presentation.validation.registerSchedulingValidators
import ke.co.smartroundclinic.consultation.consultationModule
import ke.co.smartroundclinic.consultation.domain.usecase.call.StaleCallCleanupTask
import ke.co.smartroundclinic.consultation.koin.consultationKoinModule
import ke.co.smartroundclinic.consultation.presentation.validation.registerConsultationValidators
import ke.co.smartroundclinic.support.supportModule
import ke.co.smartroundclinic.support.koin.supportModule as supportKoinModule
import ke.co.smartroundclinic.support.validation.registerSupportValidators
import ke.co.smartroundclinic.patient.patientModule
import ke.co.smartroundclinic.patient.koin.patientModule as patientKoinModule
import ke.co.smartroundclinic.patient.validation.registerPatientValidators
import ke.co.smartroundclinic.notification.presentation.validation.registerNotificationValidators
import ke.co.smartroundclinic.payments.paymentsModule
import ke.co.smartroundclinic.payments.koin.paymentsKoinModule
import ke.co.smartroundclinic.payments.presentation.validation.registerPaymentValidators
import ke.co.smartroundclinic.medicalrecords.medicalRecordsModule
import ke.co.smartroundclinic.medicalrecords.koin.medicalRecordsKoinModule
import ke.co.smartroundclinic.referral.referralModule
import ke.co.smartroundclinic.referral.koin.referralKoinModule
import ke.co.smartroundclinic.doctorchat.doctorChatModule
import ke.co.smartroundclinic.doctorchat.koin.doctorChatKoinModule
import ke.co.smartroundclinic.doctorchat.domain.usecase.call.StaleDoctorCallCleanupTask
import ke.co.smartroundclinic.notification.domain.usecase.StaleDeviceTokenCleanupTask

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    configureInfraModule(
        appModules = listOf(appConfigModule, databaseModule, httpModule, storageModule, redisModule, authKoinModule, adminKoinModule, doctorKoinModule, notificationKoinModule, schedulingKoinModule, supportKoinModule, patientKoinModule, articleKoinModule, consultationKoinModule, paymentsKoinModule, medicalRecordsKoinModule, referralKoinModule, doctorChatKoinModule),
        validators = {
            registerDoctorValidators()
            registerAdminValidators()
            registerSchedulingValidators()
            registerSupportValidators()
            registerPatientValidators()
            registerConsultationValidators()
            registerNotificationValidators()
                registerPaymentValidators()
        },
        backgroundTasks = {
            val staleCallCleanup: StaleCallCleanupTask by inject()
            val staleDoctorCallCleanup: StaleDoctorCallCleanupTask by inject()
            val staleDeviceTokenCleanup: StaleDeviceTokenCleanupTask by inject()
            listOf(staleCallCleanup, staleDoctorCallCleanup, staleDeviceTokenCleanup)
        },
    )
    authModule()
    adminModule()
    articleModule()
    notificationModule()
    doctorModule()
    schedulingModule()
    supportModule()
    patientModule()
    consultationModule()
    paymentsModule()
    medicalRecordsModule()
    referralModule()
    doctorChatModule()
    val root = routing {
        get("/") {
            call.respondText("Hello World!")
        }
        get("/robots.txt") {
            call.respondText("User-agent: *\nDisallow: /\n", ContentType.Text.Plain)
        }
        // iOS Universal Links: must be served with no file extension as application/json, no
        // redirects. Lists both mobile apps so a tap on an /a/{id} link opens whichever one the
        // device has installed. TODO: doctor/patient entries below use the shared debug-keystore
        // fingerprint for local verification — swap in the Play Console release fingerprints
        // before this can verify against Play-installed (non-debug) builds.
        get("/.well-known/apple-app-site-association") {
            call.respondText(APPLE_APP_SITE_ASSOCIATION, ContentType.Application.Json)
        }
        get("/.well-known/assetlinks.json") {
            call.respondText(ANDROID_ASSET_LINKS, ContentType.Application.Json)
        }
    }
    syncPermissionCatalog(root) { catalog ->
        seedDefaultPolicyGroups(catalog)
    }
}

// iOS Team ID (shared by both mobile apps, from iosApp.xcodeproj DEVELOPMENT_TEAM).
private const val APPLE_TEAM_ID = "65X556349W"

private val APPLE_APP_SITE_ASSOCIATION = """
    {
      "applinks": {
        "apps": [],
        "details": [
          { "appID": "$APPLE_TEAM_ID.ke.co.smartroundclinic.doctor", "paths": ["/a/*"] },
          { "appID": "$APPLE_TEAM_ID.ke.co.smartroundclinic.patientapp", "paths": ["/a/*"] }
        ]
      }
    }
""".trimIndent()

// SHA256 cert fingerprints below are the shared local ~/.android/debug.keystore, for verifying
// App Links against debug builds only. Replace with each app's Play Console release fingerprint
// (App signing > App signing key certificate) before this covers Play-installed builds.
private val ANDROID_ASSET_LINKS = """
    [
      {
        "relation": ["delegate_permission/common.handle_all_urls"],
        "target": {
          "namespace": "android_app",
          "package_name": "ke.co.smartroundclinic.doctor",
          "sha256_cert_fingerprints": ["36:12:66:8F:E6:D2:A4:90:DC:9E:0D:9C:A0:F5:BE:26:F7:B7:8C:D7:7F:0A:D2:19:8D:01:D7:DD:27:32:F6:98"]
        }
      },
      {
        "relation": ["delegate_permission/common.handle_all_urls"],
        "target": {
          "namespace": "android_app",
          "package_name": "ke.co.smartroundclinic.patient",
          "sha256_cert_fingerprints": ["36:12:66:8F:E6:D2:A4:90:DC:9E:0D:9C:A0:F5:BE:26:F7:B7:8C:D7:7F:0A:D2:19:8D:01:D7:DD:27:32:F6:98"]
        }
      }
    ]
""".trimIndent()
