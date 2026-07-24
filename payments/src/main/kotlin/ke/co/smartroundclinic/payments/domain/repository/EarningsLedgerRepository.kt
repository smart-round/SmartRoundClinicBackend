package ke.co.smartroundclinic.payments.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.EarningsLedgerEntity
import ke.co.smartroundclinic.payments.data.entity.EarningsLedgerLeg

interface EarningsLedgerRepository {
    /** Idempotent upsert (keyed by paymentId) — call only after the leg's IntaSend transfer succeeds.
     *  Stamps the leg's credited-at with the current time. */
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

    /** Same idempotent upsert, but stamps the leg with a caller-supplied historical timestamp —
     *  for one-time backfill of payments that were already credited before this ledger existed.
     *  Never touches a leg the live [recordLegCredited] path could also target, since a payment's
     *  leg is either already-credited (backfill-only) or not-yet-credited (live-only), never both. */
    suspend fun backfillLegCredited(
        leg: EarningsLedgerLeg,
        paymentId: String,
        appointmentId: String?,
        doctorId: String,
        grossAmount: Double,
        commissionRate: Double,
        commissionAmount: Double,
        netAmount: Double,
        creditedAt: String,
    ): Resource<EarningsLedgerEntity>

    suspend fun getByDoctorId(doctorId: String): Resource<List<EarningsLedgerEntity>>
    suspend fun getAllForAdmin(): Resource<List<EarningsLedgerEntity>>
    suspend fun getAll(page: Int, size: Int): Resource<Pair<List<EarningsLedgerEntity>, Long>>
}
