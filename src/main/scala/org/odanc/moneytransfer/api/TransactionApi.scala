package org.odanc.moneytransfer.api

import cats.effect.Effect
import cats.implicits._
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
        val amount = transaction.amount
        if (amount <= 0) BadRequest("amount can't have non-positive value".asJson)
        else if (transaction.from === transaction.to) BadRequest("amount can't be transfered to the same account".asJson)
        else repository.getAccount(transaction.from) flatMap {
          case None => NotFound(s"account with id ${transaction.from} doesn't exist".asJson)
          case Some(from) => repository.getAccount(transaction.to) flatMap {
            case None => NotFound(s"account with id ${transaction.to} doesn't exist".asJson)
            case Some(to) =>
              if (from.amount < amount) BadRequest(s"account ${transaction.from} doesn't have enough amount".asJson)
              else {
                val fromAmount = from.amount - amount
                val toAmount = to.amount + amount
                repository.updateAccounts(from.copy(amount = fromAmount), to.copy(amount = toAmount)) flatMap { _ =>
                  Ok(transaction.asJson)
                }
              }
          }
        }
      }
  }
}



object TransactionApi {

  def apply[F[_]](repository: AccountRepository[F]) (implicit E: Effect[F]): HttpService[F] =
    new TransactionApi[F](repository).service
}