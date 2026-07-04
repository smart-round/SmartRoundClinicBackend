package ke.co.smartroundclinic.auth.koin

import ke.co.smartroundclinic.auth.data.repository.CredentialHasherImpl
import ke.co.smartroundclinic.auth.data.repository.JwtTokenProvider
import ke.co.smartroundclinic.auth.data.repository.TokenRepositoryImpl
import ke.co.smartroundclinic.auth.data.repository.UserRepositoryImpl
import ke.co.smartroundclinic.auth.domain.repository.CredentialsHasher
import ke.co.smartroundclinic.auth.domain.repository.TokenProvider
import ke.co.smartroundclinic.auth.domain.repository.TokenRepository
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.auth.domain.service.UserService
import ke.co.smartroundclinic.auth.domain.usecase.AccountVerificationUseCase
import ke.co.smartroundclinic.auth.domain.usecase.SignInUseCase
import ke.co.smartroundclinic.auth.domain.usecase.CreateAdminUseCase
import ke.co.smartroundclinic.auth.domain.usecase.GetUserUserUseCase
import ke.co.smartroundclinic.auth.domain.usecase.RefreshTokenUseCase
import ke.co.smartroundclinic.auth.domain.usecase.RemoveProfilePictureUseCase
import ke.co.smartroundclinic.auth.domain.usecase.UploadProfilePictureUseCase
import ke.co.smartroundclinic.auth.domain.usecase.RequestPasswordResetUseCase
import ke.co.smartroundclinic.auth.domain.usecase.ResendAccountVerificationOtpUseCase
import ke.co.smartroundclinic.auth.domain.usecase.ResendPasswordResetOtpUseCase
import ke.co.smartroundclinic.auth.domain.usecase.ResetPasswordUseCase
import ke.co.smartroundclinic.auth.domain.usecase.RevokeTokenUseCase
import ke.co.smartroundclinic.auth.domain.usecase.SendAccountVerificationOtpUseCase
import ke.co.smartroundclinic.auth.domain.usecase.SignUpUseCase
import ke.co.smartroundclinic.auth.domain.usecase.SignUpWithPictureUseCase
import ke.co.smartroundclinic.auth.domain.usecase.UpdateUserUseCase
import ke.co.smartroundclinic.auth.domain.usecase.VerifyAccountOtpUseCase
import ke.co.smartroundclinic.auth.domain.usecase.GetUsersByRoleUseCase
import ke.co.smartroundclinic.auth.domain.usecase.AdminUpdateUserUseCase
import ke.co.smartroundclinic.auth.domain.usecase.FilterUsersByRoleUseCase
import ke.co.smartroundclinic.auth.domain.usecase.GetUserStatsUseCase
import ke.co.smartroundclinic.auth.domain.usecase.NotifyNewDoctorSignUpUseCase
import ke.co.smartroundclinic.auth.domain.usecase.TrackUserPresenceUseCase
import ke.co.smartroundclinic.auth.domain.usecase.UpgradeToSuperAdminUseCase
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.PatientNameResolver
import ke.co.smartroundclinic.common.PatientProfileResolver
import ke.co.smartroundclinic.common.PolicyGroupPermissionResolver
import ke.co.smartroundclinic.common.UserProfilePictureResolver
import ke.co.smartroundclinic.infra.storage.StorageRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

val authModule = module {
    single<CredentialsHasher> { CredentialHasherImpl() }
    single<TokenProvider> { JwtTokenProvider() }
    single<UserRepositoryImpl>(createdAtStart = true) {
        UserRepositoryImpl(
            get(named("authDb")),
            get(),
            get(),
            getOrNull<PolicyGroupPermissionResolver>(),
            getOrNull<StorageRepository>(),
        )
    }
    single<UserRepository> { get<UserRepositoryImpl>() }
    single<PatientNameResolver> { get<UserRepositoryImpl>() }
    single<UserProfilePictureResolver> { get<UserRepositoryImpl>() }
    single<TokenRepository> { TokenRepositoryImpl(get(named("authDb"))) }

    single { SignInUseCase(get()) }
    single { CreateAdminUseCase(get(), get(), get()) }
    single { UpdateUserUseCase(get()) }
    single { GetUserUserUseCase(get(), get()) }
    single { VerifyAccountOtpUseCase(get(), get()) }
    single { AccountVerificationUseCase(get(), get(), get(),get()) }
    single { SendAccountVerificationOtpUseCase(get(), get()) }
    single { ResendAccountVerificationOtpUseCase(get(), get(), get()) }
    single { RequestPasswordResetUseCase(get(), get(), get(), get()) }
    single { ResendPasswordResetOtpUseCase(get(), get(), get(), get()) }
    single { ResetPasswordUseCase(get(), get(), get(), get()) }
    single { RefreshTokenUseCase(get(), get(), get()) }
    single { RevokeTokenUseCase(get(), get()) }
    single { UploadProfilePictureUseCase(get(), get()) }
    single { RemoveProfilePictureUseCase(get(), get()) }
    single { GetUsersByRoleUseCase(get(), get(), getOrNull<PatientProfileResolver>()) }
    single { AdminUpdateUserUseCase(get(), get(), get(), getOrNull<NotificationSender>()) }
    single { FilterUsersByRoleUseCase(get(), get(), getOrNull<PatientProfileResolver>()) }
    single { UpgradeToSuperAdminUseCase(get()) }
    single { GetUserStatsUseCase(get()) }
    single { TrackUserPresenceUseCase(get()) }
    single { NotifyNewDoctorSignUpUseCase(get(), get()) }
    single {
        SignUpUseCase(
            get(),
            get(),
            get(),
            get(),
        )
    }
    single { SignUpWithPictureUseCase(get(), get(), get(), get(), get()) }

    single {
        UserService(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(), // SignUpWithPictureUseCase
        )
    }

}
