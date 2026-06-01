package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.viewmodel.CrediarioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(
    viewModel: CrediarioViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.allProducts.collectAsState()
    val selectedRole by viewModel.selectedRole.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("product_mgmt_screen"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Adicionar Produto") },
                text = { Text("Novo Produto") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("btn_add_product")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Title
            Column {
                Text(
                    text = "Gerenciar Produtos",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Administração de produtos e controle de estoque",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Role indicators
                val (label, badgeColor, roleIcon) = when (selectedRole) {
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

            // Products list
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "Nenhum produto cadastrado.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("products_lazy_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductItemCard(
                            product = product,
                            onEdit = { editingProduct = product },
                            onDelete = { viewModel.deleteProduct(product) },
                            onIncrementStock = {
                                viewModel.updateProduct(
                                    id = product.id,
                                    name = product.name,
                                    costPrice = product.costPrice,
                                    salePrice = product.salePrice,
                                    stock = product.stock + 1,
                                    photoUrl = product.photoUrl
                                )
                            },
                            onDecrementStock = {
                                if (product.stock > 0) {
                                    viewModel.updateProduct(
                                        id = product.id,
                                        name = product.name,
                                        costPrice = product.costPrice,
                                        salePrice = product.salePrice,
                                        stock = product.stock - 1,
                                        photoUrl = product.photoUrl
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Product Dialog
    if (showAddDialog) {
        ProductFormDialog(
            title = "Adicionar Produto",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, cost, sale, stock, photo ->
                viewModel.addProduct(name, cost, sale, stock, photo)
                showAddDialog = false
            }
        )
    }

    // Edit Product Dialog
    if (editingProduct != null) {
        val prod = editingProduct!!
        ProductFormDialog(
            title = "Editar Produto",
            initialName = prod.name,
            initialCostPrice = prod.costPrice.toString(),
            initialSalePrice = prod.salePrice.toString(),
            initialStock = prod.stock.toString(),
            initialPhotoUrl = prod.photoUrl ?: "",
            onDismiss = { editingProduct = null },
            onConfirm = { name, cost, sale, stock, photo ->
                viewModel.updateProduct(prod.id, name, cost, sale, stock, photo)
                editingProduct = null
            }
        )
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onIncrementStock: () -> Unit,
    onDecrementStock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row with visual photo placeholder / details + actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minimal decorative vector photo box
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = product.formatSalePrice(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "(Custo: ${product.formatCostPrice()})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Edit / Delete quick action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_edit_prod_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_delete_prod_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remover",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Stock controller section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock Label / Level Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Estoque:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val stockColor = when {
                        product.stock <= 0 -> Color(0xFFD32F2F)
                        product.stock < 5 -> Color(0xFFE65100)
                        else -> Color(0xFF2E7D32)
                    }
                    val stockBg = stockColor.copy(alpha = 0.1f)

                    Surface(
                        color = stockBg,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, stockColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = if (product.stock == 0) "ESGOTADO" else "${product.stock} unidades",
                            color = stockColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Inline quick stock adjustments (+ / -)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onDecrementStock,
                        enabled = product.stock > 0,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_dec_stock_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Diminuir Estoque",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    Text(
                        text = "${product.stock}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onIncrementStock,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_inc_stock_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Aumentar Estoque",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialog(
    title: String,
    initialName: String = "",
    initialCostPrice: String = "",
    initialSalePrice: String = "",
    initialStock: String = "",
    initialPhotoUrl: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, cost: Double, sale: Double, stock: Int, photo: String?) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var costPrice by remember { mutableStateOf(initialCostPrice) }
    var salePrice by remember { mutableStateOf(initialSalePrice) }
    var stock by remember { mutableStateOf(initialStock) }
    var photoUrl by remember { mutableStateOf(initialPhotoUrl) }

    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Produto") },
                    leadingIcon = { Icon(Icons.Default.Inventory, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_prod_name")
                )

                OutlinedTextField(
                    value = costPrice,
                    onValueChange = { costPrice = it },
                    label = { Text("Preço de Custo (R$)") },
                    leadingIcon = { Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFFD32F2F)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_prod_cost")
                )

                OutlinedTextField(
                    value = salePrice,
                    onValueChange = { salePrice = it },
                    label = { Text("Preço de Venda (R$)") },
                    leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF2E7D32)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_prod_sale")
                )

                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    label = { Text("Estoque Inicial") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_prod_stock")
                )

                OutlinedTextField(
                    value = photoUrl,
                    onValueChange = { photoUrl = it },
                    label = { Text("Foto do Produto (Opcional - link)") },
                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_prod_photo")
                )

                if (hasError) {
                    Text(
                        text = "Preencha todos os campos obrigatórios com valores numéricos corretos.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costPrice.toDoubleOrNull()
                    val sale = salePrice.toDoubleOrNull()
                    val stockQty = stock.toIntOrNull()

                    if (name.isNotBlank() && cost != null && sale != null && stockQty != null) {
                        onConfirm(
                            name,
                            cost,
                            sale,
                            stockQty,
                            photoUrl.ifBlank { null }
                        )
                    } else {
                        hasError = true
                    }
                },
                modifier = Modifier.testTag("dialog_btn_confirm")
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("dialog_btn_dismiss")) {
                Text("Cancelar")
            }
        }
    )
}
