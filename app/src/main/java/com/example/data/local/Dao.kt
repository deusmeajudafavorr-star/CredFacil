package com.example.data.local

import androidx.room.*
import com.example.data.model.CreditSale
import com.example.data.model.Customer
import com.example.data.model.Installment
import com.example.data.model.Product
import com.example.data.model.Employee
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("SELECT * FROM customers WHERE synced = 0")
    suspend fun getUnsyncedCustomers(): List<Customer>

    @Query("UPDATE customers SET synced = :synced WHERE id = :id")
    suspend fun updateCustomerSyncStatus(id: Long, synced: Boolean)
}

@Dao
interface CreditSaleDao {
    @Query("SELECT * FROM credit_sales ORDER BY saleDate DESC")
    fun getAllCreditSales(): Flow<List<CreditSale>>

    @Query("SELECT * FROM credit_sales WHERE customerId = :customerId ORDER BY saleDate DESC")
    fun getSalesByCustomer(customerId: Long): Flow<List<CreditSale>>

    @Query("SELECT * FROM credit_sales WHERE id = :id")
    suspend fun getSaleById(id: Long): CreditSale?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditSale(sale: CreditSale): Long

    @Delete
    suspend fun deleteCreditSale(sale: CreditSale)

    @Query("DELETE FROM credit_sales WHERE customerId = :customerId")
    suspend fun deleteSalesByCustomer(customerId: Long)

    @Query("SELECT * FROM credit_sales WHERE synced = 0")
    suspend fun getUnsyncedSales(): List<CreditSale>

    @Query("UPDATE credit_sales SET synced = :synced WHERE id = :id")
    suspend fun updateSaleSyncStatus(id: Long, synced: Boolean)
}

@Dao
interface InstallmentDao {
    @Query("SELECT * FROM installments ORDER BY dueDate ASC")
    fun getAllInstallments(): Flow<List<Installment>>

    @Query("SELECT * FROM installments WHERE saleId = :saleId ORDER BY installmentNumber ASC")
    fun getInstallmentsBySale(saleId: Long): Flow<List<Installment>>

    @Query("SELECT * FROM installments WHERE customerId = :customerId ORDER BY dueDate ASC")
    fun getInstallmentsByCustomer(customerId: Long): Flow<List<Installment>>

    @Query("SELECT * FROM installments WHERE id = :id")
    suspend fun getInstallmentById(id: Long): Installment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallments(installments: List<Installment>)

    @Update
    suspend fun updateInstallment(installment: Installment)

    @Query("DELETE FROM installments WHERE saleId = :saleId")
    suspend fun deleteInstallmentsBySale(saleId: Long)

    @Query("DELETE FROM installments WHERE customerId = :customerId")
    suspend fun deleteInstallmentsByCustomer(customerId: Long)

    @Query("SELECT * FROM installments WHERE synced = 0")
    suspend fun getUnsyncedInstallments(): List<Installment>

    @Query("UPDATE installments SET synced = :synced WHERE id = :id")
    suspend fun updateInstallmentSyncStatus(id: Long, synced: Boolean)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("SELECT * FROM products WHERE synced = 0")
    suspend fun getUnsyncedProducts(): List<Product>

    @Query("UPDATE products SET synced = :synced WHERE id = :id")
    suspend fun updateProductSyncStatus(id: Long, synced: Boolean)
}

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getEmployeeById(id: Long): Employee?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee): Long

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Delete
    suspend fun deleteEmployee(employee: Employee)

    @Query("SELECT * FROM employees WHERE synced = 0")
    suspend fun getUnsyncedEmployees(): List<Employee>

    @Query("UPDATE employees SET synced = :synced WHERE id = :id")
    suspend fun updateEmployeeSyncStatus(id: Long, synced: Boolean)
}

