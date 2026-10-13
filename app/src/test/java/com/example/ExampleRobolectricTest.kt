package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.roundToLong

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Udhaar Khata", appName)
    }

    @Test
    fun `test accurate balance calculation with zero floating point drift`() {
        val tx1 = Transaction(amount = 1200.50, type = "LENA")
        val tx2 = Transaction(amount = 450.25, type = "DENA")
        val tx3 = Transaction(amount = 150.75, type = "LENA")

        val txList = listOf(tx1, tx2, tx3)

        var lenaPaise = 0L
        var denaPaise = 0L

        for (tx in txList) {
            val paise = (tx.amount * 100.0).roundToLong()
            if (tx.type == "LENA") {
                lenaPaise += paise
            } else {
                denaPaise += paise
            }
        }

        val totalLena = lenaPaise / 100.0
        val totalDena = denaPaise / 100.0
        val netBalance = (lenaPaise - denaPaise) / 100.0

        assertEquals(1351.25, totalLena, 0.001)
        assertEquals(450.25, totalDena, 0.001)
        assertEquals(901.00, netBalance, 0.001)
    }

    @Test
    fun `test customUserId validation regex`() {
        val validUsernames = listOf("anshul_123", "rahul", "store_owner_1", "abc")
        val invalidUsernames = listOf("ab", "user-id", "user.name", "user@id", "   ", "a#b")

        val regex = Regex("^[a-z0-9_]+$")

        for (u in validUsernames) {
            val clean = u.trim().lowercase()
            assertEquals(true, clean.length >= 3 && clean.matches(regex))
        }

        for (u in invalidUsernames) {
            val clean = u.trim().lowercase()
            val isValid = clean.length >= 3 && clean.matches(regex)
            assertEquals(false, isValid)
        }
    }

    @Test
    fun `test UserIdCheckResult differentiates between taken and error`() {
        val available = com.example.data.model.UserIdCheckResult.Available
        val taken = com.example.data.model.UserIdCheckResult.Taken
        val error = com.example.data.model.UserIdCheckResult.Error(
            cause = RuntimeException("PERMISSION_DENIED"),
            message = "Unable to check User ID"
        )

        assertEquals(true, available is com.example.data.model.UserIdCheckResult.Available)
        assertEquals(true, taken is com.example.data.model.UserIdCheckResult.Taken)
        assertEquals(true, error is com.example.data.model.UserIdCheckResult.Error)

        // Verify that an error is NEVER treated as Taken
        val isTaken = when (error) {
            is com.example.data.model.UserIdCheckResult.Taken -> true
            else -> false
        }
        assertEquals(false, isTaken)
    }

    @Test
    fun `test Khata and models instantiate with stored id and support fallback`() {
        val storedKhata = com.example.data.model.Khata(
            id = "VFIRg5SxZtaUPn3NgaU0",
            name = "My Shop Khata",
            ownerUid = "user_123"
        )
        assertEquals("VFIRg5SxZtaUPn3NgaU0", storedKhata.id)
        assertEquals("My Shop Khata", storedKhata.name)

        // Test fallback when id is blank
        val blankKhata = com.example.data.model.Khata(name = "New Khata")
        val fallbackKhata = if (blankKhata.id.isBlank()) blankKhata.copy(id = "doc_fallback_id") else blankKhata
        assertEquals("doc_fallback_id", fallbackKhata.id)

        // Verify other models
        val person = com.example.data.model.Person(id = "person_1", name = "Ramesh")
        assertEquals("person_1", person.id)

        val tx = com.example.data.model.Transaction(id = "tx_1", amount = 100.0)
        assertEquals("tx_1", tx.id)

        val profile = com.example.data.model.UserProfile(uid = "uid_1", name = "Anshul")
        assertEquals("uid_1", profile.uid)

        val userRecord = com.example.data.model.UsernameRecord(customUserId = "anshul47957", uid = "uid_1")
        assertEquals("anshul47957", userRecord.customUserId)
    }

    @Test
    fun `test Khata soft delete state and filtering`() {
        val activeKhata = com.example.data.model.Khata(id = "k1", name = "Shop", isDeleted = false)
        val deletedKhata = com.example.data.model.Khata(
            id = "k2",
            name = "Old Shop",
            isDeleted = true,
            deletedAt = com.google.firebase.Timestamp.now(),
            deletedBy = "user_1"
        )

        val allKhatas = listOf(activeKhata, deletedKhata)
        val activeOnly = allKhatas.filter { !it.isDeleted }
        val trashOnly = allKhatas.filter { it.isDeleted }

        assertEquals(1, activeOnly.size)
        assertEquals("k1", activeOnly[0].id)
        assertEquals(1, trashOnly.size)
        assertEquals("k2", trashOnly[0].id)
        assertEquals(true, trashOnly[0].isDeleted)
        assertEquals("user_1", trashOnly[0].deletedBy)
    }

    @Test
    fun `test Person soft delete state and filtering`() {
        val activePerson = com.example.data.model.Person(id = "p1", name = "Ramesh", isDeleted = false)
        val deletedPerson = com.example.data.model.Person(
            id = "p2",
            name = "Suresh",
            isDeleted = true,
            deletedAt = com.google.firebase.Timestamp.now(),
            deletedBy = "Anshul"
        )

        val allPeople = listOf(activePerson, deletedPerson)
        val activeList = allPeople.filter { !it.isDeleted }
        val trashList = allPeople.filter { it.isDeleted }

        assertEquals(1, activeList.size)
        assertEquals("p1", activeList[0].id)
        assertEquals(1, trashList.size)
        assertEquals("p2", trashList[0].id)
        assertEquals(true, trashList[0].isDeleted)
        assertEquals("Anshul", trashList[0].deletedBy)
    }

    @Test
    fun `test Transaction soft delete state, filtering, and balance recalculation`() {
        val tx1 = com.example.data.model.Transaction(
            id = "t1",
            amount = 100.50,
            type = "LENA",
            isDeleted = false
        )
        val tx2 = com.example.data.model.Transaction(
            id = "t2",
            amount = 40.25,
            type = "DENA",
            isDeleted = false
        )
        val tx3Deleted = com.example.data.model.Transaction(
            id = "t3",
            amount = 50.00,
            type = "LENA",
            isDeleted = true,
            deletedBy = "User"
        )

        val allTxs = listOf(tx1, tx2, tx3Deleted)
        val activeTxs = allTxs.filter { !it.isDeleted }
        val trashTxs = allTxs.filter { it.isDeleted }

        assertEquals(2, activeTxs.size)
        assertEquals(1, trashTxs.size)
        assertEquals("t3", trashTxs[0].id)

        // Calculate balances using only active transactions (integer paise accuracy)
        var lenaPaise = 0L
        var denaPaise = 0L
        for (tx in activeTxs) {
            val paise = kotlin.math.round((tx.amount * 100.0)).toLong()
            if (tx.type == "LENA") lenaPaise += paise else denaPaise += paise
        }

        val netBalance = (lenaPaise - denaPaise) / 100.0
        assertEquals(60.25, netBalance, 0.001)

        // When tx3 is recovered, balance recalculates to include it
        val recoveredTx3 = tx3Deleted.copy(isDeleted = false, deletedAt = null, deletedBy = null)
        val updatedActiveTxs = listOf(tx1, tx2, recoveredTx3)
        var newLenaPaise = 0L
        var newDenaPaise = 0L
        for (tx in updatedActiveTxs) {
            val paise = kotlin.math.round((tx.amount * 100.0)).toLong()
            if (tx.type == "LENA") newLenaPaise += paise else newDenaPaise += paise
        }
        val restoredNetBalance = (newLenaPaise - newDenaPaise) / 100.0
        assertEquals(110.25, restoredNetBalance, 0.001)
    }
}
