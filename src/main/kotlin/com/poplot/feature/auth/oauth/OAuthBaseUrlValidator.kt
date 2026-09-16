package com.poplot.feature.auth.oauth

import java.net.URI

internal fun requireHttpsBaseUrl(baseUrl: String): String {
    val uri = try {
        URI.create(baseUrl)
    } catch (exception: IllegalArgumentException) {
        throw IllegalArgumentException("OAuth base URL must be a valid HTTPS URL.", exception)
    }
    require(uri.scheme.equals(HTTPS_SCHEME, ignoreCase = true) && !uri.host.isNullOrBlank()) {
        "OAuth base URL must be a valid HTTPS URL."
    }
    return baseUrl
}

private const val HTTPS_SCHEME = "https"
