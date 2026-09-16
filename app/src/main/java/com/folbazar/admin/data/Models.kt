package com.folbazar.admin.data

data class Product(
    val id: String,
    val title: String,
    val price: Double,
    val oldPrice: Double? = null,
    val stock: Int = 0,
    val imageUrl: String? = null,
    val category: String? = null,
    val active: Boolean = true
)

data class Order(
    val id: String,
    val customer: String,
    val amount: Double,
    val status: String,
    val createdAt: String? = null
)

data class Customer(
    val id: String,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val createdAt: String? = null
)
