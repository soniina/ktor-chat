package learn.ktor.services

import kotlinx.coroutines.test.runTest
import learn.ktor.config.DatabaseFactory
import learn.ktor.repositories.UserRepository
import kotlin.test.*

class UserServiceTest {

    private val userRepository = UserRepository()
    private val userService = UserService(userRepository)

    @BeforeTest
    fun setup() {
        DatabaseFactory.connect(DatabaseFactory.h2TestConfig())
    }

    @Test
    fun `should register new user`() = runTest {
        val result = userService.register("alice", "password")
        assertTrue(result)
    }

    @Test
    fun `should not register duplicate usernames`() = runTest {
        userService.register("alice", "password")
        val result = userService.register("alice", "another")
        assertFalse(result)
    }

    @Test
    fun `should authenticate user`() = runTest {
        userService.register("alice", "password")
        val result = userService.authenticate("alice", "password")
        assertTrue(result)
    }

    @Test
    fun `should not authenticate invalid password`() = runTest {
        userService.register("alice", "password")
        val result = userService.authenticate("alice", "wrong")
        assertFalse(result)
    }

    @Test
    fun `should not authenticate unregistered user`() = runTest {
        val result = userService.authenticate("alice", "password")
        assertFalse(result)
    }
}