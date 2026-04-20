package ke.co.smartroundclinic.auth.presentation.dto.request

import ke.co.smartroundclinic.auth.data.entity.UserEntity

data class AdminUpdateUserReq(
    val accountStatus: UserEntity.AccountStatus? = null,
    val verificationStatus: UserEntity.VerificationStatus? = null,
)
