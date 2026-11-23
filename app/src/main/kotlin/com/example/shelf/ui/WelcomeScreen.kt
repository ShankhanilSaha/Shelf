package com.example.shelf.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.shelf.R
import com.example.shelf.core.ui.component.BookshelfIllustration
import com.example.shelf.core.ui.icon.ShelfIcons
import com.example.shelf.core.ui.preview.ThemePreviews
import com.example.shelf.core.ui.theme.ShelfTheme

/** First-launch introduction; finishing it sets the onboarding flag in DataStore. */
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            BookshelfIllustration()
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WelcomePoint(ShelfIcons.SearchOutlined, stringResource(R.string.welcome_point_find))
                WelcomePoint(ShelfIcons.LibraryOutlined, stringResource(R.string.welcome_point_shelves))
                WelcomePoint(ShelfIcons.Streak, stringResource(R.string.welcome_point_goal))
                WelcomePoint(ShelfIcons.Offline, stringResource(R.string.welcome_point_offline))
            }
            Button(onClick = onGetStarted) {
                Text(stringResource(R.string.welcome_get_started))
            }
        }
    }
}

@Composable
private fun WelcomePoint(icon: ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@ThemePreviews
@Composable
private fun WelcomeScreenPreview() {
    ShelfTheme(dynamicColor = false) {
        WelcomeScreen(onGetStarted = {})
    }
}
