package org.odanc.moneytransfer.services

import cats.effect.{Effect, IO}
import cats.syntax.all._
import fs2.async.mutable.Semaphore
import org.odanc.moneytransfer.models.{Error, _}

import scala.concurrent.ExecutionContext.Implicits.global

class TransactionService[F[_]] private(val service: AccountService[F])(implicit E: Effect[F]) {

  def processTransaction(transaction: Transaction): F[Either[Error, Transaction]] = {
    val from = transaction.from
    val to = transaction.to

    val fromNotFound = Either.left[Error, Transaction](NotFoundError(from))
    val toNotFound = Either.left[Error, Transaction](NotFoundError(to))

    service.getAccount(from) flatMap { isFromFound =>
      isFromFound.fold(fromNotFound.pure) { _ =>
        service.getAccount(to) flatMap { isToFound =>
          isToFound.fold(toNotFound.pure) { _ =>
            executeTransaction(transaction)
          }
        }
      }
    }
  }

  def executeTransaction(transaction: Transaction): F[Either[Error, Transaction]] = {
//    val transferAmountTask = for {
//      semaphore <- Semaphore[IO](1)
//      _ <- semaphore.decrement
//      transferAmount <- IO {
//        transferAmount(transaction)
//      }
//      _ <- semaphore.increment
//    } yield transferAmount
//
//    transferAmountTask.unsafeRunSync()

    transferAmount(transaction)
  }

  private def transferAmount(transaction: Transaction) = {
    val from = transaction.from
    val to = transaction.to
    val amount = transaction.amount

    service.getAccount(from) flatMap { fromAccountFound =>
      val fromAccount = fromAccountFound.get
      val notEnoughAmountError = Either.left[Error, Transaction](NotEnoughAmountError(from))

      if (fromAccount.amount < amount) notEnoughAmountError.pure
      else service.getAccount(to) flatMap { toAccountFound =>
        val toAccount = toAccountFound.get
        val fromAmount = fromAccount.amount
        val toAmount = toAccount.amount

//        service.addAccount(fromAccount.copy(amount = fromAmount - amount))
//        service.addAccount(toAccount.copy(amount = toAmount + amount))
        val newFromAccount = fromAccount.copy(name = "Kek")
        val newToAccount = toAccount.copy(name = "Lol")

        service.addAccount(AccountTemplate("Kek", BigDecimal("1000.01")))

//        service.addAccount(newFromAccount)
//        service.addAccount(newToAccount)

//        service.updateAccount(fromAccount, newFromAccount)
//        service.updateAccount(toAccount, newToAccount)

        transaction.asRight[Error].pure
      }
    }
  }
}



object TransactionService {

  def apply[F[_]](service: AccountService[F])(implicit E: Effect[F]): TransactionService[F] =
    new TransactionService[F](service)
}