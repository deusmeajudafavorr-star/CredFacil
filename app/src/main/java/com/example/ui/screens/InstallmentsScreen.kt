package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InstallmentWithDetails
import com.example.data.model.Employee
import com.example.ui.viewmodel.CrediarioViewModel
import com.example.ui.viewmodel.InstallmentFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InstallmentsScreen(
    viewModel: CrediarioViewModel,
    modifier: Modifier = Modifier
) {
    val installmentsDetailed by viewModel.filteredInstallmentsDetailed.collectAsState()
    val allInstallmentsDetailed by viewModel.allInstallmentsDetailed.collectAsState()
    val activeFilter by viewModel.installmentsFilter.collectAsState()
    val searchQuery by viewModel.installmentsSearchQuery.collectAsState()
    val selectedRole by viewModel.selectedRole.collectAsState()

    // Payment Dialog state triggers
    var payingInstallment by remember { mutableStateOf<InstallmentWithDetails?>(null) }
    var addressDetailedToShow by remember { mutableStateOf<InstallmentWithDetails?>(null) }
    var historyDetailedToShow by remember { mutableStateOf<InstallmentWithDetails?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("installments_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App top header
        Column {
            Text(
                text = "Parcelas",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Controle global de recebimentos e vencimentos",
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

        // Search Bar Search Filter
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.installmentsSearchQuery.value = it },
            placeholder = { Text("Buscar por cliente ou produto...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.installmentsSearchQuery.value = "" }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("installment_search_bar"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )

        // Horizontal Category Filter Pills Row (All, Pending, Overdue, Paid)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(InstallmentFilter.values()) { filter ->
                val isSelected = filter == activeFilter
                val countDisplay = when (filter) {
                    InstallmentFilter.TODAS -> ""
                    else -> ""
                }
                
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.installmentsFilter.value = filter },
                    label = {
                        Text(
                            text = getFilterLabel(filter),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("installment_filter_chip_${filter.name}")
                )
            }
        }

        // List representation of Installment items
        if (installmentsDetailed.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nenhuma parcela encontrada",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Não há cobranças que correspondem ao filtro ou busca selecionada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val groupedByCustomer = remember(installmentsDetailed) {
                installmentsDetailed.groupBy { it.installment.customerId }
            }
            val sortedCustomers = remember(groupedByCustomer) {
                groupedByCustomer.entries.toList().sortedWith(
                    compareByDescending<Map.Entry<Long, List<InstallmentWithDetails>>> { entry ->
                        entry.value.any { it.installment.isOverdue() }
                    }.thenByDescending { entry ->
                        entry.value.any { it.installment.paidDate == null }
                    }.thenBy { entry ->
                        entry.value.firstOrNull()?.customerName ?: ""
                    }
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedCustomers, key = { it.key }) { (customerId, customerInstallments) ->
                    CustomerInstallmentCard(
                        customerId = customerId,
                        customerInstallments = customerInstallments,
                        allInstallments = allInstallmentsDetailed,
                        onAddressClick = { addressDetailedToShow = customerInstallments.first() },
                        onHistoryClick = { historyDetailedToShow = customerInstallments.first() }
                    )
                }
            }
        }
    }

    // Modal dialogue popup settled payments
    payingInstallment?.let { detailed ->
        val employees by viewModel.allEmployees.collectAsState()
        val cobradores = employees.filter { it.role == "Cobrador" }
        PaymentConfirmationDialog(
            installmentWithDetails = detailed,
            cobradores = cobradores,
            onDismiss = { payingInstallment = null },
            onConfirmPayment = { amount, collectorId ->
                viewModel.receivePayment(detailed.installment.id, amount, collectorId)
                payingInstallment = null
            }
        )
    }

    // Address and House Photo Modal Dialogue
    addressDetailedToShow?.let { detailed ->
        AlertDialog(
            onDismissRequest = { addressDetailedToShow = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Endereço de Entrega", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("Cliente:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(detailed.customerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }

                    Column {
                        Text("Venda:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(detailed.saleDescription, style = MaterialTheme.typography.bodyMedium)
                    }

                    Column {
                        Text("Endereço do Cliente:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = detailed.customerAddress.ifBlank { "Nenhum endereço cadastrado para este cliente." },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // 📸 House Photo display
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Foto da Fachada da Casa:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (detailed.customerHousePhoto != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.HomeWork,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Foto de Fachada Cadastrada",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = detailed.customerHousePhoto.substringAfterLast("/"),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            "Sem foto cadastrada para esta venda",
                                            color = MaterialTheme.colorScheme.outline,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { addressDetailedToShow = null }) {
                    Text("FECHAR")
                }
            }
        )
    }

    // Payment History Modal Dialogue
    historyDetailedToShow?.let { detailed ->
        val customerInstallments = allInstallmentsDetailed.filter {
            it.installment.customerId == detailed.installment.customerId
        }.sortedBy { it.installment.installmentNumber }

        AlertDialog(
            onDismissRequest = { historyDetailedToShow = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Histórico de Parcelas", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("Cliente:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(detailed.customerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 4.dp))

                    Text("Histórico de Cobranças:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(customerInstallments) { item ->
                            val instItem = item.installment
                            val isItemPaid = instItem.paidDate != null
                            val isItemLate = instItem.isOverdue()

                            val itemBgColor = when {
                                isItemPaid -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                isItemLate -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            }
                            val itemBorderColor = when {
                                isItemPaid -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                isItemLate -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = itemBgColor),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, itemBorderColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Parcela ${instItem.installmentNumber} • ${instItem.formatAmount()}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isItemPaid) MaterialTheme.colorScheme.primary else if (isItemLate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isItemPaid) {
                                                "Paga em: ${formatTimestampDate(instItem.paidDate ?: 0)}"
                                            } else {
                                                "Vence em: ${instItem.formatDueDate()}"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "Venda: ${item.saleDescription}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            fontSize = 10.sp
                                        )
                                    }

                                    // Status or Pay Button
                                    if (!isItemPaid) {
                                        Button(
                                            onClick = { payingInstallment = item },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp).testTag("btn_pay_history_${instItem.id}"),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AttachMoney,
                                                    contentDescription = "Receber",
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text("Receber", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        val statusText = when {
                                            isItemPaid -> "PAGA"
                                            isItemLate -> "ATRASADA"
                                            else -> "PENDENTE"
                                        }
                                        val statusColor = when {
                                            isItemPaid -> MaterialTheme.colorScheme.primary
                                            isItemLate -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                        Text(
                                            text = statusText,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            color = statusColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { historyDetailedToShow = null }) {
                    Text("FECHAR")
                }
            }
        )
    }
}

@Composable
fun InstallmentRowItem(
    detailed: InstallmentWithDetails,
    onAddressClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onPayClick: () -> Unit
) {
    val inst = detailed.installment
    val isPaid = inst.paidDate != null
    val isLate = inst.isOverdue()
    
    val borderTagColor = when {
        isPaid -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        isLate -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("installment_row_item_${inst.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder(enabled = true).copy(
            brush = androidx.compose.ui.graphics.SolidColor(borderTagColor)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Client and Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = detailed.customerName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = if (inst.synced) "Sincronizado" else "Disponível apenas offline",
                            tint = if (inst.synced) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "${detailed.saleDescription} • Parcela ${inst.installmentNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = inst.formatAmount(),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium,
                    color = when {
                        isPaid -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        isLate -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            // Middle Info: Vencimento & Status tag capsule badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Vencimento: ${inst.formatDueDate()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Badges Status pill
                val badgeContainerColor = when {
                    isPaid -> MaterialTheme.colorScheme.primaryContainer
                    isLate -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val badgeContentColor = when {
                    isPaid -> MaterialTheme.colorScheme.onPrimaryContainer
                    isLate -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val badgeLabel = when {
                    isPaid -> "PAGA"
                    isLate -> "ATRASADA"
                    else -> "PENDENTE"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeContainerColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeContentColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    )
                }
            }

            // Quick actions layout with Address and Payment reception
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Address button
                    OutlinedButton(
                        onClick = onAddressClick,
                        modifier = Modifier.height(32.dp).testTag("btn_address_${inst.id}"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Endereço",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Endereço", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Histórico button
                    OutlinedButton(
                        onClick = onHistoryClick,
                        modifier = Modifier.height(32.dp).testTag("btn_history_${inst.id}"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Histórico",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Histórico", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!isPaid) {
                    Button(
                        onClick = onPayClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("btn_receive_payment_${inst.id}"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = "ReceberPagamento",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Receber Pagamento", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Payment date confirmation log
                    val dateFormatted = formatTimestampDate(inst.paidDate ?: System.currentTimeMillis())
                    Text(
                        text = "Pago em $dateFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

fun getFilterLabel(filter: InstallmentFilter): String {
    return when (filter) {
        InstallmentFilter.TODAS -> "Todas"
        InstallmentFilter.PENDENTES -> "Pendentes"
        InstallmentFilter.ATRASADAS -> "Atrasadas"
        InstallmentFilter.PAGAS -> "Pagas"
    }
}

@Composable
fun CustomerInstallmentCard(
    customerId: Long,
    customerInstallments: List<InstallmentWithDetails>,
    allInstallments: List<InstallmentWithDetails>,
    onAddressClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstDetails = customerInstallments.first()
    
    val fullCustomerInstallments = remember(allInstallments, customerId) {
        allInstallments.filter { it.installment.customerId == customerId }
    }
    
    val overdueInstallments = fullCustomerInstallments.filter { it.installment.isOverdue() }
    val pendingInstallments = fullCustomerInstallments.filter { it.installment.paidDate == null && !it.installment.isOverdue() }
    val paidInstallments = fullCustomerInstallments.filter { it.installment.paidDate != null }
    
    val overdueCount = overdueInstallments.size
    val overdueTotal = overdueInstallments.sumOf { it.installment.amount }
    
    val pendingCount = pendingInstallments.size
    val pendingTotal = pendingInstallments.sumOf { it.installment.amount }
    
    val isAnyOverdue = overdueCount > 0
    val isAnyPending = pendingCount > 0
    
    val borderTagColor = when {
        isAnyOverdue -> MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
        isAnyPending -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }
    
    val cardBg = when {
        isAnyOverdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.05f)
        isAnyPending -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("customer_installment_card_$customerId"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderTagColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isAnyOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = firstDetails.customerName,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (isAnyOverdue) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ATRASO",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = firstDetails.customerAddress.ifBlank { "Sem endereço cadastrado" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Resumo do Crediário:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (overdueCount > 0) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$overdueCount Atrasada${if (overdueCount > 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = formatCurrency(overdueTotal),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    
                    if (pendingCount > 0) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$pendingCount A vencer",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = formatCurrency(pendingTotal),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    if (overdueCount == 0 && pendingCount == 0) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Todas as parcelas quitadas (${paidInstallments.size} pagas)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAddressClick,
                    modifier = Modifier.weight(1f).height(38.dp).testTag("btn_address_cust_${customerId}"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Endereço",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Endereço", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onHistoryClick,
                    modifier = Modifier.weight(1.2f).height(38.dp).testTag("btn_history_cust_${customerId}"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAnyOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        contentColor = if (isAnyOverdue) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Cobrança",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cobrança / Parcelas", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
