package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.model.Customer
import com.example.data.model.CustomerWithStats
import com.example.data.model.Employee
import com.example.ui.viewmodel.CrediarioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

@Composable
fun CustomerScreen(
    viewModel: CrediarioViewModel,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.filteredCustomers.collectAsState()
    val searchQuery by viewModel.clientSearchQuery.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val selectedRole by viewModel.selectedRole.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("customer_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Clientes",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Gerenciamento de clientes e endereços",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Profile Indicator Badge
                    val (label, badgeColor, roleIcon) = when (selectedRole) {
                        com.example.ui.viewmodel.UserRole.VENDEDOR -> Triple("VENDEDOR 🔵", Color(0xFF2196F3), Icons.Default.AddShoppingCart)
                        com.example.ui.viewmodel.UserRole.COBRADOR -> Triple("COBRADOR 🟠", Color(0xFFFFA000), Icons.Default.ReceiptLong)
                        com.example.ui.viewmodel.UserRole.ADMINISTRADOR -> Triple("ADMINISTRADOR 🟢", Color(0xFF4CAF50), Icons.Default.AdminPanelSettings)
                        else -> Triple("", MaterialTheme.colorScheme.outline, Icons.Default.Person)
                    }
                    if (label.isNotEmpty()) {
                        Surface(
                            color = badgeColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f)),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = roleIcon,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = badgeColor
                                )
                            }
                        }
                    }
                }
                
                IconButton(
                    onClick = { showAddDialog = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("add_customer_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Customer")
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.clientSearchQuery.value = it },
                placeholder = { Text("Buscar cliente por nome ou CPF...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clientSearchQuery.value = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_search_bar"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            // Customers List
            if (customers.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.People,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "Nenhum cliente cadastrado" else "Nenhum cliente correspondente",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "Clique no botão '+' acima para cadastrar seu primeiro cliente do crediário." else "Tente buscar com termos diferentes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(customers, key = { it.customer.id }) { customerWithStats ->
                        CustomerCard(
                            customerWithStats = customerWithStats,
                            onClick = { viewModel.showCustomerDetails(customerWithStats) }
                        )
                    }
                }
            }
        }

        // Expanded Customer Detail Flow/Modal (Covers Edit & Delete & History)
        selectedCustomer?.let { customerWithStats ->
            CustomerDetailsModal(
                customerWithStats = customerWithStats,
                viewModel = viewModel,
                onDismiss = { viewModel.closeCustomerDetails() }
            )
        }

        // Add Customer Dialog
        if (showAddDialog) {
            AddCustomerDialog(
                onDismiss = { showAddDialog = false },
                onAddConfirm = { name, cpf, phone, email, address ->
                    viewModel.createCustomer(name, cpf, phone, email, address)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun CustomerCard(
    customerWithStats: CustomerWithStats,
    onClick: () -> Unit
) {
    val customer = customerWithStats.customer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("customer_card_${customer.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Basic details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = if (customer.synced) "Sincronizado" else "Disponível apenas offline",
                            tint = if (customer.synced) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "CPF: ${formatCpf(customer.cpf)} • Tel: ${formatPhone(customer.phone)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Indicators for overdue debt
                if (customerWithStats.overdueCount > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${customerWithStats.overdueCount} em atraso",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)

            // Debt info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Débito em aberto:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = customerWithStats.formatTotalDebt(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (customerWithStats.totalDebt > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (customer.address.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Endereço",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = customer.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// Dialog to Register Customers
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onAddConfirm: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Novo Cliente do Crediário",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome Completo *") },
                        modifier = Modifier.fillMaxWidth().testTag("input_customer_name"),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = cpf,
                        onValueChange = { cpf = it },
                        label = { Text("CPF *") },
                        modifier = Modifier.fillMaxWidth().testTag("input_customer_cpf"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Celular/Telefone *") },
                        modifier = Modifier.fillMaxWidth().testTag("input_customer_phone"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("Endereço Completo *") },
                        placeholder = { Text("Rua, número, bairro, cidade...") },
                        modifier = Modifier.fillMaxWidth().testTag("input_customer_address"),
                        singleLine = false,
                        maxLines = 3
                    )
                }
                item {
                    Text(
                        "* Campos marcados com asterisco são obrigatórios.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAddConfirm(name, cpf, phone, email, addressInput)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("CADASTRAR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// Expanded Modal detailing user profiles, credit lists, and configuration operations
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsModal(
    customerWithStats: CustomerWithStats,
    viewModel: CrediarioViewModel,
    onDismiss: () -> Unit
) {
    val customer = customerWithStats.customer
    
    // Edit Screen State Mode
    var isEditMode by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(customer.name) }
    var editCpf by remember { mutableStateOf(customer.cpf) }
    var editPhone by remember { mutableStateOf(customer.phone) }
    var editEmail by remember { mutableStateOf(customer.email) }
    var editAddress by remember { mutableStateOf(customer.address) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Fetch installments lists associated with this customer
    val allInstallments by viewModel.allInstallmentsDetailed.collectAsState()
    val customersInstallments = remember(allInstallments, customer.id) {
        allInstallments.filter { it.installment.customerId == customer.id }
            .sortedBy { it.installment.dueDate }
    }

    var selectedInstallmentToPay by remember { mutableStateOf<com.example.data.model.InstallmentWithDetails?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("customer_detail_modal"),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header of Drawer / Modal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditMode) "Editar Cadastro" else "Ficha do Cliente",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                if (!isEditMode) {
                    // Profile Dashboard
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val unpaidInstallments = remember(customersInstallments) {
                            customersInstallments.filter { it.installment.paidDate == null }
                        }
                        val paidInstallments = remember(customersInstallments) {
                            customersInstallments.filter { it.installment.paidDate != null }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = customer.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("CPF: ${formatCpf(customer.cpf)}")
                                        Text("Tel: ${formatPhone(customer.phone)}")
                                        if (customer.email.isNotEmpty()) {
                                            Text("E-mail: ${customer.email}")
                                        }
                                        Text("Cadastrado em: ${formatTimestampDate(customer.createdAt)}")
                                    }
                                }
                            }

                            item {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = { isEditMode = true },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Editar Ficha")
                                    }
                                    Button(
                                        onClick = { showDeleteConfirm = true },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f).testTag("delete_customer_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Excluir Cliente")
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = "Histórico de Cobranças / Crediário",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }

                            if (unpaidInstallments.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Parcelas Em Aberto (${unpaidInstallments.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(unpaidInstallments) { detailed ->
                                    val inst = detailed.installment
                                    val isOverdueState = inst.isOverdue()
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isOverdueState) {
                                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            }
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = detailed.saleDescription,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = "Parcela ${inst.installmentNumber} • Vence em: ${inst.formatDueDate()}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                if (isOverdueState) {
                                                    Text(
                                                        text = "Atrasada",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.error,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                Text(
                                                    text = inst.formatAmount(),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isOverdueState) {
                                                        MaterialTheme.colorScheme.error
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                                
                                                Spacer(modifier = Modifier.height(4.dp))
                                                
                                                Button(
                                                    onClick = { selectedInstallmentToPay = detailed },
                                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                                    modifier = Modifier.height(28.dp),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text("Receber", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (paidInstallments.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Parcelas Pagas (${paidInstallments.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                    )
                                }
                                items(paidInstallments) { detailed ->
                                    val inst = detailed.installment
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = detailed.saleDescription,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = "Parcela ${inst.installmentNumber} • Venceu em: ${inst.formatDueDate()}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                val paymentDate = formatTimestampDate(inst.paidDate ?: System.currentTimeMillis())
                                                Text(
                                                    text = "Pago em: $paymentDate",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                Text(
                                                    text = inst.formatAmount(),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (unpaidInstallments.isEmpty() && paidInstallments.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Sem compras parceladas.", color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Edit registry Fields
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Nome Completo *") },
                            modifier = Modifier.fillMaxWidth().testTag("edit_customer_name"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editCpf,
                            onValueChange = { editCpf = it },
                            label = { Text("CPF *") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = it },
                            label = { Text("Celular *") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editEmail,
                            onValueChange = { editEmail = it },
                            label = { Text("E-mail") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editAddress,
                            onValueChange = { editAddress = it },
                            label = { Text("Endereço Completo *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { isEditMode = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancelar")
                            }
                            Button(
                                onClick = {
                                    viewModel.updateCustomerDetails(
                                        customer.id,
                                        editName,
                                        editCpf,
                                        editPhone,
                                        editEmail,
                                        editAddress
                                    )
                                    isEditMode = false
                                },
                                modifier = Modifier.weight(1f).testTag("save_customer_edit_button")
                            ) {
                                Text("Salvar")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )

    // Secondary Pay dialogues triggers from detail profile tabs
    selectedInstallmentToPay?.let { detailed ->
        val employees by viewModel.allEmployees.collectAsState()
        val cobradores = employees.filter { it.role == "Cobrador" }
        PaymentConfirmationDialog(
            installmentWithDetails = detailed,
            cobradores = cobradores,
            onDismiss = { selectedInstallmentToPay = null },
            onConfirmPayment = { amount, collectorId ->
                viewModel.receivePayment(detailed.installment.id, amount, collectorId)
                selectedInstallmentToPay = null
            }
        )
    }

    // Delete confirmation prompt
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Sua confirmação é necessária", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Deseja realmente excluir permanentemente ${customer.name}? Essa ação irá apagar todas as vendas parceladas e parcelas vinculadas a este cliente."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(customer)
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_customer_button")
                ) {
                    Text("EXCLUIR PERMANENTEMENTE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// Utility formatting functions for Brazilian standards
fun formatCpf(cpf: String): String {
    if (cpf.length != 11) return cpf
    return "${cpf.substring(0, 3)}.${cpf.substring(3, 6)}.${cpf.substring(6, 9)}-${cpf.substring(9, 11)}"
}

fun formatPhone(phone: String): String {
    val clean = phone.replace(Regex("[^0-9]"), "")
    if (clean.length == 11) {
        return "(${clean.substring(0, 2)}) ${clean.substring(2, 7)}-${clean.substring(7, 11)}"
    }
    if (clean.length == 10) {
        return "(${clean.substring(0, 2)}) ${clean.substring(2, 6)}-${clean.substring(6, 10)} "
    }
    return phone
}

fun formatTimestampDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    return sdf.format(Date(timestamp))
}
