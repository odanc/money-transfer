package org.odanc.moneytransfer.services

import cats.effect.Effect
import cats.implicits._
import io.chrisdavenport.fuuid.FUUID
import org.odanc.moneytransfer.models.{Account, AccountTemplate}
import org.odanc.moneytransfer.repository.AccountRepository

class AccountService[F[_]] private(private val repository: AccountRepository[F])(implicit E: Effect[F]) {

  def addAccount(template: AccountTemplate): F[Account] = for {
    id <- FUUID.randomFUUID
    newAccount <- createAccount(id, template)
    _ <- repository.addAccount(newAccount)
  } yield newAccount

  def addAccount(account: Account): F[Option[Account]] = repository.addAccount(account)

  def getAccount(id: FUUID): F[Option[Account]] = repository.getAccount(id)

  def getAccounts: F[Iterable[Account]] = repository.getAccounts

  def updateAccount(oldAccount: Account, newAccount: Account): F[Unit] =
    repository.replaceAccount(oldAccount, newAccount)

  private def createAccount(id: FUUID, template: AccountTemplate) =
    E.pure(Account(id, template.name, template.amount))
}



object AccountService {

  def apply[F[_]](implicit E: Effect[F]): F[AccountService[F]] =
    AccountRepository.init[F] map { repository =>
      new AccountService[F](repository)
    }
}