package com.example.database

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class SafiriRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndReadUserProfile_authenticatedOwner_succeeds() = runBlocking {
        val uniqueEmail = "alice_${UUID.randomUUID().toString().replace("-", "").take(8)}@test.com"
        val uid = signInTestUser(uniqueEmail)
        val repo = SafiriFirestoreRepository(firestore)

        val profile = FirestoreUserProfile(
            id = uid,
            userId = uid,
            email = uniqueEmail,
            displayName = "Alice Kamau",
            role = "PARENT",
            phone = "0712345678"
        )
        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.createUserProfile(profile) }
        assertTrue("Profile create failed: ${createResult.exceptionOrNull()}", createResult.isSuccess)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getUserProfile(uid) }.getOrThrow()
        assertNotNull(fetched)
        assertEquals("Alice Kamau", fetched?.displayName)
        assertEquals("PARENT", fetched?.role)
    }

    @Test
    fun createAndObserveStudentsForParent_authenticatedParent_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = SafiriFirestoreRepository(firestore)
        val studentId = "st_${UUID.randomUUID().toString().replace("-", "")}"

        val student = FirestoreStudent(
            id = studentId,
            parentUid = aliceUid,
            name = "Brian Kamau",
            initials = "BK",
            parentName = "Alice Kamau",
            parentEmail = ALICE_EMAIL,
            parentPhone = "0712345678",
            assignedBusPlate = "KDE 732X",
            assignedDriverName = "Erick Mwangi",
            pickupStop = "Kilimani Stop",
            dropoffStop = "St. Mary's Academy",
            status = "NOT_BOARDED",
            classGrade = "Grade 3",
            schoolName = "St. Mary's Academy"
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.createStudent(studentId, student) }
        assertTrue("Student create failed: ${createResult.exceptionOrNull()}", createResult.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeStudentsForParent(aliceUid).first { list -> list.any { it.id == studentId } }
        }
        assertTrue(observed.any { it.id == studentId && it.name == "Brian Kamau" })
    }

    @Test
    fun getStudentById_crossParentAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = SafiriFirestoreRepository(firestore)
        val studentId = "st_${UUID.randomUUID().toString().replace("-", "")}"

        val student = FirestoreStudent(
            id = studentId,
            parentUid = aliceUid,
            name = "Zara Kamau",
            initials = "ZK",
            parentName = "Alice Kamau",
            parentEmail = ALICE_EMAIL,
            parentPhone = "0712345678",
            assignedBusPlate = "KDE 732X",
            assignedDriverName = "Erick Mwangi",
            pickupStop = "Kilimani Stop",
            dropoffStop = "St. Mary's Academy",
            status = "NOT_BOARDED"
        )
        withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.createStudent(studentId, student).getOrThrow() }

        signInTestUser(BOB_EMAIL)
        val bobRepo = SafiriFirestoreRepository(firestore)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getStudentById(studentId) }
        assertTrue(result.isFailure)
        val firestoreEx = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeStudents_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = SafiriFirestoreRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeStudentsForParent("unauth_uid").first()
            }
            fail("Expected PERMISSION_DENIED when observing students while unauthenticated")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun sendInterportalMessage_parentRoleSucceeds_driverSpoofDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = SafiriFirestoreRepository(firestore)
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "")}"

        val validResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.sendInterportalMessage(
                msgId,
                FirestoreInterportalMessage(
                    id = msgId,
                    senderUid = aliceUid,
                    senderName = "Alice Kamau",
                    senderRole = "PARENT",
                    recipientRole = "DRIVER",
                    targetBusPlate = "KDE 732X",
                    content = "Ready at Kilimani stop",
                    displayTime = "7:05 AM"
                )
            )
        }
        assertTrue("Valid parent message failed: ${validResult.exceptionOrNull()}", validResult.isSuccess)

        val spoofId = "msg_${UUID.randomUUID().toString().replace("-", "")}"
        val spoofResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.sendInterportalMessage(
                spoofId,
                FirestoreInterportalMessage(
                    id = spoofId,
                    senderUid = aliceUid,
                    senderName = "Spoof Driver",
                    senderRole = "DRIVER",
                    recipientRole = "ALL",
                    targetBusPlate = "KDE 732X",
                    content = "Fake driver dispatch"
                )
            )
        }
        assertTrue(spoofResult.isFailure)
        val firestoreEx = spoofResult.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun createIncidentAndUpvote_authenticatedUser_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = SafiriFirestoreRepository(firestore)
        val incId = "inc_${UUID.randomUUID().toString().replace("-", "")}"

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createIncident(
                incId,
                FirestoreCommunityIncident(
                    id = incId,
                    reporterUid = aliceUid,
                    title = "Slow traffic at Valley Arcade",
                    location = "Gitanga Rd",
                    category = "Traffic",
                    votes = 1L,
                    isActive = true
                )
            )
        }
        assertTrue("Incident create failed: ${createResult.exceptionOrNull()}", createResult.isSuccess)

        val upvoteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.incrementIncidentVotes(incId, 1L)
        }
        assertTrue("Incident upvote failed: ${upvoteResult.exceptionOrNull()}", upvoteResult.isSuccess)

        val activeIncidents = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeActiveIncidents().first { list -> list.any { it.id == incId && it.votes == 2L } }
        }
        assertTrue(activeIncidents.any { it.id == incId && it.votes == 2L })
    }

    private companion object {
        const val ALICE_EMAIL = "alice.safiri@test.com"
        const val BOB_EMAIL = "bob.safiri@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
