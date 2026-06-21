package ke.co.smartroundclinic.support.presentation.dto.request

import ke.co.smartroundclinic.support.domain.model.IssueCategory
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

@Serializable
data class CreateIssueCategoryReq(
    val name: String,
    val description: String? = null,
) {
    fun toModel() = IssueCategory(
        id = ObjectId().toString(),
        name = name,
        description = description,
        status = true,
        createdAt = Clock.System.now().toString(),
        updatedAt = null,
    )
}

@Serializable
data class UpdateIssueCategoryReq(
    val name: String? = null,
    val description: String? = null,
    val status: Boolean? = null,
)
