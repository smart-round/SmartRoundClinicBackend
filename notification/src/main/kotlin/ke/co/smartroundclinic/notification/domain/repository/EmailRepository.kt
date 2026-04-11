package ke.co.smartroundclinic.notification.domain.repository

import ke.co.smartroundclinic.notification.domain.model.EmailWithTemplate

interface EmailRepository {
    suspend fun sendEmailWithTemplate(emailWithTemplate: EmailWithTemplate)
}