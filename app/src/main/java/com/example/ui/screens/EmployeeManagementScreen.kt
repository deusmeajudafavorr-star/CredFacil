package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Employee
import com.example.ui.viewmodel.CrediarioViewModel
import com.example.ui.viewmodel.EmployeeWithStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeManagementScreen(
    viewModel: CrediarioViewModel,
    modifier: Modifier = Modifier
) {
    val employeesWithStats by viewModel.employeesWithStats.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) } // 0 = Vendedores, 1 = Cobradores
    var showAddDialog by remember { mutableStateOf(false) }
    var employeeToDelete by remember { mutableStateOf<Employee?>(null) }
    
    // Filtered lists based on tab
    val filteredEmployees = remember(employeesWithStats, selectedTab) {
        val targetRole = if (selectedTab == 0) "Vendedor" else "Cobrador"
        employeesWithStats.filter { it.employee.role == targetRole }
    }
    
    val totalTabVolume = remember(filteredEmployees) {
        filteredEmployees.sumOf { it.totalVolume }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("employee_management_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Screen Header
            Text(
                text = "Gestão de Colaboradores",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Cadastre, gerencie e acompanhe a performance de vendedores e cobradores.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Tabs to separate roles
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("employee_role_tabs"),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storefront, contentDescription = null, size = 18.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Vendedores", fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("tab_vendedores")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RequestQuote, contentDescription = null, size = 18.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Cobradores", fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("tab_cobradores")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Aggregate metrics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = if (selectedTab == 0) "Volume de Vendas da Equipe" else "Volume Total de Cobranças",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR")).format(totalTabVolume),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${filteredEmployees.size} Colaborador(es) listado(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // List of Employees
            if (filteredEmployees.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Default.Storefront else Icons.Default.RequestQuote,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nenhum cadastrado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedTab == 0) {
                                "Nenhum vendedor registrado ainda. Toque no botão + para adicionar um novo vendedor à equipe."
                            } else {
                                "Nenhum cobrador registrado ainda. Toque no botão + para adicionar um novo cobrador à equipe."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("employees_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredEmployees, key = { it.employee.id }) { empWithStats ->
                        EmployeeRowItem(
                            empWithStats = empWithStats,
                            selectedTab = selectedTab,
                            onDeleteClick = { employeeToDelete = empWithStats.employee }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_employee_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Adicionar Funcionário")
        }
    }

    // Modal popup to Add Employee
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var role by remember { mutableStateOf(if (selectedTab == 0) "Vendedor" else "Cobrador") }
        var expandedRoleDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Novo Funcionário", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome Completo *") },
                        singleLine = true,
                        placeholder = { Text("Ex: Carlos Silva") },
                        modifier = Modifier.fillMaxWidth().testTag("input_employee_name")
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Telefone / Contato") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        placeholder = { Text("Ex: (11) 98888-7777") },
                        modifier = Modifier.fillMaxWidth().testTag("input_employee_phone")
                    )

                    // Role selector Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = role,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cargo / Função *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_employee_role")
                                .clickable { expandedRoleDropdown = true },
                            trailingIcon = {
                                IconButton(onClick = { expandedRoleDropdown = !expandedRoleDropdown }) {
                                    Icon(
                                        imageVector = if (expandedRoleDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Expand role dropdown"
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            enabled = false
                        )
                        DropdownMenu(
                            expanded = expandedRoleDropdown,
                            onDismissRequest = { expandedRoleDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Vendedor") },
                                onClick = {
                                    role = "Vendedor"
                                    expandedRoleDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Cobrador") },
                                onClick = {
                                    role = "Cobrador"
                                    expandedRoleDropdown = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addEmployee(name, role, phone)
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_employee_button"),
                    enabled = name.isNotBlank()
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    modifier = Modifier.testTag("cancel_employee_button")
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Delete confirmation prompt
    employeeToDelete?.let { employee ->
        AlertDialog(
            onDismissRequest = { employeeToDelete = null },
            title = { Text("Remover Funcionário?", fontWeight = FontWeight.Bold) },
            text = { Text("Tem certeza de que deseja remover ${employee.name} de sua equipe? Esta ação é definitiva.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEmployee(employee)
                        employeeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remover", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { employeeToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun EmployeeRowItem(
    empWithStats: EmployeeWithStats,
    selectedTab: Int,
    onDeleteClick: () -> Unit
) {
    val emp = empWithStats.employee
    val initials = if (emp.name.isNotBlank()) {
        emp.name.split(" ").take(2).map { it.take(1) }.joinToString("").uppercase()
    } else {
        "?"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("employee_row_item_${emp.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle with Initials
            Surface(
                shape = CircleShape,
                color = if (selectedTab == 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initials,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selectedTab == 0) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = emp.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (emp.phone.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = emp.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Performance Statistics
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = if (selectedTab == 0) "Vendas" else "Cobranças",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = empWithStats.formatVolume(),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete action button
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.testTag("delete_employee_btn_${emp.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Deletar funcionário",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// Icon helper function for Tabs
@Composable
private fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp) {
    androidx.compose.material3.Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = androidx.compose.ui.Modifier.size(size)
    )
}
