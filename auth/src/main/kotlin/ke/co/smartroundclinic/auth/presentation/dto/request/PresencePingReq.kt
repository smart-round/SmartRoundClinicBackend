package ke.co.smartroundclinic.auth.presentation.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class PresencePingReq(val isActive: Boolean)
