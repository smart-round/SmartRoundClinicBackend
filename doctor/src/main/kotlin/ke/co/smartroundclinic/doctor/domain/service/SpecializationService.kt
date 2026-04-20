package ke.co.smartroundclinic.doctor.domain.service

import ke.co.smartroundclinic.doctor.domain.model.Specialization
import ke.co.smartroundclinic.doctor.domain.usecase.specialization.AddSpecializationUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.specialization.GetMySpecializationsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.specialization.GetMySpecializationsWithDetailsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.specialization.RemoveSpecializationUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.specialization.UpdateSpecializationUseCase

class SpecializationService(
    private val addUseCase: AddSpecializationUseCase,
    private val removeUseCase: RemoveSpecializationUseCase,
    private val getMyUseCase: GetMySpecializationsUseCase,
    private val updateUseCase: UpdateSpecializationUseCase,
    private val getMyWithDetailsUseCase: GetMySpecializationsWithDetailsUseCase,
) {
    suspend fun add(model: Specialization) = addUseCase(model)
    suspend fun remove(id: String, doctorId: String) = removeUseCase(id, doctorId)
    suspend fun getMy(doctorId: String) = getMyUseCase(doctorId)
    suspend fun update(id: String, doctorId: String, specializationId: String, subSpecializationId: String?) =
        updateUseCase(id, doctorId, specializationId, subSpecializationId)
    suspend fun getMyWithDetails(doctorId: String) = getMyWithDetailsUseCase(doctorId)
}