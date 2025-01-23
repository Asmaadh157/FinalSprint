package com.example.gestiondesproject
data class Task(
    val title: String = "",
    var fileUri: String = "",
    val id: String? = null,
    val groupId: String = "",
    var status: String = "pending" // "pending", "accepted", "rejected", "modified"
)
