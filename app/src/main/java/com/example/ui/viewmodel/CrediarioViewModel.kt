package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.CustomerWithStats
import com.example.data.model.InstallmentWithDetails
import com.example.data.model.Product
import com.example.data.model.Employee
import com.example.data.model.CreditSale
import com.example.data.model.Installment
import com.example.data.repository.CrediarioRepository
import com.example.data.repository.NetworkMonitor
import com.example.data.repository.SyncSummary
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class UserRole {
    NONE,
    VENDEDOR,
    COBRADOR,
    ADMINISTRADOR
}

enum class AppScreen {
    DASHBOARD,
    CLIENTES,
    VENDAS,
    PARCELAS,
    PRODUTOS,
    FUNCIONARIOS
}

enum class InstallmentFilter {
    TODAS,
    PENDENTES,
    ATRASADAS,
    PAGAS
}

// Simple state class to represent the Dashboard statistics
data class DashboardStats(
    val totalAReceber: Double = 0.0,
    val totalAtrasado: Double = 0.0,
    val totalRecebidoHoje: Double = 0.0,
    val clientesAtivosCount: Int = 0,
    val parcelasAtrasadasCount: Int = 0,
    val vendasHojeCount: Int = 0,
    val vendasHojeVolume: Double = 0.0,
    val vendasTotalCount: Int = 0,
    val vendasTotalVolume: Double = 0.0,
    val melhorVendedorNome: String = "Nenhum",
    val melhorVendedorVolume: Double = 0.0,
    val cobrancasHojeCount: Int = 0,
    val cobrancasMesVolume: Double = 0.0
)

data class EmployeeWithStats(
    val employee: Employee,
    val totalVolume: Double
) {
    fun formatVolume(): String = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR")).format(totalVolume)
}

