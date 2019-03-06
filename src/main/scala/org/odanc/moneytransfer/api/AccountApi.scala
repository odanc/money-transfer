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

class AccountApi[F[_]] private(private val service: AccountService[F])(implicit E: Effect[F]) extends Http4sDsl[F] {
  private val ACCOUNTS = "accounts"

  private val createApi: HttpService[F] = HttpService[F] {

    case GET -> Root / ACCOUNTS =>
      service.getAccounts flatMap {
        case Seq() => NoContent()
        case accounts => Ok(accounts.asJson)
      }

    case GET -> Root / ACCOUNTS / FUUIDVar(id) =>
      service.getAccount(id) flatMap { maybeFound =>
        maybeFound.fold(NotFound(ErrorMessage(s"Account $id doesn't exist").asJson)) { found =>
          Ok(found.asJson)
        }
      }

    case request @ POST -> Root / ACCOUNTS =>
      request.decode[AccountTemplate] { template =>
        if (template.amount < 0) BadRequest(ErrorMessage("Amount can't be negative").asJson)
        else service.addAccount(template) flatMap { account =>
          Created(account.asJson)
        }
      }
  }
}



object AccountApi {

  def apply[F[_]](service: AccountService[F])(implicit E: Effect[F]): HttpService[F] =
    new AccountApi[F](service).createApi
}