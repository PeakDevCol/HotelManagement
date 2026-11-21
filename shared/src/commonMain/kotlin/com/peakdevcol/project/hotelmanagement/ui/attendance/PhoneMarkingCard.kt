package com.peakdevcol.project.hotelmanagement.ui.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.MarkDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.MarkKind
import com.peakdevcol.project.hotelmanagement.domain.model.MarkOutcome
import com.peakdevcol.project.hotelmanagement.domain.model.formatBogotaTime
import com.peakdevcol.project.hotelmanagement.domain.model.formatWeekday
import com.peakdevcol.project.hotelmanagement.domain.model.operationalDateOf
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.marking_checking
import com.peakdevcol.project.hotelmanagement.resources.marking_entered_at
import com.peakdevcol.project.hotelmanagement.resources.marking_entry
import com.peakdevcol.project.hotelmanagement.resources.marking_entry_done
import com.peakdevcol.project.hotelmanagement.resources.marking_entry_failed
import com.peakdevcol.project.hotelmanagement.resources.marking_entry_offline
import com.peakdevcol.project.hotelmanagement.resources.marking_exit
import com.peakdevcol.project.hotelmanagement.resources.marking_exit_done
import com.peakdevcol.project.hotelmanagement.resources.marking_exit_failed
import com.peakdevcol.project.hotelmanagement.resources.marking_exit_offline
import com.peakdevcol.project.hotelmanagement.resources.marking_forgotten
import com.peakdevcol.project.hotelmanagement.resources.marking_locating
import com.peakdevcol.project.hotelmanagement.resources.marking_needs_review
import com.peakdevcol.project.hotelmanagement.resources.marking_no_entry
import com.peakdevcol.project.hotelmanagement.resources.marking_offline_title
import com.peakdevcol.project.hotelmanagement.resources.marking_pending
import com.peakdevcol.project.hotelmanagement.resources.marking_privacy
import com.peakdevcol.project.hotelmanagement.resources.marking_title
import com.peakdevcol.project.hotelmanagement.resources.marking_worked_so_far
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Banner
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.BannerTone
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonSize
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIcon
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionCard
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import org.jetbrains.compose.resources.stringResource

/**
 * RF-ASI-03 to RF-ASI-05: one big button, entry or exit, whichever comes next. What happened stays
 * written under it; when it failed, the message says the way out, and the same button tries again.
 */
@Composable
fun PhoneMarkingCard(marking: PhoneMarking, onIntent: (AttendanceIntent) -> Unit) {
    val entry = marking.nextMark == MarkKind.ENTRY
    SectionCard(title = stringResource(Res.string.marking_title)) {
        EntryStatus(marking)
        MajesticButton(
            text = stringResource(if (entry) Res.string.marking_entry else Res.string.marking_exit),
            onClick = { onIntent(AttendanceIntent.Mark) },
            size = ButtonSize.Large,
            icon = if (entry) MajesticIcons.logIn else MajesticIcons.logOut,
            enabled = !marking.isLoading,
            loading = marking.isMarking,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(if (marking.isMarking) Res.string.marking_locating else Res.string.marking_privacy),
            style = MajesticTheme.typography.caption,
            color = MajesticTheme.colors.inkMuted,
        )
        marking.result?.let { MarkResultBanner(it) }
    }
}

@Composable
private fun EntryStatus(marking: PhoneMarking) {
    val current = marking.currentEntry
    val forgotten = marking.forgottenEntry
    Row(horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space3), verticalAlignment = Alignment.CenterVertically) {
        MajesticIcon(
            icon = if (current == null) MajesticIcons.schedule else MajesticIcons.timelapse,
            contentDescription = null,
            tint = if (current == null) MajesticTheme.colors.inkMuted else MajesticTheme.colors.primary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when {
                    marking.isLoading -> stringResource(Res.string.marking_checking)
                    current == null -> stringResource(Res.string.marking_no_entry)
                    else -> stringResource(Res.string.marking_entered_at, formatBogotaTime(current.entryTime))
                },
                style = MajesticTheme.typography.bodyLarge,
                color = MajesticTheme.colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (current != null) {
                Text(
                    text = current.progress(marking),
                    style = MajesticTheme.typography.caption,
                    color = MajesticTheme.colors.inkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    if (current == null && forgotten != null) {
        val clockIn = forgotten.entryTime
        Banner(
            tone = BannerTone.Warning,
            icon = MajesticIcons.warning,
            message = stringResource(Res.string.marking_forgotten, formatWeekday(operationalDateOf(clockIn, dayStartMinutes = 0)), formatBogotaTime(clockIn)),
        )
    }
}

/** "Llevas 3 h 15 min · pendiente de enviar". */
@Composable
private fun AttendanceRecord.progress(marking: PhoneMarking): String = listOfNotNull(
    stringResource(Res.string.marking_worked_so_far, durationLabel(marking.now - entryTime)),
    if (clockIn.at == null) stringResource(Res.string.marking_pending) else null,
    if (needsReview) stringResource(Res.string.marking_needs_review) else null,
).joinToString(" · ")

@Composable
private fun MarkResultBanner(result: MarkResult) {
    when (result) {
        is MarkResult.Done -> DoneBanner(result.outcome)
        is MarkResult.Failed -> Banner(
            tone = BannerTone.Danger,
            icon = MajesticIcons.error,
            title = stringResource(if (result.kind == MarkKind.ENTRY) Res.string.marking_entry_failed else Res.string.marking_exit_failed),
            message = result.error.text(),
        )
    }
}

@Composable
private fun DoneBanner(outcome: MarkOutcome) {
    val time = formatBogotaTime(outcome.time)
    val worked = outcome.workedTime?.let { durationLabel(it) }.orEmpty()
    if (outcome.delivery == MarkDelivery.PENDING) {
        Banner(
            tone = BannerTone.Warning,
            icon = MajesticIcons.cloudUpload,
            title = stringResource(Res.string.marking_offline_title),
            message = if (outcome.kind == MarkKind.ENTRY) {
                stringResource(Res.string.marking_entry_offline, time)
            } else {
                stringResource(Res.string.marking_exit_offline, time, worked)
            },
        )
    } else {
        Banner(
            tone = BannerTone.Info,
            icon = MajesticIcons.checkCircle,
            message = if (outcome.kind == MarkKind.ENTRY) {
                stringResource(Res.string.marking_entry_done, time)
            } else {
                stringResource(Res.string.marking_exit_done, time, worked)
            },
        )
    }
}
