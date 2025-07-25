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

    @Test
    fun `should get undelivered messages for user`()  = runTest {
        val message1 = messageRepository.saveMessage("alice", "bob", "First")
        val message2 = messageRepository.saveMessage("alice", "bob", "Second")
        val message3 = messageRepository.saveMessage("alice", "bob", "Third")

        messageRepository.markAsDelivered(message1.id)

        val undelivered = messageRepository.getUndeliveredMessagesFor("bob")

        assertEquals(2, undelivered.size)
        assertEquals(message2.content, undelivered.first().content)
        assertEquals(message3.content, undelivered.last().content)
    }

    @Test
    fun `should mark message as delivered`() = runTest {
        val message = messageRepository.saveMessage("alice", "bob", "Hello!")

        val before = messageRepository.getUndeliveredMessagesFor("bob")
        assertTrue(before.any { it.id == message.id })

        messageRepository.markAsDelivered(message.id)

        val after = messageRepository.getUndeliveredMessagesFor("bob")
        assertFalse(after.any { it.id == message.id })
    }

    @Test
    fun `should return empty list if no undelivered messages`() = runTest {
        messageRepository.saveMessage("alice", "bob", "Delivered").also {
            messageRepository.markAsDelivered(it.id)
        }

        val undelivered = messageRepository.getUndeliveredMessagesFor("bob")
        assertTrue(undelivered.isEmpty())
    }
}