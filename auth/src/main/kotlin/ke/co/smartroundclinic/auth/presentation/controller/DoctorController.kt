package ke.co.smartroundclinic.auth.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.domain.service.UserService
import ke.co.smartroundclinic.auth.presentation.dto.request.AdminUpdateUserReq
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.DoctorOnboardingHandler
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val ADMIN = "ADMIN"

fun Route.doctorController(userService: UserService, onboardingHandler: DoctorOnboardingHandler?) {
    route("/auth/doctors") {
        post("sign-up") {
            if (onboardingHandler == null) {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    mapOf("message" to "Doctor onboarding is not available")
                )
                return@post
            }

            var fullName: String? = null
            var email: String? = null
            var password: String? = null
            var kraPin: String? = null
            var gender: String? = null
            var phoneNumber: String? = null
            var dateOfBirth: String? = null

            var kmpdcRegNumber: String? = null
            var title: String? = null
            var bio: String? = null
            var yearsOfExperience: String? = null
            var languages: String? = null
            var facilityName: String? = null

            var specializationId: String? = null
            var subSpecializationId: String? = null

            var licenceName: String? = null
            var licenceBytes: ByteArray? = null
            var licenceContentType: String? = null

            var profilePictureBytes: ByteArray? = null
            var profilePictureContentType: String? = null

            var bankName: String? = null
            var branchName: String? = null
            var bankCode: String? = null
            var branchCode: String? = null
            var accountNumber: String? = null
            var accountName: String? = null

            call.receiveMultipart().forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> when (part.name) {
                        "fullName" -> fullName = part.value
                        "email" -> email = part.value
                        "password" -> password = part.value
                        "gender" -> gender = part.value
                        "kraPin" -> kraPin = part.value
                        "phoneNumber" -> phoneNumber = part.value
                        "dateOfBirth" -> dateOfBirth = part.value
                        "kmpdcRegNumber" -> kmpdcRegNumber = part.value
                        "title" -> title = part.value
                        "bio" -> bio = part.value
                        "yearsOfExperience" -> yearsOfExperience = part.value
                        "languages" -> languages = part.value
                        "facilityName" -> facilityName = part.value
                        "specializationId" -> specializationId = part.value
                        "subSpecializationId" -> subSpecializationId = part.value
                        "licenceName" -> licenceName = part.value
                        "bankName" -> bankName = part.value
                        "branchName" -> branchName = part.value
                        "bankCode" -> bankCode = part.value
                        "branchCode" -> branchCode = part.value
                        "accountNumber" -> accountNumber = part.value
                        "accountName" -> accountName = part.value
                    }
                    is PartData.FileItem -> when (part.name) {
                        "profilePicture" -> {
                            profilePictureBytes = part.streamProvider().readBytes()
                            profilePictureContentType = part.contentType?.toString()
                        }
                        "licenceFile" -> {
                            licenceBytes = part.streamProvider().readBytes()
                            licenceContentType = part.contentType?.toString()
                        }
                    }
                    else -> Unit
                }
                part.dispose()
            }

            val missingFields = buildList {
                if (fullName.isNullOrBlank()) add("fullName")
                if (email.isNullOrBlank()) add("email")
                if (kraPin.isNullOrBlank()) add("kraPin")
                if (password.isNullOrBlank()) add("password")
                if (specializationId.isNullOrBlank()) add("specializationId")
                if (licenceName.isNullOrBlank()) add("licenceName")
                if (licenceBytes == null) add("licenceFile")
                if (licenceContentType.isNullOrBlank()) add("licenceFile content-type")
                if (bankName.isNullOrBlank()) add("bankName")
                if (branchName.isNullOrBlank()) add("branchName")
                if (bankCode.isNullOrBlank()) add("bankCode")
                if (branchCode.isNullOrBlank()) add("branchCode")
                if (accountNumber.isNullOrBlank()) add("accountNumber")
                if (accountName.isNullOrBlank()) add("accountName")
            }
            if (missingFields.isNotEmpty()) {
                throw MissingParametersException("Missing required fields: ${missingFields.joinToString()}")
            }

            val parsedLanguages = languages
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            val result = onboardingHandler.onboard(
                fullName = fullName!!,
                email = email!!,
                password = password!!,
                gender = gender ?: "NON_BINARY",
                kraPin = kraPin!!,
                phoneNumber = phoneNumber?.takeIf { it.isNotBlank() },
                dateOfBirth = dateOfBirth?.takeIf { it.isNotBlank() },
                profilePictureBytes = profilePictureBytes,
                profilePictureContentType = profilePictureContentType,
                kmpdcRegNumber = kmpdcRegNumber?.takeIf { it.isNotBlank() },
                title = title?.takeIf { it.isNotBlank() },
                bio = bio?.takeIf { it.isNotBlank() },
                yearsOfExperience = yearsOfExperience?.toIntOrNull(),
                languages = parsedLanguages,
                facilityName = facilityName?.takeIf { it.isNotBlank() },
                specializationId = specializationId!!,
                subSpecializationId = subSpecializationId?.takeIf { it.isNotBlank() },
                licenceName = licenceName!!,
                licenceBytes = licenceBytes!!,
                licenceContentType = licenceContentType!!,
                bankName = bankName!!,
                branchName = branchName!!,
                bankCode = bankCode!!,
                branchCode = branchCode!!,
                accountNumber = accountNumber!!,
                accountName = accountName!!,
            )

            val response = when (result) {
                is Resource.Success -> DefaultResponse<Nothing?>(
                    httpStatusCode = HttpStatusCode.Created.value,
                    status = true,
                    message = result.message ?: "Account created successfully. Kindly verify your email.",
                    data = null,
                )
                is Resource.Error -> DefaultResponse<Nothing?>(
                    httpStatusCode = HttpStatusCode.BadRequest.value,
                    status = false,
                    message = result.message ?: "Sign up failed",
                    data = null,
                )
            }
            call.respond(HttpStatusCode.fromValue(response.httpStatusCode), response)
        }

        authenticate("auth-jwt") {
            get {
                call.requireRole(ADMIN) {
                    val page = call.parameters["page"]?.toIntOrNull() ?: 1
                    val size = call.parameters["size"]?.toIntOrNull() ?: 20
                    val search = call.parameters["search"]?.takeIf { it.isNotBlank() }
                    val result = userService.getUsersByRole(UserEntity.Role.DOCTOR, page, size, search)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("filter") {
                call.requireRole(ADMIN) {
                    val page = call.parameters["page"]?.toIntOrNull() ?: 1
                    val size = call.parameters["size"]?.toIntOrNull() ?: 20
                    val accountStatus = call.parameters["accountStatus"]
                        ?.takeIf { it.isNotBlank() }
                        ?.let { runCatching { UserEntity.AccountStatus.valueOf(it) }.getOrNull() }
                    val createdFrom = call.parameters["createdFrom"]?.takeIf { it.isNotBlank() }
                    val createdTo = call.parameters["createdTo"]?.takeIf { it.isNotBlank() }
                    val result = userService.filterUsers(
                        role = UserEntity.Role.DOCTOR,
                        page = page,
                        size = size,
                        accountStatus = accountStatus,
                        createdFrom = createdFrom,
                        createdTo = createdTo,
                    )
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            patch {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id path parameter is missing")
                    val body = call.receive<AdminUpdateUserReq>()
                    val result = userService.adminUpdateUser(id, body)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
