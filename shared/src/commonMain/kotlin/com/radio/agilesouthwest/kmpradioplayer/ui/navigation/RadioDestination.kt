package com.radio.agilesouthwest.kmpradioplayer.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Radio
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed interface RadioDestination {
    @Transient
    val route: String
    @Transient
    val label: String
    @Transient
    val icon: ImageVector

    @Serializable
    data object Tags : RadioDestination {
        @Transient
        override val route = "tags"
        @Transient
        override val label = "Tags"
        @Transient
        override val icon = Icons.Default.Label
    }

    @Serializable
    data class Stations(val tagName: String? = null) : RadioDestination {
        @Transient
        override val route = "stations"
        @Transient
        override val label = "Stations"
        @Transient
        override val icon = Icons.Default.Radio
    }

    @Serializable
    data object Favorites : RadioDestination {
        @Transient
        override val route = "favorites"
        @Transient
        override val label = "Favorites"
        @Transient
        override val icon = Icons.Default.Favorite
    }
}
