package ke.co.smartroundclinic.auth.data.entity

import org.bson.types.ObjectId

data class RevokedTokenEntity(
    val id: String = ObjectId().toString(),
    val token: String,
    val expiresAt: Long, // epoch milliseconds — mirrors the refresh token's own expiry
)