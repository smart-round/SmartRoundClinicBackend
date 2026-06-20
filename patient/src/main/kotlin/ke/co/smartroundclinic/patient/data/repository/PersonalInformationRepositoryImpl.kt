package ke.co.smartroundclinic.patient.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.patient.data.entity.HeightIn
import ke.co.smartroundclinic.patient.data.entity.MaritalStatus
import ke.co.smartroundclinic.patient.data.entity.PersonalInformationEntity
import ke.co.smartroundclinic.patient.data.entity.WeightIn
import ke.co.smartroundclinic.patient.domain.repository.PersonalInformationRepository
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.PatientProfile
import ke.co.smartroundclinic.common.PatientProfileResolver
import ke.co.smartroundclinic.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.conversions.Bson
import java.time.Instant

class PersonalInformationRepositoryImpl(database: MongoDatabase) : PersonalInformationRepository, PatientProfileResolver {

    private val collection = database.getCollection<PersonalInformationEntity>(
        MongoDBConstants.PATIENT_PERSONAL_INFORMATION
    )

    override suspend fun create(entity: PersonalInformationEntity): Resource<PersonalInformationEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val existing = collection
                    .find(Filters.eq(PersonalInformationEntity::patientId.name, entity.patientId))
                    .firstOrNull()
                if (existing != null)
                    return@withContext Resource.Error("Personal information already exists for this patient")
                collection.insertOne(entity)
                Resource.Success(data = entity, message = "Personal information saved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to save personal information")
            }
        }

    override suspend fun getByPatientId(patientId: String): Resource<PersonalInformationEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = collection
                    .find(Filters.eq(PersonalInformationEntity::patientId.name, patientId))
                    .firstOrNull()
                    ?: return@withContext Resource.Error("Personal information not found")
                Resource.Success(data = entity, message = "Personal information retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve personal information")
            }
        }

    override suspend fun getAll(): Resource<List<PersonalInformationEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val entities = collection.find().toList()
                Resource.Success(data = entities, message = "Personal information retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve personal information")
            }
        }

    override suspend fun getProfilesByPatientIds(patientIds: List<String>): Map<String, PatientProfile> =
        withContext(Dispatchers.IO) {
            if (patientIds.isEmpty()) return@withContext emptyMap()
            try {
                collection
                    .find(Filters.`in`(PersonalInformationEntity::patientId.name, patientIds))
                    .toList()
                    .associate { entity ->
                        entity.patientId to PatientProfile(
                            id = entity.id,
                            patientId = entity.patientId,
                            gender = entity.gender.name,
                            phoneNumber = entity.phoneNumber,
                            countryCode = entity.countryCode,
                            bloodGroup = entity.bloodGroup,
                            dateOfBirth = entity.dateOfBirth,
                            weight = entity.weight,
                            weightIn = entity.weightIn?.name,
                            height = entity.height,
                            heightIn = entity.heightIn?.name,
                            maritalStatus = entity.maritalStatus?.name,
                            allergies = entity.allergies ?: emptyList(),
                            chronicConditions = entity.chronicConditions ?: emptyList(),
                            currentMedications = entity.currentMedications ?: emptyList(),
                            createdAt = entity.createdAt,
                            updatedAt = entity.updatedAt,
                        )
                    }
            } catch (e: Exception) {
                emptyMap()
            }
        }

    override suspend fun update(
        patientId: String,
        phoneNumber: String?,
        countryCode: String?,
        bloodGroup: String?,
        dateOfBirth: String?,
        weight: Double?,
        weightIn: String?,
        height: Double?,
        heightIn: String?,
        maritalStatus: String?,
        allergies: List<String>?,
        chronicConditions: List<String>?,
        currentMedications: List<String>?,
    ): Resource<PersonalInformationEntity?> = withContext(Dispatchers.IO) {
        try {
            collection.find(Filters.eq(PersonalInformationEntity::patientId.name, patientId)).firstOrNull()
                ?: return@withContext Resource.Error("Personal information not found")

            val updates = mutableListOf<Bson>()
            phoneNumber?.takeIf { it.isNotBlank() }
                ?.let { updates.add(Updates.set(PersonalInformationEntity::phoneNumber.name, it)) }
            countryCode?.takeIf { it.isNotBlank() }
                ?.let { updates.add(Updates.set(PersonalInformationEntity::countryCode.name, it)) }
            bloodGroup?.takeIf { it.isNotBlank() }
                ?.let { updates.add(Updates.set(PersonalInformationEntity::bloodGroup.name, it)) }
            dateOfBirth?.takeIf { it.isNotBlank() }
                ?.let { updates.add(Updates.set(PersonalInformationEntity::dateOfBirth.name, it)) }
            weight?.let { updates.add(Updates.set(PersonalInformationEntity::weight.name, it)) }
            weightIn?.let { runCatching { WeightIn.valueOf(it) }.getOrNull() }
                ?.let { updates.add(Updates.set(PersonalInformationEntity::weightIn.name, it.name)) }
            height?.let { updates.add(Updates.set(PersonalInformationEntity::height.name, it)) }
            heightIn?.let { runCatching { HeightIn.valueOf(it) }.getOrNull() }
                ?.let { updates.add(Updates.set(PersonalInformationEntity::heightIn.name, it.name)) }
            maritalStatus?.let { runCatching { MaritalStatus.valueOf(it) }.getOrNull() }
                ?.let { updates.add(Updates.set(PersonalInformationEntity::maritalStatus.name, it.name)) }
            allergies?.let { updates.add(Updates.set(PersonalInformationEntity::allergies.name, it)) }
            chronicConditions?.let { updates.add(Updates.set(PersonalInformationEntity::chronicConditions.name, it)) }
            currentMedications?.let { updates.add(Updates.set(PersonalInformationEntity::currentMedications.name, it)) }

            if (updates.isEmpty()) {
                val current = collection
                    .find(Filters.eq(PersonalInformationEntity::patientId.name, patientId))
                    .firstOrNull()
                return@withContext Resource.Success(data = current, message = "No changes detected")
            }

            updates.add(Updates.set(PersonalInformationEntity::updatedAt.name, Instant.now().toString()))
            collection.updateOne(
                Filters.eq(PersonalInformationEntity::patientId.name, patientId),
                Updates.combine(updates),
            )
            val updated = collection
                .find(Filters.eq(PersonalInformationEntity::patientId.name, patientId))
                .firstOrNull()
            Resource.Success(data = updated, message = "Personal information updated successfully")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update personal information")
        }
    }
}
