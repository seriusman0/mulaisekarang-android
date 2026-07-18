package com.mulaisekarang.app.data

import com.mulaisekarang.app.data.model.TransactionsResponse
import com.mulaisekarang.app.data.network.ApiService
import javax.inject.Inject

class TransactionRepository @Inject constructor(private val api: ApiService) {

    suspend fun transactions(page: Int = 1): TransactionsResponse = api.transactions(page)
}
