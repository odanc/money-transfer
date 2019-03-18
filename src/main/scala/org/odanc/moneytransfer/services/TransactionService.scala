package org.odanc.moneytransfer.services

import cats.data.ValidatedNec
import cats.effect.{Effect, IO}
import cats.syntax.all._
import fs2.async.mutable.Semaphore
import io.chrisdavenport.fuuid.FUUID
import org.odanc.moneytransfer.models._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * A service for processing amount transactions between accounts
  *
  * @param service account service
  */
class TransactionService[F[_]] (private val service: AccountService[F])(implicit E: Effect[F]) {

  /**
    * Validates if given ids mathes for existing accounts
    *
    * @param fromId account's id
    * @param toId account's id
    * @return either an error containing all not existent accounts or a pair of retrieved accounts
    */
  def validateAccounts(fromId: FUUID, toId: FUUID): F[ValidatedNec[NotFoundError, (Account, Account)]] = {
    val fromNotFound = E.pure(NotFoundError(fromId).invalidNec[Account])
    val toNotFound = E.pure(NotFoundError(toId).invalidNec[Account])

    val validateFromId = service.getAccount(fromId) flatMap { isFromFound =>
      isFromFound.fold(fromNotFound)(fromFound => E.pure(fromFound.validNec))
    }

    val validateToId = service.getAccount(toId) flatMap { isToFound =>
      isToFound.fold(toNotFound)(toFound => E.pure(toFound.validNec))
    }

    validateFromId flatMap { validateFrom =>
      validateToId flatMap { validatedTo =>
        val validatedIds = (validateFrom, validatedTo) mapN {(_, _)}
        E.pure(validatedIds)
      }
    }
  }

  /**
    * Performs a given amount transfer between given accounts
    *
    * @param fromAccount account to tranfer amount from
    * @param toAccount account to transfer amount to
    * @param amount amount
    * @return either an error describing an account doesn't have enough amount to transfer
    *         or an account itself on transfer success
    */
  def transferAmount(fromAccount: Account, toAccount: Account, amount: BigDecimal): F[Either[NotEnoughAmountError, Account]] = {
    val transactionTask = for {
      semaphore <- Semaphore[IO](1)
      _ <- semaphore.decrement
      transactionResult <- executeTransaction(fromAccount, toAccount, amount)
      _ <- semaphore.increment
    } yield transactionResult

    transactionTask.unsafeRunSync()
  }

  private def executeTransaction(fromAccount: Account, toAccount: Account, amount: BigDecimal) = IO {
    val fromAmount = fromAccount.amount
    val notEnoughAmountError = Either.left[NotEnoughAmountError, Account](NotEnoughAmountError(fromAccount.id))

    if (fromAmount < amount) notEnoughAmountError.pure
    else {
      val toAmount = toAccount.amount

      val newFromAccount = fromAccount.copy(amount = fromAmount - amount)
      val newToAccount = toAccount.copy(amount = toAmount + amount)

      service.addAccounts(newFromAccount, newToAccount) flatMap { _ =>
        val updatedAccount = Either.right[NotEnoughAmountError, Account](newFromAccount)
        updatedAccount.pure
      }
    }
  }
}



object TransactionService {

  /**
    * Creates a new transaction service
    *
    * @param service account service
    * @return transaction service
    */
  def apply[F[_]](service: AccountService[F])(implicit E: Effect[F]): TransactionService[F] =
    new TransactionService[F](service)
}