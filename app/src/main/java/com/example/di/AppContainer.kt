package com.example.di

import android.content.Context
import com.example.data.local.SessionManager
import com.example.data.network.CloudinaryUploader
import com.example.data.network.SupabaseClient
import com.example.data.repository.*

class AppContainer(context: Context) {
    val sessionManager = SessionManager.getInstance(context)
    val supabaseClient = SupabaseClient(sessionManager)
    val cloudinaryUploader = CloudinaryUploader(sessionManager)

    val adminRepository = AdminRepository(supabaseClient, sessionManager)
    val dashboardRepository = DashboardRepository(supabaseClient)
    val districtRepository = DistrictRepository(supabaseClient)
    val operatorRepository = BusOperatorRepository(supabaseClient)
    val busRepository = BusRepository(supabaseClient)
    val routeRepository = RouteRepository(supabaseClient)
    val scheduleRepository = ScheduleRepository(supabaseClient)
    val counterRepository = CounterRepository(supabaseClient)
    val fareRepository = FareRepository(supabaseClient)
    val miniCoachRepository = MiniCoachRepository(supabaseClient)
    val tourPackageRepository = TourPackageRepository(supabaseClient)
    val bookingRepository = BookingRepository(supabaseClient)
}
