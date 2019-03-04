package org.odanc.moneytransfer.models

import io.chrisdavenport.fuuid.FUUID
import io.chrisdavenport.fuuid.circe._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._

case class Error(message: String)

object NotFoundError {
  def apply(id: FUUID): Json = Error(s"Account with id $id doesn't exist").asJson
}

object NegativeAmountError {
  def apply(): Json = Error("Can't create account with negative amount").asJson
}

object NonPositiveAmountError {
  def apply(): Json = Error("Amount can't be zero or negative").asJson
}

object SameAccountError {
  def apply(): Json = Error("Transaction to the same account is not allowed").asJson
}