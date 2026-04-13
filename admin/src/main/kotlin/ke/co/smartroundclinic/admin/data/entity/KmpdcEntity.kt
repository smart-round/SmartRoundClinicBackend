package ke.co.smartroundclinic.admin.data.entity

import org.bson.types.ObjectId

data class KmpdcPractitionerEntity(
    val id: String = ObjectId().toString(),
    val fullName: String,
    val regNumber: String,
    val address: String,
    val qualifications: String,
    val status: String,           // DoctorLicenceStatus.name
    val practitionerType: String, // PractitionerType.name (MEDICAL / DENTAL)
    val registerType: String,     // KmpdcRegisterType.name
    val speciality: String? = null,
    val subSpeciality: String? = null,
    val licenceType: String? = null,
    val discipline: String? = null,
    val specialty: String? = null,
    val lastRefreshedAt: Long = System.currentTimeMillis(),
)