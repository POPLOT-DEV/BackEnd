package com.poplot.global.security.jwt

import com.poplot.feature.auth.exception.AuthErrorCode
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
        resolveToken(request)?.let { authenticate(it, request) }
        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val authorizationHeader = request.getHeader(AUTHORIZATION_HEADER) ?: return null
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            request.setAttribute(JWT_AUTH_ERROR_ATTRIBUTE, AuthErrorCode.INVALID_ACCESS_TOKEN)
            return null
        }

        return authorizationHeader.removePrefix(BEARER_PREFIX).trim()
            .ifBlank {
                request.setAttribute(JWT_AUTH_ERROR_ATTRIBUTE, AuthErrorCode.INVALID_ACCESS_TOKEN)
                null
            }
    }

    private fun authenticate(
        token: String,
        request: HttpServletRequest,
    ) {
        try {
            if (!jwtProvider.isAccessToken(token)) {
                request.setAttribute(JWT_AUTH_ERROR_ATTRIBUTE, AuthErrorCode.INVALID_ACCESS_TOKEN)
                return
            }
            val authentication = UsernamePasswordAuthenticationToken(
                jwtProvider.getUserId(token),
                null,
                emptyList(),
            )
            SecurityContextHolder.getContext().authentication = authentication
        } catch (exception: ExpiredJwtException) {
            request.setAttribute(JWT_AUTH_ERROR_ATTRIBUTE, AuthErrorCode.EXPIRED_ACCESS_TOKEN)
            SecurityContextHolder.clearContext()
        } catch (exception: JwtException) {
            request.setAttribute(JWT_AUTH_ERROR_ATTRIBUTE, AuthErrorCode.INVALID_ACCESS_TOKEN)
            SecurityContextHolder.clearContext()
        } catch (exception: IllegalArgumentException) {
            request.setAttribute(JWT_AUTH_ERROR_ATTRIBUTE, AuthErrorCode.INVALID_ACCESS_TOKEN)
            SecurityContextHolder.clearContext()
        }
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
