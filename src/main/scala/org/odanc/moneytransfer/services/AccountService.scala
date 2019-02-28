package org.odanc.moneytransfer.services

import cats.effect.Effect
import cats.implicits._
import io.chrisdavenport.fuuid.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpService, Response}
import org.odanc.moneytransfer.repository.AccountRepository

class AccountService[F[_]] private(private val repository: AccountRepository[F])(implicit E: Effect[F]) extends Http4sDsl[F] {

  private val service: HttpService[F] = HttpService[F] {

    case GET -> Root / "accounts" =>
      repository.getAccounts.flatMap {
        case Seq() => E.pure(Response.notFound[F])
        case accounts => Response(status = Ok).withBody(accounts.asJson)
      }
  }
}



object AccountService {

  def apply[F[_]](repository: AccountRepository[F])(implicit E: Effect[F]): HttpService[F] =
    new AccountService[F](repository).service
}