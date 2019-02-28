package org.odanc.moneytransfer.models

import io.chrisdavenport.fuuid.FUUID

case class AccountTemplate(name: String, amount: String)
case class Account(id: FUUID, name: String, amount: BigDecimal)