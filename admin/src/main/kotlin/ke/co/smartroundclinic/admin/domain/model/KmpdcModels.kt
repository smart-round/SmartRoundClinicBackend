package ke.co.smartroundclinic.admin.domain.model

import ke.co.smartroundclinic.admin.data.entity.KmpdcPractitionerEntity
import org.bson.types.ObjectId


// ── Result types ─────────────────────────────────────────────────────────────

data class KmpdcRefreshSummary(
    val newRecords: Int,
    val totalScraped: Int,
    val failedRegisters: List<String>,
)

// ── Shared enums ─────────────────────────────────────────────────────────────

enum class KmpdcRegisterType {
    MEDICAL_MASTER, MEDICAL_GP, MEDICAL_REGISTRAR, MEDICAL_SENIOR_REGISTRAR, MEDICAL_SPECIALIST,
    DENTAL_MASTER, DENTAL_GP, DENTAL_REGISTRAR, DENTAL_SENIOR_REGISTRAR, DENTAL_SPECIALIST;

    val practitionerType: PractitionerType
        get() = when (this) {
            DENTAL_MASTER, DENTAL_GP, DENTAL_REGISTRAR,
            DENTAL_SENIOR_REGISTRAR, DENTAL_SPECIALIST -> PractitionerType.DENTAL
            else -> PractitionerType.MEDICAL
        }

    companion object {
        fun fromOrNull(raw: String): KmpdcRegisterType? =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
    }
}

enum class DoctorLicenceStatus {
    ACTIVE, INACTIVE, UNKNOWN;

    companion object {
        fun from(raw: String): DoctorLicenceStatus = when (raw.trim().uppercase()) {
            "ACTIVE"   -> ACTIVE
            "INACTIVE" -> INACTIVE
            else       -> UNKNOWN
        }
    }
}

enum class PractitionerType {
    MEDICAL, DENTAL
}


data class MedicalMasterRecord(
    val id: String = ObjectId().toString(),
    val fullName: String,
    val regNumber: String,
    val address: String,
    val qualifications: String,
    val speciality: String?,        // null = not assigned
    val subSpeciality: String?,     // null = no sub-specialty
    val status: DoctorLicenceStatus,
    val practitionerType: PractitionerType = PractitionerType.MEDICAL,
)

data class MedicalGeneralPractitioner(
    val id: String = ObjectId().toString(),
    val fullName: String,
    val regNumber: String,
    val address: String,
    val qualifications: String,
    val licenceType: String,        // always "General Practice"
    val status: DoctorLicenceStatus,
    val practitionerType: PractitionerType = PractitionerType.MEDICAL,
)

data class MedicalRegistrar(
    val id: String = ObjectId().toString(),
    val fullName: String,
    val regNumber: String,
    val address: String,
    val qualifications: String,
    val discipline: String?,        // training discipline e.g. "Surgery"
    val licenceType: String,        // always "Registrar"
    val status: DoctorLicenceStatus,
    val practitionerType: PractitionerType = PractitionerType.MEDICAL,
)

data class MedicalSeniorRegistrar(
    val id: String = ObjectId().toString(),
    val fullName: String,
    val regNumber: String,
    val address: String,
    val qualifications: String,
    val discipline: String?,        // training discipline
    val licenceType: String,        // always "Senior Registrar"
    val status: DoctorLicenceStatus,
    val practitionerType: PractitionerType = PractitionerType.MEDICAL,
)

data class MedicalSpecialistPractitioner(
    val id: String = ObjectId().toString(),
    val fullName: String,
    val regNumber: String,
    val address: String,
    val qualifications: String,
    val specialty: String?,         // null = not assigned by KMPDC
    val licenceType: String,        // always "Specialist Practice"
    val status: DoctorLicenceStatus,
    val practitionerType: PractitionerType = PractitionerType.MEDICAL,
)

// ═════════════════════════════════════════════════════════════════════════════
// DENTAL DATA CLASSES
// ═════════════════════════════════════════════════════════════════════════════

data class DentalMasterRecord(
    val id: String = ObjectId().toString(),
    val fullName: String,
    val regNumber: String,
    val address: String,
    val qualifications: String,
    val speciality: String?,        // null = not assigned
    val subSpeciality: String?,     // null = no sub-specialty
    val status: DoctorLicenceStatus,
    val practitionerType: PractitionerType = PractitionerType.DENTAL,
)

data class DentalGeneralPractitioner(
    val id: String = ObjectId().toString(),
    val fullName: String,
    val regNumber: String,
    val address: String,
    val qualifications: String,
    val licenceType: String,        // always "General Practice"
    val status: DoctorLicenceStatus,
    val practitionerType: PractitionerType = PractitionerType.DENTAL,
)

