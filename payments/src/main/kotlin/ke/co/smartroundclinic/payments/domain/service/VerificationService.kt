package ke.co.smartroundclinic.payments.domain.service

import ke.co.smartroundclinic.payments.domain.usecase.verification.ResolveCardBinUseCase

class VerificationService(
    private val resolveCardBinUseCase: ResolveCardBinUseCase,
) {
    suspend fun resolveCardBin(bin: String) = resolveCardBinUseCase(bin)
}
