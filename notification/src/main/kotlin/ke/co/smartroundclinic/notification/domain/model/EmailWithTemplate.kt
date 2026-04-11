package ke.co.smartroundclinic.notification.domain.model

import com.resend.Resend
import com.resend.services.emails.model.CreateEmailOptions
import kotlinx.serialization.Serializable

@Serializable
data class EmailWithTemplate(
    val from: String,
    val to: String,
    val subject: String? = null,
    val template:Template,
){

}
@Serializable
data class Template(
    val id: String,
    val variables: Map<String, String>,
)


