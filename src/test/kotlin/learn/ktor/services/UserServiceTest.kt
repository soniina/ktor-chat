package learn.ktor.services

import kotlinx.coroutines.test.runTest
import kotlin.test.*

class UserServiceTest {

    @BeforeTest
    fun setup() = runTest {
        UserService.clear()
    }

    @Test
    fun `should register new user`() = runTest {
        val result = UserService.register("alice", "password")
        assertTrue(result)
    }

    @Test
    fun `should not register duplicate usernames`() = runTest {
        UserService.register("alice", "password")
        val result = UserService.register("alice", "another")
        assertFalse(result)
    }

    @Test
    fun `should authenticate user`() = runTest {
        UserService.register("alice", "password")
        val result = UserService.authenticate("alice", "password")
        assertTrue(result)
    }

    @Test
    fun `should not authenticate invalid password`() = runTest {
        UserService.register("alice", "password")
        val result = UserService.authenticate("alice", "wrong")
        assertFalse(result)
    }

    @Test
    fun `should not authenticate unregistered user`() = runTest {
        val result = UserService.authenticate("alice", "password")
        assertFalse(result)
    }
}