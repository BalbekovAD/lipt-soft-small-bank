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
import ru.balbekovad.lipt_soft_bank.AccountRepository
import ru.balbekovad.lipt_soft_bank.BankService
import ru.balbekovad.lipt_soft_bank.ClientRepository
import java.math.BigDecimal

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
        val account = bankService.createAccount(
            clientId   = client.id,
            currency   = "USD",
            initial    = BigDecimal("100.00"),
            number     = "ACC-123"
        )

        assertThat(account.id).isGreaterThan(0)
        assertThat(account.currency).isEqualTo("USD")
        assertThat(account.balance).isEqualByComparingTo("100.00")
        assertThat(account.accountNumber).isEqualTo("ACC-123")
        assertThat(account.client.id).isEqualTo(client.id)

        val list = accountRepo.findAllByClientId(client.id)
        assertThat(list).hasSize(1)
        assertThat(list[0].accountNumber).isEqualTo("ACC-123")
    }

    @Test
    fun `getAccounts should return all accounts for a client`() {
        val client = bankService.createClient("Carol")
        bankService.createAccount(client.id, "EUR", BigDecimal("50"), "E-1")
        bankService.createAccount(client.id, "EUR", BigDecimal("75"), "E-2")

        val accounts = bankService.getAccounts(client.id)
        assertThat(accounts).hasSize(2)
        val numbers = accounts.map { it.accountNumber }
        assertThat(numbers).containsExactlyInAnyOrder("E-1", "E-2")
    }

    @Test
    fun `transfer should move funds between two accounts`() {
        val client = bankService.createClient("Dave")
        bankService.createAccount(client.id, "USD", BigDecimal("200"), "D-1")
        bankService.createAccount(client.id, "USD", BigDecimal("100"), "D-2")

        bankService.transfer(fromNum = "D-1", toNum = "D-2", amount = BigDecimal("50"), callerClientId = client.id)

        val from = accountRepo.findByAccountNumber("D-1")!!
        val to   = accountRepo.findByAccountNumber("D-2")!!
        assertThat(from.balance).isEqualByComparingTo("150.00")
        assertThat(to.balance).isEqualByComparingTo("150.00")
    }

    @Test
    fun `transfer should fail when different currencies`() {
        val client = bankService.createClient("Eve")
        bankService.createAccount(client.id, "USD", BigDecimal("100"), "X-1")
        bankService.createAccount(client.id, "EUR", BigDecimal("100"), "X-2")

        assertThrows<IllegalArgumentException> {
            bankService.transfer("X-1", "X-2", BigDecimal("10"), client.id)
        }
    }

    @Test
    fun `transfer should fail when insufficient funds`() {
        val client = bankService.createClient("Frank")
        bankService.createAccount(client.id, "USD", BigDecimal("20"), "F-1")
        bankService.createAccount(client.id, "USD", BigDecimal("0"), "F-2")

        assertThrows<IllegalArgumentException> {
            bankService.transfer("F-1", "F-2", BigDecimal("50"), client.id)
        }
    }

    @Test
    fun `transfer should fail when transferring to same account`() {
        val client = bankService.createClient("Grace")
        bankService.createAccount(client.id, "USD", BigDecimal("100"), "G-1")

        assertThrows<IllegalArgumentException> {
            bankService.transfer("G-1", "G-1", BigDecimal("10"), client.id)
        }
    }

    @Test
    fun `transfer should fail when transferring from another client's account`() {
        val c1 = bankService.createClient("Hank")
        val c2 = bankService.createClient("Ivy")
        bankService.createAccount(c1.id, "USD", BigDecimal("100"), "H-1")
        bankService.createAccount(c2.id, "USD", BigDecimal("100"), "I-1")

        assertThrows<SecurityException> {
            bankService.transfer("H-1", "I-1", BigDecimal("10"), callerClientId = c2.id)
        }
    }

    @Test
    @Transactional
    fun `concurrent transfers should not violate balance constraints`() {
        val client = bankService.createClient("Jack")
        bankService.createAccount(client.id, "USD", BigDecimal("1000"), "J-1")
        bankService.createAccount(client.id, "USD", BigDecimal("0"),    "J-2")

        TestTransaction.flagForCommit()
        TestTransaction.end()

        val threads = (1..10).map {
            Thread {
                bankService.transfer("J-1", "J-2", BigDecimal("50"), client.id)
            }
        }
        threads.forEach(Thread::start)
        threads.forEach(Thread::join)

        TestTransaction.start()

        val from = accountRepo.findByAccountNumber("J-1")!!
        val to   = accountRepo.findByAccountNumber("J-2")!!
        assertThat(from.balance).isEqualByComparingTo("500.00")
        assertThat(to.balance).isEqualByComparingTo("500.00")
    }
}
