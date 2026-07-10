package ke.co.smartroundclinic.consultation.presentation.dto.request

import kotlinx.serialization.Serializable

// type is a plain String, not the MessageType enum, so this same envelope can also carry
// transient (non-persisted) events like "TYPING" alongside the persisted "TEXT"/"FILE"/"PRESCRIPTION"
// values — wire-compatible with the old MessageType-typed field since the default enum
// serialization was already the literal uppercase names.
@Serializable
data class ConsultationWsMessage(
    val type: String,
    val message: String? = null,
    val fileName: String? = null,
    val contentType: String? = null,
    val data: String? = null,
    val isTyping: Boolean? = null,
)
