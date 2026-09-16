package com.folbazar.admin.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

class Repository(private val api: SupabaseClient = SupabaseClient()) {
    private fun s(o: JsonObject, vararg keys: String): String? = keys.asSequence()
        .mapNotNull { o[it]?.jsonPrimitive?.contentOrNull }
        .firstOrNull()
    private fun d(o: JsonObject, vararg keys: String): Double? = keys.asSequence()
        .mapNotNull { o[it]?.jsonPrimitive?.doubleOrNull }
        .firstOrNull()
    private fun i(o: JsonObject, vararg keys: String): Int? = keys.asSequence()
        .mapNotNull { o[it]?.jsonPrimitive?.intOrNull }
        .firstOrNull()
    private fun b(o: JsonObject, vararg keys: String): Boolean? = keys.asSequence()
        .mapNotNull { o[it]?.jsonPrimitive?.booleanOrNull }
        .firstOrNull()

    private fun parseCategory(o: JsonObject) = Category(
        id = s(o, "id") ?: "", name = s(o, "name") ?: "", slug = s(o, "slug") ?: "",
        imageUrl = s(o, "image_url"), description = s(o, "description"),
        active = b(o, "is_active") ?: true, sortOrder = i(o, "sort_order") ?: 0
    )

    private fun parseProduct(o: JsonObject) = Product(
        id = s(o, "id") ?: "", name = s(o, "name", "title") ?: "", slug = s(o, "slug") ?: "",
        categoryId = s(o, "category_id"), categoryName = s(o, "category_name"),
        description = s(o, "description"), price = d(o, "price") ?: 0.0,
        oldPrice = d(o, "old_price"), stock = i(o, "stock_quantity", "stock") ?: 0,
        soldQuantity = i(o, "sold_quantity") ?: 0, discountPercent = d(o, "discount_percent"),
        imageUrl = s(o, "image_url", "image"), active = b(o, "is_active", "active") ?: true,
        featured = b(o, "is_featured") ?: false, flashSale = b(o, "is_flash_sale") ?: false,
        hotDeal = b(o, "is_hot_deal") ?: false, sortOrder = i(o, "sort_order") ?: 0
    )

    private fun parseVariant(o: JsonObject) = ProductVariant(
        id = s(o, "id") ?: "", productId = s(o, "product_id") ?: "", label = s(o, "label") ?: "",
        weightGrams = i(o, "weight_grams") ?: 0, price = d(o, "price") ?: 0.0,
        oldPrice = d(o, "old_price"), stock = i(o, "stock_quantity") ?: 0,
        active = b(o, "is_active") ?: true, sortOrder = i(o, "sort_order") ?: 0
    )

    private fun parseOrder(o: JsonObject) = Order(
        id = s(o, "id") ?: "", orderNumber = s(o, "order_number") ?: s(o, "id")?.take(8).orEmpty(),
        userId = s(o, "user_id"), customer = s(o, "customer_name") ?: "Customer",
        phone = s(o, "customer_phone") ?: "", email = s(o, "customer_email"),
        address = listOfNotNull(s(o, "address"), s(o, "upazila"), s(o, "district"), s(o, "division"))
            .filter { it.isNotBlank() }.joinToString(", "),
        subtotal = d(o, "subtotal") ?: 0.0, deliveryCharge = d(o, "delivery_charge") ?: 0.0,
        discount = d(o, "discount_amount") ?: 0.0, total = d(o, "total_amount", "total") ?: 0.0,
        paymentMethod = s(o, "payment_method") ?: "cod", paymentStatus = s(o, "payment_status") ?: "pending",
        status = s(o, "status") ?: "pending", createdAt = s(o, "created_at")
    )

    private fun parseCustomer(o: JsonObject) = Customer(
        id = s(o, "id") ?: "", name = s(o, "full_name", "name") ?: s(o, "email") ?: "Customer",
        email = s(o, "email"), phone = s(o, "phone"), role = s(o, "role") ?: "customer", createdAt = s(o, "created_at")
    )

    private fun parseComplaint(o: JsonObject) = Complaint(
        id = s(o, "id") ?: "", number = s(o, "complaint_number") ?: s(o, "id")?.take(8).orEmpty(),
        customerName = s(o, "customer_name") ?: "Customer", phone = s(o, "customer_phone") ?: "",
        subject = s(o, "subject"), description = s(o, "description") ?: "",
        status = s(o, "status") ?: "open", adminNote = s(o, "admin_note"), createdAt = s(o, "created_at")
    )

    private fun parseCoupon(o: JsonObject) = Coupon(
        id = s(o, "id") ?: "", code = s(o, "code") ?: "", title = s(o, "title"),
        discountType = s(o, "discount_type") ?: "percent", discountValue = d(o, "discount_value") ?: 0.0,
        minOrder = d(o, "min_order") ?: 0.0, maxDiscount = d(o, "max_discount"),
        usageLimit = i(o, "usage_limit"), usedCount = i(o, "used_count") ?: 0,
        active = b(o, "is_active") ?: true, startsAt = s(o, "starts_at"), expiresAt = s(o, "expires_at")
    )

