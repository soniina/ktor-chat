package learn.ktor.repositories

import kotlinx.coroutines.test.runTest
import learn.ktor.config.DatabaseFactory
import kotlin.test.*

class MessageRepositoryTest {

    private val messageRepository = MessageRepository()

    @BeforeTest
    fun setup() {
        DatabaseFactory.connect(DatabaseFactory.h2TestConfig())
    }

    @Test
    fun `should save and get message`() = runTest{
        val message = messageRepository.saveMessage("alice", "bob", "Hello!")
        val messages = messageRepository.getMessagesBetween("alice", "bob")

        assertNotNull(message)
        assertTrue(messages.size == 1)
        assertTrue(messages[0].content == "Hello!")
        assertTrue(messages[0].sender == "alice")
        assertTrue(messages[0].recipient == "bob")
        assertTrue(messages[0].timestamp > 0)
    }

    @Test
    fun `should return same messages for both sender and recipient roles`() = runTest {
        messageRepository.saveMessage("alice", "bob", "Hello!")
        messageRepository.saveMessage("bob", "alice", "Hi")
        val messagesBetweenAliceAndBob = messageRepository.getMessagesBetween("alice", "bob")
        val messagesBetweenBobAndAlice = messageRepository.getMessagesBetween("bob", "alice")

        assertTrue(messagesBetweenAliceAndBob.size == 2)
        assertEquals(messagesBetweenAliceAndBob, messagesBetweenBobAndAlice)
    }

    @Test
    fun `should return empty list for non-existent conversation`() = runTest {
        val messages = messageRepository.getMessagesBetween("alice", "bob")
        assertTrue(messages.isEmpty())
    }
}