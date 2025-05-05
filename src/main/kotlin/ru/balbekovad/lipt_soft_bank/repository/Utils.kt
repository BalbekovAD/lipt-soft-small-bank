package ru.balbekovad.lipt_soft_bank.repository

import org.springframework.data.jpa.repository.JpaRepository

fun <T> JpaRepository<T, *>.saveAll(vararg entities: T): List<T> = saveAll(IterableArray(entities))

class IterableArray<out T>(private val entities: Array<T>) : Iterable<T> {
    override fun iterator(): Iterator<T> = entities.iterator()
}