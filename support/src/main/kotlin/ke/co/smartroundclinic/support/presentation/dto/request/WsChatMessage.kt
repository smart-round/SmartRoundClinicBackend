package ke.co.smartroundclinic.support.presentation.dto.request

import ke.co.smartroundclinic.support.data.entity.MessageType
import kotlinx.serialization.Serializable

@Serializable
data class WsChatMessage(
    val message: String,
)
