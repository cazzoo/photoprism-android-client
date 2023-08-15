package ua.com.radiokot.photoprism.features.gallery.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import ua.com.radiokot.photoprism.features.gallery.search.people.data.model.Person
import ua.com.radiokot.photoprism.util.LocalDate

@Parcelize
data class SearchConfig(
    /**
     * An optional set of media types to limit the search.
     * If **null**, there is no limit on media types.
     * If **empty**, no media types are allowed and the search will find nothing.
     */
    val mediaTypes: Set<GalleryMedia.TypeName>?,
    val albumUid: String?,
    /**
     * A set of [Person.id] which may be subjects or faces,
     * to find only items with them together.
     * If empty, no subject/face filters are applied.
     */
    val personIds: Set<String>,
    /**
     * Local date to find media taken before it.
     * The filter is applied to [GalleryMedia.takenAtLocal]
     */
    val beforeLocal: LocalDate?,
    val userQuery: String,
    val includePrivate: Boolean,
) : Parcelable {
    /**
     * @return copy of the config which doesn't overcome the allowed media types,
     * or the current instance if there is no specific allowance.
     *
     * @param allowedMediaTypes a non-empty set of the allowed media types,
     * or null if there is no specific allowance (all the types are allowed).
     */
    fun withOnlyAllowedMediaTypes(allowedMediaTypes: Set<GalleryMedia.TypeName>?): SearchConfig {
        require(allowedMediaTypes == null || allowedMediaTypes.isNotEmpty()) {
            "The set of allowed types must either be null or not empty"
        }

        return if (allowedMediaTypes != null)
            if (mediaTypes != null)
                copy(
                    mediaTypes = mediaTypes.intersect(allowedMediaTypes),
                )
            else
                copy(
                    mediaTypes = allowedMediaTypes
                )
        else
            this
    }

    /**
     * @return PhotoPrism query for the search request,
     * or null if there are no search criteria.
     *
     * **To get accurate results with dates, the local post filtering is needed**
     */
    fun getPhotoPrismQuery(): String? {
        val queryBuilder = StringBuilder()

        // User query goes first, hence all the other params override the input.
        if (userQuery.isNotBlank()) {
            queryBuilder.append(userQuery)
        }

        // If mediaTypes are not specified, all the types are allowed and no filter is added.
        // If they are empty, nothing is allowed (empty search results).
        if (mediaTypes != null) {
            if (mediaTypes.isEmpty()) {
                queryBuilder.append(" type:nothing")
            } else {
                queryBuilder.append(
                    " type:${
                        mediaTypes.joinToString("|") { it.value }
                    }"
                )
            }
        }

        if (beforeLocal != null) {
            // PhotoPrism "before" filter does not take into account the time.
            // "before:2023-04-30T22:57:32Z" is treated like "2023-04-30T00:00:00Z".
            // It also filters by the "TakenAt" date rather than "TakenAtLocal",
            // so an extra day is added to overcome these problems.
            //
            // When using this workaround workaround, the local post filtering is needed.
            val redundantBefore =
                LocalDate(beforeLocal.time + DAY_MS)
            queryBuilder.append(" before:\"${formatPhotoPrismDate(redundantBefore)}\"")
        }

        queryBuilder.append(" public:${!includePrivate}")

        if (albumUid != null) {
            queryBuilder.append(" album:$albumUid")
        }

        if (personIds.isNotEmpty()) {
            val subjectUids = personIds
                .filter { Person.isSubjectUid(it) }
            if (subjectUids.isNotEmpty()) {
                queryBuilder.append(" subject:${subjectUids.joinToString("&")}")
            }

            val faceIds = personIds
                .filter { Person.isFaceId(it) }
            if (faceIds.isNotEmpty()) {
                queryBuilder.append(" face:${faceIds.joinToString("&")}")
            }
        }

        return queryBuilder
            .toString()
            .takeUnless(String::isNullOrBlank)
    }

    companion object {
        val DEFAULT = SearchConfig(
            mediaTypes = null,
            albumUid = null,
            personIds = emptySet(),
            beforeLocal = null,
            userQuery = "",
            includePrivate = false,
        )

        private const val DAY_MS = 86400000L
    }
}
