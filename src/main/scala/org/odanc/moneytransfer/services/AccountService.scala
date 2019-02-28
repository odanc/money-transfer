package org.odanc.moneytransfer.services

import cats.effect.Effect
import cats.implicits._
import io.chrisdavenport.fuuid.circe._
import io.chrisdavenport.fuuid.http4s.FUUIDVar
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpService, Response}
import org.odanc.moneytransfer.models.{AccountTemplate, Transaction}
import org.odanc.moneytransfer.repository.AccountRepository

class AccountService[F[_]] private(private val repository: AccountRepository[F])(implicit E: Effect[F]) extends Http4sDsl[F] {

  private val service: HttpService[F] = HttpService[F] {

    case GET -> Root / "accounts" =>
      repository.getAccounts.flatMap {
        case Seq() => E.pure(Response(NoContent).withEmptyBody)
        case accounts => Response(Ok).withBody(accounts.asJson)
      }

    case GET -> Root / "accounts" / FUUIDVar(id) =>
      repository.getAccount(id) flatMap {
        case Some(account) => Response(Ok).withBody(account.asJson)
        case None => E.pure(Response(NotFound).withEmptyBody)
      }

    case request @ POST -> Root / "accounts" =>
      request.decode[AccountTemplate] { template =>
        repository.addAccount(template) flatMap { account =>
          Created(account.asJson)
        }
      }.handleErrorWith {
        case _: NumberFormatException => BadRequest("Amount is not numeric".asJson)
      }

    case request @ POST -> Root / "transaction" =>
      request.decode[Transaction] { transaction =>
        Created(transaction.asJson)
      }
  }
}



object AccountService {

  def apply[F[_]](repository: AccountRepository[F])(implicit E: Effect[F]): HttpService[F] =
    new AccountService[F](repository).service
}