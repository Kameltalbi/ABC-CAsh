package com.abccash.app.treasury.data

fun PaymentMethod.affectsBankTreasury(): Boolean = this != PaymentMethod.CASH

fun Payment.affectsBankTreasury(): Boolean = method.affectsBankTreasury()

fun Expense.affectsBankTreasury(): Boolean =
    isPaid && (paymentMethod?.affectsBankTreasury() != false)

fun PaymentMethod.affectsCashTreasury(): Boolean = this == PaymentMethod.CASH

fun Payment.affectsCashTreasury(): Boolean = method.affectsCashTreasury()

fun Expense.affectsCashTreasury(): Boolean =
    isPaid && paymentMethod == PaymentMethod.CASH
