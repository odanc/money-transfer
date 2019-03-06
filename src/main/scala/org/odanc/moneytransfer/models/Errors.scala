package org.odanc.moneytransfer.models

import io.chrisdavenport.fuuid.FUUID
import io.chrisdavenport.fuuid.circe._

trait Error

case class NotFoundError(id: FUUID) extends Error
case class NotEnoughAmountError(id: FUUID) extends Error
case object NonPositiveAmountError extends Error
case object SameAccountError extends Error

case class ErrorMessage(message: String)