package com.peakdevcol.project.hotelmanagement.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peakdevcol.project.hotelmanagement.domain.model.AppSection
import com.peakdevcol.project.hotelmanagement.domain.model.SectionGroup
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.logout
import com.peakdevcol.project.hotelmanagement.resources.nav_more
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Avatar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDivider
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListGroup
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIcon
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.initialsOf
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.employees.label
import org.jetbrains.compose.resources.stringResource

/**
 * "Más" on the phone: who is logged in, the sections of the role that are not in the bottom bar,
 * and logging out (RF-AUTH-03). On wider windows all of it is in the side navigation.
 */
@Composable
fun MoreScreen(state: MainState, onOpenSection: (AppSection) -> Unit, onIntent: (MainIntent) -> Unit) {
    val employee = state.employee
    val sections = state.navigation?.moreSections.orEmpty()
    Scaffold(
        topBar = { MajesticTopBar(title = stringResource(Res.string.nav_more)) },
        snackbarHost = { AppMessagesHost() },
        containerColor = MajesticTheme.colors.surface,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MajesticSpacing.space4, vertical = MajesticSpacing.space2),
            verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space6),
        ) {
            if (employee != null) {
                ListGroup {
                    ListRow(
                        title = employee.fullName,
                        subtitle = stringResource(employee.area.label()),
                        leading = { Avatar(initials = initialsOf(employee.fullName)) },
                    )
                }
            }
            SectionGroup.entries.forEach { group ->
                val ofGroup = sections.filter { it.group == group }
                if (ofGroup.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
                        SectionLabel(text = stringResource(group.title()))
                        ListGroup {
                            ofGroup.forEachIndexed { index, section ->
                                if (index > 0) ListDivider()
                                ListRow(
                                    title = stringResource(section.title()),
                                    leading = { MajesticIcon(icon = section.icon(), contentDescription = null, tint = MajesticTheme.colors.inkMuted) },
                                    trailing = { MajesticIcon(icon = MajesticIcons.chevronRight, contentDescription = null, tint = MajesticTheme.colors.inkMuted) },
                                    onClick = { onOpenSection(section) },
                                )
                            }
                        }
                    }
                }
            }
            MajesticButton(
                text = stringResource(Res.string.logout),
                onClick = { onIntent(MainIntent.LogOutClick) },
                style = ButtonStyle.DangerOutline,
                icon = MajesticIcons.logOut,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
