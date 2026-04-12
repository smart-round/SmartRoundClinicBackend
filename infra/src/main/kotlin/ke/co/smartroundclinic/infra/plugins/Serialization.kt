package ke.co.smartroundclinic.infra.plugins

import com.google.gson.stream.MalformedJsonException
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.serialization.gson.gson
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.UnsupportedMediaTypeException
import ke.co.smartroundclinic.common.Resource
import kotlinx.serialization.SerializationException

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        gson()
    }

    install(StatusPages) {
        exception<MissingParametersException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                Resource.Error(data = null, message = cause.message ?: "Request has some missing parameters")
                    .toDefaultResponse(HttpStatusCode.BadRequest.value) { null }
            )
        }
        exception<UnsupportedMediaTypeException> { call, _ ->
            call.respond(
                HttpStatusCode.UnsupportedMediaType,
                Resource.Error(data = null, message = "Content-Type must be multipart/form-data for file uploads")
                    .toDefaultResponse(HttpStatusCode.UnsupportedMediaType.value) { null }
            )
        }
        exception<UnsupportedFileFormatException> { call, cause ->
            call.respond(
                HttpStatusCode.UnprocessableEntity,
                Resource.Error(data = null, message = cause.message ?: "Unsupported file format")
                    .toDefaultResponse(HttpStatusCode.UnprocessableEntity.value) { null }
            )
        }
        exception<BadRequestException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                Resource.Error(data = null, message = "Invalid request format")
                    .toDefaultResponse(HttpStatusCode.BadRequest.value) { null }
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                Resource.Error(data = null, message = cause.localizedMessage ?: "Invalid argument")
                    .toDefaultResponse(HttpStatusCode.BadRequest.value) { null }
            )
        }
        // Catch-all — must be last
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Internal Server Error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                Resource.Error(data = null, message = "An internal server error occurred")
                    .toDefaultResponse(HttpStatusCode.InternalServerError.value) { null }
            )
        }
    }
}


class MissingParametersException(message: String) : Exception(message)

class UnsupportedFileFormatException(contentType: String) : Exception(
    "Unsupported file format '$contentType'. Only PNG, JPEG, and WebP are allowed."
)