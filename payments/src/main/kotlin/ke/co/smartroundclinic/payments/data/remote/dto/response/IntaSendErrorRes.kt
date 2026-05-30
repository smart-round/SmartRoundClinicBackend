package ke.co.smartroundclinic.payments.data.remote.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class IntaSendErrorRes(
    val detail: String? = null,
    val errors: Map<String, List<String>>? = null,
) {
    fun message(): String = detail
        ?: errors?.entries?.joinToString("; ") { (field, msgs) -> "$field: ${msgs.joinToString(", ")}" }
        ?: "Unknown IntaSend error"
}
