package ke.co.smartroundclinic.admin.presentation.dto.request

import ke.co.smartroundclinic.admin.domain.model.ServiceCategory
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

@Serializable
data class CreateServiceCategoryReq(val name: String) {
    fun toModel() = ServiceCategory(
        id = ObjectId().toString(),
        name = name,
        createdAt = Clock.System.now().toString(),
        updatedAt = null,
    )
}

@Serializable
data class UpdateServiceCategoryReq(val name: String? = null)
