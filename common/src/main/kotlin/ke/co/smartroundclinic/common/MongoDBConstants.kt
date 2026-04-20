package ke.co.smartroundclinic.common

object MongoDBConstants {

    /**
     * Databases name in the mongodb cluster
     * */
    const val AUTH_DB = "src_auth"
    const val ADMIN_DB = "src_admin"
    const val DOCTOR_DB = "src_doctor"
    const val PATIENT_DB = "src_patient"
    const val PATIENT_PERSONAL_INFORMATION = "patient_personal_information"

    /**
     *  MongoDB Entities for AUTH_DB
     * */
    const val AUTH_USER = "auth_user"
    const val AUTH_REVOKED_TOKENS = "auth_revoked_tokens"

    /**
     * MongoDB Entities for ADMIN_DB
     */
    const val ADMIN_SPECIALITIES = "admin_specialities"
    const val ADMIN_SUB_SPECIALITIES = "admin_subspecialities"
    const val ADMIN_KMPDC_PRACTITIONERS = "admin_kmpdc_practitioners"
    const val ADMIN_LOCAL_BANKS = "admin_local_banks"
    const val ADMIN_SERVICE_TIERS = "admin_service_tiers"
    const val ADMIN_COMMISSION_RATES = "admin_commission_rates"

    /**
     * MongoDB Database and Entities for SUPPORT_DB
     */
    const val SUPPORT_DB = "src_support"
    const val SUPPORT_ISSUE_CATEGORIES = "support_issue_categories"
    const val SUPPORT_TICKETS = "support_tickets"
    const val SUPPORT_TICKET_CHATS = "support_ticket_chats"

    /**
     * MongoDB Entities for DOCTOR_DB
     */
    const val DOCTOR_PROFILES = "doctor_profiles"
    const val DOCTOR_COMPLIANCE = "doctor_compliance"
    const val DOCTOR_LICENCES = "doctor_licences"
    const val DOCTOR_SPECIALIZATIONS = "doctor_specializations"
    const val DOCTOR_CERTIFICATIONS = "doctor_certifications"
    const val DOCTOR_PAYMENT_DETAILS = "doctor_payment_details"

    /**
     * MongoDB Databases and Entities for SCHEDULING
     */
    const val SCHEDULING_DB = "src_scheduling"
    const val DOCTOR_SCHEDULES = "doctor_schedules"
    const val SLOT_OVERRIDES = "slot_overrides"
    const val APPOINTMENTS = "appointments"

}