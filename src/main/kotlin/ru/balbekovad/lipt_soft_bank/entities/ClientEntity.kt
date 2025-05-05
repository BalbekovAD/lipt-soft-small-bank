package ru.balbekovad.lipt_soft_bank.entities

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.proxy.HibernateProxy

@Entity
@Table(name = "client")
data class ClientEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    val name: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val oEffectiveClass =
            if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
        val thisEffectiveClass =
            this.javaClass
        if (thisEffectiveClass != oEffectiveClass) return false
        other as ClientEntity

        return id == other.id
    }

    override fun hashCode(): Int =
        javaClass.hashCode()

    @Override
    override fun toString(): String = this::class.simpleName + "(  id = $id   ,   name = $name )"
}