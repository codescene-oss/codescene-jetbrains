package com.codescene.jetbrains.core.testdoubles

import com.codescene.jetbrains.core.contracts.INotificationService

class NoOpNotificationService : INotificationService {
    override fun showInfo(message: String) {
    }

    override fun showError(message: String) {
    }
}
