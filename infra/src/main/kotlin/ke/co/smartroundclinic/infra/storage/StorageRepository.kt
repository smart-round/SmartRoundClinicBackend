package ke.co.smartroundclinic.infra.storage

import ke.co.smartroundclinic.common.Resource

interface StorageRepository {
    /**
     * Uploads [content] to [bucket] under [key].
     * Returns the storage key on success. Call [presignedGetUrl] to build a time-limited URL.
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

    /**
     * Generates a pre-signed PUT URL so a client can upload straight to storage.
     *
     * Preferred over [upload] for anything large: [upload] requires the whole file to travel
     * to us and be held in heap before we forward it on, so the transfer is paid for twice
     * and big files threaten the process. A pre-signed PUT is a single client→storage hop.
     *
     * The caller MUST send the same [contentType] on the PUT, or the signature will not match.
     */
    suspend fun presignedPutUrl(
        bucket: String,
        key: String,
        contentType: String,
        expiresInSeconds: Long = 900,
    ): Resource<String>
}
