package ke.co.smartroundclinic.doctor.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.PractitionerLicenceEntity

interface PractitionerLicenceRepository {
    suspend fun add(entity: PractitionerLicenceEntity): Resource<PractitionerLicenceEntity>
    suspend fun update(id: String, doctorId: String, licenceName: String?): Resource<PractitionerLicenceEntity?>
    suspend fun delete(id: String, doctorId: String): Resource<Boolean>
    suspend fun getAll(doctorId: String): Resource<List<PractitionerLicenceEntity>>
    suspend fun getAllPaged(page: Int, size: Int): Resource<Pair<List<PractitionerLicenceEntity>, Long>>
    suspend fun getById(id: String, doctorId: String): Resource<PractitionerLicenceEntity?>
    suspend fun getByIdDoctorId(doctorId: String): Resource<PractitionerLicenceEntity?>
    suspend fun updateUrl(id: String, doctorId: String, url: String?): Resource<PractitionerLicenceEntity?>
}
