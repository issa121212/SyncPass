package com.rojas.syncpass.data.remote

/** OkHttp interceptors must use IOException for an interrupted authenticated request. */
internal class SessionInvalidIOException : java.io.IOException("Sesion no disponible")
