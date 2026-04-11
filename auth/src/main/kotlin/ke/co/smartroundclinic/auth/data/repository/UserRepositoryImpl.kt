package ke.co.smartroundclinic.auth.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.ktor.util.logging.KtorSimpleLogger
import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.data.entity.UserEntity.Role
import ke.co.smartroundclinic.auth.domain.repository.AuthToken
import ke.co.smartroundclinic.auth.domain.repository.CredentialsHasher
import ke.co.smartroundclinic.auth.domain.repository.TokenProvider
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bson.conversions.Bson

class UserRepositoryImpl(
    private val database: MongoDatabase,
    private val credentialsHasher: CredentialsHasher,
    private val tokenProvider: TokenProvider
) : UserRepository {

    private val collection = database.getCollection<UserEntity>(MongoDBConstants.AUTH_USER)
    private val logger = KtorSimpleLogger(this::class.simpleName!!)

    override suspend fun create(user: UserEntity): Resource<UserEntity?> {
        return try {
            val existingUser = collection.find(Filters.eq(UserEntity::email.name, user.email)).firstOrNull()
            if (existingUser != null) {
                return Resource.Error(message = "User with email ${user.email} already exists")
            }

            collection.insertOne(user.copy(password = credentialsHasher.hash(user.password)))
            Resource.Success(data = user, message = "User Created Successfully")

        } catch (e: Exception) {
            Resource.Error(message = e.localizedMessage ?: "An unknown error occurred")
        }
    }


    override suspend fun updateUser(
        userId: String,
        fullName: String?,
        email: String?,
        phoneNumber: String?,
        gender: UserEntity.Gender?
    ): Resource<UserEntity?> = withContext(Dispatchers.IO) {

        try {
            val existingUser = collection.find(Filters.eq(UserEntity::id.name, userId)).firstOrNull()
                ?: return@withContext Resource.Error("User not found")
            val updates = mutableListOf<Bson>()

            fullName
                ?.trim()
                ?.takeIf { it.isNotBlank() && it != existingUser.fullName }
                ?.let { updates.add(Updates.set(UserEntity::fullName.name, it)) }

            email
                ?.trim()
                ?.lowercase()
                ?.takeIf { it.isNotBlank() && it != existingUser.email.lowercase() }
                ?.let { newEmail ->
                    val emailOwner = collection.find(
                        Filters.and(
                            Filters.eq(UserEntity::email.name, newEmail),
                            Filters.ne(UserEntity::id.name, userId)
                        )
                    ).firstOrNull()

                    if (emailOwner != null) {
                        return@withContext Resource.Error("Email already in use")
                    }
                    updates.add(Updates.set(UserEntity::email.name, newEmail))
                }

            phoneNumber
                ?.trim()
                ?.takeIf { it.isNotBlank() && it != existingUser.phoneNumber }
                ?.let { updates.add(Updates.set(UserEntity::phoneNumber.name, it)) }

            gender
                ?.takeIf { it != existingUser.gender }
                ?.let {
                    updates.add(Updates.set(UserEntity::gender.name, it))
                }

            if (updates.isEmpty()) {
                return@withContext Resource.Success(data = existingUser, message = "No changes detected")
            }
            collection.updateOne(Filters.eq(UserEntity::id.name, userId), Updates.combine(updates))

            val updatedUser = collection.find(Filters.eq(UserEntity::id.name, userId)).firstOrNull()
            Resource.Success(data = updatedUser, message = "User updated successfully")

        } catch (e: Exception) {
            Resource.Error(message = e.localizedMessage ?: "Failed to update user")
        }
    }

    override suspend fun getUser(userId: String): Resource<UserEntity?> = withContext(Dispatchers.IO) {
        val user = collection.find(Filters.eq(UserEntity::id.name, userId)).firstOrNull()
            ?: return@withContext Resource.Error("User not found")
        Resource.Success(data = user, message = "User found")
    }

    override suspend fun getUserByEmail(email: String): Resource<UserEntity?>  = withContext(Dispatchers.IO){
        val user = collection.find(Filters.eq(UserEntity::email.name, email)).firstOrNull()
            ?: return@withContext Resource.Error("User not found")
        Resource.Success(data = user, message = "User found")
    }

    override suspend fun updateAccountVerificationStatus(email: String): Resource<UserEntity?> = withContext(
        Dispatchers.IO
    ) {
        val existingUser = collection.find(Filters.eq(UserEntity::email.name, email)).firstOrNull()
            ?: return@withContext Resource.Error(message = "User not found")

        // Build updates based on role
        val updates = if (existingUser.role == Role.DOCTOR) {
            Updates.combine(
                Updates.set(UserEntity::verificationStatus.name, UserEntity.VerificationStatus.PENDING_APPROVAL.name),
                Updates.set(UserEntity::accountStatus.name, UserEntity.AccountStatus.INACTIVE.name),
                Updates.set(UserEntity::otpCode.name, null),
                Updates.set(UserEntity::otpExpiresAt.name, null),
            )
        } else {
            Updates.combine(
                Updates.set(UserEntity::verificationStatus.name, UserEntity.VerificationStatus.VERIFIED.name),
                Updates.set(UserEntity::accountStatus.name, UserEntity.AccountStatus.ACTIVE.name),
                Updates.set(UserEntity::otpCode.name, null),
                Updates.set(UserEntity::otpExpiresAt.name, null),
            )
        }

        val updateResult = collection.updateOne(
            Filters.eq(UserEntity::email.name, email),
            updates
        )

        if (!updateResult.wasAcknowledged())
            return@withContext Resource.Error(message = "Failed to update user verification status")

        val user = collection.find(Filters.eq(UserEntity::email.name, email)).firstOrNull()

        if (existingUser.role == Role.DOCTOR) {
            Resource.Success(message = "Account verified. Kindly wait for admin approval.", data = user)
        } else {
            Resource.Success(message = "Account verified successfully.", data = user)
        }
    }

    override suspend fun updateOtp(
        userId: String,
        otpCode: String,
        otpExpiresAt: Long
    ): Resource<Nothing> = withContext(Dispatchers.IO) {
        val updateUserOtp = Updates.combine(
            listOf(
                Updates.set(UserEntity::otpCode.name, otpCode),
                Updates.set(UserEntity::otpExpiresAt.name, otpExpiresAt)
            )
        )
        collection.updateOne(Filters.eq(UserEntity::id.name, userId), updateUserOtp).let { updateResult ->
            if (!updateResult.wasAcknowledged())
                return@let Resource.Error(message = "Failed to update user otp", data = null)
            Resource.Success(message = "User otp updated successfully", data = null)
        }
    }

    override suspend fun updatePassword(
        userId: String,
        newPassword: String
    ): Resource<Nothing> = withContext(Dispatchers.IO) {

        try {
            val hashedPassword = credentialsHasher.hash(newPassword)

            val result = collection.updateOne(
                Filters.eq(UserEntity::id.name, userId),
                Updates.set(UserEntity::password.name, hashedPassword)
            )

            if (result.matchedCount == 0L) {
                return@withContext Resource.Error("User not found")
            }
            Resource.Success(data = null, message = "Password updated successfully")

        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update password")
        }
    }

    override suspend fun emailSignIn(
        email: String,
        password: String
    ): Resource<AuthToken?> = withContext(Dispatchers.IO) {
        return@withContext collection.find(Filters.eq(UserEntity::email.name, email)).firstOrNull()?.let { user ->

            val isPasswordValid = credentialsHasher.verify(hashed = user.password, text = password)
            if (!isPasswordValid) return@let Resource.Error(message = "Invalid email or password")

            val token = tokenProvider.generateTokens(userId = user.id, role = user.role.name)
            Resource.Success(message = "Login successful", data = token)

        } ?: return@withContext Resource.Error(message = "Invalid email or password")

    }


    /**
     * Initialize an admin account if none exists
     * */
    private fun initAdmin() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (collection.countDocuments() > 0) return@launch
                create(
                    UserEntity(
                        email = "admin@smartroundclinic.co.ke",
                        password = "admin",
                        fullName = "Admin",
                        role = Role.ADMIN,
                        accountStatus = UserEntity.AccountStatus.ACTIVE,
                        verificationStatus = UserEntity.VerificationStatus.VERIFIED
                    )
                ).also { resource ->
                    when (resource) {
                        is Resource.Success -> logger.info("Admin account initialized successfully")
                        is Resource.Error -> logger.error("Failed to initialize admin account: ${resource.message}")
                    }
                }
            } catch (e: Exception) {
                logger.error("Error during admin initialization: ${e.message}")
            }
        }
    }

    init {
        initAdmin()
    }
}