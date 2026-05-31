package ke.co.smartroundclinic.support.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.support.data.entity.IssueCategoryEntity
import ke.co.smartroundclinic.support.data.entity.TicketEntity
import ke.co.smartroundclinic.support.data.entity.TicketStatus
import ke.co.smartroundclinic.support.domain.repository.TicketRepository
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.Document
import java.time.Instant
import kotlin.time.Clock

class TicketRepositoryImpl(supportDb: MongoDatabase, authDb: MongoDatabase) : TicketRepository {

    private val tickets = supportDb.getCollection<TicketEntity>(MongoDBConstants.SUPPORT_TICKETS)
    private val categories = supportDb.getCollection<IssueCategoryEntity>(MongoDBConstants.SUPPORT_ISSUE_CATEGORIES)
    private val users = authDb.getCollection<Document>(MongoDBConstants.AUTH_USER)

    private suspend fun categoryName(issueCategoryId: String): String =
        categories.find(Filters.eq(IssueCategoryEntity::id.name, issueCategoryId))
            .firstOrNull()?.name ?: "Unknown"

    private suspend fun assigneeName(adminUserId: String?): String? {
        if (adminUserId == null) return null
        return users.find(Filters.eq("id", adminUserId)).firstOrNull()?.getString("fullName")
    }

    override suspend fun create(entity: TicketEntity): Resource<TicketEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val category = categories.find(Filters.eq(IssueCategoryEntity::id.name, entity.issueCategoryId)).firstOrNull()
                    ?: return@withContext Resource.Error("Issue category not found")
                if (!category.status) return@withContext Resource.Error("Issue category is inactive")
                tickets.insertOne(entity)
                Resource.Success(data = entity, message = "Ticket created successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to create ticket")
            }
        }

    override suspend fun getById(id: String): Resource<Triple<TicketEntity, String, String?>?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = tickets.find(Filters.eq(TicketEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Ticket not found")
                Resource.Success(
                    data = Triple(entity, categoryName(entity.issueCategoryId), assigneeName(entity.assignedToId)),
                    message = "Ticket retrieved successfully"
                )
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve ticket")
            }
        }

    override suspend fun getAll(page: Int, size: Int): Resource<Pair<List<Triple<TicketEntity, String, String?>>, Long>> =
        withContext(Dispatchers.IO) {
            try {
                val safePage = maxOf(1, page)
                val safeSize = minOf(maxOf(1, size), 100)
                val total = tickets.countDocuments()
                val items = tickets.find()
                    .skip((safePage - 1) * safeSize)
                    .limit(safeSize)
                    .toList()
                    .map { Triple(it, categoryName(it.issueCategoryId), assigneeName(it.assignedToId)) }
                Resource.Success(data = items to total, message = "Tickets retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve tickets")
            }
        }

    override suspend fun updateStatus(id: String, status: TicketStatus): Resource<TicketEntity?> =
        withContext(Dispatchers.IO) {
            try {
                tickets.find(Filters.eq(TicketEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Ticket not found")
                tickets.updateOne(
                    Filters.eq(TicketEntity::id.name, id),
                    Updates.combine(
                        Updates.set(TicketEntity::status.name, status),
                        Updates.set(TicketEntity::updatedAt.name, Clock.System.now().toString()),
                    )
                )
                val updated = tickets.find(Filters.eq(TicketEntity::id.name, id)).firstOrNull()
                Resource.Success(data = updated, message = "Ticket status updated successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to update ticket status")
            }
        }

    override suspend fun assign(id: String, adminUserId: String): Resource<TicketEntity?> =
        withContext(Dispatchers.IO) {
            try {
                tickets.find(Filters.eq(TicketEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Ticket not found")
                users.find(
                    Filters.and(
                        Filters.eq("id", adminUserId),
                        Filters.eq("role", "ADMIN"),
                    )
                ).firstOrNull() ?: return@withContext Resource.Error("Admin user not found")
                tickets.updateOne(
                    Filters.eq(TicketEntity::id.name, id),
                    Updates.combine(
                        Updates.set(TicketEntity::assignedToId.name, adminUserId),
                        Updates.set(TicketEntity::updatedAt.name, Clock.System.now().toString()),
                    )
                )
                val updated = tickets.find(Filters.eq(TicketEntity::id.name, id)).firstOrNull()
                Resource.Success(data = updated, message = "Ticket assigned successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to assign ticket")
            }
        }

    override suspend fun getByComplainantId(complainantId: String, page: Int, size: Int): Resource<Pair<List<Triple<TicketEntity, String, String?>>, Long>> =
        withContext(Dispatchers.IO) {
            try {
                val safePage = maxOf(1, page)
                val safeSize = minOf(maxOf(1, size), 100)
                val filter = Filters.eq(TicketEntity::complainantId.name, complainantId)
                val total = tickets.countDocuments(filter)
                val items = tickets.find(filter)
                    .sort(Document("createdAt", -1))
                    .skip((safePage - 1) * safeSize)
                    .limit(safeSize)
                    .toList()
                    .map { Triple(it, categoryName(it.issueCategoryId), assigneeName(it.assignedToId)) }
                Resource.Success(data = items to total, message = "Tickets retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve tickets")
            }
        }

    override suspend fun delete(id: String): Resource<TicketEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = tickets.find(Filters.eq(TicketEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Ticket not found")
                tickets.deleteOne(Filters.eq(TicketEntity::id.name, id))
                Resource.Success(data = entity, message = "Ticket deleted successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to delete ticket")
            }
        }
}
