package org.odanc.moneytransfer.models

case class AccountTemplate(name: String, amount: BigDecimal)
case class Account(id: String, name: String, amount: BigDecimal)