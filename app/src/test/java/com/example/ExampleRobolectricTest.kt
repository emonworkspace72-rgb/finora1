package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Finora", appName)
    }

    @Test
    fun `room database inserts and reads account and transactions correctly`() = runBlocking {
        val dao = db.finoraDao()

        val account = AccountEntity(
            id = "acc_1",
            name = "Test Checking",
            type = "BANK",
            currency = "USD",
            openingBalanceCents = 100000L
        )
        dao.upsertAccount(account)

        val retrievedAcc = dao.getAccountById("acc_1")
        assertNotNull(retrievedAcc)
        assertEquals("Test Checking", retrievedAcc?.name)

        val tx = TransactionEntity(
            id = "tx_1",
            type = "EXPENSE",
            amountCents = 2500L,
            accountId = "acc_1",
            categoryId = "cat_1",
            dateMillis = System.currentTimeMillis(),
            note = "Coffee"
        )
        dao.upsertTransaction(tx)

        val allTx = dao.getAllTransactions().first()
        assertEquals(1, allTx.size)
        assertEquals("Coffee", allTx[0].note)
    }
}
