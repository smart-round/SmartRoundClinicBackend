package ke.co.smartroundclinic.doctor.domain.service

import ke.co.smartroundclinic.doctor.domain.model.Specialization
import ke.co.smartroundclinic.doctor.domain.usecase.specialization.AddSpecializationUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.specialization.GetMySpecializationsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.specialization.RemoveSpecializationUseCase

class SpecializationService(
    private val addUseCase: AddSpecializationUseCase,
    private val removeUseCase: RemoveSpecializationUseCase,
    private val getMyUseCase: GetMySpecializationsUseCase,
) {
    suspend fun add(model: Specialization) = addUseCase(model)
    suspend fun remove(id: String, doctorId: String) = removeUseCase(id, doctorId)
    suspend fun getMy(doctorId: String) = getMyUseCase(doctorId)
}