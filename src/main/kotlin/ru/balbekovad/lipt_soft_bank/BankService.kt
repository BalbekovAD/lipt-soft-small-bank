package ru.balbekovad.lipt_soft_bank

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class BankService(
    private val clientRepo: ClientRepository,
    private val accountRepo: AccountRepository
) {
    fun createClient(name: String): ClientEntity =
        clientRepo.save(ClientEntity(name = name))

    fun createAccount(clientId: Long, currency: String, initial: BigDecimal, number: String): AccountEntity {
        val client = clientRepo.findById(clientId)
            .orElseThrow { IllegalArgumentException("Client $clientId not found") }
        return accountRepo.save(
            AccountEntity(
                client = client,
                currency = currency,
                balance = initial,
                accountNumber = number
            )
        )
    }

    fun getAccounts(clientId: Long): List<AccountEntity> {
        if (!clientRepo.existsById(clientId)) throw IllegalArgumentException("Client not found")
        return accountRepo.findAllByClientId(clientId)
    }

    @Transactional
    fun transfer(fromNum: String, toNum: String, amount: BigDecimal, callerClientId: Long) {
        if (fromNum == toNum) throw IllegalArgumentException("Нельзя переводить на тот же счёт")
        val from = accountRepo.findByAccountNumberForUpdate(fromNum)
            ?: throw IllegalArgumentException("Счёт $fromNum не найден")
        val to = accountRepo.findByAccountNumberForUpdate(toNum)
            ?: throw IllegalArgumentException("Счёт $toNum не найден")
        if (from.currency != to.currency) throw IllegalArgumentException("Разные валюты")
        if (from.client.id != callerClientId) throw SecurityException("Нельзя переводить с чужого счёта")
        if (from.balance < amount) throw IllegalArgumentException("Недостаточно средств")
        from.balance = from.balance.subtract(amount)
        to.balance = to.balance.add(amount)
        accountRepo.saveAll(listOf(from, to))
    }
}
