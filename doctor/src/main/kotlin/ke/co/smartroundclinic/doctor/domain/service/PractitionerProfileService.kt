package ke.co.smartroundclinic.doctor.domain.service

import ke.co.smartroundclinic.doctor.domain.model.PractitionerProfile
import ke.co.smartroundclinic.doctor.domain.model.PractitionerProfileUpdate
import ke.co.smartroundclinic.doctor.domain.usecase.profile.CreatePractitionerProfileUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.profile.GetMyProfileUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.profile.GetPractitionerProfileByIdUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.profile.GetPractitionerProfilesUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.profile.GetPractitionerProfileWithSpecializationsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.profile.UpdatePractitionerProfileUseCase

class PractitionerProfileService(
    private val createUseCase: CreatePractitionerProfileUseCase,
    private val updateUseCase: UpdatePractitionerProfileUseCase,
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val getByIdUseCase: GetPractitionerProfileByIdUseCase,
    private val getAllUseCase: GetPractitionerProfilesUseCase,
    private val getByIdWithSpecializationsUseCase: GetPractitionerProfileWithSpecializationsUseCase,
) {
    suspend fun create(model: PractitionerProfile) = createUseCase(model)
    suspend fun update(doctorId: String, update: PractitionerProfileUpdate) = updateUseCase(doctorId, update)
    suspend fun getMyProfile(doctorId: String) = getMyProfileUseCase(doctorId)
    suspend fun getById(id: String) = getByIdUseCase(id)
    suspend fun getAll(page: Int, size: Int) = getAllUseCase(page, size)
    suspend fun getByIdWithSpecializations(id: String) = getByIdWithSpecializationsUseCase(id)
}