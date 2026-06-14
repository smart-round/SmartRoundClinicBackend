package ke.co.smartroundclinic.admin.domain.usecase

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository

private const val ICON_URL_TTL = 86400L  // 24 hours

suspend fun resolveIconUrl(key: String?, storageRepository: StorageRepository): String? {
    if (key == null || key.startsWith("https://")) return key
    return (storageRepository.presignedGetUrl(AppConfig.r2.bucket, key, ICON_URL_TTL) as? Resource.Success)?.data ?: key
}
