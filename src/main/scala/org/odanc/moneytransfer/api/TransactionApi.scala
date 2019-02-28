package org.odanc.moneytransfer.api

import cats.effect.Effect
import io.chrisdavenport.fuuid.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpService
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.odanc.moneytransfer.models.Transaction
import org.odanc.moneytransfer.repository.AccountRepository

class TransactionApi[F[_]] private(private val repository: AccountRepository[F])(implicit E: Effect[F]) extends Http4sDsl[F] {
  private val TRANSACTIONS = "transactions"

  private val service: HttpService[F] = HttpService[F] {

    case request @ POST -> Root / TRANSACTIONS =>
      request.decode[Transaction] { transaction =>
        Created(transaction.asJson)
      }
  }
}



object TransactionApi {

  def apply[F[_]](repository: AccountRepository[F]) (implicit E: Effect[F]): HttpService[F] =
    new TransactionApi[F](repository).service
}