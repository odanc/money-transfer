package org.odanc.moneytransfer.models

import io.chrisdavenport.fuuid.FUUID

case class AccountTemplate(name: String, amount: BigDecimal)
case class Account(id: FUUID, name: String, amount: BigDecimal)
case class Transaction(from: FUUID, to: FUUID, amount: BigDecimal)