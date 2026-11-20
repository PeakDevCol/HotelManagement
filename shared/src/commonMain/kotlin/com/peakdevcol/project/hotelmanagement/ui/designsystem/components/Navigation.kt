package com.peakdevcol.project.hotelmanagement.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.back
import com.peakdevcol.project.hotelmanagement.resources.ds_hotel_name
import com.peakdevcol.project.hotelmanagement.resources.ds_system_name
import com.peakdevcol.project.hotelmanagement.resources.logout
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticRadius
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource

/** A section of the app as the navigation shows it. [badgeCount] warns about something pending. */
data class NavigationDestination<T>(
    val key: T,
    val label: String,
    val icon: DrawableResource,
    val selectedIcon: DrawableResource,
    val badgeCount: Int = 0,
)

/** Names the screen and gathers its secondary actions. The back arrow only goes on detail screens. */
@Composable
fun MajesticTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = MajesticTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MajesticSizes.topBar)
            .background(colors.surface)
            .padding(horizontal = MajesticSpacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            MajesticIconButton(icon = MajesticIcons.arrowBack, contentDescription = stringResource(Res.string.back), onClick = onBack, tint = colors.ink)
        }
        Text(
            text = title,
            style = MajesticTheme.typography.titleLarge,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = MajesticSpacing.space3),
        )
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/**
 * The bottom bar of the phone: three or four destinations by area (RF-AUTH-05, RF-EMP-05). The
 * destinations share the width equally; a label that does not fit is cut with an ellipsis and
 * never pushes the others.
 */
@Composable
fun <T> MajesticNavigationBar(
    destinations: List<NavigationDestination<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MajesticTheme.colors
    Column(modifier = modifier.fillMaxWidth().background(colors.surfaceRaised)) {
        HorizontalDivider(thickness = 1.dp, color = colors.line)
        Row(
            modifier = Modifier.fillMaxWidth().height(MajesticSizes.navigationBar).selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEach { destination ->
                val isSelected = destination.key == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(destination.key) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space1, Alignment.CenterVertically),
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 64.dp)
                            .fillMaxWidth()
                            .height(32.dp)
                            .background(if (isSelected) colors.primarySoft else colors.surfaceRaised, RoundedCornerShape(percent = 50)),
                        contentAlignment = Alignment.Center,
                    ) {
                        MajesticIcon(
                            icon = if (isSelected) destination.selectedIcon else destination.icon,
                            contentDescription = null,
                            tint = if (isSelected) colors.onPrimarySoft else colors.inkMuted,
                        )
                        if (destination.badgeCount > 0) CountBadge(destination.badgeCount, Modifier.align(Alignment.TopEnd).offset(x = (-6).dp))
                    }
                    Text(
                        text = destination.label,
                        style = MajesticTheme.typography.caption.copy(fontWeight = MajesticTheme.typography.label.fontWeight),
                        color = if (isSelected) colors.ink else colors.inkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = MajesticSpacing.space1),
                    )
                }
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    val colors = MajesticTheme.colors
    Box(
        modifier = modifier
            .heightIn(min = 18.dp)
            .widthIn(min = 18.dp)
            .background(colors.warning, CircleShape)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = MajesticTheme.typography.overline.copy(letterSpacing = MajesticTheme.typography.caption.letterSpacing),
            color = colors.surfaceRaised,
            maxLines = 1,
        )
    }
}

/** A labelled group of destinations in the side navigation, like "Operación" or "Administración". */
data class NavigationSection<T>(val title: String, val destinations: List<NavigationDestination<T>>)

/**
 * All the sections of the role on desktop and tablets, on the brand color. When the window is
 * short, the sections scroll and the footer with the employee stays at the bottom. [compact] shows
 * only icons with a short label, for widths between 600 and 839 dp.
 */
