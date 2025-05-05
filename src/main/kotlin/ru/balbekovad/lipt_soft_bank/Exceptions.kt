package ru.balbekovad.lipt_soft_bank

sealed class EntityNotFoundException(entityName: String, id: String): Exception("$entityName with id $id not found")
class ClientEntityNotFoundException(id: Long): EntityNotFoundException("ClientEntity", id.toString())
class AccountEntityNotFoundException(id: String): EntityNotFoundException("AccountEntity", id)