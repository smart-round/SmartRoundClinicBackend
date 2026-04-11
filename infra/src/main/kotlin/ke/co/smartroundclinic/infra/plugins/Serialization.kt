package ke.co.smartroundclinic.infra.plugins

import com.google.gson.stream.MalformedJsonException
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.serialization.gson.gson
import io.ktor.server.plugins.BadRequestException
import ke.co.smartroundclinic.common.Resource
import kotlinx.serialization.SerializationException

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        gson()
    }

    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                Resource.Error(data = null, message = "Invalid request format")
                    .toDefaultResponse(
                        HttpStatusCode.BadRequest.value
                    ){null}
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                Resource.Error(data = null, message = cause.localizedMessage ?: "Invalid argument").toDefaultResponse(
                    HttpStatusCode.BadRequest.value
                ){null}
            )
        }
        // Catch-all for other exceptions to ensure a consistent response format
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                Resource.Error(data = null, message = "An internal server error occurred").toDefaultResponse(
                    HttpStatusCode.InternalServerError.value
                ){null}
            )
            // log the error
            call.application.environment.log.error("Internal Server Error", cause)
        }
        exception<MissingParametersException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                Resource.Error(data = null, message = cause.message ?: "Request has some missing parameters").toDefaultResponse(
                    HttpStatusCode.BadRequest.value
                ){null}
            )
        }
    }
}


class MissingParametersException(message: String) : Exception(message)