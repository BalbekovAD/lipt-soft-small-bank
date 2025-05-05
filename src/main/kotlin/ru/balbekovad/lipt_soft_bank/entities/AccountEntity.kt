package ru.balbekovad.lipt_soft_bank.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.proxy.HibernateProxy
import java.math.BigDecimal
import java.util.Currency

@Entity
@Table(name = "account")
data class AccountEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "client_id") val client: ClientEntity,
    val currency: Currency,
    var balance: BigDecimal,
    @Column(name = "account_number", unique = true) val accountNumber: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val oEffectiveClass =
            if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
        if (javaClass != oEffectiveClass) return false
        other as AccountEntity

        return id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String = this::class.simpleName + "(  id = $id   ,   currency = $currency   ,   balance = $balance   ,   accountNumber = $accountNumber )"
}
