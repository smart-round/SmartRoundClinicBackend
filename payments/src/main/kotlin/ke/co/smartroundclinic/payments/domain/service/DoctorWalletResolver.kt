package ke.co.smartroundclinic.payments.domain.service

import ke.co.smartroundclinic.common.DoctorWalletProvisioner
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.lookup.DoctorPaymentDetailsLookup
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CreateWalletReq
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import org.slf4j.LoggerFactory

/**
 * Resolves/provisions a doctor's IntaSend wallet id. Implements [DoctorWalletProvisioner] so
 * `:doctor`'s AddPaymentDetailsUseCase can provision a wallet for a brand-new payment-details
 * record via the cross-module interface (no Gradle dependency on :payments), while [resolve] is
 * used internally within :payments to lazily backfill a wallet for doctors whose payment details
 * predate wallet support.
 */
class DoctorWalletResolver(
    private val intaSendRepository: IntaSendRepository,
    private val doctorPaymentDetailsLookup: DoctorPaymentDetailsLookup,
) : DoctorWalletProvisioner {
    private val log = LoggerFactory.getLogger(DoctorWalletResolver::class.java)

    override suspend fun provisionWallet(doctorId: String): String? = createWallet(doctorId)

    suspend fun resolve(doctorId: String): String? {
        val existing = doctorPaymentDetailsLookup.getByDoctorId(doctorId)?.walletId
        if (existing != null) return existing

        val walletId = createWallet(doctorId) ?: return null
        doctorPaymentDetailsLookup.setWalletId(doctorId, walletId)
        return walletId
    }

    private suspend fun createWallet(doctorId: String): String? {
        val created = intaSendRepository.createWallet(
            CreateWalletReq(label = "doctor-$doctorId", canDisburse = true)
        )
        val wallet = (created as? Resource.Success)?.data
        if (wallet == null) {
            log.error("createWallet doctorId=$doctorId — wallet provisioning failed: ${(created as? Resource.Error)?.message}")
            return null
        }
        return wallet.walletId
    }
}
