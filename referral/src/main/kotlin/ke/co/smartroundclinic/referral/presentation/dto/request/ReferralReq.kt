package ke.co.smartroundclinic.referral.presentation.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateReferralReq(
    val appointmentId: String,
    val receivingDoctorId: String,
    // Nullable, not a defaulted String: requests are deserialized with Gson, which allocates via
    // Unsafe and never runs the Kotlin constructor, so a default is silently skipped and an
    // omitted field lands as null. The doctor app stopped sending this; callers coerce with
    // orEmpty() so the stored column stays non-null.
    val reason: String? = null,
)
