package ke.co.smartroundclinic.doctor.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.PractitionerProfileEntity
import ke.co.smartroundclinic.doctor.domain.model.PractitionerProfileUpdate
import ke.co.smartroundclinic.doctor.presentation.dto.response.PractitionerProfileWithSpecializationsRes

interface PractitionerProfileRepository {

    suspend fun create(entity: PractitionerProfileEntity): Resource<PractitionerProfileEntity>

    suspend fun update(doctorId: String, update: PractitionerProfileUpdate): Resource<PractitionerProfileEntity?>

    suspend fun getByDoctorId(doctorId: String): Resource<PractitionerProfileEntity?>

    suspend fun getById(id: String): Resource<PractitionerProfileEntity?>

    suspend fun getAll(page: Int, size: Int): Resource<Pair<List<PractitionerProfileEntity>, Long>>

    suspend fun getByIdWithSpecializations(id: String): Resource<PractitionerProfileWithSpecializationsRes?>

}
