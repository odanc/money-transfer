package org.odanc.moneytransfer

import java.util.UUID

import cats.effect.IO
import io.chrisdavenport.fuuid.FUUID
import io.chrisdavenport.fuuid.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.Method.POST
import org.http4s.Status.{BadRequest, NotFound, Ok, PreconditionFailed}
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.implicits._
import org.http4s.{Request, Uri}
import org.odanc.moneytransfer.api.TransactionApi
import org.odanc.moneytransfer.models.{Account, ErrorMessage, Transaction}
import org.odanc.moneytransfer.repository.AccountRepository
import org.odanc.moneytransfer.services.{AccountService, TransactionService}
import org.specs2.mutable.Specification

import scala.collection.concurrent.TrieMap

class TransactionSpec extends Specification {

  "POST /transactions" >> {
    "should fail transaction with bad request" >> {
      shouldFailTransactionWithBadRequest()
    }
    "should fail transaction with account not found" >> {
      shouldFailTransactionWithAccountNotFound()
    }
    "should fail transaction with not enough amount" >> {
      shouldFailTransactionWithNotEnoughAmount()
    }
    "should transfer amount between accounts" >> {
      shouldTransferAmount()
    }
  }



  // Setup stubs
  private val id1 = FUUID.fromUUID(UUID.fromString("28b523c1-b33a-4f1e-ada9-5c965da72c9c"))
  private val id2 = FUUID.fromUUID(UUID.fromString("286a842b-6a61-4222-9df5-a491479dfb50"))
  private val notFoundId = FUUID.fromUUID(UUID.fromString("397b951c-b33a-4f1e-ada9-5c965da72c9c"))
  private val account1 = Account(id1, "Tom", BigDecimal(10))
  private val account2 = Account(id2, "Jerry", BigDecimal(20))
  private val accountRepository = new AccountRepository[IO](TrieMap(id1 -> account1, id2 -> account2))
  private val accountService = new AccountService[IO](accountRepository)
  private val transactionService = new TransactionService[IO](accountService)
  private val validTransaction = Transaction(id1, id2, BigDecimal(10))
  private val invalidTransaction = Transaction(id1, id1, BigDecimal(-1))
  private val accountNotFoundTransaction = Transaction(id1, notFoundId, BigDecimal(10))
  private val notEnoughAmountTransaction = Transaction(id1, id2, BigDecimal(100))



  // Setup endpoints
  private val postInvalidTransaction = Request[IO](POST, Uri.uri("/transactions")).withBody(invalidTransaction.asJson).unsafeRunSync()
  private val postAccountNotFoundTransaction = Request[IO](POST, Uri.uri("/transactions")).withBody(accountNotFoundTransaction.asJson).unsafeRunSync()
  private val postNotEnoughAmountTransaction = Request[IO](POST, Uri.uri("/transactions")).withBody(notEnoughAmountTransaction.asJson).unsafeRunSync()
  private val postValidTransaction = Request[IO](POST, Uri.uri("/transactions")).withBody(validTransaction.asJson).unsafeRunSync()



  // Assertions
  private def shouldFailTransactionWithBadRequest() = {
    val response = TransactionApi[IO](transactionService).orNotFound(postInvalidTransaction).unsafeRunSync()

    response.as[Seq[ErrorMessage]].unsafeRunSync().size === 2
    response.status === BadRequest
  }

  private def shouldFailTransactionWithAccountNotFound() = {
    val response = TransactionApi[IO](transactionService).orNotFound(postAccountNotFoundTransaction).unsafeRunSync()

    response.as[Seq[ErrorMessage]].unsafeRunSync().size === 1
    response.status === NotFound
  }

  private def shouldFailTransactionWithNotEnoughAmount() = {
    val response = TransactionApi[IO](transactionService).orNotFound(postNotEnoughAmountTransaction).unsafeRunSync()

    response.as[ErrorMessage].unsafeRunSync().message must endWith("enough amount")
    response.status === PreconditionFailed
  }

  private def shouldTransferAmount() = {
    val response = TransactionApi[IO](transactionService).orNotFound(postValidTransaction).unsafeRunSync()

    response.status === Ok

    accountService.getAccount(id1).unsafeRunSync().fold(false) { _ === account1.copy(amount = BigDecimal(0)) }
    accountService.getAccount(id2).unsafeRunSync().fold(false) { _ === account2.copy(amount = BigDecimal(30)) }
  }
}