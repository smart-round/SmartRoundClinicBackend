package ke.co.smartroundclinic.admin.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.admin.data.entity.SpecialityEntity
import ke.co.smartroundclinic.admin.data.entity.SubspecialtyEntity
import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.bson.conversions.Bson

class SpecialityRepositoryImpl(database: MongoDatabase) : SpecialityRepository {

    private val specialities = database.getCollection<SpecialityEntity>(MongoDBConstants.ADMIN_SPECIALITIES)
    private val subSpecialities = database.getCollection<SubspecialtyEntity>(MongoDBConstants.ADMIN_SUBSPECIALITIES)

    override suspend fun createSpeciality(specialities: List<SpecialityEntity>): Resource<Nothing> =
        withContext(Dispatchers.IO) {
            try {
                val duplicates = specialities.filter { entity ->
                    this@SpecialityRepositoryImpl.specialities
                        .find(Filters.regex(SpecialityEntity::title.name, "^${entity.title}$", "i"))
                        .firstOrNull() != null
                }.map { it.title }

                if (duplicates.isNotEmpty()) {
                    return@withContext Resource.Error("Specialit${if (duplicates.size > 1) "ies" else "y"} already exist: ${duplicates.joinToString()}")
                }

                this@SpecialityRepositoryImpl.specialities.insertMany(specialities)
                Resource.Success(data = null, message = "Specialit${if (specialities.size > 1) "ies" else "y"} created successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to create speciality")
            }
        }

    override suspend fun updateSpeciality(
        id: String,
        title: String?,
        description: String?,
        color: String?,
        iconUrl: String?,
    ): Resource<Nothing> = withContext(Dispatchers.IO) {
        try {
            val existing = specialities.find(Filters.eq(SpecialityEntity::id.name, id)).firstOrNull()
                ?: return@withContext Resource.Error("Speciality not found")

            val updates = mutableListOf<Bson>()
            title?.trim()?.takeIf { it.isNotBlank() && it != existing.title }
                ?.let { updates.add(Updates.set(SpecialityEntity::title.name, it)) }
            description?.trim()?.takeIf { it.isNotBlank() && it != existing.description }
                ?.let { updates.add(Updates.set(SpecialityEntity::description.name, it)) }
            color?.trim()?.takeIf { it.isNotBlank() && it != existing.color }
                ?.let { updates.add(Updates.set(SpecialityEntity::color.name, it)) }
            iconUrl?.trim()?.takeIf { it != existing.iconUrl }
                ?.let { updates.add(Updates.set(SpecialityEntity::iconUrl.name, it)) }

            if (updates.isEmpty()) return@withContext Resource.Success(data = null, message = "No changes detected")

            specialities.updateOne(Filters.eq(SpecialityEntity::id.name, id), Updates.combine(updates))
            Resource.Success(data = null, message = "Speciality updated successfully")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update speciality")
        }
    }

    override suspend fun getSpecialities(): Resource<List<SpecialityEntity>> =
        withContext(Dispatchers.IO) {
            try {
                Resource.Success(data = specialities.find().toList(), message = "Specialities retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve specialities")
            }
        }

    override suspend fun getSpecialityById(id: String): Resource<SpecialityEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = specialities.find(Filters.eq(SpecialityEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Speciality not found")
                Resource.Success(data = entity, message = "Speciality retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve speciality")
            }
        }

    override suspend fun getSpecialityByTitle(title: String): Resource<SpecialityEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = specialities
                    .find(Filters.regex(SpecialityEntity::title.name, "^$title$", "i"))
                    .firstOrNull()
                    ?: return@withContext Resource.Error("Speciality not found")
                Resource.Success(data = entity, message = "Speciality retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve speciality")
            }
        }

    override suspend fun getSubSpecialities(specialityId: String): Resource<List<SubspecialtyEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val list = subSpecialities
                    .find(Filters.eq(SubspecialtyEntity::specialityId.name, specialityId))
                    .toList()
                Resource.Success(data = list, message = "Subspecialities retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve subspecialities")
            }
        }

    override suspend fun getSubSpeciality(id: String): Resource<SubspecialtyEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = subSpecialities.find(Filters.eq(SubspecialtyEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Subspeciality not found")
                Resource.Success(data = entity, message = "Subspeciality retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve subspeciality")
            }
        }

    override suspend fun createSubSpeciality(
        specialityId: String,
        subspecialtyEntity: SubspecialtyEntity,
    ): Resource<SubspecialtyEntity> = withContext(Dispatchers.IO) {
        try {
            specialities.find(Filters.eq(SpecialityEntity::id.name, specialityId)).firstOrNull()
                ?: return@withContext Resource.Error("Parent speciality not found")

            subSpecialities.insertOne(subspecialtyEntity)
            Resource.Success(data = subspecialtyEntity, message = "Subspeciality created successfully")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to create subspeciality")
        }
    }

    override suspend fun updateSubSpeciality(
        id: String,
        title: String?,
        description: String?,
        color: String?,
        iconUrl: String?,
    ): Resource<SubspecialtyEntity> = withContext(Dispatchers.IO) {
        try {
            val existing = subSpecialities.find(Filters.eq(SubspecialtyEntity::id.name, id)).firstOrNull()
                ?: return@withContext Resource.Error("Subspeciality not found")

            val updates = mutableListOf<Bson>()
            title?.trim()?.takeIf { it.isNotBlank() && it != existing.title }
                ?.let { updates.add(Updates.set(SubspecialtyEntity::title.name, it)) }
            description?.trim()?.takeIf { it.isNotBlank() && it != existing.description }
                ?.let { updates.add(Updates.set(SubspecialtyEntity::description.name, it)) }
            color?.trim()?.takeIf { it.isNotBlank() && it != existing.color }
                ?.let { updates.add(Updates.set(SubspecialtyEntity::color.name, it)) }
            iconUrl?.trim()?.takeIf { it != existing.iconUrl }
                ?.let { updates.add(Updates.set(SubspecialtyEntity::iconUrl.name, it)) }

            if (updates.isEmpty()) return@withContext Resource.Success(data = existing, message = "No changes detected")

            subSpecialities.updateOne(Filters.eq(SubspecialtyEntity::id.name, id), Updates.combine(updates))
            val updated = subSpecialities.find(Filters.eq(SubspecialtyEntity::id.name, id)).firstOrNull()!!
            Resource.Success(data = updated, message = "Subspeciality updated successfully")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update subspeciality")
        }
    }

    override suspend fun deleteSubSpeciality(id: String): Resource<Nothing> =
        withContext(Dispatchers.IO) {
            try {
                val result = subSpecialities.deleteOne(Filters.eq(SubspecialtyEntity::id.name, id))
                if (result.deletedCount == 0L) return@withContext Resource.Error("Subspeciality not found")
                Resource.Success(data = null, message = "Subspeciality deleted successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to delete subspeciality")
            }
        }
}
