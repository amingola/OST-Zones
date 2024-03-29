package com.example.ostzones.api.models

import com.google.gson.annotations.SerializedName

data class PlaylistsResponse(
    val href: String,
    val limit: Long,
    val next: String,
    val offset: Long,
    val previous: String,
    val total: Long,
    val items: List<Playlist>
)

data class Playlist(
    val name: String,
    val collaborative: Boolean?,
    val description: String?,
    val href: String?,
    val id: String?,
    val images: List<Image>?,
    val owner: Owner?,
    val public: Boolean?,
    val tracks: Tracks?,
    val type: String?,
    val uri: String?,
    @SerializedName("external_urls") val externalUrls: ExternalUrls?,
    @SerializedName("snapshot_id") val snapshotId: String?
)

data class ExternalUrls(
    val spotify: String
)

data class Image(
    val url: String,
    val height: Long,
    val width: Long
)

data class Owner(
    val followers: Followers,
    val href: String,
    val id: String,
    val type: String,
    val uri: String,
    @SerializedName("external_urls") val externalUrls: ExternalUrls2,
    @SerializedName("display_name") val displayName: String
)

data class ExternalUrls2(
    val spotify: String
)

data class Followers(
    val href: String,
    val total: Long
)

data class Tracks(
    val href: String,
    val total: Long
)
