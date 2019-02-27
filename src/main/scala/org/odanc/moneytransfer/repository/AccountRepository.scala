package org.odanc.moneytransfer.repository

import cats.effect.{Effect, IO}
import cats.implicits._
import io.chrisdavenport.fuuid.FUUID
import org.odanc.moneytransfer.models.{Account, AccountTemplate}

import scala.collection.concurrent.{TrieMap, Map => CMap}

class AccountRepository[F[_]](private val storage: CMap[FUUID, Account])(implicit E: Effect[F]) {

  def addAccount(template: AccountTemplate): F[Account] = for {
    id <- FUUID.randomFUUID
    account = Account(id, template.name, template.amount)
    _ <- E.delay {
      storage.put(id, account)
    }
  } yield account

  def getAccount(id: FUUID): F[Option[Account]] = E.delay {
      storage.get(id)
    }

  def updateAccounts(account1: Account, account2: Account): F[Unit] = E.delay {
    storage.+=((account1.id, account1), (account2.id, account2))
  }
}

object AccountRepository {
  def init[F[_]](implicit E: Effect[F]): IO[AccountRepository[F]] = generate[IO].map { storage =>
    AccountRepository(storage)
  }

  def apply[F[_]](storage: CMap[FUUID, Account])(implicit E: Effect[F]): AccountRepository[F] = {
    new AccountRepository[F](storage)
  }

  private def generate[F[_]](implicit E: Effect[F]) = for {
    id1 <- FUUID.randomFUUID
    id2 <- FUUID.randomFUUID
    account1 = Account(id1, "John Doe", BigDecimal(100.00))
    account2 = Account(id2, "Jane Doe", BigDecimal(50.00))
  } yield TrieMap(id1 -> account1, id2 -> account2)
}