package ke.co.smartroundclinic.support.domain.repository

import ke.co.smartroundclinic.support.data.entity.SupportTicketChatEntity
import ke.co.smartroundclinic.common.Resource
import kotlinx.coroutines.flow.Flow

interface SupportTicketChatRepository {
    suspend fun save(entity: SupportTicketChatEntity): Resource<SupportTicketChatEntity?>
    suspend fun getByTicketId(ticketId: String): Resource<List<SupportTicketChatEntity>>
    fun watchMessages(ticketId: String): Flow<SupportTicketChatEntity>
    suspend fun getUserName(userId: String): String?
    suspend fun getComplainantId(ticketId: String): String?
}
