package ke.co.smartroundclinic.common

object MongoDBConstants {

    /**
     * Databases name in the mongodb cluster
     * */
    const val AUTH_DB = "src_auth"
    const val ADMIN_DB = "src_admin"
    const val DOCTOR_DB = "src_doctor"
    const val PATIENT_DB = "src_patient"

    /**
     *  MongoDB Entities for AUTH_DB
     * */
    const val AUTH_USER = "auth_user"
    const val AUTH_REVOKED_TOKENS = "auth_revoked_tokens"

    /**
     * MongoDB Entities for ADMIN_DB
     */
    const val ADMIN_SPECIALITIES = "admin_specialities"
    const val ADMIN_SUBSPECIALITIES = "admin_subspecialities"
    const val ADMIN_KMPDC_PRACTITIONERS = "admin_kmpdc_practitioners"


}