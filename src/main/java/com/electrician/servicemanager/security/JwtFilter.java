package com.electrician.servicemanager.security;

import com.electrician.servicemanager.entity.User;
import com.electrician.servicemanager.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        String header = req.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtUtil.isValid(token)) {
                Long userId = jwtUtil.getUserId(token);
                String role = jwtUtil.getRole(token);

                User user = userRepository.findById(userId).orElse(null);

                if (user != null && user.getIsActive()) {
                    // User ko request mein set karo
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                            user, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    // Request attribute mein bhi daalo — controllers use kar sakein
                    req.setAttribute("currentUser", user);
                }
            }
        }

        chain.doFilter(req, res);
    }
}
