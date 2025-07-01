package learn.ktor.services

import kotlinx.coroutines.test.runTest
import kotlin.test.*

class UserServiceTest {

    private val service = UserService()

    @BeforeTest
    fun setup() = runTest {
        service.clear()
    }

    @Test
    fun `should register new user`() = runTest {
        val result = service.register("alice", "password")
        assertTrue(result)
    }

    @Test
    fun `should not register duplicate usernames`() = runTest {
        service.register("alice", "password")
        val result = service.register("alice", "another")
        assertFalse(result)
    }

    @Test
    fun `should authenticate user`() = runTest {
        service.register("alice", "password")
        val result = service.authenticate("alice", "password")
        assertTrue(result)
    }

    @Test
    fun `should not authenticate invalid password`() = runTest {
        service.register("alice", "password")
        val result = service.authenticate("alice", "wrong")
        assertFalse(result)
    }

    @Test
    fun `should not authenticate unregistered user`() = runTest {
        val result = service.authenticate("alice", "password")
        assertFalse(result)
    }
}