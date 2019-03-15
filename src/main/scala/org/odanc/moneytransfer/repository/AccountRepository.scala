package org.odanc.moneytransfer.repository

import cats.effect.Effect
import cats.implicits._
import io.chrisdavenport.fuuid.FUUID
import org.odanc.moneytransfer.models.Account

import scala.collection.concurrent.{TrieMap, Map => CMap}

class AccountRepository[F[_]] private(private val storage: CMap[FUUID, Account])(implicit E: Effect[F]) {

  def addAccount(account: Account): F[Option[Account]] = E.delay {
    storage.put(account.id, account)
  }

  def addAccounts(first: Account, second: Account): F[Unit] = E.delay {
    storage += (first.id -> first, second.id -> second)
  }

  def getAccount(id: FUUID): F[Option[Account]] = E.delay {
    storage.get(id)
  }

  def getAccounts: F[Iterable[Account]] = E.delay {
    storage.values
  }
}



object AccountRepository {

  def init[F[_]](implicit E: Effect[F]): F[AccountRepository[F]] =
    initialStorage[F] map { storage =>
      AccountRepository(storage)
    }

  def apply[F[_]](storage: CMap[FUUID, Account])(implicit E: Effect[F]): AccountRepository[F] =
    new AccountRepository[F](storage)

  private def initialStorage[F[_]](implicit E: Effect[F]) = for {
    id1 <- FUUID.randomFUUID
    id2 <- FUUID.randomFUUID
    account1 <- createAccount(id1, "John Doe", BigDecimal("100.00"))
    account2 <- createAccount(id2, "Jane Doe", BigDecimal("50.00"))
  } yield TrieMap(id1 -> account1, id2 -> account2)

  private def createAccount[F[_]](id: FUUID, name: String, amount: BigDecimal)(implicit E: Effect[F]) =
    E.pure(Account(id, name, amount))
}