package com.example.mylistexpress.common

data class Product(
    val name: String,
    val isBought: Boolean,
    val imageUri: String? = null,
    val category: String? = null
)