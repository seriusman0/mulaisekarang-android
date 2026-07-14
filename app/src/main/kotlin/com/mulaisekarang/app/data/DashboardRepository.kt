package com.mulaisekarang.app.data

import com.mulaisekarang.app.data.model.DashboardSummary
import com.mulaisekarang.app.data.network.ApiService
import javax.inject.Inject

class DashboardRepository @Inject constructor(private val api: ApiService) {

    suspend fun summary(): DashboardSummary = api.dashboardSummary().data
}
