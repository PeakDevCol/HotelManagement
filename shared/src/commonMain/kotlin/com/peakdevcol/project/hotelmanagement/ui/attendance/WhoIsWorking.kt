package com.peakdevcol.project.hotelmanagement.ui.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.peakdevcol.project.hotelmanagement.domain.model.formatBogotaTime
import com.peakdevcol.project.hotelmanagement.domain.usecase.WorkingEntry
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.working_empty
import com.peakdevcol.project.hotelmanagement.resources.working_error
import com.peakdevcol.project.hotelmanagement.resources.working_hint
import com.peakdevcol.project.hotelmanagement.resources.working_since
import com.peakdevcol.project.hotelmanagement.resources.working_title
import com.peakdevcol.project.hotelmanagement.ui.currentorders.employeeNameOrUnknown
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Avatar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Banner
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.BannerTone
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDivider
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListGroup
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.initialsOf
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import org.jetbrains.compose.resources.stringResource

/** RF-ASI-09: who has the entry marked, since when and for how long, for reception and administrators. */
@Composable
fun WhoIsWorking(state: WhoIsWorkingState) {
    if (!state.isVisible) return
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        Column {
            Text(text = stringResource(Res.string.working_title), style = MajesticTheme.typography.title, color = MajesticTheme.colors.ink)
            Text(text = stringResource(Res.string.working_hint), style = MajesticTheme.typography.caption, color = MajesticTheme.colors.inkMuted)
        }
        val working = state.working
        when {
            state.loadFailed -> Banner(tone = BannerTone.Danger, icon = MajesticIcons.error, message = stringResource(Res.string.working_error))
            working.isEmpty() -> Text(text = stringResource(Res.string.working_empty), style = MajesticTheme.typography.body, color = MajesticTheme.colors.inkMuted)
            else -> ListGroup {
                working.forEachIndexed { index, entry ->
                    if (index > 0) ListDivider()
                    WorkingRow(entry = entry, state = state)
                }
            }
        }
    }
}

@Composable
private fun WorkingRow(entry: WorkingEntry, state: WhoIsWorkingState) {
    val record = entry.entry.record
    val name = employeeNameOrUnknown(entry.employeeName)
    ListRow(
        title = name,
        subtitle = state.now?.let { now -> stringResource(Res.string.working_since, formatBogotaTime(record.entryTime), durationLabel(now - record.entryTime)) },
        leading = { Avatar(initials = initialsOf(name)) },
    )
}
