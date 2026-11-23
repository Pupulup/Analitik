package com.dota.analyzer.data

import com.google.gson.annotations.SerializedName

data class Hero(
    @SerializedName("id") val id: Int,
    @SerializedName("localized_name") val localizedName: String,
    @SerializedName("name") val internalName: String,
    @SerializedName("roles") val roles: List<String> = emptyList()
)
