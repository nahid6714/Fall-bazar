package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

// 1. Admin Model
data class Admin(
    val id: String = "",
    val authUserId: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "admin",
    val active: Boolean = true,
    val createdAt: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        if (id.isNotBlank()) put("id", id)
        if (authUserId.isNotBlank()) put("auth_user_id", authUserId)
        put("email", email)
        put("name", name)
        put("role", role)
        put("is_active", active)
    }

    companion object {
        fun fromJson(json: JSONObject): Admin {
            val idVal = json.optString("id", "")
            val isActive = when {
                json.has("is_active") -> json.optBoolean("is_active", true)
                json.has("active") -> json.optBoolean("active", true)
                else -> true
            }
            val nameVal = json.optString("name", json.optString("full_name", "Admin"))
            return Admin(
                id = idVal,
                authUserId = json.optString("auth_user_id", json.optString("user_id", idVal)),
                email = json.optString("email", ""),
                name = if (nameVal.isNotBlank()) nameVal else "Admin",
                role = json.optString("role", "admin"),
                active = isActive,
                createdAt = json.optString("created_at", "")
            )
        }
    }
}

// 2. District Model
data class District(
    val id: String = "",
    val name: String = "",
    val division: String = "",
    val slug: String = "",
    val active: Boolean = true,
    val createdAt: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        if (id.isNotBlank()) put("id", id)
        put("name", name)
        put("division", division)
        put("slug", slug)
        put("is_active", active)
    }

    companion object {
        fun fromJson(json: JSONObject): District = District(
            id = json.optString("id", ""),
            name = json.optString("name", ""),
            division = json.optString("division", ""),
            slug = json.optString("slug", ""),
            active = json.optBoolean("is_active", json.optBoolean("active", true)),
            createdAt = json.optString("created_at", "")
        )
    }
}

// 3. Bus Operator Model
data class BusOperator(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val logo: String = "",
    val description: String = "",
    val phone: String = "",
    val website: String = "",
    val facebookUrl: String = "",
    val active: Boolean = true,
    val createdAt: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        if (id.isNotBlank()) put("id", id)
        put("name", name)
        put("slug", slug)
        put("logo_url", logo)
        put("description", description)
        put("phone", phone)
        put("website", website)
        put("facebook_url", facebookUrl)
        put("is_active", active)
    }

    companion object {
        fun fromJson(json: JSONObject): BusOperator = BusOperator(
            id = json.optString("id", ""),
            name = json.optString("name", ""),
            slug = json.optString("slug", ""),
            logo = json.optString("logo_url", json.optString("logo", "")),
            description = json.optString("description", ""),
            phone = json.optString("phone", ""),
            website = json.optString("website", ""),
            facebookUrl = json.optString("facebook_url", json.optString("facebookUrl", "")),
            active = json.optBoolean("is_active", json.optBoolean("active", true)),
            createdAt = json.optString("created_at", "")
        )
    }
}

// 4. Bus Model
data class Bus(
    val id: String = "",
    val operatorId: String = "",
    val name: String = "",
    val slug: String = "",
    val category: String = "Coach",
    val busType: String = "AC Deluxe",
    val description: String = "",
    val image: String = "",
    val phone: String = "",
    val isAc: Boolean = true,
    val seatCount: Int = 36,
    val active: Boolean = true,
    val createdAt: String = "",
    // Resolved relation helper
    val operatorName: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        if (id.isNotBlank()) put("id", id)
        if (operatorId.isNotBlank()) put("operator_id", operatorId)
        put("name", name)
        put("slug", slug)
        put("category", category)
        put("bus_type", busType)
        put("description", description)
        put("image_url", image)
        put("phone", phone)
        put("is_ac", isAc)
        put("seat_count", seatCount)
        put("is_active", active)
    }

    companion object {
        fun fromJson(json: JSONObject, operatorName: String = ""): Bus = Bus(
            id = json.optString("id", ""),
            operatorId = json.optString("operator_id", ""),
            name = json.optString("name", ""),
            slug = json.optString("slug", ""),
            category = json.optString("category", "Coach"),
            busType = json.optString("bus_type", if (json.optBoolean("is_ac", true)) "AC" else "Non-AC"),
            description = json.optString("description", ""),
            image = json.optString("image_url", json.optString("image", "")),
            phone = json.optString("phone", ""),
            isAc = json.optBoolean("is_ac", true),
            seatCount = json.optInt("seat_count", 36),
            active = json.optBoolean("is_active", json.optBoolean("active", true)),
            createdAt = json.optString("created_at", ""),
            operatorName = operatorName.ifBlank { json.optJSONObject("bus_operators")?.optString("name", "") ?: "" }
        )
    }
}

