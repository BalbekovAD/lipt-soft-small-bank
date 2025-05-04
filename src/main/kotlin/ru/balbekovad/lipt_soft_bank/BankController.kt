package ru.balbekovad.lipt_soft_bank

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api")
class BankController(private val service: BankService) {

    @PostMapping("/clients")
    fun createClient(@RequestParam name: String) = service.createClient(name)

    @PostMapping("/clients/{id}/accounts")
    fun createAccount(
        @PathVariable id: Long,
        @RequestParam(name = "currency") currency: String,
        @RequestParam(name = "initial") initial: BigDecimal,
        @RequestParam(name = "number") number: String
    ) = service.createAccount(id, currency, initial, number)

    @GetMapping("/clients/{id}/accounts")
    fun getAccounts(@PathVariable id: Long) = service.getAccounts(id)

    @PostMapping("/transfer")
    fun transfer(
        @RequestParam(name = "from") from: String,
        @RequestParam(name = "to") to: String,
        @RequestParam(name = "amount") amount: BigDecimal,
        @RequestParam(name = "clientId") clientId: Long
    ) {
        service.transfer(from, to, amount, clientId)
    }
}
