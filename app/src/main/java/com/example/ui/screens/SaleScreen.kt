package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.CustomerWithStats
import com.example.data.model.Product
import com.example.data.model.Employee
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.CrediarioViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleScreen(
    viewModel: CrediarioViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customersWithStats by viewModel.allCustomersWithStats.collectAsState()
    val selectedRole by viewModel.selectedRole.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    
    // Form States
    var selectedCustStats by remember { mutableStateOf<CustomerWithStats?>(null) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var productDropdownExpanded by remember { mutableStateOf(false) }
    var customerHousePhoto by remember { mutableStateOf<String?>(null) }
    var showCameraMockDialog by remember { mutableStateOf(false) }
    
    val employees by viewModel.allEmployees.collectAsState()
    val vendedores = employees.filter { it.role == "Vendedor" }
    var selectedVendedor by remember { mutableStateOf<Employee?>(null) }
    var vendedorDropdownExpanded by remember { mutableStateOf(false) }
    
    var description by remember { mutableStateOf("") }
    var totalAmountInput by remember { mutableStateOf("") }
    var installmentsCountInput by remember { mutableStateOf("3") }
    
    // Date: First installment default is 30 days from now
    val defaultFirstDueDate = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 30)
        cal.timeInMillis
    }
    var firstDueDate by remember { mutableStateOf(defaultFirstDueDate) }
    
    // Dropdown state
    var dropdownExpanded by remember { mutableStateOf(false) }
    var clientSearchFilter by remember { mutableStateOf("") }
    
    // Show manual date edit
    var showDatePicker by remember { mutableStateOf(false) }

    // Dynamic Preview Simulation computations on changes
    val doubleAmount = totalAmountInput.toDoubleOrNull() ?: 0.0
    val intInstallments = installmentsCountInput.toIntOrNull() ?: 1
    val previewInstallments = remember(doubleAmount, intInstallments, firstDueDate) {
        if (doubleAmount <= 0.0 || intInstallments <= 0) emptyList()
        else {
            val list = mutableListOf<Pair<Int, Double>>()
            val baseVal = doubleAmount / intInstallments
            var accumulatedVal = 0.0
            
            for (i in 1..intInstallments) {
                val value = if (i == intInstallments) {
                    doubleAmount - accumulatedVal
                } else {
                    val r = Math.round(baseVal * 100.0) / 100.0
                    accumulatedVal += r
                    r
                }
                list.add(Pair(i, value))
            }
            list
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("sale_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App top header
        item {
            Column {
                Text(
                     text = "Registrar Crediário",
                     style = MaterialTheme.typography.headlineMedium,
                     fontWeight = FontWeight.ExtraBold
                )
                Text(
                     text = "Lançar nova venda parcelada no livro de contas",
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
        }

        // Form Fields
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Dados do Cliente e Venda",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Customer Dropdown Search and Selection
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCustStats?.customer?.name ?: "Selecione o Cliente...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cliente do Crediário *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_customer_dropdown_trigger")
                                .clickable { dropdownExpanded = true },
                            trailingIcon = {
                                IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                    Icon(
                                        imageVector = if (dropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Expand customer selection"
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            enabled = false // Disable direct text input, use click to open menu
                        )

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .heightIn(max = 240.dp)
                                .testTag("select_customer_dropdown_menu")
                        ) {
                            OutlinedTextField(
                                value = clientSearchFilter,
                                onValueChange = { clientSearchFilter = it },
                                placeholder = { Text("Filtrar clientes por nome...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                singleLine = true,
                                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) }
                            )

                            val listToDisplay = if (clientSearchFilter.isBlank()) {
                                customersWithStats
                            } else {
                                customersWithStats.filter { it.customer.name.contains(clientSearchFilter, ignoreCase = true) }
                            }

                            if (listToDisplay.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Nenhum cliente disponível") },
                                    onClick = {}
                                )
                            } else {
                                listToDisplay.forEach { stats ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(stats.customer.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("Cpf: ${stats.customer.cpf}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = 120.dp)) {
                                                    Text("Endereço:", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(stats.customer.address, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedCustStats = stats
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Display customer address info nicely
                    selectedCustStats?.let { stats ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("customer_address_details_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Endereço Cadastrado para Entrega:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stats.customer.address.ifBlank { "Nenhum endereço cadastrado para este cliente." },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Product Selection
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedProduct?.name ?: "Selecione o Produto...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Produto Selecionado") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_product_dropdown_trigger")
                                .clickable { productDropdownExpanded = true },
                            trailingIcon = {
                                IconButton(onClick = { productDropdownExpanded = !productDropdownExpanded }) {
                                    Icon(
                                        imageVector = if (productDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Expand product selection"
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
                            expanded = productDropdownExpanded,
                            onDismissRequest = { productDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .heightIn(max = 240.dp)
                                .testTag("select_product_dropdown_menu")
                        ) {
                            if (products.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Nenhum produto cadastrado") },
                                    onClick = {}
                                )
                            } else {
                                products.forEach { product ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("Estoque: ${product.stock} un", fontSize = 11.sp, color = if (product.stock <= 0) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Text(product.formatSalePrice(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        onClick = {
                                            selectedProduct = product
                                            description = product.name
                                            totalAmountInput = product.salePrice.toString()
                                            productDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Vendedor Selection
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedVendedor?.name ?: "Selecione o Vendedor...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Vendedor Responsável") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_vendedor_dropdown_trigger")
                                .clickable { vendedorDropdownExpanded = true },
                            trailingIcon = {
                                IconButton(onClick = { vendedorDropdownExpanded = !vendedorDropdownExpanded }) {
                                    Icon(
                                        imageVector = if (vendedorDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Expand vendedor selection"
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
                            expanded = vendedorDropdownExpanded,
                            onDismissRequest = { vendedorDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .heightIn(max = 240.dp)
                                .testTag("select_vendedor_dropdown_menu")
                        ) {
                            if (vendedores.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Nenhum vendedor cadastrado", color = Color.Red) },
                                    onClick = { vendedorDropdownExpanded = false }
                                )
                            } else {
                                vendedores.forEach { employee ->
                                    DropdownMenuItem(
                                        text = { Text(employee.name) },
                                        onClick = {
                                            selectedVendedor = employee
                                            vendedorDropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("vendedor_item_${employee.id}")
                                    )
                                }
                            }
                        }
                    }

                    // Description of transaction
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrição / Itens Vendidos *") },
                        placeholder = { Text("Ex: Smart Tv Samsung 50\", Camisas, etc") },
                        modifier = Modifier.fillMaxWidth().testTag("input_sale_description"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    // Numerical total money input
                    OutlinedTextField(
                        value = totalAmountInput,
                        onValueChange = { totalAmountInput = it },
                        label = { Text("Valor Total (R$) *") },
                        placeholder = { Text("0.00") },
                        modifier = Modifier.fillMaxWidth().testTag("input_sale_amount"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    // Installments Counts picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = installmentsCountInput,
                            onValueChange = { installmentsCountInput = it },
                            label = { Text("Número de Parcelas *") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_sale_installments"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )

                        // Quick helpers buttons for installments counts
                        val quickCounts = listOf("1", "3", "6", "12")
                        quickCounts.forEach { count ->
                            ElevatedButton(
                                onClick = { installmentsCountInput = count },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .padding(top = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = "${count}x")
                            }
                        }
                    }

                    // Due Date Button Choice preset
                    OutlinedTextField(
                        value = formatDate(firstDueDate),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Primeiro Vencimento *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        enabled = false
                    )

                    // 📸 Foto da Casa do Cliente (OBRIGATÓRIO)
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                    
                    Text(
                        text = "Foto da Casa do Cliente (Obrigatório) *",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (customerHousePhoto != null) {
                        Surface(
                            color = Color(0xFF2E7D32).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().testTag("house_photo_preview_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Foto Registrada com Sucesso!",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF2E7D32)
                                        )
                                        Text(
                                            text = customerHousePhoto ?: "",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                OutlinedButton(
                                    onClick = { showCameraMockDialog = true },
                                    modifier = Modifier.fillMaxWidth().testTag("btn_retake_photo"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tirar Nova Foto")
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().testTag("house_photo_missing_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Aviso: É obrigatório anexar a foto da casa para liberar a venda.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Button(
                                    onClick = { showCameraMockDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("btn_take_photo"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("📸 Tirar foto da residência")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Installment Preview simulation layout block
        if (previewInstallments.isNotEmpty() && selectedCustStats != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Simulação de Cobrança",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Serão geradas as seguintes parcelas automáticas:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = firstDueDate

                        // Draw list of preview elements
                        previewInstallments.forEach { pair ->
                            val number = pair.first
                            val amt = pair.second
                            val dueDateMillis = calendar.timeInMillis
                            val dateStr = formatDate(dueDateMillis)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Parcela $number de $intInstallments",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Vencimento: $dateStr",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatCurrency(amt),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            // Add 1 month for subsequent previews too
                            calendar.add(Calendar.MONTH, 1)
                        }
                    }
                }
            }
        }

        // Submit action container
        item {
            Button(
                onClick = {
                    val client = selectedCustStats?.customer
                    if (client == null) {
                        return@Button
                    }
                    viewModel.addCreditSale(
                        customerId = client.id,
                        description = description,
                        totalAmount = doubleAmount,
                        installmentsCount = intInstallments,
                        firstDueDate = firstDueDate,
                        productId = selectedProduct?.id,
                        customerHousePhoto = customerHousePhoto,
                        sellerId = selectedVendedor?.id
                    )
                    // Reset fields
                    selectedCustStats = null
                    selectedProduct = null
                    selectedVendedor = null
                    customerHousePhoto = null
                    description = ""
                    totalAmountInput = ""
                    installmentsCountInput = "3"
                    firstDueDate = defaultFirstDueDate
                    // Return back to dashboard or show completion message
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_sale_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = selectedCustStats != null && description.isNotBlank() && doubleAmount > 0f && intInstallments >= 1 && customerHousePhoto != null && (vendedores.isEmpty() || selectedVendedor != null)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lançar Crediário",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }

    // Material design custom calendar popup selector
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = firstDueDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val sel = datePickerState.selectedDateMillis
                        if (sel != null) {
                            firstDueDate = sel
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Selecionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Camera viewfinder simulator dialog
    if (showCameraMockDialog) {
        AlertDialog(
            onDismissRequest = { showCameraMockDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Câmera do Dispositivo", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Enquadre a fachada da casa do cliente no visor abaixo:",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black,
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            
                            Text(
                                text = "MOCK CAM VIEW",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val randomHash = (100000..999999).random()
                        customerHousePhoto = "content://media/external/images/media/house_capture_$randomHash.jpg"
                        showCameraMockDialog = false
                    },
                    modifier = Modifier.testTag("camera_btn_capture")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Capturar Foto")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCameraMockDialog = false },
                    modifier = Modifier.testTag("camera_btn_cancel")
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    return sdf.format(Date(millis))
}
