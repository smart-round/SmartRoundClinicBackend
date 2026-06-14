package ke.co.smartroundclinic.article.domain.usecase.article

import ke.co.smartroundclinic.article.domain.repository.ArticleRepository
import ke.co.smartroundclinic.article.presentation.dto.response.ArticleRes
import ke.co.smartroundclinic.article.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.Resource

class DeleteArticleUseCase(
    private val repository: ArticleRepository,
    private val notificationSender: NotificationSender? = null,
) {
    suspend operator fun invoke(id: String): DefaultResponse<ArticleRes?> {
        val result = repository.delete(id)
        if (result is Resource.Success) {
            result.data?.let { entity ->
                runCatching {
                    notificationSender?.send(
                        title = PushNotificationEvents.ARTICLE_DELETED,
                        message = "Your article \"${entity.title}\" has been removed by an administrator.",
                        channel = NotificationChannel.PUSH_NOTIFICATION,
                        destination = NotificationDestination.DOCTOR,
                        recipientId = entity.doctorId,
                    )
                }
            }
        }
        return result.toDefaultResponse { it?.toModel()?.toRes() }
    }
}
