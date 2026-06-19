package com.abccash.app.treasury.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.abccash.app.treasury.data.ExpenseRecurrence
import com.abccash.app.treasury.data.PaymentMethod
import com.abccash.app.treasury.data.TransactionType
import com.abccash.app.treasury.data.UserPermission

@Composable
fun PaymentMethod.localizedLabel(): String = stringResource(labelRes)

@Composable
fun ExpenseRecurrence.localizedLabel(): String = stringResource(labelRes)

@Composable
fun TransactionType.localizedTitle(forecast: Boolean = false): String =
    stringResource(if (forecast) forecastTitleRes else titleRes)

@Composable
fun UserPermission.localizedLabel(): String = stringResource(labelRes)
