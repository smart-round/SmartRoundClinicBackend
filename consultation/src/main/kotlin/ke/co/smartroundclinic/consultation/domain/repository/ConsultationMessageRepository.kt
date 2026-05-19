package ke.co.smartroundclinic.consultation.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import kotlinx.coroutines.flow.Flow

interface ConsultationMessageRepository {
    suspend fun save(entity: ConsultationMessageEntity): Resource<ConsultationMessageEntity>
    suspend fun getByConsultationId(consultationId: String, page: Int, size: Int): Resource<Pair<List<ConsultationMessageEntity>, Long>>
    fun watchMessages(consultationId: String): Flow<ConsultationMessageEntity>
    suspend fun getUserName(userId: String): String?
}
