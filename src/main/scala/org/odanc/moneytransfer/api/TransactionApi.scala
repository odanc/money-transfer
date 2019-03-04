package org.odanc.moneytransfer.api

import cats.data.Validated.{Invalid, Valid}
import cats.effect.concurrent.Semaphore
import cats.effect.{Effect, IO, _}
import cats.syntax.all._
import io.chrisdavenport.fuuid.circe._
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpService, Response}
import org.odanc.moneytransfer.models._
import org.odanc.moneytransfer.services.AccountService

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class TransactionApi[F[_]] private(private val service: AccountService[F])(implicit E: Effect[F], ec: ExecutionContext) extends Http4sDsl[F] {
  private val TRANSACTIONS = "transactions"
  private implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)

  private val createApi: HttpService[F] = HttpService[F] {

    case request @ POST -> Root / TRANSACTIONS =>
      request.decode[Transaction] { transaction =>
        val amount = transaction.amount
        val from = transaction.from
        val to = transaction.to

        val validateAmount = if (amount > 0) amount.validNec else Error("Non-positive amount").invalidNec
        val validateIds = if (from =!= to) (from, to).validNec else Error("Same account").invalidNec

        val validated = (validateAmount, validateIds) mapN { (amount, fromTo) =>
          Transaction(fromTo._1, fromTo._2, amount)
        }

        validated match {
          case Invalid(e) => BadRequest(e)
          case Valid(a) => processTransaction(a)
        }
      }
  }

  private def processTransaction(transaction: Transaction): F[Response[F]] = {
    val first = transaction.from
    val second = transaction.to

    service.getAccount(first) flatMap { maybeFoundFrom =>
      maybeFoundFrom.fold(NotFound(NotFoundError(first))) { _ =>
        service.getAccount(second) flatMap { maybeFoundTo =>
          maybeFoundTo.fold(NotFound(NotFoundError(second))) { _ =>
            processUpdateAccounts(transaction)
          }
        }
      }
    }
  }

  private def processUpdateAccounts(transaction: Transaction): F[Response[F]] = {
    val from = transaction.from
    val to = transaction.to
    val amount = transaction.amount

    for {
      s <- Semaphore[IO](1)
      _ <- s.acquire
      fromAcc: Option[Account] <- service.getAccount(from)
      fromAccount: Account = fromAcc.get
      toAcc: Option[Account] <- service.getAccount(to)
      toAccount = toAcc.get
      fromAccAmount = fromAccount.amount
      toAccAmount = toAccount.amount
      newFromAcc = fromAccount.copy(amount = fromAccAmount - amount)
      newToAcc = toAccount.copy(amount = toAccAmount + amount)
      updated1 <- service.addAccount(newFromAcc)
      updated2 <- service.addAccount(newToAcc)
      _ <- s.release
    } yield updated1

    Ok(transaction)
  }
}



object TransactionApi {

  def apply[F[_]](service: AccountService[F])(implicit E: Effect[F]): HttpService[F] =
    new TransactionApi[F](service).createApi
}