    private fun array(text: String): JsonArray = Json.parseToJsonElement(text).jsonArray

    suspend fun categories(): Result<List<Category>> = runCatching { withContext(Dispatchers.IO) {
        array(api.get("categories", "?select=*&order=sort_order.asc,name.asc")).map { parseCategory(it.jsonObject) }
    }}

    suspend fun products(): Result<List<Product>> = runCatching { withContext(Dispatchers.IO) {
        val raw = api.get("products", "?select=*,categories(name)&order=created_at.desc")
        array(raw).map { el ->
            val o = el.jsonObject.toMutableMap()
            val cat = o["categories"]?.jsonObject
            if (cat != null) o["category_name"] = cat["name"] ?: JsonNull
            parseProduct(JsonObject(o))
        }
    }}

    suspend fun variants(productId: String): Result<List<ProductVariant>> = runCatching { withContext(Dispatchers.IO) {
        array(api.get("product_variants", "?select=*&product_id=eq.$productId&order=sort_order.asc")).map { parseVariant(it.jsonObject) }
    }}

    suspend fun orders(): Result<List<Order>> = runCatching { withContext(Dispatchers.IO) {
        array(api.get("orders", "?select=*&order=created_at.desc")).map { parseOrder(it.jsonObject) }
    }}

    suspend fun customers(): Result<List<Customer>> = runCatching { withContext(Dispatchers.IO) {
        array(api.get("profiles", "?select=id,full_name,phone,email,role,created_at&order=created_at.desc")).map { parseCustomer(it.jsonObject) }
    }}

    suspend fun complaints(): Result<List<Complaint>> = runCatching { withContext(Dispatchers.IO) {
        array(api.get("complaints", "?select=*&order=created_at.desc")).map { parseComplaint(it.jsonObject) }
    }}

    suspend fun wishlistSummary(): Result<List<WishlistSummary>> = runCatching { withContext(Dispatchers.IO) {
        val rows = array(api.get("wishlists", "?select=product_id"))
        rows.mapNotNull { it.jsonObject["product_id"]?.jsonPrimitive?.contentOrNull }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
            .map { WishlistSummary(it.key, it.value) }
    }}

    suspend fun coupons(): Result<List<Coupon>> = runCatching { withContext(Dispatchers.IO) {
        array(api.get("coupons", "?select=*&order=created_at.desc")).map { parseCoupon(it.jsonObject) }
    }}

    suspend fun addCategory(name: String, description: String?, imageUrl: String?, sortOrder: Int): Result<Category> = runCatching { withContext(Dispatchers.IO) {
        val slug = slug(name) + "-" + System.currentTimeMillis().toString().takeLast(6)
        val body = buildJsonObject { put("name", name); put("slug", slug); put("description", description); put("image_url", imageUrl); put("sort_order", sortOrder); put("is_active", true) }
        parseCategory(array(api.post("categories", body.toString())).first().jsonObject)
    }}

    suspend fun updateCategory(c: Category): Result<Category> = runCatching { withContext(Dispatchers.IO) {
        val body = buildJsonObject { put("name", c.name); put("description", c.description); put("image_url", c.imageUrl); put("sort_order", c.sortOrder); put("is_active", c.active) }
        parseCategory(array(api.patch("categories", "id=eq.${c.id}", body.toString())).first().jsonObject)
    }}

    suspend fun deleteCategory(id: String): Result<Unit> = runCatching { withContext(Dispatchers.IO) { api.delete("categories", "id=eq.$id"); Unit }}

    suspend fun addProduct(name: String, description: String?, price: Double, oldPrice: Double?, stock: Int, categoryId: String?, imageUrl: String?, featured: Boolean, flash: Boolean, hot: Boolean): Result<Product> = runCatching { withContext(Dispatchers.IO) {
        val slug = slug(name) + "-" + System.currentTimeMillis().toString().takeLast(6)
        val body = buildJsonObject {
            put("name", name); put("slug", slug); put("description", description); put("price", price); put("old_price", oldPrice)
            put("stock_quantity", stock); put("category_id", categoryId); put("image_url", imageUrl); put("is_active", true)
            put("is_featured", featured); put("is_flash_sale", flash); put("is_hot_deal", hot)
        }
        val obj = array(api.post("products", body.toString())).first().jsonObject
        parseProduct(obj)
    }}

