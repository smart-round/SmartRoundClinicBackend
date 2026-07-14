package ke.co.smartroundclinic.payments.domain.service

import ke.co.smartroundclinic.common.DoctorWalletProvisioner
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.lookup.DoctorPaymentDetailsLookup
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CreateWalletReq
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import org.slf4j.LoggerFactory

/**
 * Resolves/provisions a doctor's IntaSend wallet id — a doctor gets exactly one, ever.
 * [provisionWallet] (via [DoctorWalletProvisioner], consumed by `:doctor`'s
 * AddPaymentDetailsUseCase across the cross-module interface) and [resolve] (used internally
 * within :payments to lazily backfill a wallet for doctors whose payment details predate wallet
 * support) both reuse an existing valid walletId if one is on file before ever creating a new
 * one — neither will mint a duplicate wallet for a doctor who already has one.
 */
class DoctorWalletResolver(
    private val intaSendRepository: IntaSendRepository,
    private val doctorPaymentDetailsLookup: DoctorPaymentDetailsLookup,
    private val config: IntaSendConfig,
) : DoctorWalletProvisioner {
    private val log = LoggerFactory.getLogger(DoctorWalletResolver::class.java)

    override suspend fun provisionWallet(doctorId: String): String? =
        validExistingWalletId(doctorId) ?: createWallet(doctorId)

    suspend fun resolve(doctorId: String): String? {
        validExistingWalletId(doctorId)?.let { return it }
        val walletId = createWallet(doctorId) ?: return null
        doctorPaymentDetailsLookup.setWalletId(doctorId, walletId)
        return walletId
    }

    /**
     * Returns the doctor's stored walletId if it's a real, usable per-doctor wallet — null if
     * there's no payment-details record yet, or if the stored value is stale data pointing at a
     * shared platform wallet (never reuse that; a fresh one gets created and persisted instead).
     */
    private suspend fun validExistingWalletId(doctorId: String): String? {
        val existing = doctorPaymentDetailsLookup.getByDoctorId(doctorId)?.walletId ?: return null
        if (existing == config.collectionsWalletId || existing == config.commissionWalletId) {
            log.error(
                "doctorId=$doctorId — stored walletId=$existing is a shared platform wallet " +
                "(collections/commission), not a doctor wallet — re-provisioning"
            )
            return null
        }
        return existing
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
