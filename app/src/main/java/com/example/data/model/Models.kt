package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val customUserId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UsernameRecord(
    val customUserId: String = "",
    val uid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

sealed class UserIdCheckResult {
    data object Available : UserIdCheckResult()
    data object Taken : UserIdCheckResult()
    data class Error(val cause: Throwable, val message: String) : UserIdCheckResult()
}

@IgnoreExtraProperties
data class Khata(
    val id: String = "",
    val name: String = "",
    val ownerUid: String = "",
    val ownerName: String = "",
    val memberUids: List<String> = emptyList(),
    val members: Map<String, String> = emptyMap(),
    val totalPeople: Long = 0L,
    val totalLena: Double = 0.0,
    val totalDena: Double = 0.0,
    val netBalance: Double = 0.0,
    @get:PropertyName("isDeleted")
    @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false,
    val deletedAt: Timestamp? = null,
    val deletedBy: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Person(
    val id: String = "",
    val khataId: String = "",
    val name: String = "",
    val phone: String = "",
    val totalLena: Double = 0.0,
    val totalDena: Double = 0.0,
    val netBalance: Double = 0.0,
    @get:PropertyName("isDeleted")
    @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false,
    val deletedAt: Timestamp? = null,
    val deletedBy: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val lastTransactionAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Transaction(
    val id: String = "",
    val khataId: String = "",
    val personId: String = "",
    val personName: String = "",
    val amount: Double = 0.0,
    val type: String = "LENA", // "LENA" (You gave / You will receive) or "DENA" (You got / You will pay)
    val note: String = "",
    val timestamp: Timestamp? = null,
    @get:PropertyName("isDeleted")
    @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false,
    val deletedAt: Timestamp? = null,
    val deletedBy: String? = null,
    @ServerTimestamp val createdTimestamp: Timestamp? = null,
    val createdByUid: String = "",
    val createdByName: String = "",
    val updatedByUid: String? = null,
    val updatedByName: String? = null
)

@IgnoreExtraProperties
data class KhataHistory(
    val id: String = "",
    val khataId: String = "",
    val actionType: String = "",
    val description: String = "",
    val amount: Double? = null,
    val personName: String? = null,
    val performedByUid: String = "",
    val performedByName: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)

@IgnoreExtraProperties
data class ShareRequest(
    val id: String = "",
    val khataId: String = "",
    val khataName: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val fromCustomUserId: String = "",
    val toUid: String = "",
    val toName: String = "",
    val toCustomUserId: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED, CANCELLED
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
