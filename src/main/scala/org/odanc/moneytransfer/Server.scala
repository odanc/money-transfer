package org.odanc.moneytransfer

import cats.effect.IO
import cats.implicits._
import fs2.{Stream, StreamApp}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder
import org.odanc.moneytransfer.api.{AccountApi, TransactionApi}
import org.odanc.moneytransfer.services.{AccountService, TransactionService}

import scala.concurrent.ExecutionContext.Implicits.global

object Server extends StreamApp[IO] with Http4sDsl[IO] {

  def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, StreamApp.ExitCode] =
    Stream.eval(AccountService[IO]) flatMap { accountService =>
      val transactionService = TransactionService(accountService)

      // error highlighting in IDEA due to scala plugin bug
      val services = AccountApi(accountService) <+> TransactionApi(transactionService)
      BlazeBuilder[IO]
        .mountService(services, "/api")
        .serve
    }
}