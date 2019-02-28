package org.odanc.moneytransfer

import cats.effect.IO
import cats.implicits._
import fs2.{Stream, StreamApp}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder
import org.odanc.moneytransfer.repository.AccountRepository
import org.odanc.moneytransfer.services.AccountService

import scala.concurrent.ExecutionContext.Implicits.global

object Server extends StreamApp[IO] with Http4sDsl[IO] {

  def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, StreamApp.ExitCode] =
    Stream.eval(AccountRepository.init[IO]) flatMap { repository =>
      BlazeBuilder[IO]
        .mountService(AccountService(repository))
        .serve
    }
}