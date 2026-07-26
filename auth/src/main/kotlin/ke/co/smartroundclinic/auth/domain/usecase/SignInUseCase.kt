package ke.co.smartroundclinic.auth.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.auth.domain.repository.AuthToken
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource


class SignInUseCase(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(email: String, password: String): DefaultResponse<AuthToken?> = when(val result = userRepository.emailSignIn(email, password)){
        is Resource.Success -> {
            result.toDefaultResponse(HttpStatusCode.OK.value){it}
        }
        is Resource.Error -> {
            // Credentials were valid and the account exists (data carries its status) but access
            // is restricted — suspended/inactive/unverified — so 403 fits better than 401, which
            // is reserved for unknown email or wrong password (no data).
            val statusCode = if (result.data != null) HttpStatusCode.Forbidden.value else HttpStatusCode.Unauthorized.value
            result.toDefaultResponse(statusCode){it}
        }
    }

}