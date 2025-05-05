package ru.balbekovad.lipt_soft_bank

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.Currency

@RestController
@RequestMapping("/api")
class BankController(private val service: BankService) {

    @PostMapping("/clients")
    fun createClient(@RequestParam name: String) = service.createClient(name)

    @PostMapping("/clients/{clientId}/accounts")
    fun createAccount(
        @PathVariable clientId: Long,
        @RequestParam(name = "currency") currency: Currency,
        @RequestParam(name = "initial") initial: BigDecimal,
        @RequestParam(name = "number") number: String
    ) = service.createAccount(clientId, currency, initial, number)

    @GetMapping("/clients/{clientId}/accounts")
    fun getAccounts(@PathVariable clientId: Long) = service.getAccounts(clientId)

    @PostMapping("/transfer")
    fun transfer(
        @RequestParam(name = "clientId") clientId: Long,
        @RequestParam(name = "from") from: String,
        @RequestParam(name = "to") to: String,
        @RequestParam(name = "amount") amount: BigDecimal
    ) {
        service.transfer(clientId, from, to, amount)
    }
}
