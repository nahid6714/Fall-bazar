package com.example.data.network

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object MockDataStore {

    private val tables = mutableMapOf<String, MutableList<JSONObject>>()

    init {
        seedInitialData()
    }

    @Synchronized
    fun getTable(tableName: String): JSONArray {
        val list = tables[tableName] ?: mutableListOf()
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array
    }

    @Synchronized
    fun insert(tableName: String, json: JSONObject): JSONObject {
        val list = tables.getOrPut(tableName) { mutableListOf() }
        if (!json.has("id") || json.optString("id").isBlank()) {
            json.put("id", UUID.randomUUID().toString())
        }
        if (!json.has("created_at")) {
            json.put("created_at", "2026-09-13T10:00:00Z")
        }
        list.add(0, json)
        return json
    }

    @Synchronized
    fun update(tableName: String, id: String, json: JSONObject): JSONObject {
        val list = tables.getOrPut(tableName) { mutableListOf() }
        val index = list.indexOfFirst { it.optString("id") == id }
        if (index != -1) {
            val existing = list[index]
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                existing.put(key, json.get(key))
            }
            return existing
        } else {
            json.put("id", id)
            list.add(json)
            return json
        }
    }

    @Synchronized
    fun delete(tableName: String, id: String) {
        val list = tables[tableName] ?: return
        list.removeAll { it.optString("id") == id }
    }

    private fun seedInitialData() {
        // 1. Districts
        val districtsList = mutableListOf(
            JSONObject().apply {
                put("id", "dist-01")
                put("name", "ঢাকা")
                put("division", "ঢাকা")
                put("slug", "dhaka")
                put("active", true)
            },
            JSONObject().apply {
                put("id", "dist-02")
                put("name", "চট্টগ্রাম")
                put("division", "চট্টগ্রাম")
                put("slug", "chattogram")
                put("active", true)
            },
            JSONObject().apply {
                put("id", "dist-03")
                put("name", "কক্সবাজার")
                put("division", "চট্টগ্রাম")
                put("slug", "coxs-bazar")
                put("active", true)
            },
            JSONObject().apply {
                put("id", "dist-04")
                put("name", "সিলেট")
                put("division", "সিলেট")
                put("slug", "sylhet")
                put("active", true)
            },
            JSONObject().apply {
                put("id", "dist-05")
                put("name", "রাজশাহী")
                put("division", "রাজশাহী")
                put("slug", "rajshahi")
                put("active", true)
            },
            JSONObject().apply {
                put("id", "dist-06")
                put("name", "খুলনা")
                put("division", "খুলনা")
                put("slug", "khulna")
                put("active", true)
            }
        )
        tables["districts"] = districtsList

        // 2. Bus Operators
        val operatorsList = mutableListOf(
            JSONObject().apply {
                put("id", "op-01")
                put("name", "গ্রীন লাইন পরিবহন")
                put("slug", "green-line-paribahan")
                put("logo", "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=400")
                put("description", "বাংলাদেশের অন্যতম প্রিমিয়াম লাক্সারি এসি বাস সার্ভিস।")
                put("phone", "01711123456")
                put("website", "https://greenlinebd.com")
                put("facebook_url", "https://facebook.com/greenline")
                put("active", true)
            },
            JSONObject().apply {
                put("id", "op-02")
                put("name", "হানিফ এন্টারপ্রাইজ")
                put("slug", "hanif-enterprise")
                put("logo", "https://images.unsplash.com/photo-1570125909232-eb263c188f7e?w=400")
                put("description", "সারা দেশে বিস্তৃত নেটওয়ার্ক সম্বলিত ঐতিহ্যবাহী পরিবহন।")
                put("phone", "01713000000")
                put("website", "https://hanif.com.bd")
                put("facebook_url", "https://facebook.com/hanif")
                put("active", true)
            },
            JSONObject().apply {
                put("id", "op-03")
                put("name", "শ্যামলী এন.আর ট্রাভেলস")
                put("slug", "shyamoli-nr-travels")
                put("logo", "https://images.unsplash.com/photo-1494515843206-f3117d3f51b7?w=400")
                put("description", "বিশ্বস্ত ও আরামদায়ক আন্তঃজেলা ও আন্তর্জাতিক বাস সার্ভিস।")
                put("phone", "01711223344")
                put("website", "https://shyamolitravels.com")
                put("facebook_url", "https://facebook.com/shyamoli")
                put("active", true)
            },
            JSONObject().apply {
                put("id", "op-04")
                put("name", "সোহাগ পরিবহন")
                put("slug", "shohagh-paribahan")
                put("logo", "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=400")
                put("description", "প্রথম শ্রেণির স্ক্যানিয়া মাল্টি-অ্যাক্সেল এসি বাস।")
                put("phone", "01711556677")
                put("website", "https://shohagh.com")
                put("facebook_url", "https://facebook.com/shohagh")
                put("active", true)
            }
        )
        tables["bus_operators"] = operatorsList

        // 3. Buses
        val busesList = mutableListOf(
            JSONObject().apply {
                put("id", "bus-01")
                put("operator_id", "op-01")
                put("name", "গ্রীন লাইন স্ক্যানিয়া মাল্টি-অ্যাক্সেল")
                put("slug", "green-line-scania-multi-axle")
                put("category", "First Class")
                put("bus_type", "AC Multi-Axle Sleeper/Seater")
                put("description", "অত্যাধুনিক আরামদায়ক আসন, জিপিএস ট্র্যাকিং ও ফ্রি ওয়াইফাই সুবিধা।")
                put("image", "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=600")
                put("phone", "01711123456")
                put("is_ac", true)
                put("seat_count", 32)
                put("active", true)
            },
            JSONObject().apply {
                put("id", "bus-02")
                put("operator_id", "op-02")
                put("name", "হানিফ হুন্দাই বিজনেস ক্লাস")
                put("slug", "hanif-hyundai-business-class")
                put("category", "Business Class")
                put("bus_type", "AC Coach")
                put("description", "ঢাকা-চট্টগ্রাম ও ঢাকা-কক্সবাজার রুটে এক্সপ্রেস সার্ভিস।")
                put("image", "https://images.unsplash.com/photo-1570125909232-eb263c188f7e?w=600")
                put("phone", "01713000000")
                put("is_ac", true)
                put("seat_count", 28)
                put("active", true)
            },
            JSONObject().apply {
                put("id", "bus-03")
                put("operator_id", "op-03")
                put("name", "শ্যামলী ভলভো এক্সিকিউটিভ")
                put("slug", "shyamoli-volvo-executive")
                put("category", "Executive Class")
                put("bus_type", "AC Coach")
                put("description", "ঢাকা-সিলেট রুটে নিরাপদ ও দ্রুত ভ্রমণ।")
                put("image", "https://images.unsplash.com/photo-1494515843206-f3117d3f51b7?w=600")
                put("phone", "01711223344")
                put("is_ac", true)
                put("seat_count", 36)
                put("active", true)
            },
            JSONObject().apply {
                put("id", "bus-04")
                put("operator_id", "op-02")
                put("name", "হানিফ ডিলাক্স নন-এসি")
                put("slug", "hanif-deluxe-non-ac")
                put("category", "Economy")
                put("bus_type", "Non-AC")
                put("description", "সাশ্রয়ী মূল্যে নিরাপদ ভ্রমণ।")
                put("image", "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=600")
                put("phone", "01713000000")
                put("is_ac", false)
                put("seat_count", 40)
                put("active", true)
            }
        )
        tables["buses"] = busesList

        // 4. Routes
        val routesList = mutableListOf(
            JSONObject().apply {
                put("id", "route-01")
                put("from_district_id", "dist-01")
                put("to_district_id", "dist-03")
                put("distance_km", 395.0)
                put("estimated_duration", "৮-৯ ঘণ্টা")
                put("description", "ঢাকা - চট্টগ্রাম - কক্সবাজার মহাসড়ক")
                put("active", true)
            },
            JSONObject().apply {
                put("id", "route-02")
                put("from_district_id", "dist-01")
                put("to_district_id", "dist-02")
                put("distance_km", 248.0)
                put("estimated_duration", "৫-৬ ঘণ্টা")
                put("description", "ঢাকা - চট্টগ্রাম এক্সপ্রেসওয়ে")
                put("active", true)
            },
            JSONObject().apply {
                put("id", "route-03")
                put("from_district_id", "dist-01")
                put("to_district_id", "dist-04")
                put("distance_km", 240.0)
                put("estimated_duration", "৫-৬ ঘণ্টা")
                put("description", "ঢাকা - সিলেট হাইওয়ে")
                put("active", true)
            },
            JSONObject().apply {
                put("id", "route-04")
                put("from_district_id", "dist-01")
                put("to_district_id", "dist-05")
                put("distance_km", 256.0)
                put("estimated_duration", "৫ ঘণ্টা")
                put("description", "যমুনা সেতু লিংক রোড")
                put("active", true)
            }
        )
        tables["routes"] = routesList

        // 5. Bus Routes (Schedules)
        val busRoutesList = mutableListOf(
            JSONObject().apply {
                put("id", "br-01")
                put("bus_id", "bus-01")
                put("route_id", "route-01")
                put("departure_time", "10:30 PM")
                put("arrival_time", "07:00 AM")
                put("fare", 2200.0)
                put("boarding_point", "রাজারবাগ / ফকিরাপুল")
                put("dropping_point", "কলাতলী মোড়, কক্সবাজার")
                put("service_days", "প্রতিদিন")
                put("active", true)
            },
            JSONObject().apply {
                put("id", "br-02")
                put("bus_id", "bus-02")
                put("route_id", "route-02")
                put("departure_time", "07:30 AM")
                put("arrival_time", "01:30 PM")
                put("fare", 1400.0)
                put("boarding_point", "সায়েদাবাদ জনপথ")
                put("dropping_point", "দামপাড়া, চট্টগ্রাম")
                put("service_days", "প্রতিদিন")
                put("active", true)
            },
            JSONObject().apply {
                put("id", "br-03")
                put("bus_id", "bus-03")
                put("route_id", "route-03")
                put("departure_time", "11:00 PM")
                put("arrival_time", "05:00 AM")
                put("fare", 1100.0)
                put("boarding_point", "মহাখালী বাস টার্মিনাল")
                put("dropping_point", "কদমতলী, সিলেট")
                put("service_days", "প্রতিদিন")
                put("active", true)
            }
        )
        tables["bus_routes"] = busRoutesList

        // 6. Counters
        val countersList = mutableListOf(
            JSONObject().apply {
                put("id", "counter-01")
                put("bus_id", "bus-01")
                put("district_id", "dist-01")
                put("counter_name", "রাজারবাগ মেইন কাউন্টার")
                put("address", "মতিঝিল রাজারবাগ পুলিশ লাইনের বিপরীত")
                put("phone", "01711123456")
                put("alternate_phone", "01711987654")
                put("lat", 23.7380)
                put("lon", 90.4180)
                put("description", "গ্রীন লাইনের মূল বুকিং অফিস ও লাউঞ্জ।")
                put("active", true)
            },
            JSONObject().apply {
                put("id", "counter-02")
                put("bus_id", "bus-02")
                put("district_id", "dist-01")
                put("counter_name", "সায়েদাবাদ বাস কাউন্টার")
                put("address", "সায়েদাবাদ বাস টার্মিনাল, ঢাকা")
                put("phone", "01713000000")
                put("alternate_phone", "01713111111")
                put("lat", 23.7120)
                put("lon", 90.4280)
                put("description", "হানিফ এন্টারপ্রাইজের কেন্দ্রীয় টার্মিনাল কাউন্টার।")
                put("active", true)
            },
            JSONObject().apply {
                put("id", "counter-03")
                put("bus_id", "bus-02")
                put("district_id", "dist-02")
                put("counter_name", "দামপাড়া কাউন্টার, চট্টগ্রাম")
                put("address", "দামপাড়া বাস স্ট্যান্ড, চট্টগ্রাম")
                put("phone", "01713222222")
                put("alternate_phone", "")
                put("lat", 22.3569)
                put("lon", 91.8215)
                put("description", "যাত্রী লাউঞ্জ ও টিকিট কাউন্টার।")
                put("active", true)
            }
        )
        tables["counters"] = countersList

        // 7. Fares
        val faresList = mutableListOf(
            JSONObject().apply {
                put("id", "fare-01")
                put("bus_id", "bus-01")
                put("route_id", "route-01")
                put("fare_amount", 2200.0)
                put("fare_type", "VIP Sleeper")
                put("effective_date", "2026-01-01")
                put("notes", "কক্সবাজার বিশেষ এসি প্যাকেজ")
                put("active", true)
            },
            JSONObject().apply {
                put("id", "fare-02")
                put("bus_id", "bus-02")
                put("route_id", "route-02")
                put("fare_amount", 1400.0)
                put("fare_type", "Business Class")
                put("effective_date", "2026-01-01")
                put("notes", "ঢাকা-চট্টগ্রাম এক্সপ্রেস")
                put("active", true)
            }
        )
        tables["fares"] = faresList

        // 8. Mini Coaches
        val miniCoachesList = mutableListOf(
            JSONObject().apply {
                put("id", "mc-01")
                put("name", "টয়োটা কোস্টার ডিলাক্স")
                put("slug", "toyota-coaster-deluxe")
                put("vehicle_type", "Toyota Coaster")
                put("capacity", 29)
                put("is_ac", true)
                put("image", "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=600")
                put("description", "ফ্যামিলি ট্যুর, কর্পোরেট ইভেন্ট ও পিকনিকের জন্য সেরা পছন্দ।")
                put("per_day_rate", 14000.0)
                put("per_km_rate", 55.0)
                put("driver_charge", 1000.0)
                put("extra_day_rate", 12000.0)
                put("phone", "01711009988")
                put("active", true)
            },
            JSONObject().apply {
                put("id", "mc-02")
                put("name", "নিশান সিভিলিয়ান এক্সিকিউটিভ")
                put("slug", "nissan-civilian-executive")
                put("vehicle_type", "Nissan Civilian")
                put("capacity", 26)
                put("is_ac", true)
                put("image", "https://images.unsplash.com/photo-1570125909232-eb263c188f7e?w=600")
                put("description", "উন্নত এয়ার কন্ডিশনিং ও আরামদায়ক পুশব্যাক সিট।")
                put("per_day_rate", 13000.0)
                put("per_km_rate", 50.0)
                put("driver_charge", 1000.0)
                put("extra_day_rate", 11000.0)
                put("phone", "01711009988")
                put("active", true)
            }
        )
        tables["mini_coaches"] = miniCoachesList

        // 9. Tour Packages
        val tourPackagesList = mutableListOf(
            JSONObject().apply {
                put("id", "tour-01")
                put("title", "সাজেক ভ্যালি প্রিমিয়াম মেঘের রাজ্য ট্যুর")
                put("slug", "sajek-valley-premium-tour")
                put("destination", "সাজেক, খাগড়াছড়ি")
                put("duration_days", 3)
                put("duration_nights", 2)
                put("price_per_person", 7500.0)
                put("minimum_people", 4)
                put("image", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600")
                put("short_description", "মেঘের উপরে থাকা, রিসোর্ট ও চান্দের গাড়ি সহ সম্পূর্ণ প্যাকেজ।")
                put("full_description", "ঢাকা থেকে এসি বাসে যাত্রা, খাগড়াছড়ি পৌঁছানো, সাজেক ভ্যালির সেরা রিসোর্টে রাত্রিযাপন ও দর্শনীয় স্থান ভ্রমণ।")
                put("itinerary", "দিন ১: ঢাকা থেকে যাত্রা। দিন ২: সাজেক ও হেলিপ্যাড সূর্যাস্ত। দিন ৩: আলুটিলা গুহা ও ফিরতি যাত্রা।")
                put("included", "ঢাকা-খাগড়াছড়ি এসি বাস টিকিট, সাজেক রিসোর্ট, চান্দের গাড়ি, সকল বেলার খাবার")
                put("excluded", "ব্যক্তিগত খরচ, রাইড ফি")
                put("terms", "বুকিংয়ের সময় ৫০% অগ্রিম প্রদেয়।")
                put("phone", "01711887766")
                put("active", true)
            },
            JSONObject().apply {
                put("id", "tour-02")
                put("title", "কক্সবাজার সমুদ্র সৈকত রিল্যাক্স ট্যুর")
                put("slug", "coxs-bazar-relax-tour")
                put("destination", "কক্সবাজার ও ইনানী")
                put("duration_days", 4)
                put("duration_nights", 3)
                put("price_per_person", 8900.0)
                put("minimum_people", 2)
                put("image", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600")
                put("short_description", "৩ তারা হোটেল, মেরিন ড্রাইভ ভ্রমণ ও প্রিমিয়াম বাস জার্নি।")
                put("full_description", "বিশ্বের দীর্ঘতম সমুদ্র সৈকতের মনমাতানো ঢেউ ও ইনানীর পাথুরে সৈকত উপভোগ।")
                put("itinerary", "দিন ১: রওনা। দিন ২: সমুদ্র দর্শন। দিন ৩: মেরিন ড্রাইভ ও হিমছড়ি। দিন ৪: সকালের সৈকত ও ফেরা।")
                put("included", "গ্রীন লাইন এসি বাস, ৩ তারকা হোটেল রুম, সকালের নাস্তা")
                put("excluded", "দুপুর ও রাতের খাবার")
                put("terms", "যাত্রা বাতিলের ক্ষেত্রে ২৪ ঘণ্টা পূর্বে জানাতে হবে।")
                put("phone", "01711887766")
                put("active", true)
            }
        )
        tables["tour_packages"] = tourPackagesList

        // 10. Bookings
        val bookingsList = mutableListOf(
            JSONObject().apply {
                put("id", "bk-01")
                put("booking_id", "BT-98214")
                put("customer_name", "তানভীর আহমেদ")
                put("phone", "01712345678")
                put("email", "tanvir@example.com")
                put("booking_type", "bus")
                put("pickup", "রাজারবাগ, ঢাকা")
                put("destination", "কক্সবাজার")
                put("travel_date", "2026-09-20")
                put("return_date", "2026-09-24")
                put("passengers", 2)
                put("vehicle_type", "AC Scania Multi-Axle")
                put("trip_days", 5)
                put("estimated_price", 4400.0)
                put("special_request", "জানালা পাশের সিট পছন্দ")
                put("status", "pending")
                put("created_at", "2026-09-13T04:15:00Z")
                put("bus_id", "bus-01")
            },
            JSONObject().apply {
                put("id", "bk-02")
                put("booking_id", "BT-98215")
                put("customer_name", "মাহমুদুল হাসান")
                put("phone", "01819876543")
                put("email", "mahmud@example.com")
                put("booking_type", "mini_coach")
                put("pickup", "উত্তরা, ঢাকা")
                put("destination", "শ্রীমঙ্গল, মৌলভীবাজার")
                put("travel_date", "2026-09-25")
                put("return_date", "2026-09-27")
                put("passengers", 24)
                put("vehicle_type", "Toyota Coaster (29 Seat)")
                put("trip_days", 3)
                put("estimated_price", 42000.0)
                put("special_request", "অফিস পিকনিক ট্যুর, অভিজ্ঞ চালক প্রয়োজন")
                put("status", "confirmed")
                put("created_at", "2026-09-12T16:40:00Z")
                put("mini_coach_id", "mc-01")
            },
            JSONObject().apply {
                put("id", "bk-03")
                put("booking_id", "BT-98216")
                put("customer_name", "সালমা চৌধুরী")
                put("phone", "01911223344")
                put("email", "salma@example.com")
                put("booking_type", "tour_package")
                put("pickup", "ঢাকা")
                put("destination", "সাজেক ভ্যালি")
                put("travel_date", "2026-10-02")
                put("return_date", "2026-10-05")
                put("passengers", 4)
                put("vehicle_type", "Tour Package")
                put("trip_days", 4)
                put("estimated_price", 30000.0)
                put("special_request", "পাহাড়মুখী কটেজ ব্যালকনি সিট")
                put("status", "pending")
                put("created_at", "2026-09-11T09:20:00Z")
                put("tour_package_id", "tour-01")
            },
            JSONObject().apply {
                put("id", "bk-04")
                put("booking_id", "BT-98217")
                put("customer_name", "রফিকুল ইসলাম")
                put("phone", "01615554433")
                put("email", "rafiq@example.com")
                put("booking_type", "bus")
                put("pickup", "সায়েদাবাদ, ঢাকা")
                put("destination", "চট্টগ্রাম")
                put("travel_date", "2026-09-15")
                put("return_date", "")
                put("passengers", 1)
                put("vehicle_type", "Hyundai Business Class")
                put("trip_days", 1)
                put("estimated_price", 1400.0)
                put("special_request", "সকালের ট্রিপ")
                put("status", "completed")
                put("created_at", "2026-09-10T12:00:00Z")
                put("bus_id", "bus-02")
            }
        )
        tables["bookings"] = bookingsList
    }
}
