package com.openfy.music.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class AccountProvider {
    SPOTIFY,
    YOUTUBE,
    GUEST
}

@Serializable
data class UserProfile(
    val id: String,
    val displayName: String,
    val channel: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class ActiveAccount(
    val provider: AccountProvider = AccountProvider.GUEST,
    val email: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val sessionToken: String? = null,
    val isConnected: Boolean = false
)
