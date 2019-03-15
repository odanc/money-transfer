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


          case Valid(_) =>
            val accountService = service.service

            val check1 = accountService.getAccount(from) flatMap { isFromFound =>
              isFromFound.fold(E.pure(NotFoundError(from).invalidNec[Account])) { fromFound =>
                E.pure(fromFound.validNec)
              }
            }

            val check2 = accountService.getAccount(to) flatMap { isToFound =>
              isToFound.fold(E.pure(NotFoundError(to).invalidNec[Account])) { toFound =>
                E.pure(toFound.validNec)
              }
            }

            val checked: F[ValidatedNec[NotFoundError, (Account, Account)]] = check1 flatMap { ch1 =>
              check2 flatMap { ch2 =>
                E.pure {
                  (ch1, ch2) mapN { (fromFound, toFound) =>
                    (fromFound, toFound)
                  }
                }
              }
            }

            checked flatMap {
              case Invalid(errors) =>
                val errorMessages = errors.toList map { error =>
                  ErrorMessage(s"Account ${error.id} doesn't exist")
                }
                NotFound(errorMessages.asJson)

              case Valid((fromAccount, toAccount)) =>
                val fromAmount = fromAccount.amount

                if (fromAmount < amount) PreconditionFailed("notEnoughMoney".asJson)
                else {
                  val toAmount = toAccount.amount

                  val newFromAccount = fromAccount.copy(amount = fromAmount - amount)
                  val newToAccount = toAccount.copy(amount = toAmount + amount)

//                  accountService.addAccount(newFromAccount) flatMap { _ =>
//                    accountService.addAccount(newToAccount) flatMap { _ =>
//                      Ok("cool".asJson)
//                    }
//                  }

                  accountService.addAccounts(newFromAccount, newToAccount) flatMap { _ =>
                    Ok(transaction.asJson)
                  }
                }
            }
        }
      }
  }
}



object TransactionApi {

  def apply[F[_]](service: TransactionService[F])(implicit E: Effect[F]): HttpService[F] =
    new TransactionApi[F](service).createApi
}