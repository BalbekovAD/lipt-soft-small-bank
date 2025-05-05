package ru.balbekovad.lipt_soft_bank.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
import ru.balbekovad.lipt_soft_bank.BankService
import ru.balbekovad.lipt_soft_bank.entities.AccountEntity
import ru.balbekovad.lipt_soft_bank.repository.AccountRepository
import ru.balbekovad.lipt_soft_bank.repository.ClientRepository
import java.math.BigDecimal
import java.util.*
import kotlin.test.assertEquals

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BankServiceTest {

    @Autowired
    private lateinit var bankService: BankService

    @Autowired
    private lateinit var clientRepo: ClientRepository

    @Autowired
    private lateinit var accountRepo: AccountRepository

    @BeforeEach
    fun cleanDatabase() {
        accountRepo.deleteAll()
        clientRepo.deleteAll()
    }

    @Test
    fun `createClient should persist a new client`() {
        val client = bankService.createClient("Alice")
        assertThat(client.id).isGreaterThan(0)
        assertThat(client.name).isEqualTo("Alice")

        val found = clientRepo.findById(client.id)
        assertThat(found).isPresent
        assertThat(found.get().name).isEqualTo("Alice")
    }

    @Test
    fun `createAccount should persist a new account linked to existing client`() {
        val client = bankService.createClient("Bob")
        val currency = Currency.getInstance(Locale.US)
        val expectedBalance = BigDecimal(100)
        val expectedAccountNumber = "ACC-123"
        val account = bankService.createAccount(
            clientId = client.id,
            currency = currency,
            initial = expectedBalance,
            number = expectedAccountNumber
        )

        assertThat(account.id).isGreaterThan(0)
        assertThat(account.currency).isEqualTo(currency)
        assertEquals(expectedBalance, account.balance)
        assertThat(account.balance).isEqualTo(expectedBalance)
        assertThat(account.accountNumber).isEqualTo(expectedAccountNumber)
        assertThat(account.client.id).isEqualTo(client.id)

        val list = accountRepo.findAllByClientId(client.id)
        assertThat(list).hasSize(1)
        assertThat(list.single().accountNumber).isEqualTo(expectedAccountNumber)
    }

    @Test
    fun `getAccounts should return all accounts for a client`() {
        val client = bankService.createClient("Carol")
        val currency = Currency.getInstance(Locale.US)

        val expectedNumbers = arrayOf("E-1", "E-2")

        bankService.createAccount(client.id, currency, BigDecimal(50), expectedNumbers[0])
        bankService.createAccount(client.id, currency, BigDecimal(75), expectedNumbers[1])

        val accounts = bankService.getAccounts(client.id)
        assertThat(accounts).apply {
            hasSize(2)
            map<String>(AccountEntity::accountNumber).containsExactlyInAnyOrder(*expectedNumbers)
        }
    }

    @Test
    fun `transfer should move funds between two accounts`() {
        val client = bankService.createClient("Dave")
        val currency = Currency.getInstance(Locale.US)
        val numbers = listOf("D-1", "D-2")
        bankService.createAccount(client.id, currency, BigDecimal(200), numbers[0])
        bankService.createAccount(client.id, currency, BigDecimal(100), numbers[1])

        bankService.transfer(clientId = client.id, fromNum = numbers[0], toNum = numbers[1], amount = BigDecimal(50))

        val from = accountRepo.findByAccountNumber(numbers[0])!!
        val to = accountRepo.findByAccountNumber(numbers[1])!!
        assertThat(from.balance).isEqualByComparingTo("150.00")
        assertThat(to.balance).isEqualByComparingTo("150.00")
    }

    @Test
    fun `transfer should fail when different currencies`() {
        val client = bankService.createClient("Eve")
        bankService.createAccount(client.id, Currency.getInstance(Locale.US), BigDecimal(100), "X-1")
        bankService.createAccount(client.id, Currency.getInstance(Locale.of("ru", "RU")), BigDecimal(100), "X-2")

        assertThrows<IllegalArgumentException> {
            bankService.transfer(client.id, "X-1", "X-2", BigDecimal(10))
        }
    }

    @Test
    fun `transfer should fail when insufficient funds`() {
        val client = bankService.createClient("Frank")
        val currency = Currency.getInstance(Locale.US)
        bankService.createAccount(client.id, currency, BigDecimal(20), "F-1")
        bankService.createAccount(client.id, currency, BigDecimal(0), "F-2")

        assertThrows<IllegalArgumentException> {
            bankService.transfer(client.id, "F-1", "F-2", BigDecimal(50))
        }
    }

    @Test
    fun `transfer should fail when transferring to same account`() {
        val client = bankService.createClient("Grace")
        bankService.createAccount(client.id, Currency.getInstance(Locale.US), BigDecimal(100), "G-1")

        assertThrows<IllegalArgumentException> {
            bankService.transfer(client.id, "G-1", "G-1", BigDecimal(10))
        }
    }

    @Test
    fun `transfer should fail when transferring from another client's account`() {
        val c1 = bankService.createClient("Hank")
        val c2 = bankService.createClient("Ivy")
        val currency = Currency.getInstance(Locale.US)
        bankService.createAccount(c1.id, currency, BigDecimal(100), "H-1")
        bankService.createAccount(c2.id, currency, BigDecimal(100), "I-1")

        assertThrows<SecurityException> {
            bankService.transfer(clientId = c2.id, "H-1", "I-1", BigDecimal(10))
        }
    }

    @Test
    @Transactional
    fun `concurrent transfers should not violate balance constraints`() {
        val client = bankService.createClient("Jack")
        val currency = Currency.getInstance(Locale.US)
        bankService.createAccount(client.id, currency, BigDecimal(1000), "J-1")
        bankService.createAccount(client.id, currency, BigDecimal(0), "J-2")

        TestTransaction.flagForCommit()
        TestTransaction.end()

        val threads = (1..10).map {
            Thread {
                bankService.transfer(client.id, "J-1", "J-2", BigDecimal(50))
            }
        }
        threads.forEach(Thread::start)
        threads.forEach(Thread::join)

        TestTransaction.start()

        val from = accountRepo.findByAccountNumber("J-1")!!
        val to = accountRepo.findByAccountNumber("J-2")!!
        assertThat(from.balance).isEqualByComparingTo("500.00")
        assertThat(to.balance).isEqualByComparingTo("500.00")
    }
}
