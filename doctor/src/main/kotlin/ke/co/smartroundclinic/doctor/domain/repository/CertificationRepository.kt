package ke.co.smartroundclinic.doctor.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.PractitionerCertificationsEntity

interface CertificationRepository {
    suspend fun add(entity: PractitionerCertificationsEntity): Resource<PractitionerCertificationsEntity>
    suspend fun update(id: String, doctorId: String, name: String?, date: String?): Resource<PractitionerCertificationsEntity?>
    suspend fun delete(id: String, doctorId: String): Resource<Boolean>
    suspend fun getAll(doctorId: String): Resource<List<PractitionerCertificationsEntity>>
    suspend fun getById(id: String, doctorId: String): Resource<PractitionerCertificationsEntity?>
    suspend fun updateUrl(id: String, doctorId: String, url: String?): Resource<PractitionerCertificationsEntity?>
}
