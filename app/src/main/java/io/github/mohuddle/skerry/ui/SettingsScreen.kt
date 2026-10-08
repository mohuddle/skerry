package io.github.mohuddle.skerry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.mohuddle.skerry.R
import io.github.mohuddle.skerry.allowlist.LaunchableApp
import io.github.mohuddle.skerry.allowlist.filterLaunchableApps

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    islandOn: Boolean,
    onIslandChange: (Boolean) -> Unit,
    overlayGranted: Boolean,
    onAllowOverlay: () -> Unit,
    apps: List<LaunchableApp>?,
    allowedPackages: Set<String>,
    onAppChange: (String, Boolean) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    IslandSwitch(islandOn = islandOn, onIslandChange = onIslandChange)
                    OverlaySection(overlayGranted = overlayGranted, onAllowOverlay = onAllowOverlay)
                }
            }
            appsSection(
                apps = apps,
                query = query,
                onQueryChange = { query = it },
                allowedPackages = allowedPackages,
                onAppChange = onAppChange,
            )
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

private fun LazyListScope.appsSection(
    apps: List<LaunchableApp>?,
    query: String,
    onQueryChange: (String) -> Unit,
    allowedPackages: Set<String>,
    onAppChange: (String, Boolean) -> Unit,
) {
    item {
        val searchLabel = stringResource(R.string.apps_search)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.apps_title),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.apps_body),
                color = MaterialTheme.colorScheme.onBackground,
            )
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = searchLabel },
                singleLine = true,
                label = { Text(searchLabel) },
            )
        }
    }
    when (apps) {
        null -> item {
            Text(
                text = stringResource(R.string.apps_loading),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        else -> {
            val shown = filterLaunchableApps(apps, query)
            if (shown.isEmpty()) {
                item {
                    Text(
                        text = stringResource(
                            if (query.isBlank()) R.string.apps_none else R.string.apps_empty,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            } else {
                items(shown, key = { it.packageName }) { app ->
                    AppAllowRow(
                        app = app,
                        allowed = app.packageName in allowedPackages,
                        onAppChange = onAppChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppAllowRow(
    app: LaunchableApp,
    allowed: Boolean,
    onAppChange: (String, Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = app.packageName,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = allowed,
            onCheckedChange = { onAppChange(app.packageName, it) },
            modifier = Modifier.semantics { contentDescription = app.label },
        )
    }
}
