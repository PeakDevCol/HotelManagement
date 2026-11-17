package com.peakdevcol.project.hotelmanagement.ui.employees

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.employees_empty
import com.peakdevcol.project.hotelmanagement.resources.employees_error
import com.peakdevcol.project.hotelmanagement.resources.employees_inactive
import com.peakdevcol.project.hotelmanagement.resources.employees_new
import com.peakdevcol.project.hotelmanagement.resources.employees_row_detail
import com.peakdevcol.project.hotelmanagement.resources.ic_add
import com.peakdevcol.project.hotelmanagement.resources.menu_employees
import com.peakdevcol.project.hotelmanagement.ui.components.ScreenTopBar
import com.peakdevcol.project.hotelmanagement.ui.currentorders.CenteredMessage
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EmployeesScreen(
    onBack: () -> Unit,
    onEmployeeClick: (employeeId: String?) -> Unit,
    viewModel: EmployeesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is EmployeesEffect.NavigateToEmployee -> onEmployeeClick(effect.employeeId)
            }
        }
    }

    Scaffold(
        topBar = { ScreenTopBar(title = stringResource(Res.string.menu_employees), onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onIntent(EmployeesIntent.NewEmployee) }) {
                Icon(painter = painterResource(Res.drawable.ic_add), contentDescription = stringResource(Res.string.employees_new))
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.loadFailed -> CenteredMessage(
                message = stringResource(Res.string.employees_error),
                modifier = Modifier.padding(padding),
                onRetry = { viewModel.onIntent(EmployeesIntent.Retry) },
            )
            state.employees.isEmpty() -> CenteredMessage(stringResource(Res.string.employees_empty), Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 88.dp),
            ) {
                items(state.employees, key = { it.id }) { employee ->
                    EmployeeRow(employee, onClick = { viewModel.onIntent(EmployeesIntent.OpenEmployee(employee.id)) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun EmployeeRow(employee: User, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = employee.fullName,
            style = MaterialTheme.typography.bodyLarge,
            color = if (employee.active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = employee.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = stringResource(
                Res.string.employees_row_detail,
                stringResource(employee.type.label()),
                stringResource(employee.area.label()),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!employee.active) {
            Text(
                text = stringResource(Res.string.employees_inactive),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
