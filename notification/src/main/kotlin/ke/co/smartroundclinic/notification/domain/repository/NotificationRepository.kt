package ke.co.smartroundclinic.notification.domain.repository

import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.notification.data.entity.NotificationEntity

interface NotificationRepository {
    suspend fun create(entity: NotificationEntity): Resource<NotificationEntity?>
    suspend fun getById(id: String): Resource<NotificationEntity?>
    suspend fun getForUser(
        userId: String,
        destination: NotificationDestination,
        page: Int,
        size: Int,
    ): Resource<Pair<List<NotificationEntity>, Long>>
    suspend fun getAll(page: Int, size: Int): Resource<Pair<List<NotificationEntity>, Long>>
    suspend fun markAsRead(
        id: String,
        callerId: String,
        callerDestination: NotificationDestination,
    ): Resource<NotificationEntity?>
    suspend fun delete(id: String): Resource<NotificationEntity?>
}
