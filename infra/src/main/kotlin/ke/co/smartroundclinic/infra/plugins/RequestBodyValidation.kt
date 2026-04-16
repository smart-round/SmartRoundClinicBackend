package ke.co.smartroundclinic.infra.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.application
import io.ktor.server.request.receive
import io.ktor.util.AttributeKey
import kotlin.reflect.KClass

// ── Registry storage ──────────────────────────────────────────────────────────

// @PublishedApi so inline functions in this file can access it.
@PublishedApi
internal val ValidatorsKey =
    AttributeKey<MutableMap<KClass<*>, (Any) -> Unit>>("RequestBodyValidators")

// ── Setup (call once in configureInfraModule) ─────────────────────────────────

/**
 * Initialises the request-body validation registry on this application.
 * Called automatically by [configureInfraModule].
 */
fun Application.configureRequestBodyValidation() {
    attributes.put(ValidatorsKey, mutableMapOf())
}

// ── Registration extension ────────────────────────────────────────────────────

/**
 * Registers a validator for request body type [T].
 * Call this from each feature module's `Application.xxxModule()` function.
 *
 * ```kotlin
 * registerValidator<AddPaymentDetailsReq> { req ->
 *     require(req.bankName?.isNotBlank() == true) { "bankName is required" }
 * }
 * ```
 *
 * Any [IllegalArgumentException] thrown in the block is caught by [StatusPages]
 * and returned as **400 Bad Request** automatically.
 */
inline fun <reified T : Any> Application.registerValidator(noinline block: (T) -> Unit) {
    @Suppress("UNCHECKED_CAST")
    attributes[ValidatorsKey][T::class] = block as (Any) -> Unit
}

// ── Call extension ────────────────────────────────────────────────────────────

/**
 * Deserialises the request body as [T] and runs its registered validator (if any).
 *
 * ```kotlin
 * val body = call.receiveValidated<AddPaymentDetailsReq>()
 * ```
 */
@Suppress("UNCHECKED_CAST")
suspend inline fun <reified T : Any> ApplicationCall.receiveValidated(): T {
    val body = receive<T>()
    val validator = application.attributes
        .getOrNull(ValidatorsKey)
        ?.get(T::class) as? (T) -> Unit
    validator?.invoke(body)
    return body
}
