package com.peakdevcol.project.hotelmanagement.ui.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.Shift
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.error_check_connection
import com.peakdevcol.project.hotelmanagement.resources.my_schedule_empty_message
import com.peakdevcol.project.hotelmanagement.resources.my_schedule_empty_title
import com.peakdevcol.project.hotelmanagement.resources.my_schedule_error
import com.peakdevcol.project.hotelmanagement.resources.my_schedule_hint
import com.peakdevcol.project.hotelmanagement.resources.my_schedule_title
import com.peakdevcol.project.hotelmanagement.resources.nav_attendance
import com.peakdevcol.project.hotelmanagement.resources.retry
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDivider
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListGroup
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIcon
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** RF-ASI-02: the shifts of the employee. The second PR of M8 puts marking the entry and the exit on top. */
@Composable
fun AttendanceScreen(onBack: (() -> Unit)? = null, viewModel: AttendanceViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AttendanceContent(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

/** [onBack] is the arrow back to "Más", when the section was opened from there on the phone. */
@Composable
fun AttendanceContent(state: AttendanceState, onIntent: (AttendanceIntent) -> Unit, onBack: (() -> Unit)? = null) {
    Scaffold(
        topBar = { MajesticTopBar(title = stringResource(Res.string.nav_attendance), onBack = onBack) },
        snackbarHost = { AppMessagesHost() },
        containerColor = MajesticTheme.colors.surface,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = MajesticSizes.content)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(MajesticSpacing.space4),
                verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space4),
            ) {
                Column {
                    Text(text = stringResource(Res.string.my_schedule_title), style = MajesticTheme.typography.title, color = MajesticTheme.colors.ink)
                    Text(text = stringResource(Res.string.my_schedule_hint), style = MajesticTheme.typography.caption, color = MajesticTheme.colors.inkMuted)
                }
                MySchedule(state = state, onIntent = onIntent)
            }
        }
    }
}

@Composable
private fun MySchedule(state: AttendanceState, onIntent: (AttendanceIntent) -> Unit) {
    when {
        state.isLoading -> MajesticLoading()
        state.loadFailed -> EmptyState(
            icon = MajesticIcons.error,
            title = stringResource(Res.string.my_schedule_error),
            message = stringResource(Res.string.error_check_connection),
            actionText = stringResource(Res.string.retry),
            onAction = { onIntent(AttendanceIntent.Retry) },
        )
        state.days.isEmpty() -> EmptyState(
            icon = MajesticIcons.calendar,
            title = stringResource(Res.string.my_schedule_empty_title),
            message = stringResource(Res.string.my_schedule_empty_message),
        )
        else -> state.days.forEach { day ->
            Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
                SectionLabel(text = dayLabel(day.date, state.today))
                ListGroup {
                    day.shifts.forEachIndexed { index, shift ->
                        if (index > 0) ListDivider()
                        ShiftRow(shift)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftRow(shift: Shift) {
    ListRow(
        title = shift.timeRange(),
        subtitle = shift.details(),
        leading = { MajesticIcon(icon = MajesticIcons.schedule, contentDescription = null, tint = MajesticTheme.colors.inkMuted) },
    )
}
