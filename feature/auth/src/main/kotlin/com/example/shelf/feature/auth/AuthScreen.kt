package com.example.shelf.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.shelf.core.ui.icon.ShelfIcons
import com.example.shelf.core.ui.preview.ThemePreviews
import com.example.shelf.core.ui.text.asString
import com.example.shelf.core.ui.theme.ShelfTheme
import com.example.shelf.core.ui.R as UiR

@Composable
internal fun AuthScreen(
    uiState: AuthUiState,
    onSignInClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.auth_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(ShelfIcons.Back, contentDescription = stringResource(UiR.string.ui_navigate_up))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = ShelfIcons.Sync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Text(
                text = stringResource(R.string.auth_headline),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            val textModifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
            when (uiState) {
                AuthUiState.Loading -> CircularProgressIndicator()

                AuthUiState.Unavailable -> {
                    Text(
                        stringResource(R.string.auth_unavailable),
                        textAlign = TextAlign.Center,
                        modifier = textModifier,
                    )
                    OutlinedButton(onClick = onBack) { Text(stringResource(R.string.auth_back)) }
                }

                is AuthUiState.SignedOut -> SignedOutContent(uiState, onSignInClick, textModifier)

                is AuthUiState.SignedIn -> Text(
                    text = stringResource(
                        R.string.auth_signed_in_as,
                        uiState.user.email ?: uiState.user.displayName.orEmpty(),
                    ),
                    textAlign = TextAlign.Center,
                    modifier = textModifier,
                )
            }
        }
    }
}

@Composable
private fun SignedOutContent(state: AuthUiState.SignedOut, onSignInClick: () -> Unit, textModifier: Modifier) {
    Text(stringResource(R.string.auth_message), textAlign = TextAlign.Center, modifier = textModifier)
    Text(
        text = stringResource(R.string.auth_privacy),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = textModifier,
    )
    Button(
        onClick = onSignInClick,
        enabled = !state.signingIn,
        modifier = Modifier.testTag(AuthTestTags.SIGN_IN),
    ) {
        if (state.signingIn) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Icon(ShelfIcons.Account, contentDescription = null)
        }
        Text(stringResource(R.string.auth_sign_in_with_google), modifier = Modifier.padding(start = 8.dp))
    }
    state.error?.let { error ->
        Text(
            text = error.asString(),
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = textModifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@ThemePreviews
@Composable
private fun AuthScreenPreview() {
    ShelfTheme(dynamicColor = false) {
        AuthScreen(uiState = AuthUiState.SignedOut(signingIn = false, error = null), onSignInClick = {}, onBack = {})
    }
}