// 5. Route Model
data class Route(
    val id: String = "",
    val fromDistrictId: String = "",
    val toDistrictId: String = "",
    val distanceKm: Double = 0.0,
    val estimatedDuration: String = "",
    val description: String = "",
    val active: Boolean = true,
    val createdAt: String = "",
    // Resolved district names
    val fromDistrictName: String = "",
    val toDistrictName: String = ""
) {
    val title: String get() = if (fromDistrictName.isNotBlank() && toDistrictName.isNotBlank()) "$fromDistrictName → $toDistrictName" else "Route #$id"

    fun toJson(): JSONObject = JSONObject().apply {
        if (id.isNotBlank()) put("id", id)
        put("from_district_id", fromDistrictId)
        put("to_district_id", toDistrictId)
        put("distance_km", distanceKm)
        put("estimated_duration", estimatedDuration)
        put("description", description)
        put("is_active", active)
    }

    companion object {
        fun fromJson(json: JSONObject, fromName: String = "", toName: String = ""): Route = Route(
            id = json.optString("id", ""),
            fromDistrictId = json.optString("from_district_id", ""),
            toDistrictId = json.optString("to_district_id", ""),
            distanceKm = json.optDouble("distance_km", 0.0),
            estimatedDuration = json.optString("estimated_duration", ""),
            description = json.optString("description", ""),
            active = json.optBoolean("is_active", json.optBoolean("active", true)),
            createdAt = json.optString("created_at", ""),
            fromDistrictName = fromName,
            toDistrictName = toName
        )
    }
}

// 6. Bus Route / Schedule Model
data class BusRoute(
    val id: String = "",
    val busId: String = "",
    val routeId: String = "",
    val departureTime: String = "",
    val arrivalTime: String = "",
    val fare: Double = 0.0,
    val boardingPoint: String = "",
    val droppingPoint: String = "",
    val serviceDays: String = "প্রতিদিন", // Daily
    val active: Boolean = true,
    val createdAt: String = "",
    // Resolved relation labels
    val busName: String = "",
    val routeTitle: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        if (id.isNotBlank()) put("id", id)
        put("bus_id", busId)
        put("route_id", routeId)
        put("departure_time", departureTime)
        put("arrival_time", arrivalTime)
        put("fare", fare)
        put("boarding_point", boardingPoint)
        put("dropping_point", droppingPoint)
        put("service_days", serviceDays)
        put("is_active", active)
    }

    companion object {
        fun fromJson(json: JSONObject, busName: String = "", routeTitle: String = ""): BusRoute = BusRoute(
            id = json.optString("id", ""),
            busId = json.optString("bus_id", ""),
            routeId = json.optString("route_id", ""),
            departureTime = json.optString("departure_time", ""),
            arrivalTime = json.optString("arrival_time", ""),
            fare = json.optDouble("fare", 0.0),
            boardingPoint = json.optString("boarding_point", ""),
            droppingPoint = json.optString("dropping_point", ""),
            serviceDays = json.optString("service_days", "প্রতিদিন"),
            active = json.optBoolean("is_active", json.optBoolean("active", true)),
            createdAt = json.optString("created_at", ""),
            busName = busName,
            routeTitle = routeTitle
        )
    }
}

// 7. Counter Model
data class Counter(
    val id: String = "",
    val busId: String = "",
    val districtId: String = "",
    val counterName: String = "",
    val address: String = "",
    val phone: String = "",
    val alternatePhone: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val description: String = "",
    val active: Boolean = true,
    val createdAt: String = "",
    val busName: String = "",
    val districtName: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        if (id.isNotBlank()) put("id", id)
        if (busId.isNotBlank()) put("bus_id", busId)
        if (districtId.isNotBlank()) put("district_id", districtId)
        put("counter_name", counterName)
        put("address", address)
        put("phone", phone)
        put("alternate_phone", alternatePhone)
        put("latitude", lat)
        put("longitude", lon)
        put("description", description)
        put("is_active", active)
    }

    companion object {
        fun fromJson(json: JSONObject, busName: String = "", districtName: String = ""): Counter {
            val cName = when {
                json.has("counter_name") -> json.optString("counter_name", "")
                json.has("name") -> json.optString("name", "")
                else -> ""
            }
            val latVal = when {
                json.has("lat") -> json.optDouble("lat", 0.0)
                json.has("latitude") -> json.optDouble("latitude", 0.0)
                else -> 0.0
            }
            val lonVal = when {
                json.has("lon") -> json.optDouble("lon", 0.0)
                json.has("longitude") -> json.optDouble("longitude", 0.0)
                else -> 0.0
            }
            return Counter(
                id = json.optString("id", ""),
                busId = json.optString("bus_id", ""),
                districtId = json.optString("district_id", ""),
                counterName = cName,
                address = json.optString("address", ""),
                phone = json.optString("phone", ""),
                alternatePhone = json.optString("alternate_phone", ""),
                lat = latVal,
                lon = lonVal,
                description = json.optString("description", ""),
                active = json.optBoolean("is_active", json.optBoolean("active", true)),
                createdAt = json.optString("created_at", ""),
                busName = busName,
                districtName = districtName
            )
        }
    }
}

