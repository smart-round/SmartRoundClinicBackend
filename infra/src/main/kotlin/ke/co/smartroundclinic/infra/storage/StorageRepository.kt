package ke.co.smartroundclinic.infra.storage

import ke.co.smartroundclinic.common.Resource

interface StorageRepository {
    /**
     * Uploads [content] to the given [bucket] under [key].
     * Returns the public URL of the uploaded object on success.
     */
    suspend fun upload(
        bucket: String,
        key: String,
        content: ByteArray,
        contentType: String,
    ): Resource<String>

    /**
     * Deletes the object at [key] inside [bucket].
     */
    suspend fun delete(bucket: String, key: String): Resource<Nothing>

    /**
     * Generates a pre-signed GET URL valid for [expiresInSeconds] seconds.
     */
    suspend fun presignedGetUrl(
        bucket: String,
        key: String,
        expiresInSeconds: Long = 3600,
    ): Resource<String>
}
