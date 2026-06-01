package com.example.data.repository

import com.example.data.local.CreditSaleDao
import com.example.data.local.CustomerDao
import com.example.data.local.InstallmentDao
import com.example.data.local.ProductDao
import com.example.data.local.EmployeeDao
import com.example.data.model.*
import com.example.data.api.CentralSyncService
import com.example.data.api.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar

class CrediarioRepository(
    private val customerDao: CustomerDao,
    private val creditSaleDao: CreditSaleDao,
    private val installmentDao: InstallmentDao,
    private val productDao: ProductDao,
    private val employeeDao: EmployeeDao,
    private val syncService: CentralSyncService = CentralSyncService()
) {
    // 1. Reactive streams
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val allCreditSales: Flow<List<CreditSale>> = creditSaleDao.getAllCreditSales()
    val allInstallments: Flow<List<Installment>> = installmentDao.getAllInstallments()
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allEmployees: Flow<List<Employee>> = employeeDao.getAllEmployees()

    // 2. Complex reactive streams combining data
    val customersWithStats: Flow<List<CustomerWithStats>> = combine(
        allCustomers,
        allInstallments
    ) { customers, installments ->
        val now = System.currentTimeMillis()
        customers.map { customer ->
            val custInstallments = installments.filter { it.customerId == customer.id }
            val totalDebt = custInstallments
                .filter { it.paidDate == null }
                .sumOf { it.amount - it.amountPaid }
            
            val overdueCount = custInstallments
                .count { it.paidDate == null && now > it.dueDate }

            val limitUsed = custInstallments
                .filter { it.paidDate == null }
                .sumOf { it.amount - it.amountPaid }

            CustomerWithStats(
                customer = customer,
                totalDebt = totalDebt,
                overdueCount = overdueCount,
                limitUsed = limitUsed
            )
        }
    }

    val installmentsDetailed: Flow<List<InstallmentWithDetails>> = combine(
        allInstallments,
        allCustomers,
        allCreditSales
    ) { installments, customers, sales ->
        installments.mapNotNull { inst ->
            val customer = customers.find { it.id == inst.customerId } ?: return@mapNotNull null
            val sale = sales.find { it.id == inst.saleId }

            InstallmentWithDetails(
                installment = inst,
                customerName = customer.name,
                saleDescription = sale?.description ?: "Venda Removida",
                customerAddress = customer.address,
                customerHousePhoto = sale?.customerHousePhoto
            )
        }
    }

    // 3. Customer Operations
    suspend fun addCustomer(customer: Customer): Long {
        return customerDao.insertCustomer(customer)
    }

    suspend fun updateCustomer(customer: Customer) {
        customerDao.updateCustomer(customer)
    }

    suspend fun deleteCustomer(customer: Customer) {
        // Also cleanup their sales and installments
        installmentDao.deleteInstallmentsByCustomer(customer.id)
        creditSaleDao.deleteSalesByCustomer(customer.id)
        customerDao.deleteCustomer(customer)
    }

    // 3b. Product Operations
    suspend fun addProduct(product: Product): Long {
        return productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
    }

    // 3c. Employee Operations
    suspend fun addEmployee(employee: Employee): Long {
        return employeeDao.insertEmployee(employee)
    }

    suspend fun updateEmployee(employee: Employee) {
        employeeDao.updateEmployee(employee)
    }

    suspend fun deleteEmployee(employee: Employee) {
        employeeDao.deleteEmployee(employee)
    }

    // 4. Sales and Installment Generator Operations
    suspend fun registerCreditSale(
        customerId: Long,
        description: String,
        totalAmount: Double,
        installmentsCount: Int,
        firstDueDate: Long,
        productId: Long? = null,
        customerHousePhoto: String? = null,
        sellerId: Long? = null
    ): Long {
        // Optional stock decrement
        if (productId != null) {
            val prod = productDao.getProductById(productId)
            if (prod != null) {
                val newStock = (prod.stock - 1).coerceAtLeast(0)
                productDao.updateProduct(prod.copy(stock = newStock, synced = false))
            }
        }

        val sale = CreditSale(
            customerId = customerId,
            description = description,
            totalAmount = totalAmount,
            installmentsCount = installmentsCount,
            saleDate = System.currentTimeMillis(),
            customerHousePhoto = customerHousePhoto,
            sellerId = sellerId
        )
        val saleId = creditSaleDao.insertCreditSale(sale)

        // Generate installments
        val installments = mutableListOf<Installment>()
        val exactAmountPerInstallment = totalAmount / installmentsCount
        
        // Let's handle penny roundings nicely by placing the remaining cents in the last installment
        var accumulated = 0.0
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = firstDueDate

        for (i in 1..installmentsCount) {
            val amount = if (i == installmentsCount) {
                totalAmount - accumulated
            } else {
                // Round to 2 decimals
                val rounded = Math.round(exactAmountPerInstallment * 100.0) / 100.0
                accumulated += rounded
                rounded
            }

            installments.add(
                Installment(
                    saleId = saleId,
                    customerId = customerId,
                    installmentNumber = i,
                    amount = amount,
                    dueDate = calendar.timeInMillis,
                    amountPaid = 0.0,
                    paidDate = null,
                    status = "PENDENT"
                )
            )
            // Increment by 1 month for subsequent installments
            calendar.add(Calendar.MONTH, 1)
        }

        installmentDao.insertInstallments(installments)
        return saleId
    }

    suspend fun deleteCreditSale(sale: CreditSale) {
        installmentDao.deleteInstallmentsBySale(sale.id)
        creditSaleDao.deleteCreditSale(sale)
    }

    // 5. Installment Actions (Payment)
    suspend fun payInstallment(installmentId: Long, paymentAmount: Double, collectorId: Long? = null): Boolean {
        val inst = installmentDao.getInstallmentById(installmentId) ?: return false
        val newAmountPaid = (inst.amountPaid + paymentAmount).coerceAtMost(inst.amount)
        val isFullyPaid = newAmountPaid >= inst.amount

        val updatedInstallment = inst.copy(
            amountPaid = newAmountPaid,
            paidDate = if (isFullyPaid) System.currentTimeMillis() else null,
            status = if (isFullyPaid) "PAID" else "PENDENT",
            synced = false,
            collectorId = collectorId ?: inst.collectorId
        )
        installmentDao.updateInstallment(updatedInstallment)
        return true
    }
    
    suspend fun undoInstallmentPayment(installmentId: Long): Boolean {
        val inst = installmentDao.getInstallmentById(installmentId) ?: return false
        val updatedInstallment = inst.copy(
            amountPaid = 0.0,
            paidDate = null,
            status = "PENDENT",
            synced = false
        )
        installmentDao.updateInstallment(updatedInstallment)
        return true
    }

    // 6. Bulk Online Sync Orchestrator
    suspend fun synchronizeWithCentralServer(): SyncSummary {
        var customersSynced = 0
        var salesSynced = 0
        var installmentsSynced = 0
        var success = true
        val errorMessages = mutableListOf<String>()

        try {
            // 1. Sync unsynced customers
            val unsyncedCustomers = customerDao.getUnsyncedCustomers()
            if (unsyncedCustomers.isNotEmpty()) {
                val result = syncService.syncCustomerBatch(unsyncedCustomers)
                if (result.success) {
                    result.syncedIds.forEach { id ->
                        customerDao.updateCustomerSyncStatus(id, true)
                        customersSynced++
                    }
                } else {
                    success = false
                    errorMessages.add(result.message ?: "Clientes falharam")
                }
            }

            // 2. Sync unsynced credit sales
            val unsyncedSales = creditSaleDao.getUnsyncedSales()
            if (unsyncedSales.isNotEmpty()) {
                val result = syncService.syncSaleBatch(unsyncedSales)
                if (result.success) {
                    result.syncedIds.forEach { id ->
                        creditSaleDao.updateSaleSyncStatus(id, true)
                        salesSynced++
                    }
                } else {
                    success = false
                    errorMessages.add(result.message ?: "Vendas falharam")
                }
            }

            // 3. Sync unsynced installments (payments updates)
            val unsyncedInstallments = installmentDao.getUnsyncedInstallments()
            if (unsyncedInstallments.isNotEmpty()) {
                val result = syncService.syncInstallmentBatch(unsyncedInstallments)
                if (result.success) {
                    result.syncedIds.forEach { id ->
                        installmentDao.updateInstallmentSyncStatus(id, true)
                        installmentsSynced++
                    }
                } else {
                    success = false
                    errorMessages.add(result.message ?: "Parcelas falharam")
                }
            }
        } catch (e: Exception) {
            success = false
            errorMessages.add(e.localizedMessage ?: "Erro de rede desconhecido")
        }

        val message = when {
            customersSynced == 0 && salesSynced == 0 && installmentsSynced == 0 -> "Tudo sincronizado! Nenhum novo dado pendente."
            success -> "Sucesso! Sincronizados: $customersSynced cliente(s), $salesSynced venda(s), $installmentsSynced parcela(s)."
            else -> "Sincronização parcial efetuada. Erros: ${errorMessages.joinToString("; ")}"
        }

        return SyncSummary(
            customersSyncedCount = customersSynced,
            salesSyncedCount = salesSynced,
            installmentsSyncedCount = installmentsSynced,
            isSuccess = success,
            message = message
        )
    }
}

data class SyncSummary(
    val customersSyncedCount: Int,
    val salesSyncedCount: Int,
    val installmentsSyncedCount: Int,
    val isSuccess: Boolean,
    val message: String
)
