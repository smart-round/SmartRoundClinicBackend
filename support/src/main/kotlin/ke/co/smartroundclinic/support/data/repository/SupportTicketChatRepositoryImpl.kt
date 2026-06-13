package ke.co.smartroundclinic.support.data.repository

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.support.data.entity.SupportTicketChatEntity
import ke.co.smartroundclinic.support.domain.repository.SupportTicketChatRepository
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.Document

class SupportTicketChatRepositoryImpl(
    supportDb: MongoDatabase,
    authDb: MongoDatabase,
) : SupportTicketChatRepository {

    private val chats = supportDb.getCollection<SupportTicketChatEntity>(MongoDBConstants.SUPPORT_TICKET_CHATS)
    private val tickets = supportDb.getCollection<Document>(MongoDBConstants.SUPPORT_TICKETS)
    private val users = authDb.getCollection<Document>(MongoDBConstants.AUTH_USER)

    override suspend fun save(entity: SupportTicketChatEntity): Resource<SupportTicketChatEntity?> =
        withContext(Dispatchers.IO) {
            try {
                chats.insertOne(entity)
                Resource.Success(data = entity, message = "Message sent")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to send message")
            }
        }

    override suspend fun getByTicketId(ticketId: String): Resource<List<SupportTicketChatEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val messages = chats.find(Filters.eq(SupportTicketChatEntity::ticketId.name, ticketId))
                    .toList()
                Resource.Success(data = messages, message = "Messages retrieved")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve messages")
            }
        }

    override fun watchMessages(ticketId: String): Flow<SupportTicketChatEntity> =
        chats.watch(
            listOf(
                Aggregates.match(
                    Filters.and(
                        Filters.eq("operationType", "insert"),
                        Filters.eq("fullDocument.ticketId", ticketId),
                    )
                )
            )
        ).mapNotNull { it.fullDocument }

    override suspend fun getUserName(userId: String): String? =
        withContext(Dispatchers.IO) {
            try {
                users.find(Filters.eq("id", userId)).firstOrNull()?.getString("fullName")
            } catch (_: Exception) {
                null
            }
        }

    override suspend fun getComplainantId(ticketId: String): String? =
        withContext(Dispatchers.IO) {
            try {
                tickets.find(Filters.eq("id", ticketId)).firstOrNull()?.getString("complainantId")
            } catch (_: Exception) {
                null
            }
        }
}