@Composable
fun <T> SideNavigation(
    sections: List<NavigationSection<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    employeeName: String,
    employeeArea: String,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = MajesticTheme.colors
    val typography = MajesticTheme.typography
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(if (compact) MajesticSizes.sideNavigationCompact else MajesticSizes.sideNavigation)
            .background(colors.brand)
            .padding(start = MajesticSpacing.space3, top = MajesticSpacing.space6, end = MajesticSpacing.space3, bottom = MajesticSpacing.space4),
    ) {
        Row(
            modifier = Modifier.padding(start = MajesticSpacing.space2, end = MajesticSpacing.space2, bottom = MajesticSpacing.space6),
            horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HotelLogo()
            if (!compact) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(Res.string.ds_system_name), style = typography.title.copy(fontSize = typography.bodyLarge.fontSize, fontWeight = typography.amount.fontWeight), color = colors.onBrand, maxLines = 1)
                    Text(text = stringResource(Res.string.ds_hotel_name), style = typography.caption, color = colors.onBrandMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            sections.forEach { section ->
                if (!compact) {
                    SectionLabel(
                        text = section.title,
                        color = colors.onBrandMuted,
                        modifier = Modifier.padding(start = MajesticSpacing.space3, top = MajesticSpacing.space4, bottom = MajesticSpacing.space2),
                    )
                }
                section.destinations.forEach { destination ->
                    SideNavigationItem(destination = destination, selected = destination.key == selected, compact = compact, onClick = { onSelect(destination.key) })
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = colors.brandRaised, modifier = Modifier.padding(top = MajesticSpacing.space2))
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = MajesticSpacing.space4, start = MajesticSpacing.space2),
            horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!compact) {
                Box(
                    modifier = Modifier.size(MajesticSizes.avatar).background(colors.brandRaised, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = initialsOf(employeeName), style = typography.label, color = colors.onBrand, maxLines = 1)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = employeeName, style = typography.label, color = colors.onBrand, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = employeeArea, style = typography.caption, color = colors.onBrandMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            MajesticIconButton(icon = MajesticIcons.logOut, contentDescription = stringResource(Res.string.logout), onClick = onLogOut, tint = colors.onBrandMuted)
        }
    }
}

@Composable
private fun <T> SideNavigationItem(destination: NavigationDestination<T>, selected: Boolean, compact: Boolean, onClick: () -> Unit) {
    val colors = MajesticTheme.colors
    val content = if (selected) colors.onBrand else colors.onBrandMuted
    val itemModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = MajesticSizes.control)
        .background(if (selected) colors.brandRaised else colors.brand, RoundedCornerShape(MajesticRadius.medium))
        .selectable(selected = selected, role = Role.Tab, onClick = onClick)
    if (compact) {
        Column(
            modifier = itemModifier.padding(vertical = MajesticSpacing.space2),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MajesticIcon(icon = if (selected) destination.selectedIcon else destination.icon, contentDescription = null, tint = content)
            Text(text = destination.label, style = MajesticTheme.typography.caption, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        Row(
            modifier = itemModifier.padding(horizontal = MajesticSpacing.space3),
            horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MajesticIcon(icon = if (selected) destination.selectedIcon else destination.icon, contentDescription = null, tint = content)
            Text(text = destination.label, style = MajesticTheme.typography.label, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (destination.badgeCount > 0) CountBadge(destination.badgeCount)
        }
    }
}

/** Switches between views of the same content without leaving the screen. They scroll if they do not fit. */
@Composable
fun MajesticTabs(tabs: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MajesticTheme.colors
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = colors.surfaceRaised.copy(alpha = 0f),
        contentColor = colors.primary,
        edgePadding = 0.dp,
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedIndex, matchContentSize = true),
                width = 24.dp,
                height = 3.dp,
                color = colors.primary,
            )
        },
        divider = { HorizontalDivider(thickness = 1.dp, color = colors.line) },
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                selectedContentColor = colors.primary,
                unselectedContentColor = colors.inkMuted,
                text = { Text(text = tab, style = MajesticTheme.typography.label, maxLines = 1) },
            )
        }
    }
}