    suspend fun updateProduct(p: Product): Result<Product> = runCatching { withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("name", p.name); put("description", p.description); put("price", p.price); put("old_price", p.oldPrice)
            put("stock_quantity", p.stock); put("category_id", p.categoryId); put("image_url", p.imageUrl); put("is_active", p.active)
            put("is_featured", p.featured); put("is_flash_sale", p.flashSale); put("is_hot_deal", p.hotDeal); put("sort_order", p.sortOrder)
        }
        parseProduct(array(api.patch("products", "id=eq.${p.id}", body.toString())).first().jsonObject)
    }}

    suspend fun deleteProduct(id: String): Result<Unit> = runCatching { withContext(Dispatchers.IO) { api.delete("products", "id=eq.$id"); Unit }}

    suspend fun addVariant(productId: String, label: String, grams: Int, price: Double, oldPrice: Double?, stock: Int): Result<ProductVariant> = runCatching { withContext(Dispatchers.IO) {
        val body = buildJsonObject { put("product_id", productId); put("label", label); put("weight_grams", grams); put("price", price); put("old_price", oldPrice); put("stock_quantity", stock); put("is_active", true) }
        parseVariant(array(api.post("product_variants", body.toString())).first().jsonObject)
    }}

    suspend fun deleteVariant(id: String): Result<Unit> = runCatching { withContext(Dispatchers.IO) { api.delete("product_variants", "id=eq.$id"); Unit }}

    suspend fun updateOrderStatus(id: String, status: String): Result<Order> = runCatching { withContext(Dispatchers.IO) {
        parseOrder(array(api.patch("orders", "id=eq.$id", buildJsonObject { put("status", status) }.toString())).first().jsonObject)
    }}

    suspend fun updatePaymentStatus(id: String, status: String): Result<Order> = runCatching { withContext(Dispatchers.IO) {
        parseOrder(array(api.patch("orders", "id=eq.$id", buildJsonObject { put("payment_status", status) }.toString())).first().jsonObject)
    }}

    suspend fun updateCustomerRole(id: String, role: String): Result<Customer> = runCatching { withContext(Dispatchers.IO) {
        parseCustomer(array(api.patch("profiles", "id=eq.$id", buildJsonObject { put("role", role) }.toString())).first().jsonObject)
    }}

    suspend fun updateComplaint(id: String, status: String, note: String?): Result<Complaint> = runCatching { withContext(Dispatchers.IO) {
        parseComplaint(array(api.patch("complaints", "id=eq.$id", buildJsonObject { put("status", status); put("admin_note", note) }.toString())).first().jsonObject)
    }}

    suspend fun addCoupon(code: String, title: String?, type: String, value: Double, minOrder: Double, maxDiscount: Double?, limit: Int?, startsAt: String?, expiresAt: String?): Result<Coupon> = runCatching { withContext(Dispatchers.IO) {
        val body = buildJsonObject { put("code", code.uppercase()); put("title", title); put("discount_type", type); put("discount_value", value); put("min_order", minOrder); put("max_discount", maxDiscount); put("usage_limit", limit); put("starts_at", startsAt); put("expires_at", expiresAt); put("is_active", true) }
        parseCoupon(array(api.post("coupons", body.toString())).first().jsonObject)
    }}

    suspend fun updateCoupon(c: Coupon): Result<Coupon> = runCatching { withContext(Dispatchers.IO) {
        val body = buildJsonObject { put("code", c.code.uppercase()); put("title", c.title); put("discount_type", c.discountType); put("discount_value", c.discountValue); put("min_order", c.minOrder); put("max_discount", c.maxDiscount); put("usage_limit", c.usageLimit); put("is_active", c.active); put("starts_at", c.startsAt); put("expires_at", c.expiresAt) }
        parseCoupon(array(api.patch("coupons", "id=eq.${c.id}", body.toString())).first().jsonObject)
    }}

    suspend fun deleteCoupon(id: String): Result<Unit> = runCatching { withContext(Dispatchers.IO) { api.delete("coupons", "id=eq.$id"); Unit }}

    data class AdminSession(val accessToken: String, val userId: String, val email: String)

    suspend fun signInAdmin(email: String, password: String): Result<AdminSession> = runCatching { withContext(Dispatchers.IO) {
        val res = Json.parseToJsonElement(api.signIn(email, password)).jsonObject
        val token = res["access_token"]?.jsonPrimitive?.contentOrNull ?: throw IllegalStateException(res["msg"]?.jsonPrimitive?.contentOrNull ?: "লগইন ব্যর্থ হয়েছে")
        val user = res["user"]?.jsonObject ?: throw IllegalStateException("Supabase user তথ্য পাওয়া যায়নি")
        val userId = user["id"]?.jsonPrimitive?.contentOrNull ?: throw IllegalStateException("User ID পাওয়া যায়নি")
        val profiles = array(api.getWithBearer("profiles", "?select=role&id=eq.$userId", token))
        val role = profiles.firstOrNull()?.jsonObject?.get("role")?.jsonPrimitive?.contentOrNull ?: "customer"
        if (role != "admin") throw IllegalStateException("এই অ্যাকাউন্টটি Admin নয়। profiles.role = admin করুন।")
        AdminSession(token, userId, user["email"]?.jsonPrimitive?.contentOrNull ?: email)
    }}

    private fun slug(value: String): String = value.lowercase().trim().replace(Regex("[^a-z0-9\\u0980-\\u09FF]+"), "-").trim('-').ifBlank { "item" }
}
