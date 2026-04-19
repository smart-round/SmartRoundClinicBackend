package ke.co.smartroundclinic.support.presentation.dto.request

import ke.co.smartroundclinic.support.data.entity.MessageType
import kotlinx.serialization.Serializable

@Serializable
data class WsChatMessage(
    val type: MessageType,
    val message: String? = null,
    val fileName: String? = null,
    val contentType: String? = null,
    val data: String? = null,
)
