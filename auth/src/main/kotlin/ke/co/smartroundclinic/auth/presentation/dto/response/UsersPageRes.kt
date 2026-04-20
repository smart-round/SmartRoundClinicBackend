package ke.co.smartroundclinic.auth.presentation.dto.response

data class UsersPageRes(
    val items: List<UserRes>,
    val total: Long,
    val page: Int,
    val size: Int,
)