class CrediarioViewModel(
    private val repository: CrediarioRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    // Sync UI states
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncSummary = MutableStateFlow<SyncSummary?>(null)
    val lastSyncSummary: StateFlow<SyncSummary?> = _lastSyncSummary.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allCustomers.first().let { currentList ->
                if (currentList.isEmpty()) {
                    seedSampleData()
                }
            }
            repository.allProducts.first().let { currentProducts ->
                if (currentProducts.isEmpty()) {
                    seedSampleProducts()
                }
            }
        }

        // Automatic synchronization on network reconnection
        viewModelScope.launch {
            isOnline.collect { online ->
                if (online) {
                    performSynchronization(automatic = true)
                }
            }
        }
    }

    fun performSynchronization(automatic: Boolean = false) {
        viewModelScope.launch {
            if (_isSyncing.value) return@launch
            if (!isOnline.value) {
                if (!automatic) {
                    _toastMessage.emit("Dispositivo offline! Dados salvos localmente no celular.")
                }
                return@launch
            }
            
            _isSyncing.value = true
            try {
                val summary = repository.synchronizeWithCentralServer()
                _lastSyncSummary.value = summary
                if (summary.customersSyncedCount > 0 || summary.salesSyncedCount > 0 || summary.installmentsSyncedCount > 0) {
                    _toastMessage.emit(summary.message)
                } else if (!automatic) {
                    _toastMessage.emit("Banco de dados já está em sincronia total!")
                }
            } catch (e: Exception) {
                if (!automatic) {
                    _toastMessage.emit("Falha ao subir dados para servidor central: ${e.localizedMessage}")
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun seedSampleData() {
        val idCarlos = repository.addCustomer(
            Customer(
                name = "Carlos Silva Mendonça",
                cpf = "12345678901",
                phone = "11987654321",
                email = "carlos.silva@email.com",
                creditLimit = 3000.0,
                address = "Rua das Flores, 123 - Centro, São Paulo - SP"
            )
        )
        val idAna = repository.addCustomer(
            Customer(
                name = "Ana Oliveira Souza",
                cpf = "98765432109",
                phone = "21999887766",
                email = "ana.oliveira@email.com",
                creditLimit = 5000.0,
                address = "Av. Paulista, 1500 - Bela Vista, São Paulo - SP"
            )
        )
        val idRoberto = repository.addCustomer(
            Customer(
                name = "Roberto Santos Lima",
                cpf = "45678912345",
                phone = "31988887777",
                email = "roberto.santos@email.com",
                creditLimit = 1500.0,
                address = "Rua Diamante, 45 - Eldorado, Contagem - MG"
            )
        )

        val calPast = Calendar.getInstance()
        calPast.add(Calendar.DAY_OF_YEAR, -40)
        
        repository.registerCreditSale(
            customerId = idCarlos,
            description = "Smartphone Motorola G54",
            totalAmount = 900.0,
            installmentsCount = 3,
            firstDueDate = calPast.timeInMillis
        )
        
        val carlosInsts = repository.allInstallments.first().filter { it.customerId == idCarlos }
        if (carlosInsts.isNotEmpty()) {
            repository.payInstallment(carlosInsts[0].id, carlosInsts[0].amount)
        }

        val calFuture = Calendar.getInstance()
        calFuture.add(Calendar.DAY_OF_YEAR, 30)
        repository.registerCreditSale(
            customerId = idAna,
            description = "Geladeira Consul Frost Free",
            totalAmount = 2400.0,
            installmentsCount = 6,
            firstDueDate = calFuture.timeInMillis
        )

        val calOverdue = Calendar.getInstance()
        calOverdue.add(Calendar.DAY_OF_YEAR, -15)
        repository.registerCreditSale(
            customerId = idRoberto,
            description = "Guarda-Roupa Casal MDF",
            totalAmount = 800.0,
            installmentsCount = 2,
            firstDueDate = calOverdue.timeInMillis
        )
    }

    private suspend fun seedSampleProducts() {
        repository.addProduct(
            Product(
                name = "Celular X",
                costPrice = 800.0,
                salePrice = 1200.0,
                stock = 12
            )
        )
        repository.addProduct(
            Product(
                name = "TV 32\"",
                costPrice = 950.0,
                salePrice = 1450.0,
                stock = 5
            )
        )
    }

    // 1. Navigation and Screen States
    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedRole = MutableStateFlow(UserRole.NONE)
    val selectedRole: StateFlow<UserRole> = _selectedRole.asStateFlow()

    fun selectRole(role: UserRole) {
        _selectedRole.value = role
        when (role) {
            UserRole.VENDEDOR -> {
                _currentScreen.value = AppScreen.VENDAS
            }
            UserRole.COBRADOR -> {
                _currentScreen.value = AppScreen.PARCELAS
            }
            UserRole.ADMINISTRADOR -> {
                _currentScreen.value = AppScreen.DASHBOARD
            }
            UserRole.NONE -> {
                _currentScreen.value = AppScreen.DASHBOARD
            }
        }
    }

    fun clearRole() {
        _selectedRole.value = UserRole.NONE
    }

    // UI Feedback messages
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    // 2. Search & Filtering streams
    val clientSearchQuery = MutableStateFlow("")
    val installmentsFilter = MutableStateFlow(InstallmentFilter.TODAS)
    val installmentsSearchQuery = MutableStateFlow("")

    // Selected customer for detail views
    val selectedCustomer = MutableStateFlow<CustomerWithStats?>(null)

    // 3. Data Streams from Repository
    val allCustomersWithStats: StateFlow<List<CustomerWithStats>> = repository.customersWithStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInstallmentsDetailed: StateFlow<List<InstallmentWithDetails>> = repository.installmentsDetailed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCreditSales = repository.allCreditSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEmployees: StateFlow<List<Employee>> = repository.allEmployees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employeesWithStats: StateFlow<List<EmployeeWithStats>> = combine(
        repository.allEmployees,
        repository.allCreditSales,
        repository.allInstallments
    ) { employees, sales, installments ->
        employees.map { emp ->
            val totalVolume = if (emp.role == "Vendedor") {
                sales.filter { it.sellerId == emp.id }.sumOf { it.totalAmount }
            } else {
                installments.filter { it.collectorId == emp.id && it.paidDate != null }.sumOf { it.amountPaid }
            }
            EmployeeWithStats(emp, totalVolume)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Filtered lists for the UI
    val filteredCustomers: StateFlow<List<CustomerWithStats>> = combine(
        allCustomersWithStats,
        clientSearchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.customer.name.contains(query, ignoreCase = true) ||
                        it.customer.cpf.contains(query)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredInstallmentsDetailed: StateFlow<List<InstallmentWithDetails>> = combine(
        allInstallmentsDetailed,
        installmentsFilter,
        installmentsSearchQuery
    ) { list, filter, query ->
        val now = System.currentTimeMillis()
        val filteredByStatus = when (filter) {
            InstallmentFilter.TODAS -> list.filter { it.installment.paidDate == null }
            InstallmentFilter.PENDENTES -> list.filter { it.installment.paidDate == null }
            InstallmentFilter.ATRASADAS -> list.filter { it.installment.paidDate == null && now > it.installment.dueDate }
            InstallmentFilter.PAGAS -> list.filter { it.installment.paidDate != null }
        }

        if (query.isBlank()) {
            filteredByStatus
        } else {
            filteredByStatus.filter {
                it.customerName.contains(query, ignoreCase = true) ||
                        it.saleDescription.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. Dashboard Statistics Stream
    val dashboardStats: StateFlow<DashboardStats> = combine(
        repository.allInstallments,
        repository.allCustomers,
        repository.allCreditSales,
        repository.allEmployees
    ) { installments, customers, sales, employees ->
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        // Define "today" boundary
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        // Current month boundary (start of month)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = calendar.timeInMillis

        var totalAReceber = 0.0
        var totalAtrasado = 0.0
        var totalRecebidoHoje = 0.0
        var parcelasAtrasadasCount = 0
        var cobrancasHojeCount = 0
        var cobrancasMesVolume = 0.0

        val activeCustomerIds = customers.map { it.id }.toSet()
        val activeInstallments = installments.filter { it.customerId in activeCustomerIds }
        val activeSales = sales.filter { it.customerId in activeCustomerIds }

        activeInstallments.forEach { inst ->
            if (inst.paidDate == null) {
                val outstanding = inst.amount - inst.amountPaid
                totalAReceber += outstanding

                if (now > inst.dueDate) {
                    totalAtrasado += outstanding
                    parcelasAtrasadasCount++
                }
            } else {
                // If paid, check if paid today
                if (inst.paidDate >= todayStart) {
                    totalRecebidoHoje += inst.amountPaid
                    cobrancasHojeCount++
                }
                // Check if paid in the current month
                if (inst.paidDate >= monthStart) {
                    cobrancasMesVolume += inst.amountPaid
                }
            }
        }

        // Today's sales count and volume
        val salesHoje = activeSales.filter { it.saleDate >= todayStart }
        val vendasHojeCount = salesHoje.size
        val vendasHojeVolume = salesHoje.sumOf { it.totalAmount }

        // All-time sales count and volume
        val vendasTotalCount = activeSales.size
        val vendasTotalVolume = activeSales.sumOf { it.totalAmount }

        // Top seller who sold the most (highest total amount)
        val vendedorSales = activeSales.filter { it.sellerId != null }
            .groupBy { it.sellerId }
            .mapValues { entry -> entry.value.sumOf { it.totalAmount } }
        val topSellerEntry = vendedorSales.maxByOrNull { it.value }
        val topSellerId = topSellerEntry?.key
        val topSellerVolume = topSellerEntry?.value ?: 0.0
        val topSellerEmployee = topSellerId?.let { id -> employees.find { it.id == id } }
        val melhorVendedorNome = topSellerEmployee?.name ?: "Nenhum"

        DashboardStats(
            totalAReceber = totalAReceber,
            totalAtrasado = totalAtrasado,
            totalRecebidoHoje = totalRecebidoHoje,
            clientesAtivosCount = customers.size,
            parcelasAtrasadasCount = parcelasAtrasadasCount,
            vendasHojeCount = vendasHojeCount,
            vendasHojeVolume = vendasHojeVolume,
            vendasTotalCount = vendasTotalCount,
            vendasTotalVolume = vendasTotalVolume,
            melhorVendedorNome = melhorVendedorNome,
            melhorVendedorVolume = topSellerVolume,
            cobrancasHojeCount = cobrancasHojeCount,
            cobrancasMesVolume = cobrancasMesVolume
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // Quick alert of upcoming/overdue installments for Dashboard
    val dashboardCriticalInstallments: StateFlow<List<InstallmentWithDetails>> = allInstallmentsDetailed
        .map { list ->
            val now = System.currentTimeMillis()
            list.filter { it.installment.paidDate == null && (now > it.installment.dueDate) }
                .sortedBy { it.installment.dueDate }
                .take(5) // Just show top 5 urgent
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 6. Navigation Actions
    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun showCustomerDetails(customer: CustomerWithStats) {
        selectedCustomer.value = customer
    }

    fun closeCustomerDetails() {
        selectedCustomer.value = null
    }

    // 7. Data Mutating Actions
    fun createCustomer(name: String, cpf: String, phone: String, email: String, address: String) {
        viewModelScope.launch {
            if (name.isBlank() || cpf.isBlank() || phone.isBlank() || address.isBlank()) {
                _toastMessage.emit("Preencha todos os campos obrigatórios (Nome, CPF, Celular e Endereço)")
                return@launch
            }
            try {
                val customer = Customer(
                    name = name.trim(),
                    cpf = cpf.replace(Regex("[^0-9]"), ""),
                    phone = phone.trim(),
                    email = email.trim(),
                    creditLimit = 1000000.0,
                    address = address.trim()
                )
                repository.addCustomer(customer)
                _toastMessage.emit("Cliente cadastrado com sucesso!")
                if (isOnline.value) {
                    performSynchronization(automatic = true)
                }
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao cadastrar cliente: ${e.localizedMessage}")
            }
        }
    }

    fun updateCustomerDetails(id: Long, name: String, cpf: String, phone: String, email: String, address: String) {
        viewModelScope.launch {
            if (name.isBlank() || cpf.isBlank() || phone.isBlank() || address.isBlank()) {
                _toastMessage.emit("Preencha todos os campos obrigatórios (Nome, CPF, Celular e Endereço)")
                return@launch
            }
            try {
                val existing = repository.allCustomers.firstOrNull()?.find { it.id == id }
                if (existing != null) {
                    val updated = existing.copy(
                        name = name.trim(),
                        cpf = cpf.replace(Regex("[^0-9]"), ""),
                        phone = phone.trim(),
                        email = email.trim(),
                        address = address.trim()
                    )
                    repository.updateCustomer(updated)
                    _toastMessage.emit("Dados do cliente atualizados!")
                    if (isOnline.value) {
                        performSynchronization(automatic = true)
                    }
                    // Also update selectedCustomer if open
                    val currentSelected = selectedCustomer.value
                    if (currentSelected != null && currentSelected.customer.id == id) {
                        selectedCustomer.value = currentSelected.copy(customer = updated)
                    }
                }
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao atualizar cliente: ${e.localizedMessage}")
            }
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            try {
                repository.deleteCustomer(customer)
                _toastMessage.emit("Cliente excluído com sucesso!")
                if (selectedCustomer.value?.customer?.id == customer.id) {
                    selectedCustomer.value = null
                }
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao excluir cliente: ${e.localizedMessage}")
            }
        }
    }

    fun addCreditSale(
        customerId: Long,
        description: String,
        totalAmount: Double,
        installmentsCount: Int,
        firstDueDate: Long,
        productId: Long? = null,
        customerHousePhoto: String? = null,
        sellerId: Long? = null
    ) {
        viewModelScope.launch {
            if (customerId <= 0 || description.isBlank() || totalAmount <= 0.0 || installmentsCount <= 0) {
                _toastMessage.emit("Preencha os campos da venda corretamente")
                return@launch
            }
            if (customerHousePhoto.isNullOrBlank()) {
                _toastMessage.emit("Erro: A foto da casa do cliente é obrigatória!")
                return@launch
            }
            try {
                // Verify credit limit
                val customerStats = allCustomersWithStats.value.find { it.customer.id == customerId }
                if (customerStats != null) {
                    val remainingLimit = customerStats.limitRemaining()
                    if (totalAmount > remainingLimit) {
                        _toastMessage.emit("Aviso: Limite de crédito insuficiente! Limite restante: R$ $remainingLimit")
                    }
                }

                repository.registerCreditSale(
                    customerId = customerId,
                    description = description.trim(),
                    totalAmount = totalAmount,
                    installmentsCount = installmentsCount,
                    firstDueDate = firstDueDate,
                    productId = productId,
                    customerHousePhoto = customerHousePhoto,
                    sellerId = sellerId
                )
                _toastMessage.emit("Venda parcelada cadastrada com sucesso!")
                if (isOnline.value) {
                    performSynchronization(automatic = true)
                }
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao registrar venda: ${e.localizedMessage}")
            }
        }
    }

    fun addProduct(name: String, costPrice: Double, salePrice: Double, stock: Int, photoUrl: String? = null) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    _toastMessage.emit("Nome do produto inválido")
                    return@launch
                }
                val prod = Product(
                    name = name.trim(),
                    costPrice = costPrice,
                    salePrice = salePrice,
                    stock = stock,
                    photoUrl = photoUrl
                )
                repository.addProduct(prod)
                _toastMessage.emit("Produto cadastrado com sucesso!")
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao cadastrar produto: ${e.localizedMessage}")
            }
        }
    }

    fun updateProduct(id: Long, name: String, costPrice: Double, salePrice: Double, stock: Int, photoUrl: String? = null) {
        viewModelScope.launch {
            try {
                val prod = Product(
                    id = id,
                    name = name.trim(),
                    costPrice = costPrice,
                    salePrice = salePrice,
                    stock = stock,
                    photoUrl = photoUrl
                )
                repository.updateProduct(prod)
                _toastMessage.emit("Produto atualizado com sucesso!")
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao atualizar produto: ${e.localizedMessage}")
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(product)
                _toastMessage.emit("Produto removido com sucesso!")
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao remover produto: ${e.localizedMessage}")
            }
        }
    }

    // employee CRUD operations
    fun addEmployee(name: String, role: String, phone: String = "") {
        viewModelScope.launch {
            if (name.isBlank() || role.isBlank()) {
                _toastMessage.emit("Nome e cargo são obrigatórios")
                return@launch
            }
            try {
                repository.addEmployee(Employee(name = name.trim(), role = role, phone = phone.trim()))
                _toastMessage.emit("Funcionário cadastrado com sucesso!")
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao cadastrar funcionário: ${e.localizedMessage}")
            }
        }
    }

    fun updateEmployee(employee: Employee) {
        viewModelScope.launch {
            try {
                repository.updateEmployee(employee)
                _toastMessage.emit("Funcionário atualizado com sucesso!")
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao atualizar funcionário: ${e.localizedMessage}")
            }
        }
    }

    fun deleteEmployee(employee: Employee) {
        viewModelScope.launch {
            try {
                repository.deleteEmployee(employee)
                _toastMessage.emit("Funcionário removido com sucesso!")
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao remover funcionário: ${e.localizedMessage}")
            }
        }
    }

    fun receivePayment(installmentId: Long, paymentAmount: Double, collectorId: Long? = null) {
        viewModelScope.launch {
            if (paymentAmount <= 0.0) {
                _toastMessage.emit("Valor do pagamento inválido")
                return@launch
            }
            try {
                val success = repository.payInstallment(installmentId, paymentAmount, collectorId)
                if (success) {
                    _toastMessage.emit("Pagamento recebido com sucesso!")
                    if (isOnline.value) {
                        performSynchronization(automatic = true)
                    }
                } else {
                    _toastMessage.emit("Erro: Parcela não encontrada")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao receber pagamento: ${e.localizedMessage}")
            }
        }
    }

    fun undoPayment(installmentId: Long) {
        viewModelScope.launch {
            try {
                val success = repository.undoInstallmentPayment(installmentId)
                if (success) {
                    _toastMessage.emit("Pagamento estornado com sucesso!")
                    if (isOnline.value) {
                        performSynchronization(automatic = true)
                    }
                } else {
                    _toastMessage.emit("Erro: Parcela não encontrada")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao estornar: ${e.localizedMessage}")
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CrediarioViewModel::class.java)) {
                val db = AppDatabase.getDatabase(context)
                val networkMonitor = NetworkMonitor(context.applicationContext)
                val repository = CrediarioRepository(
                    customerDao = db.customerDao(),
                    creditSaleDao = db.creditSaleDao(),
                    installmentDao = db.installmentDao(),
                    productDao = db.productDao(),
                    employeeDao = db.employeeDao()
                )
                @Suppress("UNCHECKED_CAST")
                return CrediarioViewModel(repository, networkMonitor) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
