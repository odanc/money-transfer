package org.odanc.moneytransfer

import cats.effect.IO
import cats.implicits._
import fs2.{Stream, StreamApp}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder
import org.odanc.moneytransfer.repository.AccountRepository
import org.odanc.moneytransfer.api.{AccountApi, TransactionApi}
import org.http4s.implicits._

import scala.concurrent.ExecutionContext.Implicits.global

object Server extends StreamApp[IO] with Http4sDsl[IO] {

  def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, StreamApp.ExitCode] =
    Stream.eval(AccountRepository.init[IO]) flatMap { repository =>
      // error highlighting in IDEA due to scala plugin bug
      val services = AccountApi(repository) <+> TransactionApi(repository)
      BlazeBuilder[IO]
        .mountService(services, "/api")
        .serve
    }
}