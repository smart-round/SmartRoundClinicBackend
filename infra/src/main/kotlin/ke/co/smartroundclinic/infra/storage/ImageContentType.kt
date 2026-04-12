package ke.co.smartroundclinic.infra.storage

/**
 * Resolves the file extension for a supported image content type.
 * Returns null if the content type is not supported — callers must reject the upload.
 *
 * Supported: image/png → "png", image/jpeg → "jpeg", image/webp → "webp"
 */
fun imageExtensionOrNull(contentType: String): String? = when {
    contentType.contains("png") -> "png"
    contentType.contains("jpeg") || contentType.contains("jpg") -> "jpeg"
    contentType.contains("webp") -> "webp"
    else -> null
}
