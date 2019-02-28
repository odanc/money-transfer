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
import org.odanc.moneytransfer.models.AccountTemplate
import org.odanc.moneytransfer.repository.AccountRepository

class AccountApi[F[_]] private(private val repository: AccountRepository[F])(implicit E: Effect[F]) extends Http4sDsl[F] {
  private val ACCOUNTS = "accounts"

  private val service: HttpService[F] = HttpService[F] {

    case GET -> Root / ACCOUNTS =>
      repository.getAccounts.flatMap {
        case Seq() => NoContent()
        case accounts => Ok(accounts.asJson)
      }

    case GET -> Root / ACCOUNTS / FUUIDVar(id) =>
      repository.getAccount(id) flatMap {
        case Some(account) => Ok(account.asJson)
        case None => NotFound()
      }

    case request @ POST -> Root / ACCOUNTS =>
      request.decode[AccountTemplate] { template =>
        repository.addAccount(template) flatMap { account =>
          Created(account.asJson)
        }
      }.handleErrorWith {
        case _: NumberFormatException => BadRequest("Amount is not numeric".asJson)
      }
  }
}



object AccountApi {

  def apply[F[_]](repository: AccountRepository[F])(implicit E: Effect[F]): HttpService[F] =
    new AccountApi[F](repository).service
}