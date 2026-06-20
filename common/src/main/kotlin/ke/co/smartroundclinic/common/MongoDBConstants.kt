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
    const val ADMIN_POLICY_GROUPS = "admin_policy_groups"
    const val ADMIN_PERMISSIONS_CATALOG = "admin_permissions_catalog"
    const val ADMIN_SPECIALITIES = "admin_specialities"
    const val ADMIN_SUB_SPECIALITIES = "admin_subspecialities"
    const val ADMIN_KMPDC_PRACTITIONERS = "admin_kmpdc_practitioners"
    const val ADMIN_LOCAL_BANKS = "admin_local_banks"
    const val ADMIN_SERVICE_TIERS = "admin_service_tiers"
    const val ADMIN_SERVICE_CATEGORIES = "admin_service_categories"
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
    const val DOCTOR_RATINGS = "doctor_ratings"

    /**
     * MongoDB Database and Entities for ARTICLE
     */
    const val ARTICLE_DB = "src_article"
    const val ARTICLE_CATEGORIES = "article_categories"
    const val ARTICLES = "articles"

    /**
     * MongoDB Database and Entities for NOTIFICATION
     */
    const val NOTIFICATION_DB = "src_notification"
    const val NOTIFICATIONS = "notifications"
    const val USER_DEVICE_TOKENS = "user_device_tokens"
    const val PUSH_NOTIFICATION_LOGS = "push_notification_logs"

    /**
     * MongoDB Database and Entities for CONSULTATION
     */
    const val CONSULTATION_DB = "src_consultation"
    const val CONSULTATION_SESSIONS = "consultation_sessions"
    const val CONSULTATION_MESSAGES = "consultation_messages"

    /**
     * MongoDB Databases and Entities for SCHEDULING
     */
    const val SCHEDULING_DB = "src_scheduling"
    const val DOCTOR_SCHEDULES = "doctor_schedules"
    const val SLOT_OVERRIDES = "slot_overrides"
    const val APPOINTMENTS = "appointments"

    /**
     * MongoDB Database and Entities for MEDICAL RECORDS
     */
    const val MEDICAL_RECORDS_DB = "src_medical_records"
    const val MEDICAL_RECORDS = "medical_records"

    /**
     * MongoDB Database and Entities for PAYMENTS
     */
    const val PAYMENTS_DB = "src_payments"
    const val PAYMENTS = "payments"
    const val PAYMENT_LOGS = "payment_logs"
    const val WITHDRAWALS = "withdrawals"
    const val PLATFORM_COMMISSION_LOGS = "platform_commission_logs"

}