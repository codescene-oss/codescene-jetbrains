package com.codescene.jetbrains.core.contracts

interface INotificationService {
    fun showInfo(message: String)

    fun showError(message: String)
}
