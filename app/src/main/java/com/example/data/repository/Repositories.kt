package com.example.data.repository

import com.example.data.local.SessionManager
import com.example.data.model.*
import com.example.data.network.SupabaseClient
import org.json.JSONObject

class AdminRepository(
    private val supabaseClient: SupabaseClient,
    private val sessionManager: SessionManager
) {
    val currentAdmin = sessionManager.currentAdmin
    val isLoggedIn = sessionManager.isLoggedIn

    suspend fun login(email: String, pass: String): Result<Admin> =
        supabaseClient.login(email, pass)

    fun logout() = sessionManager.clearSession()

    suspend fun testConnection(): Result<String> = supabaseClient.testConnection()

    fun updateConfig(url: String, anonKey: String, cloudName: String, preset: String) {
        sessionManager.updateConfig(url, anonKey, cloudName, preset)
    }

    fun getSupabaseUrl() = sessionManager.getSupabaseUrl()
    fun getSupabaseAnonKey() = sessionManager.getSupabaseAnonKey()
    fun getCloudinaryCloudName() = sessionManager.getCloudinaryCloudName()
    fun getCloudinaryPreset() = sessionManager.getCloudinaryUploadPreset()

    // App theme preference (light / dark / system)
    val themeMode = sessionManager.themeMode
    fun setThemeMode(mode: String) = sessionManager.setThemeMode(mode)

    suspend fun getAdmins(): Result<List<Admin>> {
        return supabaseClient.get("admins", "select=*&order=created_at.desc").map { arr ->
            val list = mutableListOf<Admin>()
            for (i in 0 until arr.length()) {
                list.add(Admin.fromJson(arr.getJSONObject(i)))
            }
            list
        }
    }

    /**
     * [isNew] is passed explicitly by the caller rather than inferred from `admin.id.isBlank()`,
     * because a new admin's `id` is deliberately pre-set to their Supabase Auth UUID (see
     * AdminsScreen) — inferring "new" from a blank id would misroute that case to update() and
     * silently do nothing.
     */
    suspend fun saveAdmin(admin: Admin, isNew: Boolean): Result<Admin> {
        return if (isNew) {
            supabaseClient.insert("admins", admin.toJson()).map { Admin.fromJson(it) }
        } else {
            supabaseClient.update("admins", admin.id, admin.toJson()).map { Admin.fromJson(it) }
        }
    }

    suspend fun toggleActive(admin: Admin): Result<Admin> {
        val updated = admin.copy(active = !admin.active)
        return supabaseClient.update("admins", admin.id, updated.toJson()).map { Admin.fromJson(it) }
    }

    suspend fun deleteAdmin(id: String): Result<Boolean> {
        return supabaseClient.delete("admins", id)
    }
}

