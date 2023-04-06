package ua.com.radiokot.photoprism.env.logic

import retrofit2.HttpException
import ua.com.radiokot.photoprism.api.session.model.PhotoPrismSessionCredentials
import ua.com.radiokot.photoprism.api.session.service.PhotoPrismSessionService
import ua.com.radiokot.photoprism.env.data.model.EnvAuth
import ua.com.radiokot.photoprism.env.data.model.InvalidCredentialsException
import java.net.HttpURLConnection

class PhotoPrismSessionCreator(
    private val sessionService: PhotoPrismSessionService,
) : SessionCreator {

    override fun createSession(
        auth: EnvAuth
    ): String {
        return try {
            when {
                auth.credentials != null ->
                    sessionService
                        .createSession(
                            PhotoPrismSessionCredentials(
                                username = auth.credentials.username,
                                password = auth.credentials.password,
                            )
                        )
                        .id

                else ->
                    ""
            }
        } catch (e: HttpException) {
            if (e.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw InvalidCredentialsException()
            } else {
                throw e
            }
        }
    }
}