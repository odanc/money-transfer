package org.odanc.moneytransfer

import cats.effect.IO
import cats.implicits._
import fs2.{Stream, StreamApp}
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeBuilder
import org.odanc.moneytransfer.api.AccountApi
import org.odanc.moneytransfer.services.AccountService

import scala.concurrent.ExecutionContext.Implicits.global

object Server extends StreamApp[IO] with Http4sDsl[IO] {

  def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, StreamApp.ExitCode] =
    Stream.eval(AccountService[IO]) flatMap { accountService =>
      // error highlighting in IDEA due to scala plugin bug
      val services = AccountApi(accountService)
      BlazeBuilder[IO]
        .mountService(services, "/api")
        .serve
    }
}