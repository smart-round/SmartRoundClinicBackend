package ke.co.smartroundclinic.article.koin

import ke.co.smartroundclinic.article.data.repository.ArticleCategoryRepositoryImpl
import ke.co.smartroundclinic.article.data.repository.ArticleRepositoryImpl
import ke.co.smartroundclinic.article.domain.repository.ArticleCategoryRepository
import ke.co.smartroundclinic.article.domain.repository.ArticleRepository
import ke.co.smartroundclinic.article.domain.service.ArticleCategoryService
import ke.co.smartroundclinic.article.domain.service.ArticleService
import ke.co.smartroundclinic.article.domain.usecase.article.CreateArticleUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.DeleteArticleByDoctorUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.DeleteArticleUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.GetAllArticlesUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.GetArticleByIdUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.GetArticlesByCategoryUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.GetArticlesByDoctorUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.PublishArticleByDoctorUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.PublishArticleUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.SuspendArticleByDoctorUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.SuspendArticleUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.UpdateArticleUseCase
import ke.co.smartroundclinic.article.domain.usecase.articleCategory.CreateArticleCategoryUseCase
import ke.co.smartroundclinic.article.domain.usecase.articleCategory.DeleteArticleCategoryUseCase
import ke.co.smartroundclinic.article.domain.usecase.articleCategory.GetAllArticleCategoriesUseCase
import ke.co.smartroundclinic.article.domain.usecase.articleCategory.GetArticleCategoryByIdUseCase
import ke.co.smartroundclinic.article.domain.usecase.articleCategory.ToggleArticleCategoryUseCase
import ke.co.smartroundclinic.article.domain.usecase.articleCategory.UpdateArticleCategoryUseCase
import ke.co.smartroundclinic.infra.storage.StorageRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

val articleModule = module {
    single<ArticleCategoryRepository> { ArticleCategoryRepositoryImpl(get(named("articleDb"))) }
    single<ArticleRepository> { ArticleRepositoryImpl(get(named("articleDb"))) }

    single { CreateArticleCategoryUseCase(get()) }
    single { GetArticleCategoryByIdUseCase(get()) }
    single { GetAllArticleCategoriesUseCase(get()) }
    single { UpdateArticleCategoryUseCase(get()) }
    single { ToggleArticleCategoryUseCase(get()) }
    single { DeleteArticleCategoryUseCase(get()) }
    single { ArticleCategoryService(get(), get(), get(), get(), get(), get()) }

    single { CreateArticleUseCase(get(), get(), get<StorageRepository>()) }
    single { GetArticleByIdUseCase(get()) }
    single { GetAllArticlesUseCase(get()) }
    single { GetArticlesByCategoryUseCase(get()) }
    single { GetArticlesByDoctorUseCase(get()) }
    single { UpdateArticleUseCase(get(), get<StorageRepository>()) }
    single { PublishArticleUseCase(get()) }
    single { PublishArticleByDoctorUseCase(get()) }
    single { SuspendArticleUseCase(get()) }
    single { SuspendArticleByDoctorUseCase(get()) }
    single { DeleteArticleUseCase(get()) }
    single { DeleteArticleByDoctorUseCase(get()) }
    single { ArticleService(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get<StorageRepository>()) }
}
