package ua.com.radiokot.photoprism.features.gallery.di

import android.net.Uri
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.BuildConfig
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.di.EXTERNAL_DOWNLOADS_DIRECTORY
import ua.com.radiokot.photoprism.di.INTERNAL_DOWNLOADS_DIRECTORY
import ua.com.radiokot.photoprism.di.SelfParameterHolder
import ua.com.radiokot.photoprism.di.dateFormatModule
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.envconnection.di.envConnectionFeatureModule
import ua.com.radiokot.photoprism.features.gallery.data.storage.GalleryPreferences
import ua.com.radiokot.photoprism.features.gallery.data.storage.GalleryPreferencesOnPrefs
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.folders.galleryFoldersFeatureModule
import ua.com.radiokot.photoprism.features.gallery.logic.ArchiveGalleryMediaUseCase
import ua.com.radiokot.photoprism.features.gallery.logic.DeleteGalleryMediaUseCase
import ua.com.radiokot.photoprism.features.gallery.logic.DownloadFileUseCase
import ua.com.radiokot.photoprism.features.gallery.logic.FileReturnIntentCreator
import ua.com.radiokot.photoprism.features.gallery.logic.MediaCodecVideoFormatSupport
import ua.com.radiokot.photoprism.features.gallery.logic.MediaFileDownloadUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaWebUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.PhotoPrismMediaDownloadUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.PhotoPrismMediaWebUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.PhotoPrismPreviewUrlFactory
import ua.com.radiokot.photoprism.features.gallery.search.gallerySearchFeatureModules
import ua.com.radiokot.photoprism.features.gallery.search.logic.TvDetector
import ua.com.radiokot.photoprism.features.gallery.search.logic.TvDetectorImpl
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.DownloadMediaFileViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryFastScrollViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryViewModel
import ua.com.radiokot.photoprism.util.downloader.ObservableDownloader
import ua.com.radiokot.photoprism.util.downloader.OkHttpObservableDownloader

class ImportSearchBookmarksUseCaseParams(
    val fileUri: Uri,
) : SelfParameterHolder()

val galleryFeatureModule = module {
    includes(envConnectionFeatureModule)
    includes(dateFormatModule)
    includes(gallerySearchFeatureModules)
    includes(galleryFoldersFeatureModule)

    single {
        FileReturnIntentCreator(
            fileProviderAuthority = BuildConfig.FILE_PROVIDER_AUTHORITY,
            context = get(),
        )
    } bind FileReturnIntentCreator::class

    singleOf(::TvDetectorImpl) bind TvDetector::class

    single {
        GalleryPreferencesOnPrefs(
            preferences = get(named(APP_NO_BACKUP_PREFERENCES)),
            keyPrefix = "gallery"
        )
    } bind GalleryPreferences::class

    scope<EnvSession> {
        scoped {
            val session = get<EnvSession>()

            PhotoPrismPreviewUrlFactory(
                apiUrl = session.envConnectionParams.apiUrl.toString(),
                previewToken = session.previewToken,
                videoFormatSupport = MediaCodecVideoFormatSupport()
            )
        } bind MediaPreviewUrlFactory::class

        scoped {
            val session = get<EnvSession>()

            PhotoPrismMediaDownloadUrlFactory(
                apiUrl = session.envConnectionParams.apiUrl.toString(),
                downloadToken = session.downloadToken,
            )
        } bind MediaFileDownloadUrlFactory::class

        scoped {
            val session = get<EnvSession>()

            PhotoPrismMediaWebUrlFactory(
                webLibraryUrl = session.envConnectionParams.webLibraryUrl,
            )
        } bind MediaWebUrlFactory::class

        scopedOf(DownloadFileUseCase::Factory)

        // Downloader must be session-scoped to have the correct
        // HTTP client (e.g. for mTLS)
        scopedOf(::OkHttpObservableDownloader) bind ObservableDownloader::class

        viewModelOf(::DownloadMediaFileViewModel)

        scoped {
            SimpleGalleryMediaRepository.Factory(
                photoPrismPhotosService = get(),
                thumbnailUrlFactory = get(),
                downloadUrlFactory = get(),
                webUrlFactory = get(),
                // Always 80 elements – not great, not terrible.
                // It is better, of course, to dynamically adjust
                // to the max number of items on the screen.
                defaultPageLimit = 80,
            )
        } bind SimpleGalleryMediaRepository.Factory::class

        viewModelOf(::GallerySearchViewModel)

        viewModelOf(::GalleryFastScrollViewModel)

        viewModel {
            GalleryViewModel(
                galleryMediaRepositoryFactory = get(),
                internalDownloadsDir = get(named(INTERNAL_DOWNLOADS_DIRECTORY)),
                externalDownloadsDir = get(named(EXTERNAL_DOWNLOADS_DIRECTORY)),
                downloadMediaFileViewModel = get(),
                connectionParams = get<EnvSession>().envConnectionParams,
                galleryPreferences = get(),
                archiveGalleryMediaUseCase = get(),
                deleteGalleryMediaUseCase = get(),
                searchViewModel = get(),
                fastScrollViewModel = get(),
                disconnectFromEnvUseCase = get(),
                memoriesListViewModel = get(),
                galleryExtensionsStateRepository = get(),
            )
        }

        scopedOf(::ArchiveGalleryMediaUseCase)
        scopedOf(::DeleteGalleryMediaUseCase)
    }
}
