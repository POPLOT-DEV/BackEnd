package com.poplot.global.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class RequestTraceFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = request.getHeader(TRACE_ID_HEADER)
            ?.takeUnless(String::isBlank)
            ?: UUID.randomUUID().toString()

        MDC.put(TRACE_ID, traceId)
        response.setHeader(TRACE_ID_HEADER, traceId)
        val startTime = System.currentTimeMillis()

        try {
            filterChain.doFilter(request, response)
        } finally {
            log.info(
                "{} {} - {} ({}ms)",
                request.method,
                request.requestURI,
                response.status,
                System.currentTimeMillis() - startTime,
            )
            MDC.remove(TRACE_ID)
        }
    }

    private companion object {
        const val TRACE_ID = "traceId"
        const val TRACE_ID_HEADER = "X-Request-Id"
        val log = LoggerFactory.getLogger(RequestTraceFilter::class.java)
    }
}
