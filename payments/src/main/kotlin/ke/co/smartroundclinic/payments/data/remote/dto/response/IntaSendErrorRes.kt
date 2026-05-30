package ke.co.smartroundclinic.payments.data.remote.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class IntaSendErrorItem(
    val code: String? = null,
    val detail: String? = null,
    val attr: String? = null,
)

@Serializable
data class IntaSendErrorRes(
    val type: String? = null,
    val detail: String? = null,
    val errors: List<IntaSendErrorItem>? = null,
) {
    fun message(): String = detail
        ?: errors?.joinToString("; ") { err ->
            buildString {
                err.attr?.let { append("$it: ") }
                append(err.detail ?: err.code ?: "Unknown error")
            }
        }
        ?: "Unknown IntaSend error"
}
