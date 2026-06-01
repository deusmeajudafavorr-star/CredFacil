package com.example.data.api

import com.example.data.model.CreditSale
import com.example.data.model.Customer
import com.example.data.model.Installment
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET

interface CentralSyncApi {

    @POST("sync/customers")
    suspend fun uploadCustomers(@Body customers: List<Customer>): SyncResult

    @POST("sync/sales")
    suspend fun uploadSales(@Body sales: List<CreditSale>): SyncResult

    @POST("sync/installments")
    suspend fun uploadInstallments(@Body installments: List<Installment>): SyncResult

    @GET("status")
    suspend fun checkConnection(): ConnectionStatus
}

data class SyncResult(
    val success: Boolean,
    val syncedIds: List<Long>,
    val message: String? = null
)

data class ConnectionStatus(
    val online: Boolean,
    val message: String
)
