package learn.ktor.repositories

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import learn.ktor.config.DatabaseFactory
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException
import org.jetbrains.exposed.exceptions.ExposedSQLException
import kotlin.test.*

class UserRepositoryTest {

    private val userRepository = UserRepository()

    @BeforeTest
    fun setup() {
        DatabaseFactory.connect(DatabaseFactory.h2TestConfig())
    }

    @Test
    fun `should add and get user`() = runTest {
        userRepository.addUser("alice", "password")
        val user = userRepository.getUserByUsername("alice")
        assertNotNull(user)
        assertEquals("alice", user.username)
    }

    @Test
    fun `should not allow duplicate usernames`() = runTest {
        userRepository.addUser("alice", "password")

        val exception = assertFailsWith<ExposedSQLException> {
            runBlocking {
                userRepository.addUser("alice", "other")
            }
        }

        assertTrue(exception.cause is JdbcSQLIntegrityConstraintViolationException)
    }

    @Test
    fun `should return null for non-existent user`() = runTest {
        val results = listOf(
            userRepository.getUserByUsername("alice"),
            userRepository.getUsernameById(99),
            userRepository.getIdByUsername("alice")
        )

        results.forEach { assertNull(it) }
    }

    @Test
    fun `should delete user`() = runTest {
        userRepository.addUser("alice", "password")
        val result = userRepository.deleteUser("alice")
        assertTrue(result)
    }

    @Test
    fun `should not delete non-existent user`() = runTest {
        val result = userRepository.deleteUser("alice")
        assertFalse(result)
    }

    @Test
    fun `should get user id by username`() = runTest {
        val username = "alice"
        val user = userRepository.addUser(username, "password")

        assertNotNull(user)
        assertEquals(user.id, userRepository.getIdByUsername(username))
    }

    @Test
    fun `should get username by user id`() = runTest {
        val username = "alice"
        val user = userRepository.addUser(username, "password")

        assertNotNull(user)
        assertEquals(username, userRepository.getUsernameById(user.id))
    }
}