// 8. Fare Model
data class Fare(
    val id: String = "",
    val busId: String = "",
    val routeId: String = "",
    val fareAmount: Double = 0.0,
    val fareType: String = "Regular",
    val effectiveDate: String = "",
    val notes: String = "",
    val active: Boolean = true,
    val createdAt: String = "",
    val busName: String = "",
    val routeTitle: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        if (id.isNotBlank()) put("id", id)
        put("bus_id", busId)
        put("route_id", routeId)
        put("fare", fareAmount)
        put("fare_type", fareType)
        put("effective_from", effectiveDate)
        put("notes", notes)
        put("is_active", active)
    }

    companion object {
        fun fromJson(json: JSONObject, busName: String = "", routeTitle: String = ""): Fare = Fare(
            id = json.optString("id", ""),
            busId = json.optString("bus_id", ""),
            routeId = json.optString("route_id", ""),
            fareAmount = json.optDouble("fare", json.optDouble("fare_amount", json.optDouble("amount", 0.0))),
            fareType = json.optString("fare_type", "Regular"),
            effectiveDate = json.optString("effective_from", json.optString("effective_date", "")),
            notes = json.optString("notes", ""),
            active = json.optBoolean("is_active", json.optBoolean("active", true)),
            createdAt = json.optString("created_at", ""),
            busName = busName,
            routeTitle = routeTitle
        )
    }
}

// 9. Mini Coach Model
data class MiniCoach(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val vehicleType: String = "Toyota Coaster",
    val capacity: Int = 29,
    val isAc: Boolean = true,
    val image: String = "",
    val description: String = "",
    val perDayRate: Double = 0.0,
    val perKmRate: Double = 0.0,
    val driverCharge: Double = 0.0,
    val extraDayRate: Double = 0.0,
    val phone: String = "",
    val active: Boolean = true,
    val createdAt: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        if (id.isNotBlank()) put("id", id)
        put("name", name)
        put("slug", slug)
        put("vehicle_type", vehicleType)
        put("capacity", capacity)
        put("is_ac", isAc)
        put("image_url", image)
        put("description", description)
        put("per_day_rate", perDayRate)
        put("per_km_rate", perKmRate)
        put("driver_charge", driverCharge)
        put("extra_day_rate", extraDayRate)
        put("phone", phone)
        put("is_active", active)
    }

    companion object {
        fun fromJson(json: JSONObject): MiniCoach = MiniCoach(
            id = json.optString("id", ""),
            name = json.optString("name", ""),
            slug = json.optString("slug", ""),
            vehicleType = json.optString("vehicle_type", "Toyota Coaster"),
            capacity = json.optInt("capacity", 29),
            isAc = json.optBoolean("is_ac", true),
            image = json.optString("image_url", json.optString("image", "")),
            description = json.optString("description", ""),
            perDayRate = json.optDouble("per_day_rate", 0.0),
            perKmRate = json.optDouble("per_km_rate", 0.0),
            driverCharge = json.optDouble("driver_charge", 0.0),
            extraDayRate = json.optDouble("extra_day_rate", 0.0),
            phone = json.optString("phone", ""),
            active = json.optBoolean("is_active", json.optBoolean("active", true)),
            createdAt = json.optString("created_at", "")
        )
    }
}

