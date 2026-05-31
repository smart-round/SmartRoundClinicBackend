package ke.co.smartroundclinic.support.domain.repository

import ke.co.smartroundclinic.support.data.entity.TicketEntity
import ke.co.smartroundclinic.support.data.entity.TicketStatus
import ke.co.smartroundclinic.common.Resource

interface TicketRepository {
    suspend fun create(entity: TicketEntity): Resource<TicketEntity?>
    suspend fun getById(id: String): Resource<Triple<TicketEntity, String, String?>?>
    suspend fun getAll(page: Int, size: Int): Resource<Pair<List<Triple<TicketEntity, String, String?>>, Long>>
    suspend fun getByComplainantId(complainantId: String, page: Int, size: Int): Resource<Pair<List<Triple<TicketEntity, String, String?>>, Long>>
    suspend fun updateStatus(id: String, status: TicketStatus): Resource<TicketEntity?>
    suspend fun assign(id: String, adminUserId: String): Resource<TicketEntity?>
    suspend fun delete(id: String): Resource<TicketEntity?>
}
