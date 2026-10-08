package com.sentinel.ai.ui.navigation

import androidx.compose.material.icons.filled.HealthAndSafety

import com.sentinel.ai.ui.i18n.LocalizedText as Text

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sentinel.ai.ui.components.NavigationIconButton
import com.sentinel.ai.ui.components.SentinelCenterAlignedTopAppBar
import com.sentinel.ai.ui.components.SentinelLargeTopAppBar
import com.sentinel.ai.ui.components.SentinelMediumTopAppBar
import com.sentinel.ai.ui.components.SentinelShield
import com.sentinel.ai.ui.components.SentinelTopAppBar
import com.sentinel.ai.ui.components.TopAppBarVariant
import com.sentinel.ai.ui.theme.SentinelMotion
import com.sentinel.ai.ui.theme.SentinelShapes
import com.sentinel.ai.ui.theme.SentinelSize
import com.sentinel.ai.ui.theme.SentinelSpacing

// ---------------------------------------------------------------------------------------------
// Sentinel navigation shell
//
// Reusable, stateless building blocks for the adaptive application shell: navigation
// destinations, a contextual top app bar, an animated bottom navigation bar, a tablet-ready
// navigation rail, and a modal navigation drawer. All pieces reuse the existing [Screen] routes
// and Sentinel design tokens; no navigation logic or destinations are changed.
// ---------------------------------------------------------------------------------------------

/** Logical grouping used to organize destinations inside the navigation drawer. */
private enum class NavGroup {
    Primary,
    Secondary
}

/**
 * A navigable destination rendered in the bottom bar, rail and drawer.
 *
 * @property screen the existing [Screen] route target.
 * @property label human-readable label for a11y and display.
 * @property icon leading icon representing the destination.
 * @property group drawer section this destination belongs to.
 */
private data class SentinelNavDestination(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val group: NavGroup
)

private val primaryDestinations = listOf(
    SentinelNavDestination(Screen.Dashboard, "Home", Icons.Filled.Home, NavGroup.Primary),
    SentinelNavDestination(Screen.Scanner, "Scan", Icons.Filled.QrCodeScanner, NavGroup.Primary),
    SentinelNavDestination(Screen.History, "History", Icons.Filled.History, NavGroup.Primary),
    SentinelNavDestination(Screen.Settings, "Settings", Icons.Filled.Settings, NavGroup.Primary)
)

// Secondary destinations were previously only reachable via deep links; surfacing them in the
// drawer improves discoverability without altering any route or navigation logic.
private val secondaryDestinations = listOf(
    SentinelNavDestination(Screen.Alerts, "Alerts", Icons.Filled.Notifications, NavGroup.Secondary),
    SentinelNavDestination(Screen.IncidentHelp, "Help after a scam", Icons.Filled.HealthAndSafety, NavGroup.Secondary)
)

private val allDestinations = primaryDestinations + secondaryDestinations

/** Resolves a display title for a route, falling back to the start destination label. */
private fun titleForRoute(route: String?): String {
    if (route?.startsWith("threat_details") == true) return "Threat details"
    val base = route?.substringBefore("/")
    if (base == Screen.Dashboard.route) return "SafeX AI"
    if (base == Screen.About.route) return "About"
    return allDestinations.firstOrNull { it.screen.route == base }?.label ?: primaryDestinations.first().label
}

/** Picks a top app bar variant for the active destination. */
private fun variantForRoute(route: String?): TopAppBarVariant {
    return when (route?.substringBefore("/")) {
        Screen.Dashboard.route -> TopAppBarVariant.Small
        else -> TopAppBarVariant.Medium
    }
}

/** True when the bottom navigation bar should be hidden (e.g. on detail screens). */
private fun shouldHideBottomBar(route: String?): Boolean =
    (route?.startsWith("threat_details") == true || route == Screen.IncidentHelp.route)

// ---------------------------------------------------------------------------------------------
// Material Motion transitions
//
// Subtle fade-through style transitions applied between destinations. Durations come from the
// existing [SentinelMotion] tokens to stay consistent with the rest of the design system.
// ---------------------------------------------------------------------------------------------

private val sentinelEnterTransition = fadeIn(animationSpec = tween(SentinelMotion.DurationShort))

private val sentinelExitTransition = fadeOut(animationSpec = tween(SentinelMotion.DurationShort))

private val sentinelPopEnterTransition = fadeIn(animationSpec = tween(SentinelMotion.DurationShort))
private val sentinelPopExitTransition = fadeOut(animationSpec = tween(SentinelMotion.DurationShort))

/** Exposed transition specs so the NavHost can wire them without redeclaring them. */
internal val SentinelNavEnterTransition
    get() = sentinelEnterTransition
internal val SentinelNavExitTransition
    get() = sentinelExitTransition
internal val SentinelNavPopEnterTransition
    get() = sentinelPopEnterTransition
internal val SentinelNavPopExitTransition
    get() = sentinelPopExitTransition

// ---------------------------------------------------------------------------------------------
// Contextual top app bar
// ---------------------------------------------------------------------------------------------

