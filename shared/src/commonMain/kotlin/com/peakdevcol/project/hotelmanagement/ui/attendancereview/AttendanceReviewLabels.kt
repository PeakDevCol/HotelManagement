package com.peakdevcol.project.hotelmanagement.ui.attendancereview

import androidx.compose.runtime.Composable
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionError
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionKind
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionStep
import com.peakdevcol.project.hotelmanagement.domain.model.Mark
import com.peakdevcol.project.hotelmanagement.domain.model.MarkKind
import com.peakdevcol.project.hotelmanagement.domain.model.MarkSource
import com.peakdevcol.project.hotelmanagement.domain.model.PeriodPreset
import com.peakdevcol.project.hotelmanagement.domain.model.ReportError
import com.peakdevcol.project.hotelmanagement.domain.model.formatBogotaTime
import com.peakdevcol.project.hotelmanagement.domain.model.formatThousands
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.correction_error_changed
import com.peakdevcol.project.hotelmanagement.resources.correction_error_exit_before_entry
import com.peakdevcol.project.hotelmanagement.resources.correction_error_in_future
import com.peakdevcol.project.hotelmanagement.resources.correction_error_invalid_time
import com.peakdevcol.project.hotelmanagement.resources.correction_error_missing_reason
import com.peakdevcol.project.hotelmanagement.resources.correction_error_network
import com.peakdevcol.project.hotelmanagement.resources.correction_error_not_allowed
import com.peakdevcol.project.hotelmanagement.resources.correction_error_nothing_to_choose
import com.peakdevcol.project.hotelmanagement.resources.correction_error_too_long
import com.peakdevcol.project.hotelmanagement.resources.correction_error_unknown
import com.peakdevcol.project.hotelmanagement.resources.editor_clock_kiosk
import com.peakdevcol.project.hotelmanagement.resources.editor_clock_phone
import com.peakdevcol.project.hotelmanagement.resources.editor_clock_server
import com.peakdevcol.project.hotelmanagement.resources.editor_entry
import com.peakdevcol.project.hotelmanagement.resources.editor_entry_lower
import com.peakdevcol.project.hotelmanagement.resources.editor_exit
import com.peakdevcol.project.hotelmanagement.resources.editor_exit_lower
import com.peakdevcol.project.hotelmanagement.resources.editor_source_admin
import com.peakdevcol.project.hotelmanagement.resources.editor_source_kiosk
import com.peakdevcol.project.hotelmanagement.resources.editor_source_phone
import com.peakdevcol.project.hotelmanagement.resources.history_changed
import com.peakdevcol.project.hotelmanagement.resources.history_confirmed
import com.peakdevcol.project.hotelmanagement.resources.history_registered
import com.peakdevcol.project.hotelmanagement.resources.history_set
import com.peakdevcol.project.hotelmanagement.resources.order_error_no_session
import com.peakdevcol.project.hotelmanagement.resources.report_error_invalid_period
import com.peakdevcol.project.hotelmanagement.resources.report_error_network
import com.peakdevcol.project.hotelmanagement.resources.report_error_not_allowed
import com.peakdevcol.project.hotelmanagement.resources.report_error_period_too_long
import com.peakdevcol.project.hotelmanagement.resources.report_error_unknown
import com.peakdevcol.project.hotelmanagement.resources.review_preset_last_fortnight
import com.peakdevcol.project.hotelmanagement.resources.review_preset_this_fortnight
import com.peakdevcol.project.hotelmanagement.resources.review_preset_this_month
import com.peakdevcol.project.hotelmanagement.resources.review_preset_this_week
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

fun PeriodPreset.label(): StringResource = when (this) {
    PeriodPreset.THIS_FORTNIGHT -> Res.string.review_preset_this_fortnight
    PeriodPreset.LAST_FORTNIGHT -> Res.string.review_preset_last_fortnight
    PeriodPreset.THIS_WEEK -> Res.string.review_preset_this_week
    PeriodPreset.THIS_MONTH -> Res.string.review_preset_this_month
}

fun MarkKind.title(): StringResource = when (this) {
    MarkKind.ENTRY -> Res.string.editor_entry
    MarkKind.EXIT -> Res.string.editor_exit
}

private fun MarkKind.lowerName(): StringResource = when (this) {
    MarkKind.ENTRY -> Res.string.editor_entry_lower
    MarkKind.EXIT -> Res.string.editor_exit_lower
}

/** "Celular, a 40 m del hotel": where the mark was made, never the coordinates (RF-ASI-10). */
@Composable
fun Mark.sourceLabel(): String = when (source) {
    MarkSource.PHONE -> stringResource(Res.string.editor_source_phone, formatThousands((distanceMeters ?: 0).toLong()))
    MarkSource.KIOSK -> stringResource(Res.string.editor_source_kiosk)
    MarkSource.ADMIN -> stringResource(Res.string.editor_source_admin)
}

/** "hora del celular" or "hora del computador", for the clock of the device that marked. */
fun Mark.deviceClockName(): StringResource =
    if (source == MarkSource.KIOSK) Res.string.editor_clock_kiosk else Res.string.editor_clock_phone

/** RF-ASI-07: what a correction changed, as the history tells it. */
@Composable
fun CorrectionStep.description(record: AttendanceRecord): String {
    val mark = correction.mark ?: return stringResource(Res.string.history_registered)
    val markName = stringResource(mark.lowerName())
    val time = correction.time?.let(::formatBogotaTime).orEmpty()
    val previous = previousTime
    return when {
        correction.kind == CorrectionKind.DEVICE_TIME -> {
            val clock = record.markOf(mark)?.deviceClockName() ?: Res.string.editor_clock_phone
            stringResource(Res.string.history_confirmed, markName, time, stringResource(clock))
        }
        correction.kind == CorrectionKind.SERVER_TIME -> stringResource(Res.string.history_confirmed, markName, time, stringResource(Res.string.editor_clock_server))
        previous == null -> stringResource(Res.string.history_set, markName, time)
        else -> stringResource(Res.string.history_changed, markName, formatBogotaTime(previous), time)
    }
}

fun ReportError.messageRes(): StringResource = when (this) {
    ReportError.InvalidPeriod -> Res.string.report_error_invalid_period
    ReportError.PeriodTooLong -> Res.string.report_error_period_too_long
    ReportError.NotAllowed -> Res.string.report_error_not_allowed
    ReportError.Network -> Res.string.report_error_network
    ReportError.Unknown -> Res.string.report_error_unknown
}

fun CorrectionError.messageRes(): StringResource = when (this) {
    CorrectionError.InvalidTime -> Res.string.correction_error_invalid_time
    CorrectionError.MissingReason -> Res.string.correction_error_missing_reason
    CorrectionError.InFuture -> Res.string.correction_error_in_future
    CorrectionError.ExitBeforeEntry -> Res.string.correction_error_exit_before_entry
    CorrectionError.TooLong -> Res.string.correction_error_too_long
    CorrectionError.NothingToChoose -> Res.string.correction_error_nothing_to_choose
    CorrectionError.Changed -> Res.string.correction_error_changed
    CorrectionError.NoSession -> Res.string.order_error_no_session
    CorrectionError.NotAllowed -> Res.string.correction_error_not_allowed
    CorrectionError.Network -> Res.string.correction_error_network
    CorrectionError.Unknown -> Res.string.correction_error_unknown
}
