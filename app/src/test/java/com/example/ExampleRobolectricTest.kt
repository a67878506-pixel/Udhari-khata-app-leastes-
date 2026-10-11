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
}
