package org.odanc.moneytransfer.repository

import cats.effect.Effect
import cats.implicits._
import io.chrisdavenport.fuuid.FUUID
import org.odanc.moneytransfer.models.Account

import scala.collection.concurrent.{TrieMap, Map => CMap}

/**
  * A repository for performing manipulations with accounts storage: retrieving and inserting new accounts
  *
  * @param storage an actual storage for holding accounts
  */
class AccountRepository[F[_]] private(private val storage: CMap[FUUID, Account])(implicit E: Effect[F]) {

  /**
    * Inserts a given account in the storage
    *
    * @param account account
    * @return inserted account on success
    */
  def addAccount(account: Account): F[Option[Account]] = E.delay {
    storage.put(account.id, account)
  }

  /**
    * Inserts given accounts in the storage at the time
    *
    * @param first account
    * @param second account
    * @return no need to return data, works as a side-effect
    */
  def addAccounts(first: Account, second: Account): F[Unit] = E.delay {
    storage += (first.id -> first, second.id -> second)
  }

  /**
    * Retrives an account by given id from the storage
    *
    * @param id account's id
    * @return account if it does exist
    */
  def getAccount(id: FUUID): F[Option[Account]] = E.delay {
    storage.get(id)
  }

  /**
    * Retrieves all accounts from the storage
    *
    * @return all existing accounts
    */
  def getAccounts: F[Iterable[Account]] = E.delay {
    storage.values
  }
}



object AccountRepository {

  /**
    * Creates an accounts repository containing initial set of accounts
    *
    * @return an accounts repository
    */
  def init[F[_]](implicit E: Effect[F]): F[AccountRepository[F]] =
    initialStorage[F] map { storage =>
      AccountRepository(storage)
    }

  /**
    * Creates a new repository
    *
    * @param storage an actual storage for holding accounts
    * @return an accounts repository
    */
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