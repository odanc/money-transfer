package org.odanc.moneytransfer.repository

import java.util.UUID

import cats.effect.{Effect, IO}
import cats.implicits._
import org.odanc.moneytransfer.models.{Account, AccountTemplate}

import scala.collection.concurrent.TrieMap

class AccountRepository[F[_]](private val storage: TrieMap[String, Account])(implicit E: Effect[F]) {
  private def generateId: F[String] = E.delay {
    UUID.randomUUID().toString
  }

  def addAccount(template: AccountTemplate): F[Account] = for {
    id <- generateId
    account <- E.pure(Account(id, template.name, template.amount))
    _ <- E.delay {
      storage.put(id, account)
    }
  } yield account

  def getAccount(id: String): F[Option[Account]] = E.delay {
      storage.get(id)
    }

  def updateAccounts(account1: Account, account2: Account): F[Unit] = E.delay {
    storage.+=((account1.id, account1), (account2.id, account2))
  }
}

object AccountRepository {
  def init[F[_]](implicit E: Effect[F]): IO[AccountRepository[F]] = IO {
    new AccountRepository[F](generateAccounts)
  }

  private def generateAccounts = {
    val account1 = Account("1", "John Doe", BigDecimal(100.0))
    val account2 = Account("2", "Jane Doe", BigDecimal(50.0))
    TrieMap((account1.id, account1), (account2.id, account2))
  }
}