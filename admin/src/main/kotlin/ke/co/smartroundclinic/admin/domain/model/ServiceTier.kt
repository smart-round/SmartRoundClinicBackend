package ke.co.smartroundclinic.admin.domain.model

data class ServiceTier(
    val id:String,
    val name: String,
    val tierPrice: Double,
    val consultationDuration: Long,
    val gracePeriod: Long,
    val chatAccessWindow: Long,
    val followUpWindow: Long,
    val followUpFee: Long,
    val createdAt: String,
    val updatedAt: String?,
)