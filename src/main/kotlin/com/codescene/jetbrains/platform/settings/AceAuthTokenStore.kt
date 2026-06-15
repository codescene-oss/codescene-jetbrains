package com.codescene.jetbrains.platform.settings

import com.codescene.jetbrains.core.util.isAceTokenConfigured
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe

object AceAuthTokenStore {
    private val credentialAttributes =
        CredentialAttributes(generateServiceName("com.codescene.vanilla", "ace-auth-token"))

    fun getToken(): String = PasswordSafe.instance.getPassword(credentialAttributes).orEmpty()

    fun setToken(token: String) {
        if (token.isBlank()) {
            PasswordSafe.instance.set(credentialAttributes, null)
        } else {
            PasswordSafe.instance.set(credentialAttributes, Credentials("ace", token))
        }
    }

    fun hasToken(): Boolean = isAceTokenConfigured(getToken())
}
