package learn.ktor.repositories

import kotlinx.coroutines.test.runTest
import learn.ktor.config.DatabaseFactory
import learn.ktor.model.User
import kotlin.test.*

class MessageRepositoryTest {

    private val userRepository = UserRepository()
    private val messageRepository = MessageRepository()

    private lateinit var sender: User
    private lateinit var recipient: User

    @BeforeTest
    fun setup() = runTest {
        DatabaseFactory.connect(
            url = "jdbc:h2:mem:test-${System.nanoTime()};DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver"
        )
        DatabaseFactory.init(listOf(Messages))

        sender = userRepository.addUser("test_sender", "password")!!
        recipient = userRepository.addUser("test_recipient", "password")!!
    }

    @Test
    fun `should save and get message`() = runTest{
        val message = messageRepository.saveMessage(sender.id, recipient.id, "Hello!")
        val messages = messageRepository.getMessagesBetween(sender.id, recipient.id)

        assertNotNull(message)
        assertEquals(1, messages.size)
        assertEquals("Hello!", messages[0].content)

        assertEquals(sender.id, messages[0].senderId)
        assertEquals(recipient.id, messages[0].recipientId)
        assertTrue(messages[0].timestamp > 0)
    }

    @Test
    fun `should return same messages for both sender and recipient roles`() = runTest {
        messageRepository.saveMessage(sender.id, recipient.id, "Hello!")
        messageRepository.saveMessage(recipient.id, sender.id, "Hi")
        val messagesBetweenAliceAndBob = messageRepository.getMessagesBetween(sender.id, recipient.id)
        val messagesBetweenBobAndAlice = messageRepository.getMessagesBetween(recipient.id, sender.id)

        assertEquals(2, messagesBetweenAliceAndBob.size)
        assertEquals(messagesBetweenAliceAndBob, messagesBetweenBobAndAlice)
    }

    @Test
    fun `should return empty list for non-existent conversation`() = runTest {
        val messages = messageRepository.getMessagesBetween(sender.id, recipient.id)
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `should get undelivered messages for user`()  = runTest {
        val message1 = messageRepository.saveMessage(sender.id, recipient.id, "First")
        val message2 = messageRepository.saveMessage(sender.id, recipient.id, "Second")
        val message3 = messageRepository.saveMessage(sender.id, recipient.id, "Third")

        messageRepository.markAsDelivered(message1.id)

        val undelivered = messageRepository.getUndeliveredMessagesFor(recipient.id)

        assertEquals(2, undelivered.size)
        assertEquals(message2.content, undelivered.first().content)
        assertEquals(message3.content, undelivered.last().content)
    }

    @Test
    fun `should mark message as delivered`() = runTest {
        val message = messageRepository.saveMessage(sender.id, recipient.id, "Hello!")

        val before = messageRepository.getUndeliveredMessagesFor(recipient.id)
        assertTrue(before.any { it.id == message.id })

        messageRepository.markAsDelivered(message.id)

        val after = messageRepository.getUndeliveredMessagesFor(recipient.id)
        assertFalse(after.any { it.id == message.id })
    }

    @Test
    fun `should return empty list if no undelivered messages`() = runTest {
        messageRepository.saveMessage(sender.id, recipient.id, "Delivered").also {
            messageRepository.markAsDelivered(it.id)
        }

        val undelivered = messageRepository.getUndeliveredMessagesFor(recipient.id)
        assertTrue(undelivered.isEmpty())
    }
}
