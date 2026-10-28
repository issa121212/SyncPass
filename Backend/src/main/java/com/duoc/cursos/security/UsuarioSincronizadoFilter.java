package com.duoc.cursos.security;

import com.duoc.cursos.service.SesionUsuarioService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.io.IOException;

/** Runs after JWT verification and before controllers open read-only business transactions. */
public class UsuarioSincronizadoFilter extends OncePerRequestFilter {
    private final SesionUsuarioService sessions;
    private final HandlerExceptionResolver errors;
    public UsuarioSincronizadoFilter(SesionUsuarioService sessions, HandlerExceptionResolver errors) {
        this.sessions = sessions; this.errors = errors;
    }
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            try { sessions.sincronizar(token.getToken()); }
            catch (Exception ex) { errors.resolveException(req, res, null, ex); return; }
        }
        chain.doFilter(req, res);
    }
}