data class DentalRegistrar(
    val id: String = ObjectId().toString(),
    val fullName: String,
    val regNumber: String,
    val address: String,
    val qualifications: String,
    val discipline: String?,        // training discipline e.g. "Orthodontics"
    val licenceType: String,        // always "Registrar"
    val status: DoctorLicenceStatus,
    val practitionerType: PractitionerType = PractitionerType.DENTAL,
)

data class DentalSeniorRegistrar(
    val id: String = ObjectId().toString(),
    val fullName: String,
    val regNumber: String,
    val address: String,
    val qualifications: String,
    val discipline: String?,        // training discipline
    val licenceType: String,        // always "Senior Registrar"
    val status: DoctorLicenceStatus,
    val practitionerType: PractitionerType = PractitionerType.DENTAL,
)

data class DentalSpecialistPractitioner(
    val id: String = ObjectId().toString(),
    val fullName: String,
    val regNumber: String,
    val address: String,
    val qualifications: String,
    val specialty: String?,         // null = not assigned by KMPDC
    val licenceType: String,        // always "Specialist Practice"
    val status: DoctorLicenceStatus,
    val practitionerType: PractitionerType = PractitionerType.DENTAL,
)

// ═════════════════════════════════════════════════════════════════════════════
// toEntity() MAPPERS
// ═════════════════════════════════════════════════════════════════════════════

fun MedicalMasterRecord.toEntity() = KmpdcPractitionerEntity(
    fullName = fullName, regNumber = regNumber, address = address,
    qualifications = qualifications, status = status.name,
    practitionerType = practitionerType.name,
    registerType = KmpdcRegisterType.MEDICAL_MASTER.name,
    speciality = speciality, subSpeciality = subSpeciality,
)

fun MedicalGeneralPractitioner.toEntity() = KmpdcPractitionerEntity(
    fullName = fullName, regNumber = regNumber, address = address,
    qualifications = qualifications, status = status.name,
    practitionerType = practitionerType.name,
    registerType = KmpdcRegisterType.MEDICAL_GP.name,
    licenceType = licenceType,
)

fun MedicalRegistrar.toEntity() = KmpdcPractitionerEntity(
    fullName = fullName, regNumber = regNumber, address = address,
    qualifications = qualifications, status = status.name,
    practitionerType = practitionerType.name,
    registerType = KmpdcRegisterType.MEDICAL_REGISTRAR.name,
    discipline = discipline, licenceType = licenceType,
)

fun MedicalSeniorRegistrar.toEntity() = KmpdcPractitionerEntity(
    fullName = fullName, regNumber = regNumber, address = address,
    qualifications = qualifications, status = status.name,
    practitionerType = practitionerType.name,
    registerType = KmpdcRegisterType.MEDICAL_SENIOR_REGISTRAR.name,
    discipline = discipline, licenceType = licenceType,
)

fun MedicalSpecialistPractitioner.toEntity() = KmpdcPractitionerEntity(
    fullName = fullName, regNumber = regNumber, address = address,
    qualifications = qualifications, status = status.name,
    practitionerType = practitionerType.name,
    registerType = KmpdcRegisterType.MEDICAL_SPECIALIST.name,
    specialty = specialty, licenceType = licenceType,
)

fun DentalMasterRecord.toEntity() = KmpdcPractitionerEntity(
    fullName = fullName, regNumber = regNumber, address = address,
    qualifications = qualifications, status = status.name,
    practitionerType = practitionerType.name,
    registerType = KmpdcRegisterType.DENTAL_MASTER.name,
    speciality = speciality, subSpeciality = subSpeciality,
)

fun DentalGeneralPractitioner.toEntity() = KmpdcPractitionerEntity(
    fullName = fullName, regNumber = regNumber, address = address,
    qualifications = qualifications, status = status.name,
    practitionerType = practitionerType.name,
    registerType = KmpdcRegisterType.DENTAL_GP.name,
    licenceType = licenceType,
)

fun DentalRegistrar.toEntity() = KmpdcPractitionerEntity(
    fullName = fullName, regNumber = regNumber, address = address,
    qualifications = qualifications, status = status.name,
    practitionerType = practitionerType.name,
    registerType = KmpdcRegisterType.DENTAL_REGISTRAR.name,
    discipline = discipline, licenceType = licenceType,
)

fun DentalSeniorRegistrar.toEntity() = KmpdcPractitionerEntity(
    fullName = fullName, regNumber = regNumber, address = address,
    qualifications = qualifications, status = status.name,
    practitionerType = practitionerType.name,
    registerType = KmpdcRegisterType.DENTAL_SENIOR_REGISTRAR.name,
    discipline = discipline, licenceType = licenceType,
)

fun DentalSpecialistPractitioner.toEntity() = KmpdcPractitionerEntity(
    fullName = fullName, regNumber = regNumber, address = address,
    qualifications = qualifications, status = status.name,
    practitionerType = practitionerType.name,
    registerType = KmpdcRegisterType.DENTAL_SPECIALIST.name,
    specialty = specialty, licenceType = licenceType,
)