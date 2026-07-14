package com.mulaisekarang.app.data

import com.mulaisekarang.app.data.model.DashboardSummary
import com.mulaisekarang.app.data.network.ApiService

class DashboardRepository(private val api: ApiService) {

    suspend fun summary(): DashboardSummary = api.dashboardSummary().data
}
