package com.example.ostzones.api

import com.google.gson.annotations.SerializedName

data class SpotifyUser(
//    @SerializedName("country"          ) var country         : String?           = null,
//    @SerializedName("display_name"     ) var displayName     : String?           = null,
//    @SerializedName("email"            ) var email           : String?           = null,
//    @SerializedName("explicit_content" ) var explicitContent : ExplicitContent?  = ExplicitContent(),
//    @SerializedName("external_urls"    ) var externalUrls    : ExternalUrls?     = ExternalUrls(),
//    @SerializedName("followers"        ) var followers       : Followers?        = Followers(),
//    @SerializedName("href"             ) var href            : String?           = null,
    @SerializedName("id") val id: String? = null
//    @SerializedName("images"           ) var images          : ArrayList<Images> = arrayListOf(),
//    @SerializedName("product"          ) var product         : String?           = null,
//    @SerializedName("type"             ) var type            : String?           = null,
//    @SerializedName("uri"              ) var uri             : String?           = null
)