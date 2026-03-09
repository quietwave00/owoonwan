package org.example.owoonwan.auth.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.owoonwan.auth.AuthAttributes;
import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.auth.service.AuthService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class SessionAuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorizationHeader = request.getHeader("Authorization");
        AuthenticatedUser authenticatedUser = authService.authenticate(authorizationHeader);
        request.setAttribute(AuthAttributes.AUTHENTICATED_USER, authenticatedUser);
        return true;
    }
}
