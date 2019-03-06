package org.odanc.moneytransfer.api

import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec
import cats.effect.Effect
import cats.syntax.all._
import io.chrisdavenport.fuuid.FUUID
import io.chrisdavenport.fuuid.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpService
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.odanc.moneytransfer.models._
import org.odanc.moneytransfer.services.TransactionService

class TransactionApi[F[_]] private(private val service: TransactionService[F])(implicit E: Effect[F]) extends Http4sDsl[F] {
  private val TRANSACTIONS = "transactions"

  private val createApi: HttpService[F] = HttpService[F] {

    case request @ POST -> Root / TRANSACTIONS =>
      request.decode[Transaction] { transaction =>
        val amount = transaction.amount
        val from = transaction.from
        val to = transaction.to

        val validatePositiveAmount: ValidatedNec[Error, BigDecimal] =
          if (amount > 0) amount.validNec else NonPositiveAmountError.invalidNec

        val validateSameIds: ValidatedNec[Error, (FUUID, FUUID)] =
          if (from =!= to) (from, to).validNec else SameAccountError.invalidNec

        val validatedTransaction =
          (validatePositiveAmount, validateSameIds) mapN { (amount, fromTo) =>
            Transaction(fromTo._1, fromTo._2, amount)
          }

        validatedTransaction match {

          case Invalid(errors) =>
            val errorMessages = errors.toList map {
              case NonPositiveAmountError => ErrorMessage("Amount can't be zero or negative")
              case SameAccountError => ErrorMessage("Transactions to the same account are not allowed")
            }
            BadRequest(errorMessages.asJson)

          case Valid(validTransaction) =>
            service.processTransaction(validTransaction) flatMap {
              case Left(error) => error match {
                case NotFoundError(id) => NotFound(ErrorMessage(s"Account $id doesn't exist").asJson)
                case NotEnoughAmountError(id) => PreconditionFailed(ErrorMessage(s"Account $id doesn't have enough amount").asJson)
              }
              case Right(_) => Ok(transaction.asJson)
            }
        }
      }
  }
}



object TransactionApi {

  def apply[F[_]](service: TransactionService[F])(implicit E: Effect[F]): HttpService[F] =
    new TransactionApi[F](service).createApi
}