package learn.ktor.repositories

import kotlinx.coroutines.test.runTest
import learn.ktor.config.DatabaseFactory
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
        val user = userRepository.getByUsername("alice")
        assertNotNull(user)
        assertEquals("alice", user.username)
    }

    @Test
    fun `should not allow duplicate usernames`() = runTest {
        val user1 = userRepository.addUser("alice", "password")
        val user2 = userRepository.addUser("alice", "other")
        assertNotNull(user1)
        assertNull(user2)
    }

    @Test
    fun `should return null for non-existent user`() = runTest {
        val user = userRepository.getByUsername("alice")
        assertNull(user)
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
}