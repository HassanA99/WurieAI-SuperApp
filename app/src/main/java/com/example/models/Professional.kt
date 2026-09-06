package com.example.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Professional(
    val id: String,
    val name: String,
    val category: String, // e.g., "Plumber", "Electrician"
    val rating: Double,
    val reviewsCount: Int,
    val distance: String, // e.g., "2.5 km away"
    val description: String,
    val hourlyRate: String // e.g., "Le 150,000/hr"
)
