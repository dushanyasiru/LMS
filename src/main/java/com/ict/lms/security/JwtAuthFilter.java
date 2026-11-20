package com.ict.lms.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ict.lms.model.AppUser;
import com.ict.lms.repo.AppUserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Runs on every request: if there's a valid "Authorization: Bearer <token>"
 * header, it marks the request as authenticated.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserRepository users;

    public JwtAuthFilter(JwtService jwtService, AppUserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                AuthUser tokenUser = jwtService.parse(header.substring(7));
                // The token only proves who the user is (via uid); re-read role/grade/name
                // from the database so a teacher editing a student's grade (or role) takes
                // effect on that student's very next request, instead of waiting up to
                // app.jwt.expiration-hours for the old token to expire.
                AppUser dbUser = users.findById(tokenUser.id()).orElse(null);
                if (dbUser != null) {
                    AuthUser user = new AuthUser(dbUser.getId(), dbUser.getEmail(), dbUser.getRole(),
                            dbUser.getGrade(), dbUser.getFullName());
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()));
                    var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) {
                // Invalid or expired token -> request stays unauthenticated.
            }
        }
        chain.doFilter(request, response);
    }
}
