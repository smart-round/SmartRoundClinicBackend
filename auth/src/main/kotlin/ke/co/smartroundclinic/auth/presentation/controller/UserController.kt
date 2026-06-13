package ke.co.smartroundclinic.auth.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.http.content.PartData
import io.ktor.utils.io.toByteArray
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveMultipart
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.domain.service.UserService
import ke.co.smartroundclinic.auth.presentation.dto.request.AdminUpdateUserReq
import ke.co.smartroundclinic.auth.presentation.dto.request.RefreshTokenReq
import ke.co.smartroundclinic.auth.presentation.dto.request.ResetPasswordReq
import ke.co.smartroundclinic.auth.presentation.dto.request.SignInReq
import ke.co.smartroundclinic.auth.presentation.dto.request.CreateAdminReq
import ke.co.smartroundclinic.auth.presentation.dto.request.SignUpReq
import ke.co.smartroundclinic.auth.presentation.dto.request.UpdateUserReq
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getRole
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireImageContentType
import ke.co.smartroundclinic.infra.plugins.requireRole

fun Route.userController(userService: UserService) {
    route("/auth/user") {
        post("sign-in") {
            val signInReq = call.receive<SignInReq>()
            val result = userService.signIn(signInReq.email, signInReq.password)
            call.respond(
                status = HttpStatusCode.fromValue(result.httpStatusCode),
                message = result
            )
        }

        authenticate("auth-jwt") {
            post("create-admin") {
                call.requireRole(UserEntity.Role.SUPER_ADMIN.name) {
                    val createAdminReq = call.receive<CreateAdminReq>()
                    val result = userService.createAdmin(createAdminReq)
                    call.respond(
                        status = HttpStatusCode.fromValue(result.httpStatusCode),
                        message = result
                    )
                }
            }

            post("create-super-admin") {
                call.requireRole(UserEntity.Role.SUPER_ADMIN.name) {
                    val createAdminReq = call.receive<CreateAdminReq>()
                    val result = userService.createSuperAdmin(createAdminReq)
                    call.respond(
                        status = HttpStatusCode.fromValue(result.httpStatusCode),
                        message = result
                    )
                }
            }

            get("admins") {
                call.requireRole(UserEntity.Role.SUPER_ADMIN.name) {
                    val page = call.parameters["page"]?.toIntOrNull() ?: 1
                    val size = call.parameters["size"]?.toIntOrNull() ?: 20
                    val search = call.parameters["search"]
                    val result = userService.getUsersByRole(UserEntity.Role.ADMIN, page, size, search)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            patch("status") {
                call.requireRole(UserEntity.Role.SUPER_ADMIN.name) {
                    val id = call.parameters["id"] ?: throw MissingParametersException("id is required")
                    val body = call.receive<AdminUpdateUserReq>()
                    val result = userService.adminUpdateUser(id, body)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            patch("upgrade") {
                call.requireRole(UserEntity.Role.SUPER_ADMIN.name) {
                    val id = call.parameters["id"] ?: throw MissingParametersException("id is required")
                    val result = userService.upgradeToSuperAdmin(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }

        post("sign-up") {
            val createAdminReq = call.receive<SignUpReq>()
            val role = call.parameters["role"]
                ?: throw MissingParametersException("Role parameter is missing example: (/?role = DOCTOR/PATIENT)")
            val result = userService.signUp(role = role, body = createAdminReq)
            call.respond(
                status = HttpStatusCode.fromValue(result.httpStatusCode),
                message = result
            )
        }

        post("sign-up-with-picture") {
            val role = call.parameters["role"]
                ?: throw MissingParametersException("Role parameter is missing example: (/?role = DOCTOR/PATIENT)")

            var fullName: String? = null
            var email: String? = null
            var password: String? = null
            var gender: String? = null
            var phoneNumber: String? = null
            var dateOfBirth: String? = null
            var imageBytes: ByteArray? = null
            var contentType: String? = null

            call.receiveMultipart().forEachPart { part ->
                when {
                    part is PartData.FormItem -> when (part.name) {
                        "fullName" -> fullName = part.value
                        "email" -> email = part.value
                        "password" -> password = part.value
                        "gender" -> gender = part.value
                        "phoneNumber" -> phoneNumber = part.value
                        "dateOfBirth" -> dateOfBirth = part.value
                    }
                    part is PartData.FileItem && part.name == "file" -> {
                        imageBytes = part.provider().toByteArray()
                        contentType = part.contentType?.toString() ?: ""
                    }
                }
                part.dispose()
            }

            val signUpReq = SignUpReq(
                fullName = fullName ?: throw MissingParametersException("fullName is required"),
                email = email ?: throw MissingParametersException("email is required"),
                password = password ?: throw MissingParametersException("password is required"),
                phoneNumber = phoneNumber?.takeIf { it.isNotBlank() },
                dateOfBirth = dateOfBirth?.takeIf { it.isNotBlank() },
            ).let { req ->
                gender?.takeIf { it.isNotBlank() }
                    ?.let { g -> req.copy(gender = runCatching { ke.co.smartroundclinic.auth.data.entity.UserEntity.Gender.valueOf(g) }.getOrElse { req.gender }) }
                    ?: req
            }

            if (imageBytes != null) requireImageContentType(contentType ?: "")

            val result = userService.signUpWithPicture(
                role = role,
                body = signUpReq,
                imageBytes = imageBytes,
                contentType = contentType,
            )
            call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
        }

        route("account-verification") {
            get {
                val email =
                    call.parameters["email"] ?: throw MissingParametersException(message = "Email parameter is missing")
                val otpCode = call.parameters["otpCode"]
                    ?: throw MissingParametersException(message = "OtpCode parameter is missing")
                val result = userService.accountVerification(email, otpCode)
                call.respond(
                    status = HttpStatusCode.fromValue(result.httpStatusCode),
                    message = result
                )
            }

            get("resend-otp") {
                val email =
                    call.parameters["email"] ?: throw MissingParametersException(message = "Email parameter is missing")
                val result = userService.resendAccountVerificationOtp(email)
                call.respond(
                    status = HttpStatusCode.fromValue(result.httpStatusCode),
                    message = result
                )
            }
        }



        route("password-reset") {
            post("request") {
                val email = call.parameters["email"]
                    ?: throw MissingParametersException(message = "Email parameter is missing")
                val result = userService.requestPasswordReset(email)
                call.respond(
                    status = HttpStatusCode.fromValue(result.httpStatusCode),
                    message = result
                )
            }

            get("resend-otp") {
                val email = call.parameters["email"]
                    ?: throw MissingParametersException(message = "Email parameter is missing")
                val result = userService.resendPasswordResetOtp(email)
                call.respond(
                    status = HttpStatusCode.fromValue(result.httpStatusCode),
                    message = result
                )
            }

            post {
                val body = call.receive<ResetPasswordReq>()
                val result = userService.resetPassword(body.email, body.otpCode, body.newPassword)
                call.respond(
                    status = HttpStatusCode.fromValue(result.httpStatusCode),
                    message = result
                )
            }
        }

        route("token") {
            post("refresh") {
                val body = call.receive<RefreshTokenReq>()
                val result = userService.refreshToken(body.refreshToken)
                call.respond(
                    status = HttpStatusCode.fromValue(result.httpStatusCode),
                    message = result
                )
            }

            authenticate("auth-jwt") {
                delete("revoke") {
                    val body = call.receive<RefreshTokenReq>()
                    val result = userService.revokeToken(body.refreshToken)
                    call.respond(
                        status = HttpStatusCode.fromValue(result.httpStatusCode),
                        message = result
                    )
                }
            }
        }

        authenticate("auth-jwt") {
            put {
                call.requireRole(
                    *UserEntity.allRoles
                ) {
                    val userId = call.getUserId() ?: return@requireRole
                    val updateUserReq = call.receive<UpdateUserReq>()
                    val result = userService.updateUser(userId, updateUserReq)
                    call.respond(
                        status = HttpStatusCode.fromValue(result.httpStatusCode),
                        message = result
                    )
                }
            }
        }

        authenticate("auth-jwt") {
            delete("profile-picture") {
                call.requireRole(*UserEntity.allRoles) {
                    val userId = call.getUserId() ?: return@requireRole
                    val result = userService.removeProfilePicture(userId)
                    call.respond(
                        status = HttpStatusCode.fromValue(result.httpStatusCode),
                        message = result
                    )
                }
            }

            post("profile-picture") {
                call.requireRole(*UserEntity.allRoles) {
                    val userId = call.getUserId() ?: return@requireRole

                    var imageBytes: ByteArray? = null
                    var contentType = ""

                    call.receiveMultipart().forEachPart { part ->
                        if (part is PartData.FileItem) {
                            imageBytes = part.provider().toByteArray()
                            contentType = part.contentType?.toString() ?: ""
                        }
                        part.dispose()
                    }

                    val bytes = imageBytes
                        ?: return@requireRole call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("message" to "No image file provided")
                        )
                    requireImageContentType(contentType)
                    val result = userService.uploadProfilePicture(userId, bytes, contentType)
                    call.respond(
                        status = HttpStatusCode.fromValue(result.httpStatusCode),
                        message = result
                    )
                }
            }
        }

        authenticate("auth-jwt") {
            get {
                    val userId = call.getUserId() ?: return@get
                    val result = userService.getUser(userId)
                    call.respond(
                        status = HttpStatusCode.fromValue(result.httpStatusCode),
                        message = result
                    )
            }
        }
    }

}
