package ru.balbekovad.lipt_soft_bank.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.balbekovad.lipt_soft_bank.entities.ClientEntity
import ru.balbekovad.lipt_soft_bank.ClientEntityNotFoundException

interface ClientRepository : JpaRepository<ClientEntity, Long>

operator fun ClientRepository.get(clientId: Long): ClientEntity =
    findById(clientId).orElseThrow { ClientEntityNotFoundException(clientId) }