package ke.co.smartroundclinic.consultation.presentation.dto.request

import ke.co.smartroundclinic.consultation.data.entity.MessageType
import kotlinx.serialization.Serializable

@Serializable
data class StartConsultationReq(val appointmentId: String)

@Serializable
data class ConsultationWsMessage(
    val type: MessageType,
    val message: String? = null,
    val fileName: String? = null,
    val contentType: String? = null,
    val data: String? = null,
)
