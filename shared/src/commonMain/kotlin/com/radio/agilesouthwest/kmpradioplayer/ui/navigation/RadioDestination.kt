package com.radio.agilesouthwest.kmpradioplayer.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Radio
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
sealed interface RadioDestination {
    val route: String
    val label: String
    val icon: ImageVector

    @Serializable
    data object Tags : RadioDestination {
        override val route = "tags"
        override val label = "Tags"
        override val icon = Icons.Default.Label
    }

    @Serializable
    data object Stations : RadioDestination {
        override val route = "stations"
        override val label = "Stations"
        override val icon = Icons.Default.Radio
    }

    @Serializable
    data object Favorites : RadioDestination {
        override val route = "favorites"
        override val label = "Favorites"
        override val icon = Icons.Default.Favorite
    }
}
