package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val cpf: String,
    val phone: String,
    val email: String,
    val creditLimit: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false,
    val address: String = ""
) {
    fun formatLimit(): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(creditLimit)
}

@Entity(tableName = "credit_sales")
data class CreditSale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val description: String,
    val totalAmount: Double,
    val installmentsCount: Int,
    val saleDate: Long = System.currentTimeMillis(),
    val synced: Boolean = false,
    val customerHousePhoto: String? = null,
    val sellerId: Long? = null
) {
    fun formatTotal(): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(totalAmount)
}

@Entity(tableName = "installments")
data class Installment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val customerId: Long,
    val installmentNumber: Int,  // e.g. 1 of 3
    val amount: Double,
    val dueDate: Long,
    val paidDate: Long? = null,
    val amountPaid: Double = 0.0,
    val status: String = "PENDENT", // PENDENT, PAID, OVERDUE (dynamically evaluated or stored)
    val synced: Boolean = false,
    val collectorId: Long? = null
) {
    fun formatAmount(): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(amount)
    
    fun formatDueDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        return sdf.format(Date(dueDate))
    }

    fun isOverdue(): Boolean {
        if (paidDate != null) return false
        val todayStart = System.currentTimeMillis()
        return todayStart > dueDate
    }
}

// Data holder class for UI representation of an installment with details
data class InstallmentWithDetails(
    val installment: Installment,
    val customerName: String,
    val saleDescription: String,
    val customerAddress: String = "",
    val customerHousePhoto: String? = null
)

// Data holder class for Customer with credit stats
data class CustomerWithStats(
    val customer: Customer,
    val totalDebt: Double,
    val overdueCount: Int,
    val limitUsed: Double
) {
    fun formatTotalDebt(): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(totalDebt)
    fun limitRemaining(): Double = (customer.creditLimit - limitUsed).coerceAtLeast(0.0)
    fun limitUsedPercent(): Float = if (customer.creditLimit > 0) (limitUsed / customer.creditLimit).toFloat().coerceIn(0f, 1f) else 0f
}

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val costPrice: Double,
    val salePrice: Double,
    val stock: Int,
    val photoUrl: String? = null,
    val synced: Boolean = false
) {
    fun formatCostPrice(): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(costPrice)
    fun formatSalePrice(): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(salePrice)
}

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val role: String, // "Vendedor" or "Cobrador"
    val phone: String = "",
    val active: Boolean = true,
    val synced: Boolean = false
)

