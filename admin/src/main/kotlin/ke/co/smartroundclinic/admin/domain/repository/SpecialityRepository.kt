package ke.co.smartroundclinic.admin.domain.repository

import ke.co.smartroundclinic.admin.data.entity.SpecialityEntity
import ke.co.smartroundclinic.admin.data.entity.SubspecialtyEntity
import ke.co.smartroundclinic.admin.domain.model.ServiceTier
import ke.co.smartroundclinic.common.Resource

interface SpecialityRepository {
    suspend fun createSpeciality(specialities:List<SpecialityEntity>): Resource<Nothing>
    suspend fun updateSpeciality(
        id:String,
        title: String?,
        serviceTierId: String?,
        description: String?,
        color: String?,
        iconUrl: String?
    ): Resource<Nothing>
    suspend fun getSpecialities(): Resource<List<SpecialityEntity>>
    suspend fun getSpecialityById(id:String): Resource<SpecialityEntity?>
    suspend fun getSpecialityByTitle(title:String): Resource<SpecialityEntity?>

    suspend fun getSubSpecialities(specialityId: String): Resource<List<SubspecialtyEntity>>
    suspend fun getSubSpeciality(id: String): Resource<SubspecialtyEntity?>
    suspend fun createSubSpeciality(specialityId: String, subspecialtyEntity: SubspecialtyEntity): Resource<SubspecialtyEntity>
    suspend fun updateSubSpeciality(id: String, title: String?, description: String?,color: String?, iconUrl: String?): Resource<SubspecialtyEntity>
    suspend fun updateSpecialityIcon(id: String, iconUrl: String?): Resource<SpecialityEntity?>
    suspend fun updateSubSpecialityIcon(id: String, iconUrl: String?): Resource<SubspecialtyEntity?>
    suspend fun deleteSpeciality(id: String): Resource<Nothing>
    suspend fun deleteSubSpeciality(id: String): Resource<Nothing>
    suspend fun deleteAllSubSpecialityBySpecialityId(specialityId: String): Resource<Nothing>
    suspend fun deleteSpecialityWithSubSpecialities(id: String): Resource<Nothing>
    suspend fun assignToServiceTier(specialityId: String, serviceTierId: String): Resource<Nothing>
    suspend fun unassignFromServiceTier(specialityId: String): Resource<Nothing>
    suspend fun assignToServiceCategory(specialityId: String, serviceCategoryId: String): Resource<Nothing>
    suspend fun unassignFromServiceCategory(specialityId: String): Resource<Nothing>
}