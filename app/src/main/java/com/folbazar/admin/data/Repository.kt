package com.folbazar.admin.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

class Repository(private val api: SupabaseClient = SupabaseClient()) {

    private fun parseProduct(o: JsonObject) = Product(
        id = o["id"]?.jsonPrimitive?.content ?: "",
        title = (o["title"] ?: o["name"])?.jsonPrimitive?.contentOrNull ?: "",
        price = o["price"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
        oldPrice = (o["old_price"] ?: o["oldPrice"])?.jsonPrimitive?.doubleOrNull,
        stock = o["stock"]?.jsonPrimitive?.intOrNull ?: 0,
        imageUrl = (o["image_url"] ?: o["image"])?.jsonPrimitive?.contentOrNull,
        category = o["category"]?.jsonPrimitive?.contentOrNull,
        active = (o["is_active"] ?: o["active"])?.jsonPrimitive?.booleanOrNull ?: true
    )

    private fun parseOrder(o: JsonObject) = Order(
        id = (o["id"] ?: o["order_id"])?.jsonPrimitive?.content ?: "",
        customer = (o["customer_name"] ?: o["name"])?.jsonPrimitive?.contentOrNull ?: "Customer",
        amount = (o["total"] ?: o["amount"])?.jsonPrimitive?.doubleOrNull ?: 0.0,
        status = o["status"]?.jsonPrimitive?.contentOrNull ?: "pending",
        createdAt = o["created_at"]?.jsonPrimitive?.contentOrNull
    )

    private fun parseCustomer(o: JsonObject) = Customer(
        id = o["id"]?.jsonPrimitive?.content ?: "",
        name = (o["full_name"] ?: o["name"])?.jsonPrimitive?.contentOrNull
            ?: o["email"]?.jsonPrimitive?.contentOrNull
            ?: "Customer",
        phone = o["phone"]?.jsonPrimitive?.contentOrNull,
        address = o["address"]?.jsonPrimitive?.contentOrNull,
        createdAt = o["created_at"]?.jsonPrimitive?.contentOrNull
    )

    // ---------- Reads ----------

    suspend fun products(): Result<List<Product>> = runCatching {
        withContext(Dispatchers.IO) {
            Json.parseToJsonElement(api.get("products", "?select=*&order=created_at.desc"))
                .jsonArray.map { parseProduct(it.jsonObject) }
        }
    }

    suspend fun orders(): Result<List<Order>> = runCatching {
        withContext(Dispatchers.IO) {
            Json.parseToJsonElement(api.get("orders", "?select=*&order=created_at.desc"))
                .jsonArray.map { parseOrder(it.jsonObject) }
        }
    }

    suspend fun customers(): Result<List<Customer>> = runCatching {
        withContext(Dispatchers.IO) {
            // The project's Supabase schema stores customer accounts in
            // public.profiles (linked to auth.users), not public.customers.
            Json.parseToJsonElement(api.get("profiles", "?select=id,full_name,phone,email,created_at&order=created_at.desc"))
                .jsonArray.map { parseCustomer(it.jsonObject) }
        }
    }

    // ---------- Product CRUD ----------

    suspend fun addProduct(
        title: String, price: Double, stock: Int, category: String?, imageUrl: String?
    ): Result<Product> = runCatching {
        withContext(Dispatchers.IO) {
            val body = buildJsonObject {
                put("title", title)
                put("price", price)
                put("stock", stock)
                put("category", category)
                put("image_url", imageUrl)
                put("is_active", true)
            }
            val arr = Json.parseToJsonElement(api.post("products", body.toString())).jsonArray
            parseProduct(arr.first().jsonObject)
        }
    }

    suspend fun updateProduct(p: Product): Result<Product> = runCatching {
        withContext(Dispatchers.IO) {
            val body = buildJsonObject {
                put("title", p.title)
                put("price", p.price)
                put("stock", p.stock)
                put("category", p.category)
                put("image_url", p.imageUrl)
                put("is_active", p.active)
            }
            val arr = Json.parseToJsonElement(api.patch("products", "id=eq.${p.id}", body.toString())).jsonArray
            parseProduct(arr.first().jsonObject)
        }
    }

    suspend fun deleteProduct(id: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) { api.delete("products", "id=eq.$id"); Unit }
    }

    // ---------- Orders ----------

    suspend fun updateOrderStatus(id: String, status: String): Result<Order> = runCatching {
        withContext(Dispatchers.IO) {
            val body = buildJsonObject { put("status", status) }
            val arr = Json.parseToJsonElement(api.patch("orders", "id=eq.$id", body.toString())).jsonArray
            parseOrder(arr.first().jsonObject)
        }
    }

    // ---------- Auth ----------

    data class AdminSession(
        val accessToken: String,
        val userId: String,
        val email: String
    )

    /** Authenticates with Supabase, then verifies public.profiles.role == admin. */
    suspend fun signInAdmin(email: String, password: String): Result<AdminSession> = runCatching {
        withContext(Dispatchers.IO) {
            val res = Json.parseToJsonElement(api.signIn(email, password)).jsonObject
            val token = res["access_token"]?.jsonPrimitive?.contentOrNull
                ?: throw IllegalStateException(
                    res["error_description"]?.jsonPrimitive?.contentOrNull
                        ?: res["msg"]?.jsonPrimitive?.contentOrNull
                        ?: "লগইন ব্যর্থ হয়েছে, ইমেইল/পাসওয়ার্ড যাচাই করুন"
                )

            val user = res["user"]?.jsonObject
                ?: throw IllegalStateException("Supabase user তথ্য পাওয়া যায়নি")
            val userId = user["id"]?.jsonPrimitive?.contentOrNull
                ?: throw IllegalStateException("Supabase user ID পাওয়া যায়নি")

            val profileJson = api.getWithBearer(
                "profiles",
                "?select=role&id=eq.$userId",
                token
            )
            val profiles = Json.parseToJsonElement(profileJson).jsonArray
            val role = profiles.firstOrNull()
                ?.jsonObject?.get("role")?.jsonPrimitive?.contentOrNull
                ?: "customer"

            if (role != "admin") {
                throw IllegalStateException(
                    "এই অ্যাকাউন্টটি Admin নয়। Supabase-এর profiles টেবিলে এই ইউজারের role = admin করতে হবে।"
                )
            }

            AdminSession(
                accessToken = token,
                userId = userId,
                email = user["email"]?.jsonPrimitive?.contentOrNull ?: email
            )
        }
    }
}
