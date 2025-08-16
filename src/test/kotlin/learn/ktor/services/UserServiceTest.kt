package learn.ktor.services

import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import learn.ktor.model.User
import learn.ktor.repositories.UserRepository
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*
import org.mindrot.jbcrypt.BCrypt

@ExtendWith(MockKExtension::class)
class UserServiceTest {

    @MockK
    lateinit var userRepository: UserRepository

    @InjectMockKs
    lateinit var userService:UserService

    @Test
    fun `should register new user`() = runTest {
        val user = User(1, "alice", "hashedPassword")

        coEvery { userRepository.getUserByUsername(user.username) } returns null
        coEvery { userRepository.addUser(user.username, any()) } returns user

        assertTrue(userService.register(user.username, "password"))
    }

    @Test
    fun `should not register duplicate usernames`() = runTest {
        val user = User(1, "alice", "hashedPassword")

        coEvery { userRepository.getUserByUsername(user.username) } returns user

        assertFalse(userService.register(user.username, "password"))
    }

    @Test
    fun `should authenticate user`() = runTest {
        val password = "password"
        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
        val user = User(1, "alice", hashedPassword)

        coEvery { userRepository.getUserByUsername(user.username) } returns user

        assertTrue(userService.authenticate(user.username, password))
    }

    @Test
    fun `should not authenticate invalid password`() = runTest {
        val hashedPassword = BCrypt.hashpw("another", BCrypt.gensalt())
        val user = User(1, "alice", hashedPassword)

        coEvery { userRepository.getUserByUsername(user.username) } returns user

        assertFalse(userService.authenticate(user.username, "password"))
    }

    @Test
    fun `should not authenticate unregistered user`() = runTest {
        val username = "alice"

        coEvery { userRepository.getUserByUsername(username) } returns null

        assertFalse(userService.authenticate(username, "password"))
    }
}
