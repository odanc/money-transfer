package org.odanc.moneytransfer

import java.util.UUID

import cats.effect.IO
import io.chrisdavenport.fuuid.FUUID
import io.chrisdavenport.fuuid.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.Method.{GET, POST}
import org.http4s.Status.{BadRequest, Created, NotFound, Ok}
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.implicits._
import org.http4s.{Request, Uri}
import org.odanc.moneytransfer.api.AccountApi
import org.odanc.moneytransfer.models.{Account, AccountTemplate}
import org.odanc.moneytransfer.repository.AccountRepository
import org.odanc.moneytransfer.services.AccountService
import org.specs2.mutable.Specification

import scala.collection.concurrent.TrieMap

class AccountSpec extends Specification {

  "GET /accounts" >> {
    "should return all accounts" >> {
      shouldReturnAllAccounts()
    }
    "should return no accounts" >> {
      shouldReturnNoAccounts()
    }
  }

  "GET /accounts/28b523c1-b33a-4f1e-ada9-5c965da72c9c" >> {
    "should return an account" >> {
      shouldReturnAccount()
    }
  }

  "GET /accounts/notfound-b33a-4f1e-ada9-5c965da72c9c" >> {
    "should not return an account" >> {
      shouldReturnNotFoundAccount()
    }
  }

  "POST /accounts" >> {
    "should create an account" >> {
      shouldCreateAccount()
    }
    "should not create an account" >> {
      shouldNotCreateAccount()
    }
  }



  // Setup stubs
  private val id1 = FUUID.fromUUID(UUID.fromString("28b523c1-b33a-4f1e-ada9-5c965da72c9c"))
  private val id2 = FUUID.fromUUID(UUID.fromString("286a842b-6a61-4222-9df5-a491479dfb50"))
  private val account1 = Account(id1, "Tom", BigDecimal(10))
  private val account2 = Account(id2, "Jerry", BigDecimal(20))
  private val validTemplate = AccountTemplate("Spike", BigDecimal(1))
  private val invalidTemplate = AccountTemplate("Spike", BigDecimal(-1))
  private val accountRepository = new AccountRepository[IO](TrieMap(id1 -> account1, id2 -> account2))
  private val accountService = new AccountService[IO](accountRepository)



  // Setup endpoints
  private val getAllAccounts = Request[IO](GET, Uri.uri("/accounts"))
  private val getValidAccount = Request[IO](GET, Uri.uri("/accounts/28b523c1-b33a-4f1e-ada9-5c965da72c9c"))
  private val getNotFoundAccount = Request[IO](GET, Uri.uri("/accounts/notfound-b33a-4f1e-ada9-5c965da72c9c"))
  private val postValidAccount = Request[IO](POST, Uri.uri("/accounts")).withBody(validTemplate.asJson).unsafeRunSync()
  private val postInvalidAccount = Request[IO](POST, Uri.uri("/accounts")).withBody(invalidTemplate.asJson).unsafeRunSync()



  // Assertions
  private def shouldReturnAllAccounts() = {
    val response = AccountApi[IO](accountService).orNotFound(getAllAccounts).unsafeRunSync()

    response.as[Seq[Account]].unsafeRunSync() === Seq(account1, account2)
    response.status === Ok
  }

  private def shouldReturnNoAccounts() = {
    val repository = new AccountRepository[IO](TrieMap.empty[FUUID, Account])
    val service = new AccountService[IO](repository)
    val response = AccountApi[IO](service).orNotFound(getAllAccounts).unsafeRunSync()

    response.as[Seq[Account]].unsafeRunSync().isEmpty === true
    response.status === Ok
  }

  private def shouldReturnAccount() = {
    val response = AccountApi[IO](accountService).orNotFound(getValidAccount).unsafeRunSync()

    response.as[Account].unsafeRunSync() === account1
    response.status === Ok
  }

  private def shouldReturnNotFoundAccount() = {
    val response = AccountApi[IO](accountService).orNotFound(getNotFoundAccount).unsafeRunSync()

    response.status === NotFound
  }

  private def shouldNotCreateAccount() = {
    val response = AccountApi[IO](accountService).orNotFound(postInvalidAccount).unsafeRunSync()

    response.status === BadRequest
  }

  private def shouldCreateAccount() = {
    val response = AccountApi[IO](accountService).orNotFound(postValidAccount).unsafeRunSync()

    response.as[Account].unsafeRunSync().name === "Spike"
    response.status === Created

    val allAccountsResponse = AccountApi[IO](accountService).orNotFound(getAllAccounts).unsafeRunSync()

    allAccountsResponse.as[Seq[Account]].unsafeRunSync().size === 3
  }
}