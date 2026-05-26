package ke.co.smartroundclinic.notification.domain.service

import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.notification.domain.usecase.CreateNotificationUseCase
import ke.co.smartroundclinic.notification.domain.usecase.DeleteNotificationUseCase
import ke.co.smartroundclinic.notification.domain.usecase.GetAllNotificationsUseCase
import ke.co.smartroundclinic.notification.domain.usecase.GetMyNotificationsUseCase
import ke.co.smartroundclinic.notification.domain.usecase.GetNotificationByIdUseCase
import ke.co.smartroundclinic.notification.domain.usecase.GetPushNotificationLogsUseCase
import ke.co.smartroundclinic.notification.domain.usecase.MarkNotificationAsReadUseCase
import ke.co.smartroundclinic.notification.domain.usecase.RegisterDeviceTokenUseCase
import ke.co.smartroundclinic.notification.domain.usecase.SendPushNotificationUseCase
import ke.co.smartroundclinic.notification.domain.usecase.UnregisterDeviceTokenUseCase
import ke.co.smartroundclinic.notification.presentation.dto.request.CreateNotificationReq
import ke.co.smartroundclinic.notification.presentation.dto.request.RegisterDeviceTokenReq
import ke.co.smartroundclinic.notification.presentation.dto.request.SendPushNotificationReq

class NotificationService(
    private val createUseCase: CreateNotificationUseCase,
    private val getByIdUseCase: GetNotificationByIdUseCase,
    private val getMyUseCase: GetMyNotificationsUseCase,
    private val getAllUseCase: GetAllNotificationsUseCase,
    private val markAsReadUseCase: MarkNotificationAsReadUseCase,
    private val deleteUseCase: DeleteNotificationUseCase,
    private val registerDeviceTokenUseCase: RegisterDeviceTokenUseCase,
    private val unregisterDeviceTokenUseCase: UnregisterDeviceTokenUseCase,
    private val sendPushUseCase: SendPushNotificationUseCase,
    private val getPushLogsUseCase: GetPushNotificationLogsUseCase,
) {
    suspend fun create(req: CreateNotificationReq) = createUseCase(req)
    suspend fun getById(id: String) = getByIdUseCase(id)
    suspend fun getMy(userId: String, destination: NotificationDestination, page: Int, size: Int) =
        getMyUseCase(userId, destination, page, size)
    suspend fun getAll(page: Int, size: Int) = getAllUseCase(page, size)
    suspend fun markAsRead(id: String, callerId: String, callerDestination: NotificationDestination) =
        markAsReadUseCase(id, callerId, callerDestination)
    suspend fun delete(id: String) = deleteUseCase(id)

    suspend fun registerDeviceToken(req: RegisterDeviceTokenReq, userId: String, userType: String) =
        registerDeviceTokenUseCase(req, userId, userType)
    suspend fun unregisterDeviceToken(tokenId: String, userId: String) =
        unregisterDeviceTokenUseCase(tokenId, userId)
    suspend fun sendPush(req: SendPushNotificationReq) = sendPushUseCase(req)
    suspend fun getPushLogs(page: Int, size: Int) = getPushLogsUseCase(page, size)
}