// 10. Tour Package Model
data class TourPackage(
    val id: String = "",
    val title: String = "",
    val slug: String = "",
    val destination: String = "",
    val durationDays: Int = 3,
    val durationNights: Int = 2,
    val pricePerPerson: Double = 0.0,
    val minimumPeople: Int = 4,
    val image: String = "",
    val shortDescription: String = "",
    val fullDescription: String = "",
    val itinerary: String = "",
    val included: String = "",
    val excluded: String = "",
    val terms: String = "",
    val phone: String = "",
    val active: Boolean = true,
    val createdAt: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        if (id.isNotBlank()) put("id", id)
        put("title", title)
        put("slug", slug)
        put("destination", destination)
        put("duration_days", durationDays)
        put("duration_nights", durationNights)
        put("price_per_person", pricePerPerson)
        put("minimum_people", minimumPeople)
        put("image_url", image)
        put("short_description", shortDescription)
        put("description", fullDescription)
        put("itinerary", itinerary)
        put("included", included)
        put("excluded", excluded)
        put("terms", terms)
        put("phone", phone)
        put("is_active", active)
    }

    companion object {
        fun fromJson(json: JSONObject): TourPackage = TourPackage(
            id = json.optString("id", ""),
            title = json.optString("title", ""),
            slug = json.optString("slug", ""),
            destination = json.optString("destination", ""),
            durationDays = json.optInt("duration_days", 3),
            durationNights = json.optInt("duration_nights", 2),
            pricePerPerson = json.optDouble("price_per_person", 0.0),
            minimumPeople = json.optInt("minimum_people", 4),
            image = json.optString("image_url", json.optString("image", "")),
            shortDescription = json.optString("short_description", ""),
            fullDescription = json.optString("description", json.optString("full_description", "")),
            itinerary = json.optString("itinerary", ""),
            included = json.optString("included", ""),
            excluded = json.optString("excluded", ""),
            terms = json.optString("terms", ""),
            phone = json.optString("phone", ""),
            active = json.optBoolean("is_active", json.optBoolean("active", true)),
            createdAt = json.optString("created_at", "")
        )
    }
}

// 11. Booking Model
data class Booking(
    val id: String = "",
    val bookingId: String = "",
    val customerName: String = "",
    val phone: String = "",
    val email: String = "",
    val bookingType: String = "bus", // bus, mini_coach, tour_package
    val pickup: String = "",
    val destination: String = "",
    val travelDate: String = "",
    val returnDate: String = "",
    val passengers: Int = 1,
    val vehicleType: String = "",
    val tripDays: Int = 1,
    val estimatedPrice: Double = 0.0,
    val specialRequest: String = "",
    val status: String = "pending", // pending, confirmed, cancelled, completed
    val createdAt: String = "",
    val busId: String = "",
    val miniCoachId: String = "",
    val tourPackageId: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        if (id.isNotBlank()) put("id", id)
        put("customer_name", customerName)
        put("phone", phone)
        put("email", email)
        put("booking_type", bookingType)
        put("pickup_location", pickup)
        put("destination", destination)
        put("travel_date", travelDate)
        put("return_date", returnDate)
        put("passengers", passengers)
        put("vehicle_type", vehicleType)
        put("trip_days", tripDays)
        put("estimated_price", estimatedPrice)
        put("special_request", specialRequest)
        put("status", status)
        if (busId.isNotBlank()) put("bus_id", busId)
        if (miniCoachId.isNotBlank()) put("mini_coach_id", miniCoachId)
        if (tourPackageId.isNotBlank()) put("tour_package_id", tourPackageId)
    }

    companion object {
        fun fromJson(json: JSONObject): Booking = Booking(
            id = json.optString("id", ""),
            bookingId = "BK-${json.optString("id").take(6).uppercase()}",
            customerName = json.optString("customer_name", "যাত্রী"),
            phone = json.optString("phone", ""),
            email = json.optString("email", ""),
            bookingType = json.optString("booking_type", "bus"),
            pickup = json.optString("pickup_location", json.optString("pickup", "")),
            destination = json.optString("destination", ""),
            travelDate = json.optString("travel_date", ""),
            returnDate = json.optString("return_date", ""),
            passengers = json.optInt("passengers", 1),
            vehicleType = json.optString("vehicle_type", ""),
            tripDays = json.optInt("trip_days", 1),
            estimatedPrice = json.optDouble("estimated_price", 0.0),
            specialRequest = json.optString("special_request", ""),
            status = json.optString("status", "pending"),
            createdAt = json.optString("created_at", ""),
            busId = json.optString("bus_id", ""),
            miniCoachId = json.optString("mini_coach_id", ""),
            tourPackageId = json.optString("tour_package_id", "")
        )
    }
}

// 12. Dashboard Stats
data class DashboardStats(
    val totalBuses: Int = 0,
    val totalOperators: Int = 0,
    val totalRoutes: Int = 0,
    val totalCounters: Int = 0,
    val totalMiniCoaches: Int = 0,
    val totalTourPackages: Int = 0,
    val pendingBookings: Int = 0,
    val confirmedBookings: Int = 0,
    val recentBookings: List<Booking> = emptyList()
)