class DashboardRepository(private val supabaseClient: SupabaseClient) {
    suspend fun getStats(): Result<DashboardStats> {
        return try {
            val busesRes = supabaseClient.get("buses")
            val opRes = supabaseClient.get("bus_operators")
            val routesRes = supabaseClient.get("routes")
            val countersRes = supabaseClient.get("counters")
            val mcRes = supabaseClient.get("mini_coaches")
            val tourRes = supabaseClient.get("tour_packages")
            val bookingsRes = supabaseClient.get("bookings")

            val bookingsList = mutableListOf<Booking>()
            var pending = 0
            var confirmed = 0

            bookingsRes.getOrNull()?.let { arr ->
                for (i in 0 until arr.length()) {
                    val b = Booking.fromJson(arr.getJSONObject(i))
                    bookingsList.add(b)
                    if (b.status == "pending") pending++
                    if (b.status == "confirmed") confirmed++
                }
            }

            val stats = DashboardStats(
                totalBuses = busesRes.getOrNull()?.length() ?: 0,
                totalOperators = opRes.getOrNull()?.length() ?: 0,
                totalRoutes = routesRes.getOrNull()?.length() ?: 0,
                totalCounters = countersRes.getOrNull()?.length() ?: 0,
                totalMiniCoaches = mcRes.getOrNull()?.length() ?: 0,
                totalTourPackages = tourRes.getOrNull()?.length() ?: 0,
                pendingBookings = pending,
                confirmedBookings = confirmed,
                recentBookings = bookingsList.take(6)
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DistrictRepository(private val supabaseClient: SupabaseClient) {
    suspend fun getDistricts(): Result<List<District>> {
        return supabaseClient.get("districts", "select=*&order=name.asc").map { arr ->
            val list = mutableListOf<District>()
            for (i in 0 until arr.length()) {
                list.add(District.fromJson(arr.getJSONObject(i)))
            }
            list
        }
    }

    suspend fun saveDistrict(district: District): Result<District> {
        return if (district.id.isBlank()) {
            supabaseClient.insert("districts", district.toJson()).map { District.fromJson(it) }
        } else {
            supabaseClient.update("districts", district.id, district.toJson()).map { District.fromJson(it) }
        }
    }

    suspend fun toggleActive(district: District): Result<District> {
        val updated = district.copy(active = !district.active)
        return saveDistrict(updated)
    }

    suspend fun deleteDistrict(id: String): Result<Boolean> {
        return supabaseClient.delete("districts", id)
    }
}

class BusOperatorRepository(private val supabaseClient: SupabaseClient) {
    suspend fun getOperators(): Result<List<BusOperator>> {
        return supabaseClient.get("bus_operators", "select=*&order=name.asc").map { arr ->
            val list = mutableListOf<BusOperator>()
            for (i in 0 until arr.length()) {
                list.add(BusOperator.fromJson(arr.getJSONObject(i)))
            }
            list
        }
    }

    suspend fun saveOperator(operator: BusOperator): Result<BusOperator> {
        return if (operator.id.isBlank()) {
            supabaseClient.insert("bus_operators", operator.toJson()).map { BusOperator.fromJson(it) }
        } else {
            supabaseClient.update("bus_operators", operator.id, operator.toJson()).map { BusOperator.fromJson(it) }
        }
    }

    suspend fun toggleActive(op: BusOperator): Result<BusOperator> {
        return saveOperator(op.copy(active = !op.active))
    }

    suspend fun deleteOperator(id: String): Result<Boolean> {
        return supabaseClient.delete("bus_operators", id)
    }
}

class BusRepository(private val supabaseClient: SupabaseClient) {
    suspend fun getBuses(): Result<List<Bus>> {
        return supabaseClient.get("buses", "select=*&order=created_at.desc").map { arr ->
            val list = mutableListOf<Bus>()
            for (i in 0 until arr.length()) {
                list.add(Bus.fromJson(arr.getJSONObject(i)))
            }
            list
        }
    }

    suspend fun saveBus(bus: Bus): Result<Bus> {
        return if (bus.id.isBlank()) {
            supabaseClient.insert("buses", bus.toJson()).map { Bus.fromJson(it) }
        } else {
            supabaseClient.update("buses", bus.id, bus.toJson()).map { Bus.fromJson(it) }
        }
    }

    suspend fun toggleActive(bus: Bus): Result<Bus> {
        return saveBus(bus.copy(active = !bus.active))
    }

    suspend fun deleteBus(id: String): Result<Boolean> {
        return supabaseClient.delete("buses", id)
    }
}

class RouteRepository(private val supabaseClient: SupabaseClient) {
    suspend fun getRoutes(): Result<List<Route>> {
        return supabaseClient.get("routes", "select=*&order=created_at.desc").map { arr ->
            val list = mutableListOf<Route>()
            for (i in 0 until arr.length()) {
                list.add(Route.fromJson(arr.getJSONObject(i)))
            }
            list
        }
    }

    suspend fun saveRoute(route: Route): Result<Route> {
        return if (route.id.isBlank()) {
            supabaseClient.insert("routes", route.toJson()).map { Route.fromJson(it) }
        } else {
            supabaseClient.update("routes", route.id, route.toJson()).map { Route.fromJson(it) }
        }
    }

    suspend fun toggleActive(route: Route): Result<Route> {
        return saveRoute(route.copy(active = !route.active))
    }

    suspend fun deleteRoute(id: String): Result<Boolean> {
        return supabaseClient.delete("routes", id)
    }
}

class ScheduleRepository(private val supabaseClient: SupabaseClient) {
    suspend fun getSchedules(): Result<List<BusRoute>> {
        return supabaseClient.get("bus_routes", "select=*&order=created_at.desc").map { arr ->
            val list = mutableListOf<BusRoute>()
            for (i in 0 until arr.length()) {
                list.add(BusRoute.fromJson(arr.getJSONObject(i)))
            }
            list
        }
    }

    suspend fun saveSchedule(schedule: BusRoute): Result<BusRoute> {
        return if (schedule.id.isBlank()) {
            supabaseClient.insert("bus_routes", schedule.toJson()).map { BusRoute.fromJson(it) }
        } else {
            supabaseClient.update("bus_routes", schedule.id, schedule.toJson()).map { BusRoute.fromJson(it) }
        }
    }

    suspend fun toggleActive(schedule: BusRoute): Result<BusRoute> {
        return saveSchedule(schedule.copy(active = !schedule.active))
    }

    suspend fun deleteSchedule(id: String): Result<Boolean> {
        return supabaseClient.delete("bus_routes", id)
    }
}

class CounterRepository(private val supabaseClient: SupabaseClient) {
    suspend fun getCounters(): Result<List<Counter>> {
        return supabaseClient.get("counters", "select=*&order=counter_name.asc").map { arr ->
            val list = mutableListOf<Counter>()
            for (i in 0 until arr.length()) {
                list.add(Counter.fromJson(arr.getJSONObject(i)))
            }
            list
        }
    }

    suspend fun saveCounter(counter: Counter): Result<Counter> {
        return if (counter.id.isBlank()) {
            supabaseClient.insert("counters", counter.toJson()).map { Counter.fromJson(it) }
        } else {
            supabaseClient.update("counters", counter.id, counter.toJson()).map { Counter.fromJson(it) }
        }
    }

    suspend fun toggleActive(counter: Counter): Result<Counter> {
        return saveCounter(counter.copy(active = !counter.active))
    }

    suspend fun deleteCounter(id: String): Result<Boolean> {
        return supabaseClient.delete("counters", id)
    }
}

class FareRepository(private val supabaseClient: SupabaseClient) {
    suspend fun getFares(): Result<List<Fare>> {
        return supabaseClient.get("fares", "select=*&order=created_at.desc").map { arr ->
            val list = mutableListOf<Fare>()
            for (i in 0 until arr.length()) {
                list.add(Fare.fromJson(arr.getJSONObject(i)))
            }
            list
        }
    }

    suspend fun saveFare(fare: Fare): Result<Fare> {
        return if (fare.id.isBlank()) {
            supabaseClient.insert("fares", fare.toJson()).map { Fare.fromJson(it) }
        } else {
            supabaseClient.update("fares", fare.id, fare.toJson()).map { Fare.fromJson(it) }
        }
    }

    suspend fun toggleActive(fare: Fare): Result<Fare> {
        return saveFare(fare.copy(active = !fare.active))
    }

    suspend fun deleteFare(id: String): Result<Boolean> {
        return supabaseClient.delete("fares", id)
    }
}

class MiniCoachRepository(private val supabaseClient: SupabaseClient) {
    suspend fun getMiniCoaches(): Result<List<MiniCoach>> {
        return supabaseClient.get("mini_coaches", "select=*&order=name.asc").map { arr ->
            val list = mutableListOf<MiniCoach>()
            for (i in 0 until arr.length()) {
                list.add(MiniCoach.fromJson(arr.getJSONObject(i)))
            }
            list
        }
    }

    suspend fun saveMiniCoach(coach: MiniCoach): Result<MiniCoach> {
        return if (coach.id.isBlank()) {
            supabaseClient.insert("mini_coaches", coach.toJson()).map { MiniCoach.fromJson(it) }
        } else {
            supabaseClient.update("mini_coaches", coach.id, coach.toJson()).map { MiniCoach.fromJson(it) }
        }
    }

    suspend fun toggleActive(coach: MiniCoach): Result<MiniCoach> {
        return saveMiniCoach(coach.copy(active = !coach.active))
    }

    suspend fun deleteMiniCoach(id: String): Result<Boolean> {
        return supabaseClient.delete("mini_coaches", id)
    }
}

class TourPackageRepository(private val supabaseClient: SupabaseClient) {
    suspend fun getTourPackages(): Result<List<TourPackage>> {
        return supabaseClient.get("tour_packages", "select=*&order=created_at.desc").map { arr ->
            val list = mutableListOf<TourPackage>()
            for (i in 0 until arr.length()) {
                list.add(TourPackage.fromJson(arr.getJSONObject(i)))
            }
            list
        }
    }

    suspend fun savePackage(pkg: TourPackage): Result<TourPackage> {
        return if (pkg.id.isBlank()) {
            supabaseClient.insert("tour_packages", pkg.toJson()).map { TourPackage.fromJson(it) }
        } else {
            supabaseClient.update("tour_packages", pkg.id, pkg.toJson()).map { TourPackage.fromJson(it) }
        }
    }

    suspend fun toggleActive(pkg: TourPackage): Result<TourPackage> {
        return savePackage(pkg.copy(active = !pkg.active))
    }

    suspend fun deletePackage(id: String): Result<Boolean> {
        return supabaseClient.delete("tour_packages", id)
    }
}

class BookingRepository(private val supabaseClient: SupabaseClient) {
    suspend fun getBookings(): Result<List<Booking>> {
        return supabaseClient.get("bookings", "select=*&order=created_at.desc").map { arr ->
            val list = mutableListOf<Booking>()
            for (i in 0 until arr.length()) {
                list.add(Booking.fromJson(arr.getJSONObject(i)))
            }
            list
        }
    }

    suspend fun updateStatus(id: String, newStatus: String): Result<JSONObject> {
        val payload = JSONObject().apply { put("status", newStatus) }
        return supabaseClient.update("bookings", id, payload)
    }

    suspend fun updateEstimatedPrice(id: String, newPrice: Double): Result<JSONObject> {
        val payload = JSONObject().apply { put("estimated_price", newPrice) }
        return supabaseClient.update("bookings", id, payload)
    }
}
