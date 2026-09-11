package com.poplot.global.security.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        resolveToken(request)?.let(::authenticate)
        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val authorizationHeader = request.getHeader(AUTHORIZATION_HEADER)
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?: return null

        return authorizationHeader.removePrefix(BEARER_PREFIX).trim().ifBlank { null }
    }

    private fun authenticate(token: String) {
        if (!jwtProvider.validateToken(token) || !jwtProvider.isAccessToken(token)) return

        try {
            val authentication = UsernamePasswordAuthenticationToken(
                jwtProvider.getUserId(token),
                null,
                emptyList(),
            )
            SecurityContextHolder.getContext().authentication = authentication
        } catch (exception: RuntimeException) {
            log.debug("JWT authentication failed", exception)
            SecurityContextHolder.clearContext()
        }
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
        val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
    }
}
