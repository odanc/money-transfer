package org.odanc.moneytransfer.services

import cats.effect.Effect
import cats.implicits._
import io.chrisdavenport.fuuid.FUUID
import org.odanc.moneytransfer.models.{Account, AccountTemplate}
import org.odanc.moneytransfer.repository.AccountRepository

/**
  * A service for manipulating accounts, e.g. creating new ones, retrieving single or all of them from the repository
  *
  * @param repository an account repository
  */
class AccountService[F[_]] private(private val repository: AccountRepository[F])(implicit E: Effect[F]) {

  /**
    * Creates and stores a new account crafted from the given template in the repository
    *
    * @param template an account template
    * @return created account on success
    */
  def addAccount(template: AccountTemplate): F[Account] = for {
    id <- FUUID.randomFUUID
    newAccount <- createAccount(id, template)
    _ <- repository.addAccount(newAccount)
  } yield newAccount

  /**
    * Stores a given account in the repository
    *
    * @param account account
    * @return stored account on success
    */
  def addAccount(account: Account): F[Option[Account]] = repository.addAccount(account)

  /**
    * Stores a given couple of accounts in the repository
    *
    * @param first first account
    * @param second second account
    * @return no need to return data, works as a side-effect
    */
  def addAccounts(first: Account, second: Account): F[Unit] = repository.addAccounts(first, second)

  /**
    * Retrives an account by given id from the repository
    *
    * @param id account's id
    * @return account if it does exist
    */
  def getAccount(id: FUUID): F[Option[Account]] = repository.getAccount(id)

  /**
    * Retrieves all accounts from the repository
    *
    * @return all existing accounts
    */
  def getAccounts: F[Iterable[Account]] = repository.getAccounts

  private def createAccount(id: FUUID, template: AccountTemplate) =
    E.pure(Account(id, template.name, template.amount))
}



object AccountService {

  /**
    * Creates a new account service
    *
    * @return account service
    */
  def apply[F[_]](implicit E: Effect[F]): F[AccountService[F]] =
    AccountRepository.init[F] map { repository =>
      new AccountService[F](repository)
    }
}