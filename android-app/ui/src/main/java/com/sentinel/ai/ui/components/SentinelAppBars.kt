package com.sentinel.ai.ui.components

import com.sentinel.ai.ui.i18n.LocalizedText as Text

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

// ---------------------------------------------------------------------------------------------
// Sentinel Top App Bars
//
// Reusable, stateless Material 3 top app bars that wrap the Large / Medium / Center-aligned
// variants. They apply the Sentinel color scheme, support scroll-collapsing behavior, a
// navigation icon and action icons, and therefore satisfy the shell's top app bar requirements.
// ---------------------------------------------------------------------------------------------

/** Visual style of a Sentinel top app bar. */
enum class TopAppBarVariant {
    Small,
    Medium,
    Large,
    Center
}

/**
 * Sentinel-styled small top app bar.
 *
 * @param title text shown in the bar.
 * @param modifier modifier applied to the bar.
 * @param navigationIcon leading icon (e.g. a menu or back button). Defaults to a menu icon.
 * @param actions trailing action icons.
 * @param scrollBehavior optional scroll behavior for collapsible coordination.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentinelTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = { DefaultMenuIcon() },
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = { SafeXAppBarTitle(title) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * Sentinel-styled large top app bar with a prominent headline title that collapses on scroll.
 *
 * @param title text shown in the bar.
 * @param modifier modifier applied to the bar.
 * @param navigationIcon leading icon (e.g. a menu or back button). Defaults to a menu icon.
 * @param actions trailing action icons.
 * @param scrollBehavior optional scroll behavior for collapsible coordination.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentinelLargeTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = { DefaultMenuIcon() },
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    LargeTopAppBar(
        title = { Text(text = title, style = MaterialTheme.typography.headlineSmall) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * Sentinel-styled medium top app bar that collapses to a small bar on scroll.
 *
 * @param title text shown in the bar.
 * @param modifier modifier applied to the bar.
 * @param navigationIcon leading icon (e.g. a menu or back button). Defaults to a menu icon.
 * @param actions trailing action icons.
 * @param scrollBehavior optional scroll behavior for collapsible coordination.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentinelMediumTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = { DefaultMenuIcon() },
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    MediumTopAppBar(
        title = { SafeXAppBarTitle(title) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * Sentinel-styled center-aligned top app bar.
 *
 * @param title text shown in the bar.
 * @param modifier modifier applied to the bar.
 * @param navigationIcon leading icon (e.g. a menu or back button). Defaults to a menu icon.
 * @param actions trailing action icons.
 * @param scrollBehavior optional scroll behavior for collapsible coordination.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentinelCenterAlignedTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = { DefaultMenuIcon() },
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    CenterAlignedTopAppBar(
        title = { SafeXAppBarTitle(title) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/** Default leading icon used by Sentinel top app bars: a menu button. */
@Composable
private fun DefaultMenuIcon() {
    Icon(
        imageVector = Icons.Filled.Menu,
        contentDescription = com.sentinel.ai.ui.i18n.localized("Open navigation drawer")
    )
}

/** Convenience factory for a top app bar navigation/back icon from an [ImageVector]. */
@Composable
fun NavigationIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = com.sentinel.ai.ui.i18n.localized(contentDescription)
        )
    }
}

@Composable
private fun SafeXAppBarTitle(title: String) {
    if (title == "SafeX AI") {
        androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
            SafeXLogo(Modifier.size(30.dp))
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
    } else Text(title, style = MaterialTheme.typography.titleLarge)
}
