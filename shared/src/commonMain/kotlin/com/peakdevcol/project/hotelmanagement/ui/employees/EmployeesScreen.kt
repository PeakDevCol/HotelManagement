package com.peakdevcol.project.hotelmanagement.ui.employees

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.employees_empty_message
import com.peakdevcol.project.hotelmanagement.resources.employees_empty_title
import com.peakdevcol.project.hotelmanagement.resources.employees_error
import com.peakdevcol.project.hotelmanagement.resources.employees_inactive
import com.peakdevcol.project.hotelmanagement.resources.employees_new
import com.peakdevcol.project.hotelmanagement.resources.employees_select_message
import com.peakdevcol.project.hotelmanagement.resources.employees_select_title
import com.peakdevcol.project.hotelmanagement.resources.error_check_connection
import com.peakdevcol.project.hotelmanagement.resources.menu_employees
import com.peakdevcol.project.hotelmanagement.resources.retry
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Avatar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDetailLayout
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDetailMinWidth
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDivider
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListGroup
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticFloatingActionButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIcon
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.initialsOf
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import com.peakdevcol.project.hotelmanagement.ui.main.LocalShowsMessages
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** RF-EMP-01: every employee, also the inactive ones. */
@Composable
fun EmployeesScreen(onOpenEmployee: (employeeId: String?) -> Unit, viewModel: EmployeesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is EmployeesEffect.NavigateToEmployee -> onOpenEmployee(effect.employeeId)
            }
        }
    }

    EmployeesContent(
        state = state,
        onIntent = viewModel::onIntent,
        employeeForm = { employeeId, onClose -> EmployeeFormScreen(employeeId = employeeId, onBack = onClose) },
    )
}

/** On wide windows the chosen employee opens beside the list, drawn by [employeeForm]. */
@Composable
fun EmployeesContent(
    state: EmployeesState,
    onIntent: (EmployeesIntent) -> Unit,
    employeeForm: @Composable (employeeId: String?, onClose: () -> Unit) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val besideList = maxWidth >= ListDetailMinWidth
        Scaffold(
            topBar = {
                MajesticTopBar(title = stringResource(Res.string.menu_employees)) {
                    if (besideList) {
                        MajesticButton(
                            text = stringResource(Res.string.employees_new),
                            onClick = { onIntent(EmployeesIntent.NewEmployee(besideList = true)) },
                            icon = MajesticIcons.add,
                            modifier = Modifier.padding(end = MajesticSpacing.space3),
                        )
                    }
                }
            },
            floatingActionButton = {
                if (!besideList) {
                    MajesticFloatingActionButton(
                        text = stringResource(Res.string.employees_new),
                        icon = MajesticIcons.add,
                        onClick = { onIntent(EmployeesIntent.NewEmployee(besideList = false)) },
                    )
                }
            },
            snackbarHost = { AppMessagesHost() },
            containerColor = MajesticTheme.colors.surface,
        ) { padding ->
            if (besideList) {
                ListDetailLayout(
                    modifier = Modifier.padding(padding),
                    list = { EmployeeList(state = state, onIntent = onIntent, besideList = true) },
                    detail = { OpenEmployeePane(openEmployee = state.openEmployee, onIntent = onIntent, employeeForm = employeeForm) },
                )
            } else {
                EmployeeList(state = state, onIntent = onIntent, besideList = false, modifier = Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun OpenEmployeePane(
    openEmployee: OpenEmployee?,
    onIntent: (EmployeesIntent) -> Unit,
    employeeForm: @Composable (employeeId: String?, onClose: () -> Unit) -> Unit,
) {
    if (openEmployee == null) {
        EmptyState(
            icon = MajesticIcons.employees,
            title = stringResource(Res.string.employees_select_title),
            message = stringResource(Res.string.employees_select_message),
        )
        return
    }
    CompositionLocalProvider(LocalShowsMessages provides false) {
        key(openEmployee) { employeeForm(openEmployee.employeeId) { onIntent(EmployeesIntent.CloseEmployee) } }
    }
}

@Composable
private fun EmployeeList(state: EmployeesState, onIntent: (EmployeesIntent) -> Unit, besideList: Boolean, modifier: Modifier = Modifier) {
    when {
        state.isLoading -> MajesticLoading(modifier)
        state.loadFailed -> EmptyState(
            icon = MajesticIcons.error,
            title = stringResource(Res.string.employees_error),
            message = stringResource(Res.string.error_check_connection),
            actionText = stringResource(Res.string.retry),
            onAction = { onIntent(EmployeesIntent.Retry) },
            modifier = modifier,
        )
        state.employees.isEmpty() -> EmptyState(
            icon = MajesticIcons.employees,
            title = stringResource(Res.string.employees_empty_title),
            message = stringResource(Res.string.employees_empty_message),
            modifier = modifier,
        )
        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MajesticSpacing.space4,
                end = MajesticSpacing.space4,
                top = MajesticSpacing.space2,
                // Room for the floating button, so it never covers the last employee.
                bottom = if (besideList) MajesticSpacing.space4 else MajesticSizes.floatingActionButtonClearance,
            ),
        ) {
            item(key = "employees") {
                ListGroup {
                    state.employees.forEachIndexed { index, employee ->
                        if (index > 0) ListDivider()
                        EmployeeRow(
                            employee = employee,
                            selected = besideList && state.openEmployee?.employeeId == employee.id,
                            onClick = { onIntent(EmployeesIntent.SelectEmployee(employee.id, besideList)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeRow(employee: User, selected: Boolean, onClick: () -> Unit) {
    val details = listOfNotNull(
        if (employee.active) null else stringResource(Res.string.employees_inactive),
        stringResource(employee.type.label()),
        stringResource(employee.area.label()),
    )
    ListRow(
        title = employee.fullName,
        subtitle = details.joinToString(" · "),
        leading = { Avatar(initials = initialsOf(employee.fullName).takeIf { employee.active }, icon = MajesticIcons.person) },
        trailing = if (selected) ({ MajesticIcon(icon = MajesticIcons.check, contentDescription = null, tint = MajesticTheme.colors.primary) }) else null,
        onClick = onClick,
    )
}
