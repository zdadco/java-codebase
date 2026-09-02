package ru.zdadco.indexer.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.zdadco.indexer.core.config.IndexerProperties;

import java.io.IOException;
import java.util.List;

public class BearerTokenFilter extends OncePerRequestFilter {

    private final IndexerProperties properties;

    public BearerTokenFilter(IndexerProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }
        String expected = properties.getApiToken();
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (expected != null && !expected.isBlank() && header != null && header.equals("Bearer " + expected)) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            "indexer",
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_INDEXER"))
                    )
            );
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
