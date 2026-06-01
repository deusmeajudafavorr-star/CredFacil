package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.CustomerScreen
import com.example.ui.screens.SaleScreen
import com.example.ui.screens.InstallmentsScreen
import com.example.ui.screens.RoleSelectionScreen
import com.example.ui.screens.ProductManagementScreen
import com.example.ui.screens.EmployeeManagementScreen
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.AssignmentInd
import com.example.ui.viewmodel.UserRole
import androidx.compose.material.icons.filled.Logout
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.CrediarioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                
                // Construct ViewModel through our custom DB repository Factory
                val crediarioViewModel: CrediarioViewModel = viewModel(
                    factory = CrediarioViewModel.Factory(context)
                )
                
                val currentScreen by crediarioViewModel.currentScreen.collectAsState()
                val selectedRole by crediarioViewModel.selectedRole.collectAsState()

                // Listens for Toast notifications from operations (Success / Warning)
                LaunchedEffect(key1 = true) {
                    crediarioViewModel.toastMessage.collect { message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }

                val defaultScreen = when (selectedRole) {
                    UserRole.VENDEDOR -> AppScreen.VENDAS
                    UserRole.COBRADOR -> AppScreen.PARCELAS
                    else -> AppScreen.DASHBOARD
                }

                // Intercept back button gracefully to return home based on current role
                BackHandler(enabled = selectedRole != UserRole.NONE && currentScreen != defaultScreen) {
                    crediarioViewModel.navigateTo(defaultScreen)
                }

                if (selectedRole == UserRole.NONE) {
                    RoleSelectionScreen(viewModel = crediarioViewModel)
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize().testTag("app_scaffold"),
                        bottomBar = {
                            NavigationBar(
                                modifier = Modifier.testTag("app_bottom_navigation")
                            ) {
                                if (selectedRole == UserRole.ADMINISTRADOR) {
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.DASHBOARD,
                                        onClick = { crediarioViewModel.navigateTo(AppScreen.DASHBOARD) },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.TrendingUp,
                                                contentDescription = "Início"
                                            )
                                        },
                                        label = { Text("Início") },
                                        modifier = Modifier.testTag("nav_btn_dashboard")
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.PRODUTOS,
                                        onClick = { crediarioViewModel.navigateTo(AppScreen.PRODUTOS) },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.Inventory,
                                                contentDescription = "Produtos"
                                            )
                                        },
                                        label = { Text("Produtos") },
                                        modifier = Modifier.testTag("nav_btn_produtos")
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.FUNCIONARIOS,
                                        onClick = { crediarioViewModel.navigateTo(AppScreen.FUNCIONARIOS) },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.AssignmentInd,
                                                contentDescription = "Funcionários"
                                            )
                                        },
                                        label = { Text("Funcionários") },
                                        modifier = Modifier.testTag("nav_btn_funcionarios")
                                    )
                                }
                                
                                if (selectedRole == UserRole.ADMINISTRADOR || selectedRole == UserRole.VENDEDOR) {
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.CLIENTES,
                                        onClick = { crediarioViewModel.navigateTo(AppScreen.CLIENTES) },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.People,
                                                contentDescription = "Clientes"
                                            )
                                        },
                                        label = { Text("Clientes") },
                                        modifier = Modifier.testTag("nav_btn_clientes")
                                    )
                                }

                                if (selectedRole == UserRole.VENDEDOR) {
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.VENDAS,
                                        onClick = { crediarioViewModel.navigateTo(AppScreen.VENDAS) },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.AddShoppingCart,
                                                contentDescription = "Nova Venda"
                                            )
                                        },
                                        label = { Text("Vender") },
                                        modifier = Modifier.testTag("nav_btn_vender")
                                    )
                                }
                                
                                if (selectedRole == UserRole.ADMINISTRADOR || selectedRole == UserRole.COBRADOR) {
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.PARCELAS,
                                        onClick = { crediarioViewModel.navigateTo(AppScreen.PARCELAS) },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.EventNote,
                                                contentDescription = "Parcelas"
                                            )
                                        },
                                        label = { Text("Parcelas") },
                                        modifier = Modifier.testTag("nav_btn_parcelas")
                                    )
                                }

                                NavigationBarItem(
                                    selected = false,
                                    onClick = { crediarioViewModel.clearRole() },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Logout,
                                            contentDescription = "Sair Perfil",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    label = { Text("Sair", color = MaterialTheme.colorScheme.error) },
                                    modifier = Modifier.testTag("nav_btn_logout")
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                AppScreen.DASHBOARD -> DashboardScreen(
                                    viewModel = crediarioViewModel,
                                    onNavigateTo = { crediarioViewModel.navigateTo(it) }
                                )
                                AppScreen.PRODUTOS -> ProductManagementScreen(
                                    viewModel = crediarioViewModel
                                )
                                AppScreen.CLIENTES -> CustomerScreen(
                                    viewModel = crediarioViewModel
                                )
                                AppScreen.VENDAS -> SaleScreen(
                                    viewModel = crediarioViewModel,
                                    onNavigateBack = { crediarioViewModel.navigateTo(defaultScreen) }
                                )
                                AppScreen.PARCELAS -> InstallmentsScreen(
                                    viewModel = crediarioViewModel
                                )
                                AppScreen.FUNCIONARIOS -> EmployeeManagementScreen(
                                    viewModel = crediarioViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
