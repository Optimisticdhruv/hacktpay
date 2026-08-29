package com.recoverai.config;

import com.google.firebase.auth.FirebaseAuth;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Optional production guard for merchant APIs. Razorpay's signed webhook remains independently authenticated. */
@Component
@ConditionalOnProperty(prefix = "recoverai.security", name = "auth-enabled", havingValue = "true")
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {
    private final FirebaseAuth firebaseAuth;

    public FirebaseAuthenticationFilter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || "/api/webhooks/razorpay".equals(request.getRequestURI())
                || "/actuator/health".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Merchant authentication is required.");
            return;
        }
        try {
            firebaseAuth.verifyIdToken(authorization.substring(7));
            chain.doFilter(request, response);
        } catch (Exception ignored) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid merchant authentication token.");
        }
    }
}
