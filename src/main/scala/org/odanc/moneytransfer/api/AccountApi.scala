package org.odanc.moneytransfer.api

import cats.effect.Effect
import cats.implicits._
import io.chrisdavenport.fuuid.circe._
import io.chrisdavenport.fuuid.http4s.FUUIDVar
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpService
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.odanc.moneytransfer.models.{AccountTemplate, ErrorMessage}
import org.odanc.moneytransfer.services.AccountService

/**
  * Provides endpoints for retrieving a given account (by id), all accounts and for creating a new account
  *
  * @param service account service
  */
class AccountApi[F[_]] (private val service: AccountService[F])(implicit E: Effect[F]) extends Http4sDsl[F] {
  private val ACCOUNTS = "accounts"

  private val createApi: HttpService[F] = HttpService[F] {

    /**
      * Matches for GET /accounts
      * Retrieves a list of all accounts
      */
    case GET -> Root / ACCOUNTS =>
      service.getAccounts flatMap (accounts => Ok(accounts.asJson))

    /**
      * Matches for GET /accounts/{id}
      * Retrieves an account by given id if it does exist
      */
    case GET -> Root / ACCOUNTS / FUUIDVar(id) =>
      service.getAccount(id) flatMap { isAccountFound =>
        val notFoundErrorMessage = ErrorMessage(s"Account $id doesn't exist")
        isAccountFound.fold(NotFound(notFoundErrorMessage.asJson)) { foundAccount =>
          Ok(foundAccount.asJson)
        }
      }

    /**
      * Matches for POST /accounts
      *
      * Accepts json body
      * {
      *   "name": "name",
      *   "amount": number
      * }
      *
      * Creates and retrieves a new account from input body
      */
    case request @ POST -> Root / ACCOUNTS =>
      request.decode[AccountTemplate] { template =>
        if (template.amount < 0) {
          val negativeAmountErrorMessage = ErrorMessage("Amount can't be negative")
          BadRequest(negativeAmountErrorMessage.asJson)
        }
        else service.addAccount(template) flatMap { createdAccount =>
          Created(createdAccount.asJson)
        }
      }
  }
}



object AccountApi {

  /**
    * Creates a provider for accounts api endpoints
    *
    * @param service account service
    * @return accounts api provider
    */
  def apply[F[_]](service: AccountService[F])(implicit E: Effect[F]): HttpService[F] =
    new AccountApi[F](service).createApi
}