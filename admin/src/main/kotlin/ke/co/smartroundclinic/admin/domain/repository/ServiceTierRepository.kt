package ke.co.smartroundclinic.admin.domain.repository

import ke.co.smartroundclinic.admin.data.entity.ServiceTierEntity
import ke.co.smartroundclinic.common.Resource

interface ServiceTierRepository {
    suspend fun createServiceTier(serviceTierEntity: ServiceTierEntity): Resource<ServiceTierEntity?>
    suspend fun getServiceTierById(id: String): Resource<ServiceTierEntity?>
    suspend fun getAllServiceTiers(page: Int, size: Int): Resource<Pair<List<ServiceTierEntity>, Long>>
    suspend fun updateServiceTier(
        id: String,
        name: String?,
        tierPrice: Double?,
        consultationDuration: Long?,
        gracePeriod: Long?,
        chatAccessWindow: Long?,
        followUpWindow: Long?,
        followUpFee: Long?,
    ): Resource<ServiceTierEntity?>
    suspend fun deleteServiceTier(id: String): Resource<ServiceTierEntity?>
}
