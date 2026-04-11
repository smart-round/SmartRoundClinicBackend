package ke.co.smartroundclinic.auth.domain.repository

import ke.co.smartroundclinic.common.Resource

interface TokenRepository {
    suspend fun revokeToken(token: String, expiresAt: Long): Resource<Nothing>
    suspend fun isRevoked(token: String): Boolean
}