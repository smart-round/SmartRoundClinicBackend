package ke.co.smartroundclinic.admin.data.entity

import ke.co.smartroundclinic.admin.domain.model.ServiceCategory
import org.bson.types.ObjectId
import kotlin.time.Clock

data class ServiceCategoryEntity(
    val id: String = ObjectId().toString(),
    val name: String,
    val createdAtString: String = Clock.System.now().toString(),
    val updatedAtString: String? = null,
) {
    fun toModel() = ServiceCategory(
        id = id,
        name = name,
        createdAt = createdAtString,
        updatedAt = updatedAtString,
    )
}

fun ServiceCategory.toEntity() = ServiceCategoryEntity(
    id = id,
    name = name,
    createdAtString = createdAt,
    updatedAtString = updatedAt,
)
