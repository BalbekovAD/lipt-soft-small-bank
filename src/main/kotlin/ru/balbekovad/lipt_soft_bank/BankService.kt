package ru.balbekovad.lipt_soft_bank

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.balbekovad.lipt_soft_bank.entities.AccountEntity
import ru.balbekovad.lipt_soft_bank.entities.ClientEntity
import ru.balbekovad.lipt_soft_bank.repository.AccountRepository
import ru.balbekovad.lipt_soft_bank.repository.ClientRepository
import ru.balbekovad.lipt_soft_bank.repository.get
import ru.balbekovad.lipt_soft_bank.repository.saveAll
import java.math.BigDecimal
import java.util.Currency

@Service
class BankService(
    private val clientRepo: ClientRepository,
    private val accountRepo: AccountRepository
) {
    fun createClient(name: String): ClientEntity =
        clientRepo.save(ClientEntity(name = name))

    fun createAccount(clientId: Long, currency: Currency, initial: BigDecimal, number: String): AccountEntity =
        accountRepo.save(
            AccountEntity(
                client = clientRepo[clientId],
                currency = currency,
                balance = initial,
                accountNumber = number
            )
        )

    fun getAccounts(clientId: Long): List<AccountEntity> {
        require(clientRepo.existsById(clientId)) { "Client not found" }
        return accountRepo.findAllByClientId(clientId)
    }

    @Transactional
    fun transfer(clientId: Long, fromNum: String, toNum: String, amount: BigDecimal) {
        require(fromNum != toNum) { "Can't transfer between same accounts" }

        val from = accountRepo.findByAccountNumberForUpdate(fromNum) ?: throw AccountEntityNotFoundException(fromNum)
        val to = accountRepo.findByAccountNumberForUpdate(toNum) ?: throw AccountEntityNotFoundException(toNum)

        require(from.currency == to.currency) { "Can't transfer between accounts with different currencies" }
        if (from.client.id != clientId) {
            throw SecurityException("Can't transfer from account of another client")
        }
        require(from.balance >= amount) { "Not enough money for transfer" }

        from.balance -= amount
        to.balance += amount

        accountRepo.saveAll(from, to)
    }
}
