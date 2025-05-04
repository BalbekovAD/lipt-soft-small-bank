package ru.balbekovad.lipt_soft_bank

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ClientRepository : JpaRepository<ClientEntity, Long>

interface AccountRepository : JpaRepository<AccountEntity, Long> {
    fun findAllByClientId(clientId: Long): List<AccountEntity>
    fun findByAccountNumber(accountNumber: String): AccountEntity?
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountEntity a WHERE a.accountNumber = :number")
    fun findByAccountNumberForUpdate(@Param("number") number: String): AccountEntity?
}
