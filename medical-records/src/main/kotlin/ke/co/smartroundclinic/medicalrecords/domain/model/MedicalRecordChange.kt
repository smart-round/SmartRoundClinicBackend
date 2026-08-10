package ke.co.smartroundclinic.medicalrecords.domain.model

/**
 * The record fields a patient is told about when a doctor revises an existing record.
 *
 * `summary` and `additionalNotes` are deliberately absent: a doctor reworking their own prose
 * should not ping the patient. Their latest text still rides along on the next card that does get
 * posted, since every card carries the whole record.
 */
enum class MedicalRecordField {
    DIAGNOSIS,
    PRESCRIPTION,
    LAB_REQUESTS,
    REFERRAL_NOTE,
}

/**
 * Outcome of an upsert. [isUpdate] separates a revision from a first save, and [changedFields]
 * lists which notifiable fields actually differ from what was stored — empty on a first save
 * (there is nothing to have changed) and on an edit that only touched prose.
 */
data class MedicalRecordSaveResult(
    val record: ke.co.smartroundclinic.medicalrecords.data.entity.MedicalRecordEntity,
    val isUpdate: Boolean,
    val changedFields: Set<MedicalRecordField> = emptySet(),
)
