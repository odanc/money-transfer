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

/**
  * Provides an endpoint for creating and executing amount transaction between accounts
  *
  * @param service transaction service
  */
class TransactionApi[F[_]] private(private val service: TransactionService[F])(implicit E: Effect[F]) extends Http4sDsl[F] {
  private val TRANSACTIONS = "transactions"

  private val createApi: HttpService[F] = HttpService[F] {

    /**
      * Matches for POST /transactions
      *
      * Accepts json body
      * {
      *   "from": "id1",
      *   "to": "id2",
      *   "amount": number
      * }
      *
      * Creates and executes an amount transactions between given accounts.
      * If no errors arise, returns an input json as a success flag
      */
    case request @ POST -> Root / TRANSACTIONS =>
      request.decode[Transaction] { transaction =>
        val amount = transaction.amount
        val fromId = transaction.from
        val toId = transaction.to

        val validatePositiveAmount: ValidatedNec[Error, BigDecimal] =
          if (amount > 0) amount.validNec else NonPositiveAmountError.invalidNec

        val validateSameIds: ValidatedNec[Error, (FUUID, FUUID)] =
          if (fromId =!= toId) (fromId, toId).validNec else SameAccountError.invalidNec

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


          case Valid(_) =>
            val validatedAccounts = service.validateAccounts(fromId, toId)

            validatedAccounts flatMap {

              case Invalid(errors) =>
                val errorMessages = errors.toList map { error =>
                  ErrorMessage(s"Account ${error.id} doesn't exist")
                }
                NotFound(errorMessages.asJson)

              case Valid((fromAccount, toAccount)) =>
                service.transferAmount(fromAccount, toAccount, amount) flatMap {

                  case Left(NotEnoughAmountError(id)) =>
                    val errorMessage = ErrorMessage(s"Account $id doesn't have enough amount")
                    PreconditionFailed(errorMessage.asJson)

                  case Right(_) => Ok(transaction.asJson)
              }
            }
        }
      }
  }
}



object TransactionApi {

  /**
    * Creates a provider for transactions api endpoint
    *
    * @param service transaction service
    * @return transaction api provider
    */
  def apply[F[_]](service: TransactionService[F])(implicit E: Effect[F]): HttpService[F] =
    new TransactionApi[F](service).createApi
}