package com.recoverai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * Temporary tunnel safeguard. Cloudflare adds CF-Connecting-IP to tunneled requests;
 * direct localhost development requests remain available without exposing them publicly.
 */
@Component
@ConditionalOnProperty(prefix = "recoverai", name = "public-webhook-only", havingValue = "true")
public class WebhookOnlyAccessFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        boolean fromCloudflare = request.getHeader("CF-Connecting-IP") != null;
        boolean webhook = "POST".equalsIgnoreCase(request.getMethod()) && "/api/webhooks/razorpay".equals(request.getRequestURI());
        if (fromCloudflare && !webhook) { response.sendError(HttpServletResponse.SC_FORBIDDEN, "This temporary public listener accepts Razorpay webhooks only."); return; }
        chain.doFilter(request, response);
    }
}