/**
 * Sentinel application top app bar for the shell.
 *
 * Renders the destination title with a leading navigation icon (menu to open the drawer, or a
 * back arrow on detail screens) and supports the Large / Medium / Center variants.
 *
 * @param currentRoute the active route, used to derive the title and bar variant.
 * @param onMenuClicked invoked when the leading menu icon is pressed (opens the drawer).
 * @param onBackClicked invoked when the leading back icon is pressed (detail screens only).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentinelTopBar(
    currentRoute: String?,
    onMenuClicked: () -> Unit,
    onBackClicked: (() -> Unit)? = null
) {
    val isDetail = shouldHideBottomBar(currentRoute)
    val navigationIcon: @Composable () -> Unit = {
        if (isDetail && onBackClicked != null) {
            NavigationIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = com.sentinel.ai.ui.i18n.localized("Back"),
                onClick = onBackClicked
            )
        } else {
            NavigationIconButton(
                icon = Icons.Filled.Menu,
                contentDescription = com.sentinel.ai.ui.i18n.localized("Open navigation drawer"),
                onClick = onMenuClicked
            )
        }
    }

    val title = titleForRoute(currentRoute)
    when (variantForRoute(currentRoute)) {
        TopAppBarVariant.Large -> SentinelLargeTopAppBar(
            title = title,
            navigationIcon = navigationIcon
        )
        TopAppBarVariant.Center -> SentinelCenterAlignedTopAppBar(
            title = title,
            navigationIcon = navigationIcon
        )
        TopAppBarVariant.Medium -> SentinelMediumTopAppBar(
            title = title,
            navigationIcon = navigationIcon
        )
        TopAppBarVariant.Small -> SentinelTopAppBar(
            title = title,
            navigationIcon = navigationIcon
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Bottom navigation bar
// ---------------------------------------------------------------------------------------------

/**
 * Sentinel bottom navigation bar for phone layouts.
 *
 * Uses the Material 3 [NavigationBar] with an animated selection indicator. Reuses the existing
 * primary destinations and preserves their routes.
 *
 * @param currentRoute active route used to highlight the selected item.
 * @param onDestinationSelected called with the chosen [Screen] when an item is tapped.
 */
@Composable
fun SentinelBottomNav(
    currentRoute: String?,
    onDestinationSelected: (Screen) -> Unit
) {
    val baseRoute = currentRoute?.substringBefore("/")
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        primaryDestinations.forEach { destination ->
            val selected = baseRoute == destination.screen.route
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationSelected(destination.screen) },
                icon = {
                    SentinelNavItemIcon(
                        icon = destination.icon,
                        selected = selected,
                        label = destination.label
                    )
                },
                label = {
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                alwaysShowLabel = true
            )
        }
    }
}

/**
 * Nav item icon with an animated selection indicator that fades and scales in when selected.
 */
@Composable
private fun SentinelNavItemIcon(
    icon: ImageVector,
    selected: Boolean,
    label: String
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}

// ---------------------------------------------------------------------------------------------
// Navigation rail (tablet)
// ---------------------------------------------------------------------------------------------

/**
 * Sentinel navigation rail for tablet / expanded layouts.
 *
 * Hidden automatically on phones; hosts the same primary destinations as the bottom bar.
 *
 * @param currentRoute active route used to highlight the selected item.
 * @param onDestinationSelected called with the chosen [Screen] when an item is tapped.
 */
@Composable
fun SentinelNavRail(
    currentRoute: String?,
    onDestinationSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val baseRoute = currentRoute?.substringBefore("/")
    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Spacer(modifier = Modifier.height(SentinelSpacing.MD))
        primaryDestinations.forEach { destination ->
            val selected = baseRoute == destination.screen.route
            NavigationRailItem(
                selected = selected,
                onClick = { onDestinationSelected(destination.screen) },
                icon = {
                    SentinelNavItemIcon(
                        icon = destination.icon,
                        selected = selected,
                        label = destination.label
                    )
                },
                label = {
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                alwaysShowLabel = true
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Modal navigation drawer
// ---------------------------------------------------------------------------------------------

/**
 * Content of the Sentinel modal navigation drawer.
 *
 * Reuses the existing destinations (primary plus previously deep-link-only secondary entries)
 * and preserves the current navigation hierarchy.
 *
 * @param currentRoute active route used to highlight the selected item.
 * @param onDestinationSelected called with the chosen [Screen] when an item is tapped.
 */
@Composable
fun SentinelNavDrawerContent(
    currentRoute: String?,
    onDestinationSelected: (Screen) -> Unit
) {
    val baseRoute = currentRoute?.substringBefore("/")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SentinelSpacing.MD)
    ) {
        // Drawer header / brand
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SentinelSpacing.MD, vertical = SentinelSpacing.SM),
            verticalArrangement = Arrangement.spacedBy(SentinelSpacing.XS)
        ) {
            Box(
                modifier = Modifier
                    .size(SentinelSize.AvatarSizeLarge)
                    .clip(SentinelShapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                com.sentinel.ai.ui.components.SafeXLogo(Modifier.size(52.dp))
            }
            Spacer(modifier = Modifier.height(SentinelSpacing.XS))
            Text(
                text = "SafeX AI",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "On-device scam protection",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(SentinelSpacing.SM))

        primaryDestinations.forEach { destination ->
            SentinelDrawerRow(
                destination = destination,
                selected = baseRoute == destination.screen.route,
                onClick = { onDestinationSelected(destination.screen) }
            )
        }

        Spacer(modifier = Modifier.height(SentinelSpacing.SM))
        Text(
            text = "Tools",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = SentinelSpacing.MD, vertical = SentinelSpacing.XS)
        )
        secondaryDestinations.forEach { destination ->
            SentinelDrawerRow(
                destination = destination,
                selected = baseRoute == destination.screen.route,
                onClick = { onDestinationSelected(destination.screen) }
            )
        }
    }
}

@Composable
private fun SentinelDrawerRow(
    destination: SentinelNavDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accessibleLabel = com.sentinel.ai.ui.i18n.localized(destination.label)
    NavigationDrawerItem(
        label = {
            Text(
                text = destination.label,
                style = MaterialTheme.typography.labelLarge
            )
        },
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = destination.icon,
                contentDescription = null
            )
        },
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            unselectedContainerColor = Color.Transparent
        ),
        modifier = Modifier
            .padding(horizontal = SentinelSpacing.SM, vertical = SentinelSpacing.XXS)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibleLabel
            }
    )
}
