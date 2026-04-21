package ke.co.smartroundclinic.admin.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import ke.co.smartroundclinic.admin.domain.service.PolicyGroupService
import ke.co.smartroundclinic.admin.presentation.dto.request.CreatePolicyGroupReq
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdatePolicyGroupReq
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val SUPER_ADMIN = "SUPER_ADMIN"

fun Route.policyGroupController(service: PolicyGroupService) {
    route("/admin/permissions") {
        authenticate("auth-jwt") {
            get("catalog") {
                call.requireRole(SUPER_ADMIN) {
                    val result = service.getPermissionCatalog()
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }

    route("/admin/policy-groups") {
        authenticate("auth-jwt") {
            post {
                call.requireRole(SUPER_ADMIN) {
                    val createdBy = call.getUserId() ?: return@requireRole
                    val req = call.receive<CreatePolicyGroupReq>()
                    val result = service.create(req, createdBy)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get {
                call.requireRole(SUPER_ADMIN) {
                    val result = service.list()
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            route("{id}") {
                get {
                    call.requireRole(SUPER_ADMIN) {
                        val id = call.parameters["id"]!!
                        val result = service.get(id)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                put {
                    call.requireRole(SUPER_ADMIN) {
                        val id = call.parameters["id"]!!
                        val req = call.receive<UpdatePolicyGroupReq>()
                        val result = service.update(id, req)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                delete {
                    call.requireRole(SUPER_ADMIN) {
                        val id = call.parameters["id"]!!
                        val result = service.delete(id)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                post("assign/{adminId}") {
                    call.requireRole(SUPER_ADMIN) {
                        val policyGroupId = call.parameters["id"]!!
                        val adminId = call.parameters["adminId"]!!
                        val result = service.assignAdmin(policyGroupId, adminId)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                delete("assign/{adminId}") {
                    call.requireRole(SUPER_ADMIN) {
                        val policyGroupId = call.parameters["id"]!!
                        val adminId = call.parameters["adminId"]!!
                        val result = service.removeAdmin(policyGroupId, adminId)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }
            }
        }
    }
}
