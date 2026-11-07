package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.repository.FamilyHubRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FamilyHubRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun unauthenticatedUser_cannotCreateHub() = runBlocking {
        auth.signOut()
        val repo = FamilyHubRepository(firestore)
        try {
            repo.createHub("Starlight Hub")
            fail("Expected exception for unauthenticated user")
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException || e.message?.contains("signed in") == true)
        }
    }

    @Test
    fun authenticatedUser_canCreateHubAndAddGrocery() = runBlocking {
        val aliceUid = signInTestUser("alice_test@cosmic.family")
        val repo = FamilyHubRepository(firestore)

        val hubId = repo.createHub(name = "Cosmic Galaxy Hub")
        assertNotNull(hubId)

        // Alice adds grocery item
        repo.addGroceryItem(
            hubId = hubId,
            memberUids = listOf(aliceUid),
            title = "Space Cookies",
            category = "Bakery",
            quantity = 3,
            unit = "packs",
            assignedTo = "Alice",
            notes = "Cyber vanilla flavor"
        )
    }
}
