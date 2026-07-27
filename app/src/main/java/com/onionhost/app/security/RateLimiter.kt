package com.onionhost.app.security

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class RateLimiter(
    private val maxRequestsPerMinute: Int = 120
) {
    private val requestCounts = ConcurrentHashMap<String, AtomicInteger>()
    private var lastResetTime = System.currentTimeMillis()

    @Synchronized
    fun isAllowed(clientIdentifier: String = "anon"): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastResetTime > 60_000) {
            requestCounts.clear()
            lastResetTime = now
        }

        val counter = requestCounts.getOrPut(clientIdentifier) { AtomicInteger(0) }
        return counter.incrementAndGet() <= maxRequestsPerMinute
    }
}
