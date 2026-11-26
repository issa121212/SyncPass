package com.rojas.syncpass.di

/** Conserva únicamente las dependencias de la cuenta activa. */
internal class AccountScopeRegistry<T>(private val create: (String) -> T) {
    private var active: Pair<String, T>? = null

    @Synchronized
    fun get(key: String): T {
        require(key.isNotBlank())
        return active?.takeIf { it.first == key }?.second
            ?: create(key).also { active = key to it }
    }

    @Synchronized
    fun clear() {
        active = null
    }
}
