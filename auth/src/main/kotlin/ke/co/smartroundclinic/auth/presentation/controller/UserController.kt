package ke.co.smartroundclinic.auth.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.request.receiveMultipart
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.domain.service.UserService
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
                            imageBytes = part.streamProvider().readBytes()
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
                call.requireRole(*UserEntity.allRoles) {
                    val userId = call.getUserId() ?: return@requireRole
                    val result = userService.getUser(userId)
                    call.respond(
                        status = HttpStatusCode.fromValue(result.httpStatusCode),
                        message = result
                    )
                }
            }
        }
    }

}
