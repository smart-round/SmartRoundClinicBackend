package ke.co.smartroundclinic.payments.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.EarningsLedgerEntity
import ke.co.smartroundclinic.payments.data.entity.EarningsLedgerLeg

interface EarningsLedgerRepository {
    /** Idempotent upsert (keyed by paymentId) — call only after the leg's IntaSend transfer succeeds. */
    suspend fun recordLegCredited(
        leg: EarningsLedgerLeg,
        paymentId: String,
        appointmentId: String?,
        doctorId: String,
        grossAmount: Double,
        commissionRate: Double,
        commissionAmount: Double,
        netAmount: Double,
    ): Resource<EarningsLedgerEntity>

    suspend fun getByDoctorId(doctorId: String): Resource<List<EarningsLedgerEntity>>
    suspend fun getAllForAdmin(): Resource<List<EarningsLedgerEntity>>
    suspend fun getAll(page: Int, size: Int): Resource<Pair<List<EarningsLedgerEntity>, Long>>
}
