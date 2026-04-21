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
import ke.co.smartroundclinic.admin.presentation.dto.request.ReplaceAdminPolicyGroupsReq
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdatePolicyGroupReq
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
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

            get("all") {
                call.requireRole(SUPER_ADMIN) {
                    val result = service.list()
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }


            get {
                call.requireRole(SUPER_ADMIN) {
                    val id = call.parameters[ "id"] ?: throw MissingParametersException("id path parameter is missing")
                    val result = service.get(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            put {
                call.requireRole(SUPER_ADMIN) {
                    val id = call.parameters["id"] ?: throw MissingParametersException("id path parameter is missing")
                    val req = call.receive<UpdatePolicyGroupReq>()
                    val result = service.update(id, req)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            delete {
                call.requireRole(SUPER_ADMIN) {
                    val id = call.parameters["id"] ?: throw MissingParametersException("id path parameter is missing")
                    val result = service.delete(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            route("assign") {
                // POST ?id={policyGroupId}&adminId={adminId} — add one group to an admin
                post {
                    call.requireRole(SUPER_ADMIN) {
                        val policyGroupId =
                            call.parameters["id"] ?: throw MissingParametersException("id path parameter is missing")
                        val adminId = call.parameters["adminId"]
                            ?: throw MissingParametersException("adminId path parameter is missing")
                        val result = service.assignAdmin(policyGroupId, adminId)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                // PUT — replace all groups for an admin at once { adminId, policyGroupIds: [...] }
                put {
                    call.requireRole(SUPER_ADMIN) {
                        val req = call.receive<ReplaceAdminPolicyGroupsReq>()
                        val result = service.replaceAdminPolicyGroups(req.adminId, req.policyGroupIds)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                // DELETE ?id={policyGroupId}&adminId={adminId} — remove one group from an admin
                delete {
                    call.requireRole(SUPER_ADMIN) {
                        val policyGroupId =
                            call.parameters["id"] ?: throw MissingParametersException("id path parameter is missing")
                        val adminId = call.parameters["adminId"]
                            ?: throw MissingParametersException("adminId path parameter is missing")
                        val result = service.removeAdmin(policyGroupId, adminId)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }
            }

        }
    }
}
