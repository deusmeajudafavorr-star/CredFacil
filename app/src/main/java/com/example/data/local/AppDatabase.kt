package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CreditSale
import com.example.data.model.Customer
import com.example.data.model.Installment
import com.example.data.model.Product
import com.example.data.model.Employee

@Database(entities = [Customer::class, CreditSale::class, Installment::class, Product::class, Employee::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun creditSaleDao(): CreditSaleDao
    abstract fun installmentDao(): InstallmentDao
    abstract fun productDao(): ProductDao
    abstract fun employeeDao(): EmployeeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "crediario_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
