package ke.co.smartroundclinic.infra.storage

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.presigners.presignGetObject
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.net.url.Url
import io.ktor.util.logging.KtorSimpleLogger
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.R2Config
import kotlin.time.Duration.Companion.seconds

class R2StorageRepositoryImpl(private val config: R2Config) : StorageRepository {

    private val logger = KtorSimpleLogger(this::class.simpleName!!)

    private val client: S3Client = S3Client {
        region = "auto"
        endpointUrl = Url.parse(config.endpointUrl)
        credentialsProvider = object : CredentialsProvider {
            override suspend fun resolve(attributes: Attributes) = Credentials(
                accessKeyId = config.accessKeyId,
                secretAccessKey = config.secretAccessKey,
            )
        }
        forcePathStyle = true
    }

    override suspend fun upload(
        bucket: String,
        key: String,
        content: ByteArray,
        contentType: String,
    ): Resource<String> = try {
        client.putObject(
            PutObjectRequest {
                this.bucket = bucket
                this.key = key
                this.body = ByteStream.fromBytes(content)
                this.contentType = contentType
                this.contentLength = content.size.toLong()
            }
        )
        Resource.Success(data = key, message = "File uploaded successfully")
    } catch (e: Exception) {
        logger.error("R2 upload failed for $bucket/$key: ${e.message}")
        Resource.Error(e.localizedMessage ?: "Upload failed")
    }

    override suspend fun delete(bucket: String, key: String): Resource<Nothing> = try {
        client.deleteObject(
            DeleteObjectRequest {
                this.bucket = bucket
                this.key = key
            }
        )
        Resource.Success(data = null, message = "File deleted successfully")
    } catch (e: Exception) {
        logger.error("R2 delete failed for $bucket/$key: ${e.message}")
        Resource.Error(e.localizedMessage ?: "Delete failed")
    }

    override suspend fun presignedGetUrl(
        bucket: String,
        key: String,
        expiresInSeconds: Long,
    ): Resource<String> = try {
        val presignedRequest = client.presignGetObject(
            input = aws.sdk.kotlin.services.s3.model.GetObjectRequest {
                this.bucket = bucket
                this.key = key
            },
            duration = expiresInSeconds.seconds,
        )
        Resource.Success(data = presignedRequest.url.toString(), message = "Pre-signed URL generated")
    } catch (e: Exception) {
        logger.error("R2 presign failed for $bucket/$key: ${e.message}")
        Resource.Error(e.localizedMessage ?: "Failed to generate pre-signed URL")
    }
}
