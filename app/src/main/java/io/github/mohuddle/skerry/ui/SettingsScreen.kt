package io.github.mohuddle.skerry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.mohuddle.skerry.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    islandOn: Boolean,
    onIslandChange: (Boolean) -> Unit,
    overlayGranted: Boolean,
    onAllowOverlay: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IslandSwitch(islandOn = islandOn, onIslandChange = onIslandChange)
            OverlaySection(overlayGranted = overlayGranted, onAllowOverlay = onAllowOverlay)
        }
    }
}

@Composable
private fun IslandSwitch(
    islandOn: Boolean,
    onIslandChange: (Boolean) -> Unit,
) {
    val label = stringResource(R.string.island_title)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.island_body),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Switch(
            checked = islandOn,
            onCheckedChange = onIslandChange,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}

@Composable
private fun OverlaySection(
    overlayGranted: Boolean,
    onAllowOverlay: () -> Unit,
) {
    Text(
        text = stringResource(R.string.overlay_title),
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = stringResource(R.string.overlay_body),
        color = MaterialTheme.colorScheme.onBackground,
    )
    if (overlayGranted) {
        Text(
            text = stringResource(R.string.overlay_granted),
            color = MaterialTheme.colorScheme.onBackground,
        )
    } else {
        Button(onClick = onAllowOverlay) {
            Text(stringResource(R.string.overlay_allow))
        }
    }
}
